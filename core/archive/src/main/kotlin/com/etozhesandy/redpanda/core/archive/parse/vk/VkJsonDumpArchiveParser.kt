package com.etozhesandy.redpanda.core.archive.parse.vk

import com.etozhesandy.redpanda.core.archive.parse.ChatArchiveParser
import com.etozhesandy.redpanda.core.archive.parse.ParseSink
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.dispatcher.IoDispatcher
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.AttachmentType
import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.DialogKind
import com.etozhesandy.redpanda.core.model.Friend
import com.etozhesandy.redpanda.core.model.Group
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.model.ProfileDetails
import com.etozhesandy.redpanda.core.model.Sex
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Parses the exporter that ships a small offline viewer alongside its data: `html/` (the viewer),
 * `txt/` (plain-text friend and group lists), `json/` (everything the viewer reads) and a media
 * folder of the attachments it managed to download.
 *
 * It is JSON like the API dump ([VkApiArchiveParser]) but shares none of its schema. Fields are
 * camelCase and heavily abbreviated (`from`, `time`, `link` instead of `from_id`, `date`,
 * `url`), each dialog is a single `json/dialogs/{peerId}.json` rather than a paged directory, and
 * an attachment is a flat object — its `type` names sibling fields on the attachment itself
 * instead of a nested `{type}` body.
 *
 * Two consequences worth naming:
 * - Quotes and forwards are stored **by message id**, pointing at other messages in the same
 *   file, so the file is indexed before it is walked (see [parseDialog]).
 * - Downloaded media is filed as `{ownerId}_{attachmentId}.{ext}`, with no reference from the
 *   attachment back to it, so [indexLocalMedia] rebuilds that link by name. It matters twice over:
 *   the CDN links beside it are signed and expire, and without the link the same file would import
 *   both as a remote attachment on its message and as an unattached orphan the media scan finds on
 *   disk. A file claimed this way keeps its [Attachment.sourceFolder], so it still appears among
 *   the archive's own media instead of only inside the dialog.
 */
class VkJsonDumpArchiveParser @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ChatArchiveParser {

    private val json = Json { ignoreUnknownKeys = true; allowTrailingComma = true }

    override suspend fun parse(contentRoot: File, profileId: String, sink: ParseSink) =
        withContext(defaultDispatcher) {
            val jsonDir = VkJsonDumpPaths.jsonRoot(contentRoot) ?: return@withContext
            val dialogsDir = VkJsonDumpPaths.dialogsDirIn(jsonDir) ?: return@withContext
            val ownerInfo = VkJsonDumpPaths.profileFile(jsonDir)?.let { readJsonObject(it) }
            val ownerId = ownerInfo?.str("id")
            val displayName = ownerInfo?.let(::personName) ?: contentRoot.name

            ownerInfo?.let { sink.onProfileDetails(parseProfileDetails(it)) }
            parseFriends(jsonDir, profileId)?.let { sink.onFriends(it) }
            parseGroups(jsonDir, profileId)?.let { sink.onGroups(it) }

            val companions = readCompanions(jsonDir, dialogsDir)
            val localMedia = withContext(ioDispatcher) { indexLocalMedia(contentRoot) }

            // Filtered by name, not by extension: the Russian build keeps `companions.json` inside
            // the dialogs directory, and counting it as a dialog invented an empty conversation
            // named `companions` and reported one dialog more than the export holds.
            val dialogFiles = VkJsonDumpPaths.dialogFiles(dialogsDir)
            sink.onDialogsDiscovered(dialogFiles.size)

            val semaphore = Semaphore(PARALLELISM)
            coroutineScope {
                dialogFiles.map { dialogFile ->
                    async(ioDispatcher) {
                        semaphore.withPermit {
                            parseDialog(
                                dialogFile = dialogFile,
                                companion = companions[dialogFile.nameWithoutExtension],
                                ownerId = ownerId,
                                profileId = profileId,
                                contentRoot = contentRoot,
                                localMedia = localMedia,
                                sink = sink,
                            )
                        }
                    }
                }.awaitAll()
            }

            sink.onDisplayName(displayName)
        }

