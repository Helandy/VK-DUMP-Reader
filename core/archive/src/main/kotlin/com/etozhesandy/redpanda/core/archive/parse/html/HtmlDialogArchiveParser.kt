package com.etozhesandy.redpanda.core.archive.parse.html

import com.etozhesandy.redpanda.core.archive.parse.ChatArchiveParser
import com.etozhesandy.redpanda.core.archive.parse.ParseSink
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.AttachmentType
import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.DialogKind
import com.etozhesandy.redpanda.core.model.Message
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/**
 * Reads any of the HTML export layouts, all of which share the shape
 * `{диалоги}/{категория}/{контакт}/{страница}.html`, and delegate everything that varies between
 * dumper tools to an [HtmlDialect].
 *
 * A single contact's history can run into the hundreds of thousands of messages, so batches are
 * streamed to [ParseSink] rather than built into one in-memory list ([BATCH_SIZE]) — holding a whole
 * archive at once was enough to OOM on a real export during testing.
 */
class HtmlDialogArchiveParser(
    private val dialect: HtmlDialect,
    private val ioDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
) : ChatArchiveParser {

    override suspend fun parse(contentRoot: File, profileId: String, sink: ParseSink) =
        withContext(defaultDispatcher) {
            val dialogsRoot = dialect.dialogsRoot(contentRoot)
            val ownerNameCounts = ConcurrentHashMap<String, Int>()

            val contactDirs = dialogsRoot.listFiles { file -> file.isDirectory }.orEmpty()
                .flatMap { categoryDir ->
                    categoryDir.listFiles { file -> file.isDirectory }.orEmpty()
                        .map { contactDir -> categoryDir.name to contactDir }
                }

            sink.onDialogsDiscovered(contactDirs.size)

            // The peer id alone normally identifies a dialog, but the same contact can appear under
            // two category folders, which would collide into one dialog. Qualifying every id with
            // its category instead would change all existing ids, so only the colliding ones are.
            val ambiguousPeerIds = contactDirs
                .mapNotNull { (_, contactDir) -> dialect.contactFolder(contactDir)?.peerId }
                .groupingBy { it }
                .eachCount()
                .filterValues { it > 1 }
                .keys

            val semaphore = Semaphore(PARALLELISM)
            coroutineScope {
                contactDirs.map { (category, contactDir) ->
                    async(ioDispatcher) {
                        semaphore.withPermit {
                            parseContact(contactDir, category, profileId, sink, ownerNameCounts, ambiguousPeerIds)
                        }
                    }
                }.awaitAll()
            }

            val displayName = dialect.ownerName(contentRoot)
                ?: ownerNameCounts.maxByOrNull { it.value }?.key
                ?: HtmlParseUtils.parseVkFolderName(contentRoot.name)?.peerName
                ?: contentRoot.name
            sink.onDisplayName(displayName)
        }

    private suspend fun parseContact(
        contactDir: File,
        category: String,
        profileId: String,
        sink: ParseSink,
        ownerNameCounts: ConcurrentHashMap<String, Int>,
        ambiguousPeerIds: Set<String>,
    ) {
        val peer = dialect.contactFolder(contactDir) ?: return
        val dialogId = if (peer.peerId in ambiguousPeerIds) {
            "$profileId:$category/${peer.peerId}"
        } else {
            "$profileId:${peer.peerId}"
        }
        val timestamps = HtmlTimestampParser(dialect.timestampPattern)

        var lastMessageAt = 0L
        var peerAvatarPath: String? = null
        var messageCount = 0
        var sequenceInDialog = 0
        val messageBuffer = ArrayList<Message>(BATCH_SIZE)
        val attachmentBuffer = ArrayList<Attachment>(BATCH_SIZE)
        // Every path already imported from a message, so the flat galleries below can skip the
        // copies of those same items instead of shadowing them with message-less duplicates.
        val inlinePaths = HashSet<String>()

        for (page in dialect.historyPages(contactDir)) {
            val document = Jsoup.parse(page, "UTF-8")
            for (raw in dialect.parsePage(document, peer, timestamps)) {
                // The one place the dialects' incompatible sender models are reconciled.
                val isOutgoing = raw.isOutgoing ?: (raw.senderId != peer.peerId)
                val senderId = raw.senderId ?: if (isOutgoing) OWNER_SENDER_ID else peer.peerId
                val senderName = raw.senderName?.takeIf { it.isNotBlank() }
                    ?: if (isOutgoing) OWNER_SENDER_NAME else peer.peerName

                if (isOutgoing) {
                    raw.senderName?.takeIf { it.isNotBlank() }
                        ?.let { ownerNameCounts.merge(it, 1, Int::plus) }
                } else {
                    raw.peerAvatarPath?.takeIf { it.isNotBlank() }?.let { peerAvatarPath = it }
                }

                val messageId = HtmlParseUtils.deterministicMessageId(
                    dialogId = dialogId,
                    timestamp = raw.timestampEpoch,
                    senderId = senderId,
                    text = raw.text,
                    sequence = sequenceInDialog++,
                )
                messageBuffer += Message(
                    id = messageId,
                    dialogId = dialogId,
                    profileId = profileId,
                    senderId = senderId,
                    senderName = senderName,
                    timestampEpoch = raw.timestampEpoch,
                    text = raw.text,
                    isOutgoing = isOutgoing,
                    isFavorite = false,
                )
                messageCount++
                if (raw.timestampEpoch > lastMessageAt) lastMessageAt = raw.timestampEpoch

                raw.attachments.forEachIndexed { index, attachment ->
                    if (attachment.path.isNotBlank()) inlinePaths += attachment.path
                    attachmentBuffer += Attachment(
                        id = "$messageId:$index",
                        messageId = messageId,
                        dialogId = dialogId,
                        profileId = profileId,
                        type = attachment.type,
                        path = attachment.path,
                        orderInMessage = index,
                        timestampEpoch = raw.timestampEpoch,
                        caption = attachment.caption,
                    )
                }

                if (messageBuffer.size >= BATCH_SIZE) {
                    sink.onMessages(ArrayList(messageBuffer))
                    messageBuffer.clear()
                }
                if (attachmentBuffer.size >= BATCH_SIZE) {
                    sink.onAttachments(ArrayList(attachmentBuffer))
                    attachmentBuffer.clear()
                }
            }
        }
        if (messageBuffer.isNotEmpty()) sink.onMessages(messageBuffer)
        if (attachmentBuffer.isNotEmpty()) sink.onAttachments(attachmentBuffer)

        val galleryAttachments = parseGalleries(contactDir, dialogId, profileId, inlinePaths)
        if (galleryAttachments.isNotEmpty()) sink.onAttachments(galleryAttachments)

        sink.onDialog(
            ChatDialog(
                id = dialogId,
                profileId = profileId,
                peerId = peer.peerId,
                peerName = peer.peerName,
                peerAvatarPath = peerAvatarPath,
                kind = DialogKind.PERSON,
                category = category,
                lastMessageAt = lastMessageAt,
                messageCount = messageCount,
            ),
        )
    }

    /**
     * Flat galleries carry no timestamp or sender, so their entries cannot be correlated to a
     * message ([Attachment.messageId] stays null). They also mostly repeat the very URLs the
     * messages already link to, so entries duplicating an inline path are dropped — otherwise the
     * media grid would show the message-less copy and "jump to message" would have nothing to open.
     */
    private fun parseGalleries(
        contactDir: File,
        dialogId: String,
        profileId: String,
        inlinePaths: Set<String>,
    ): List<Attachment> {
        val result = mutableListOf<Attachment>()
        // Indices run across every page of a type, not per file: a paginated gallery would
        // otherwise restart at 0 on each page and collide ids.
        val nextIndex = mutableMapOf<AttachmentType, Int>()
        for (gallery in dialect.galleries(contactDir)) {
            if (!gallery.file.exists()) continue
            val document = Jsoup.parse(gallery.file, "UTF-8")
            for (entry in dialect.parseGallery(document, gallery.type)) {
                // The counter advances for skipped entries too, so an item's id and order reflect
                // its position in the gallery as displayed rather than shifting when a neighbour
                // turns out to be a duplicate of an inline attachment.
                val index = nextIndex.getOrDefault(gallery.type, 0)
                nextIndex[gallery.type] = index + 1
                if (entry.path.isBlank() || entry.path in inlinePaths) continue
                result += Attachment(
                    id = "$dialogId:${gallery.type.name}:gallery:$index",
                    messageId = null,
                    dialogId = dialogId,
                    profileId = profileId,
                    type = entry.type,
                    path = entry.path,
                    orderInMessage = index,
                    // No per-item timestamp exists in the export; sorts first, deterministically,
                    // ahead of any timestamped message attachment.
                    timestampEpoch = 0L,
                    caption = entry.caption,
                )
            }
        }
        return result
    }

    private companion object {
        const val BATCH_SIZE = 2000
        const val PARALLELISM = 4

        /**
         * Stand-ins for the archive owner, used only by dialects whose markup names neither the
         * sender nor its id. Constant on purpose: message ids hash the sender, so anything derived
         * from the archive's location would change them between imports and duplicate every row.
         */
        const val OWNER_SENDER_ID = "owner"
        const val OWNER_SENDER_NAME = "Вы"
    }
}