    private suspend fun parseDialog(
        dialogFile: File,
        companion: JsonObject?,
        ownerId: String?,
        profileId: String,
        contentRoot: File,
        localMedia: Map<String, File>,
        sink: ParseSink,
    ) {
        val peerId = dialogFile.nameWithoutExtension
        val dialogId = "$profileId:$peerId"

        val dialogJson = readJsonObject(dialogFile) ?: return
        val messages = dialogJson.arr("messages").orEmpty()

        // The peer's own entry is usually in `companions.json`; everyone else who appears in the
        // dialog — including the authors of forwarded messages — only in the file's own `profiles`.
        val nameById = buildMap {
            dialogJson.arr("profiles")?.forEach { element ->
                val entity = element.asObject() ?: return@forEach
                val id = entity.str("id") ?: return@forEach
                personName(entity)?.let { put(id, it) }
            }
            companion?.let { entity -> personName(entity)?.let { put(peerId, it) } }
        }
        val peerName = nameById[peerId] ?: peerId
        val peerAvatarPath = companion?.str("photo")
            ?: dialogJson.arr("profiles")
                ?.mapNotNull { it.asObject() }
                ?.firstOrNull { it.str("id") == peerId }
                ?.str("photo")

        // Quotes and forwards reference messages of this same file by id, so the whole file is
        // indexed up front — a reply can point at a message that comes later in the array.
        val messagesById = buildMap {
            messages.forEach { element ->
                val message = element.asObject() ?: return@forEach
                message.str("id")?.let { put(it, message) }
            }
        }

        var lastMessageAt = 0L
        var messageCount = 0
        val distinctSenders = mutableSetOf<String>()
        val messageBuffer = ArrayList<Message>(BATCH_SIZE)
        val attachmentBuffer = ArrayList<Attachment>(BATCH_SIZE)

        for (element in messages) {
            val message = element.asObject() ?: continue
            val localId = message.str("id") ?: continue
            val fromId = message.str("from").orEmpty()
            val timestamp = (message.long("time") ?: 0L) * 1000
            distinctSenders += fromId

            val messageId = "$dialogId:$localId"
            messageBuffer += Message(
                id = messageId,
                dialogId = dialogId,
                profileId = profileId,
                senderId = fromId,
                senderName = resolveSenderName(fromId, ownerId, peerId, peerName, nameById),
                timestampEpoch = timestamp,
                text = extractMessageText(message, messagesById, nameById, ownerId, peerId, peerName),
                isOutgoing = fromId == ownerId,
                isFavorite = false,
            )
            messageCount++
            if (timestamp > lastMessageAt) lastMessageAt = timestamp

            collectAttachments(message).take(MAX_ATTACHMENTS_PER_MESSAGE)
                .forEachIndexed { index, attachment ->
                    resolveAttachment(attachment, contentRoot, localMedia)?.let { resolved ->
                        attachmentBuffer += Attachment(
                            id = "$messageId:$index",
                            messageId = messageId,
                            dialogId = dialogId,
                            profileId = profileId,
                            type = resolved.type,
                            path = resolved.path,
                            orderInMessage = index,
                            timestampEpoch = timestamp,
                            caption = resolved.caption,
                            sourceFolder = resolved.sourceFolder,
                        )
                    }
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
        if (messageBuffer.isNotEmpty()) sink.onMessages(messageBuffer)
        if (attachmentBuffer.isNotEmpty()) sink.onAttachments(attachmentBuffer)

        val kind = if ((peerId.toLongOrNull() ?: 0L) >= GROUP_PEER_ID_THRESHOLD || distinctSenders.size > 2) {
            DialogKind.GROUP
        } else {
            DialogKind.PERSON
        }

        sink.onDialog(
            ChatDialog(
                id = dialogId,
                profileId = profileId,
                peerId = peerId,
                peerName = peerName,
                peerAvatarPath = peerAvatarPath,
                kind = kind,
                category = null,
                lastMessageAt = lastMessageAt,
                messageCount = messageCount,
            ),
        )
    }

    /**
     * Own text, plus the message it quotes and every message it forwards — a forward of a forward
     * included, which real dialogs do contain.
     *
     * A `reply` is always an id; a `fwd` entry is either an id — a message stored in full elsewhere
     * in this same file — or an inline object for one that is not, typically because it came from a
     * dialog this dump doesn't include.
     */
    private fun extractMessageText(
        message: JsonObject,
        messagesById: Map<String, JsonObject>,
        nameById: Map<String, String>,
        ownerId: String?,
        peerId: String,
        peerName: String,
        depth: Int = 0,
    ): String {
        val own = message.str("text").orEmpty()
        val replyText = message.str("reply")
            ?.let { messagesById[it] }
            ?.str("text")
            ?.let { "> $it" }
        val forwardTexts = if (depth >= MAX_FORWARD_DEPTH) {
            emptyList()
        } else {
            message.arr("fwd")?.mapNotNull { element ->
                val forwarded = resolveReferenced(element, messagesById) ?: return@mapNotNull null
                val text = extractMessageText(
                    message = forwarded,
                    messagesById = messagesById,
                    nameById = nameById,
                    ownerId = ownerId,
                    peerId = peerId,
                    peerName = peerName,
                    depth = depth + 1,
                ).ifBlank { null } ?: return@mapNotNull null
                val sender = resolveSenderName(
                    fromId = forwarded.str("from").orEmpty(),
                    ownerId = ownerId,
                    peerId = peerId,
                    peerName = peerName,
                    nameById = nameById,
                )
                "[Пересланное от $sender]: $text"
            }.orEmpty()
        }
        return listOfNotNull(own.ifBlank { null }, replyText, *forwardTexts.toTypedArray()).joinToString("\n")
    }

    /**
     * Attachments to store against this message: its own, plus those of every message forwarded
     * into it as an inline object, however deeply those nest.
     *
     * Forwards and quotes given **by id** are deliberately skipped: that message is in the file in
     * its own right and carries these same attachments there, so following the reference would
     * import every quoted photo a second time.
     */
    private fun collectAttachments(message: JsonObject, depth: Int = 0): List<JsonObject> {
        val own = message.arr("attachments")?.mapNotNull { it.asObject() }.orEmpty()
        if (depth >= MAX_FORWARD_DEPTH) return own
        val inlineForwarded = message.arr("fwd")
            ?.mapNotNull { it.asObject() }
            ?.flatMap { forwarded -> collectAttachments(forwarded, depth + 1) }
            .orEmpty()
        // Own attachments stay first so their indices — and the attachment ids built from them —
        // are unchanged for messages that carry no forwards.
        return own + inlineForwarded
    }

    /** A `fwd` entry: the message it names, or the inline message it already is. */
    private fun resolveReferenced(element: JsonElement, messagesById: Map<String, JsonObject>): JsonObject? =
        element.asObject() ?: element.asId()?.let { messagesById[it] }

    private fun resolveSenderName(
        fromId: String,
        ownerId: String?,
        peerId: String,
        peerName: String,
        nameById: Map<String, String>,
    ): String = when {
        fromId == ownerId -> "Вы"
        nameById.containsKey(fromId) -> nameById.getValue(fromId)
        fromId == peerId -> peerName
        else -> fromId
    }

    /**
     * Turns one attachment into something storable, preferring a copy this dump actually
     * downloaded over the CDN link beside it — those links are signed and expire, so the local file
     * is the only one still openable months later.
     *
     * Like the API-dump parser, an unrecognised kind becomes [AttachmentType.OTHER] rather than
     * being dropped.
     */
    private fun resolveAttachment(
        attachment: JsonObject,
        contentRoot: File,
        localMedia: Map<String, File>,
    ): ResolvedAttachment? {
        val rawType = attachment.str("type") ?: return null
        val localFile = localMedia[mediaKey(attachment)]
        val local = localFile?.absolutePath
        return resolveByType(attachment, local)?.let { resolved ->
            // Only a resolution that actually took the local copy is filed under the archive folder
            // it came from — a kind that keeps its remote link has no file in the dump to list.
            if (localFile != null && resolved.path == local) {
                resolved.copy(sourceFolder = archiveFolder(localFile, contentRoot))
            } else {
                resolved
            }
        }
    }

    private fun resolveByType(attachment: JsonObject, local: String?): ResolvedAttachment? {
        val rawType = attachment.str("type") ?: return null
        return when (normalizeAttachmentType(rawType)) {
            "photo" -> ResolvedAttachment(
                type = AttachmentType.PHOTO,
                path = local ?: attachment.str("hd", "preview", "link").orEmpty(),
            )

            // `video` is a signed stream URL that stops working; the permalink outlives it, so it
            // is what a video with no downloaded copy falls back to.
            "video" -> ResolvedAttachment(
                type = AttachmentType.VIDEO,
                path = local
                    ?: attachment.str("video")
                    ?: vkPermalink("video", attachment.long("from"), attachment.long("id")),
                caption = attachment.str("title"),
            )

            // The export ships VK's own speech-to-text of the voice message, which is the only
            // thing to show for it in a list.
            "audiomessage" -> ResolvedAttachment(
                type = AttachmentType.AUDIO,
                path = local ?: attachment.str("link").orEmpty(),
                caption = attachment.str("text"),
            )

            "audio" -> ResolvedAttachment(
                type = AttachmentType.AUDIO,
                path = local ?: attachment.str("link", "url").orEmpty(),
                caption = listOfNotNull(attachment.str("artist"), attachment.str("title"))
                    .joinToString(" — ")
                    .ifBlank { null },
            )

            "doc", "document" -> ResolvedAttachment(
                type = AttachmentType.FILE,
                path = local ?: attachment.str("link", "url").orEmpty(),
                caption = documentName(attachment),
            )

            "sticker" -> ResolvedAttachment(
                type = AttachmentType.STICKER,
                path = attachment.long("sticker_id")?.let(::vkStickerUrl).orEmpty(),
            )

            "graffiti" -> ResolvedAttachment(AttachmentType.GRAFFITI, attachment.str("link", "url").orEmpty())

            "link" -> ResolvedAttachment(
                type = AttachmentType.LINK,
                path = attachment.str("link", "url").orEmpty(),
                caption = attachment.str("title", "caption"),
            )

            "wall" -> ResolvedAttachment(
                type = AttachmentType.WALL,
                path = vkPermalink("wall", attachment.long("from", "owner_id"), attachment.long("id")),
                caption = attachment.str("text")?.take(CAPTION_LIMIT),
            )

            "call" -> ResolvedAttachment(
                type = AttachmentType.CALL,
                path = "",
                caption = attachment.long("duration")?.takeIf { it > 0 }?.let(::formatDuration),
            )

            // A gift is a picture with no page of its own — kept as OTHER so the media grid isn't
            // seeded with stock artwork among the real photos.
            else -> ResolvedAttachment(
                type = AttachmentType.OTHER,
                path = local ?: attachment.str("link", "url", "preview").orEmpty(),
                caption = attachment.str("title", "text")?.take(CAPTION_LIMIT),
            )
        }
    }

    /**
     * Every downloadable file under [contentRoot], keyed the way the export names it —
     * `{ownerId}_{attachmentId}` — so an attachment can find its own copy. Keyed on the stem alone
     * because the extension follows the media, not the attachment.
     *
     * The whole tree is walked rather than a fixed folder: the media directory's name is written in
     * Russian and has been seen abbreviated differently between dumps.
     */
    private fun indexLocalMedia(contentRoot: File): Map<String, File> = buildMap {
        contentRoot.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in mediaExtensions }
            .forEach { file ->
                val stem = file.nameWithoutExtension
                if (stem.matches(MEDIA_NAME_PATTERN)) putIfAbsent(stem, file)
            }
    }

    /**
     * The folder [file] sits in, relative to [contentRoot] — the same label `OrphanMediaScanner`
     * gives the files it finds, so media the parser claims and media nobody referenced are grouped
     * under one folder name on the archive-files screen rather than two.
     */
    private fun archiveFolder(file: File, contentRoot: File): String? = file.parentFile
        ?.relativeToOrNull(contentRoot)
        ?.path
        ?.takeIf { it.isNotBlank() && !it.startsWith("..") }

    private fun mediaKey(attachment: JsonObject): String? {
        val owner = attachment.long("from") ?: return null
        val id = attachment.long("id") ?: return null
        return "${owner}_$id"
    }

    private fun parseProfileDetails(ownerInfo: JsonObject): ProfileDetails = ProfileDetails(
        vkId = ownerInfo.str("id"),
        screenName = ownerInfo.str("screenName", "screen_name"),
        avatarPath = ownerInfo.str("photo"),
        birthDate = ownerInfo.str("bdate"),
        // Same numbering as VK's own `sex`, under this export's own key.
        sex = when (ownerInfo.int("gender", "sex")) {
            1 -> Sex.FEMALE
            2 -> Sex.MALE
            else -> Sex.UNKNOWN
        },
        country = ownerInfo.placeName("country"),
        city = ownerInfo.placeName("city"),
    )

    /** The friend list, which — unlike the rest of this export — keeps VK's own snake_case keys. */
    private suspend fun parseFriends(jsonDir: File, profileId: String): List<Friend>? {
        val file = VkJsonDumpPaths.friendsFile(jsonDir) ?: return null
        val friends = readJsonArray(file) ?: return null
        return friends.mapNotNull { element ->
            val entity = element.asObject() ?: return@mapNotNull null
            val id = entity.str("id") ?: return@mapNotNull null
            Friend(
                id = id,
                profileId = profileId,
                name = personName(entity) ?: id,
                avatarPath = entity.str("photo_100", "photo_50", "photo"),
            )
        }
    }

    private suspend fun parseGroups(jsonDir: File, profileId: String): List<Group>? {
        val file = VkJsonDumpPaths.groupsFile(jsonDir) ?: return null
        val groups = readJsonArray(file) ?: return null
        return groups.mapNotNull { element ->
            val entity = element.asObject() ?: return@mapNotNull null
            val id = entity.str("id") ?: return@mapNotNull null
            Group(
                id = id,
                profileId = profileId,
                name = entity.str("name") ?: id,
                avatarPath = entity.str("photo_200", "photo_100", "photo_50"),
                screenName = entity.str("screen_name"),
            )
        }
    }

    /**
     * One entry per dialog, holding the peer's name and avatar.
     *
     * Lives in `json/` in one build of this dumper and inside the dialogs directory in the other,
     * so where it is comes from [VkJsonDumpPaths] rather than from a path spelled out here. Missing
     * altogether is fine: every dialog file also carries a `profiles` array covering its own peers.
     */
    private suspend fun readCompanions(jsonDir: File, dialogsDir: File): Map<String, JsonObject> {
        val file = VkJsonDumpPaths.companionsFile(jsonDir, dialogsDir) ?: return emptyMap()
        val companions = readJsonArray(file) ?: return emptyMap()
        return buildMap {
            companions.forEach { element ->
                val entity = element.asObject() ?: return@forEach
                entity.str("id")?.let { put(it, entity) }
            }
        }
    }

    private suspend fun readJsonObject(file: File): JsonObject? = readJsonFile(file) { it.asObject() }

    private suspend fun readJsonArray(file: File): JsonArray? = readJsonFile(file) { it as? JsonArray }

    /**
     * Reads and parses one payload, on the IO dispatcher. Null covers a missing file, unparseable
     * content and a payload of the wrong shape alike — every caller treats an absent section as a
     * normal export shape rather than a failure.
     */
    private suspend fun <T> readJsonFile(file: File, cast: (JsonElement) -> T?): T? =
        withContext(ioDispatcher) {
            val raw = runCatching { file.readText() }.getOrNull() ?: return@withContext null
            runCatching { cast(json.parseToJsonElement(stripJsAssignment(raw))) }.getOrNull()
        }

    /** A place this export writes as `{"id":1,"title":"Россия"}`, and older ones as a plain string. */
    private fun JsonObject.placeName(key: String): String? = str(key) ?: obj(key)?.str("title", "name")

    private companion object {
        const val BATCH_SIZE = 2000
        const val PARALLELISM = 4
        const val GROUP_PEER_ID_THRESHOLD = 2_000_000_000L
        const val CAPTION_LIMIT = 200
        const val MAX_ATTACHMENTS_PER_MESSAGE = 64

        /**
         * Only a stop against a forward chain that loops: an entry given by id can name a message
         * that forwards it straight back. Set well clear of real nesting, which runs a handful deep.
         */
        const val MAX_FORWARD_DEPTH = 64

        val MEDIA_NAME_PATTERN = Regex("""-?\d+_\d+""")
        val mediaExtensions = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif",
            "mp4", "mov", "webm", "mkv", "avi", "m4v", "3gp",
            "mp3", "ogg", "m4a", "wav", "opus",
        )
    }
}
