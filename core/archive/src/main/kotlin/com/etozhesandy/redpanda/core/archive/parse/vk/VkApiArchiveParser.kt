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
import com.etozhesandy.redpanda.core.model.SavedPhoto
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
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Parses a second, richer VK export layout seen in the wild: a self-contained viewer app with
 * `profile.json` at the root and `messages/{peerId}/{data.json, 1.json, 2.json, ...}` — each file
 * is a raw VK API `messages.getHistory` response dump, saved as a JS assignment
 * (`messages=[...]`) rather than bare JSON, which [stripJsAssignment] peels off before parsing.
 *
 * Unlike the HTML export ([VkArchiveParser]), messages here carry a native `out` boolean, so
 * [Message.isOutgoing] doesn't need to be inferred, and attachments are already embedded per
 * message with resolvable URLs — no separate unlinked gallery involved.
 */
class VkApiArchiveParser @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ChatArchiveParser {

    // Real VK exports have been observed with a trailing comma before a closing `}` in
    // conversations.json (e.g. `..."messages_count":2}},}`) — invalid strict JSON that would
    // otherwise silently fail to parse and blank out every dialog's peer name.
    private val json = Json { ignoreUnknownKeys = true; allowTrailingComma = true }

    override suspend fun parse(contentRoot: File, profileId: String, sink: ParseSink) =
        withContext(defaultDispatcher) {
            val ownerInfo = readJsonObject(File(contentRoot, "profile.json"))
            val displayName = ownerInfo?.let(::personName) ?: contentRoot.name

            val messagesDir = File(contentRoot, "messages")
            val conversations = readJsonObject(File(messagesDir, "conversations.json"))

            val ownerId = ownerInfo?.get("id")?.jsonPrimitive?.contentOrNull

            ownerInfo?.let { sink.onProfileDetails(parseProfileDetails(it)) }
            parseFriends(contentRoot, profileId)?.let { sink.onFriends(it) }
            parseGroups(contentRoot, profileId)?.let { sink.onGroups(it) }
            parseSavedPhotos(contentRoot, profileId)?.let { sink.onSavedPhotos(it) }

            val peerDirs = messagesDir.listFiles { file -> file.isDirectory }.orEmpty()
            sink.onDialogsDiscovered(peerDirs.size)

            val semaphore = Semaphore(PARALLELISM)
            coroutineScope {
                peerDirs.map { peerDir ->
                    async(ioDispatcher) {
                        semaphore.withPermit {
                            parsePeer(peerDir, conversations?.get(peerDir.name)?.jsonObject, ownerId, profileId, sink)
                        }
                    }
                }.awaitAll()
            }

            sink.onDisplayName(displayName)
        }

    private suspend fun parsePeer(
        peerDir: File,
        conversation: JsonObject?,
        ownerId: String?,
        profileId: String,
        sink: ParseSink,
    ) {
        val peerId = peerDir.name
        val dialogId = "$profileId:$peerId"

        val peerEntity = conversation?.get("user")?.jsonObject ?: conversation?.get("group")?.jsonObject
        val peerName = peerEntity?.let(::personName) ?: peerId
        val peerAvatarPath = peerEntity?.get("photo_100")?.jsonPrimitive?.contentOrNull
            ?: peerEntity?.get("photo_50")?.jsonPrimitive?.contentOrNull

        val dataJson = runCatching {
            json.parseToJsonElement(stripJsAssignment(File(peerDir, "data.json").readText())).jsonObject
        }.getOrNull()
        val nameById = buildNameLookup(dataJson)

        val pageCount = dataJson?.get("jsons_count")?.jsonPrimitive?.intOrNull
            ?: peerDir.listFiles { file -> file.name.removeSuffix(".json").toIntOrNull() != null }.orEmpty().size

        var lastMessageAt = 0L
        var messageCount = 0
        val distinctSenders = mutableSetOf<String>()
        val messageBuffer = ArrayList<Message>(BATCH_SIZE)
        val attachmentBuffer = ArrayList<Attachment>(BATCH_SIZE)

        for (page in 1..pageCount) {
            val pageFile = File(peerDir, "$page.json")
            if (!pageFile.exists()) continue
            val pageMessages = runCatching {
                json.parseToJsonElement(stripJsAssignment(pageFile.readText())).jsonArray
            }.getOrNull() ?: continue

            for (element in pageMessages) {
                val msg = element.jsonObject
                val localId = msg["id"]?.jsonPrimitive?.contentOrNull ?: continue
                val fromId = msg["from_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                val timestamp = (msg["date"]?.jsonPrimitive?.longOrNull ?: 0L) * 1000
                val text = extractMessageText(msg, nameById, ownerId, peerId, peerName)
                val isOutgoing = msg["out"]?.jsonPrimitive?.booleanOrNull ?: (fromId == ownerId)
                val senderName = resolveSenderName(fromId, ownerId, peerId, peerName, nameById)
                distinctSenders += fromId

                val messageId = "$dialogId:$localId"
                messageBuffer += Message(
                    id = messageId,
                    dialogId = dialogId,
                    profileId = profileId,
                    senderId = fromId,
                    senderName = senderName,
                    timestampEpoch = timestamp,
                    text = text,
                    isOutgoing = isOutgoing,
                    isFavorite = false,
                )
                messageCount++
                if (timestamp > lastMessageAt) lastMessageAt = timestamp

                collectAttachments(msg).take(MAX_ATTACHMENTS_PER_MESSAGE)
                    .forEachIndexed { index, attachmentObject ->
                        resolveAttachment(attachmentObject)?.let { resolved ->
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

    /** Resolves a display name for [fromId], preferring the per-dialog id→name lookup over the single cached peer name. */
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
     * Own text, plus (if present) a quoted [reply_message] and a labeled excerpt of every
     * [fwd_messages] entry — VK stores forwarded/quoted content in these separate substructures
     * rather than in the top-level `text` field, so a purely-forwarded message otherwise renders blank.
     */
    private fun extractMessageText(
        msg: JsonObject,
        nameById: Map<String, String>,
        ownerId: String?,
        peerId: String,
        peerName: String,
    ): String {
        val own = msg["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val replyText = msg["reply_message"]?.jsonObject?.let { reply ->
            val text = extractMessageText(reply, nameById, ownerId, peerId, peerName)
            text.ifBlank { null }?.let { "> $it" }
        }
        val fwdTexts = msg["fwd_messages"]?.jsonArray?.mapNotNull { fwd ->
            val fwdObj = fwd.jsonObject
            val fromId = fwdObj["from_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
            val sender = resolveSenderName(fromId, ownerId, peerId, peerName, nameById)
            val text = extractMessageText(fwdObj, nameById, ownerId, peerId, peerName)
            text.ifBlank { null }?.let { "[Пересланное от $sender]: $it" }
        }.orEmpty()
        return listOfNotNull(own.ifBlank { null }, replyText, *fwdTexts.toTypedArray()).joinToString("\n")
    }

    /** Builds an id→name lookup from `data.json`'s `users`/`profiles`/`groups` maps, covering everyone referenced anywhere in this peer's pages (including inside forwards/replies). */
    private fun buildNameLookup(dataJson: JsonObject?): Map<String, String> = buildMap {
        dataJson?.get("users")?.jsonObject?.forEach { (id, entity) -> entity.jsonObject.let(::personName)?.let { put(id, it) } }
        dataJson?.get("profiles")?.jsonObject?.forEach { (id, entity) -> entity.jsonObject.let(::personName)?.let { put(id, it) } }
        dataJson?.get("groups")?.jsonObject?.forEach { (id, entity) ->
            entity.jsonObject["name"]?.jsonPrimitive?.contentOrNull?.let { put(id, it) }
        }
    }

    /**
     * Turns one attachment object into something storable. **Never returns null for an attachment
     * that states a type** — an unrecognised kind becomes [AttachmentType.OTHER] rather than
     * disappearing. Dropping unknown kinds silently is what previously lost between 9 % and 41 % of
     * every dump's attachments, documents included: the branch here used to match `"doc"`, while
     * real exports write `"document"`.
     */
    private fun resolveAttachment(attachment: JsonObject): ResolvedAttachment? {
        val rawType = attachment.str("type") ?: return null
        val body = attachment.obj(rawType) ?: JsonObject(emptyMap())
        return when (normalizeAttachmentType(rawType)) {
            "photo" -> ResolvedAttachment(AttachmentType.PHOTO, largestImageUrl(body.arr("sizes")).orEmpty())

            // A video the source has removed keeps its record: no player, but a permalink and the
            // title the export still carries ("Видео недоступно").
            "video" -> ResolvedAttachment(
                type = AttachmentType.VIDEO,
                path = body.str("player") ?: permalink("video", body),
                caption = body.str("title"),
            )

            "audiomessage" -> ResolvedAttachment(AttachmentType.AUDIO, body.str("link_mp3", "link_ogg").orEmpty())

            // A named track with nothing to play: the export lists the metadata only.
            "audio" -> ResolvedAttachment(
                type = AttachmentType.AUDIO,
                path = "",
                caption = listOfNotNull(body.str("artist"), body.str("title")).joinToString(" — ").ifBlank { null },
            )

            "document", "doc" -> ResolvedAttachment(
                type = AttachmentType.FILE,
                path = body.str("url").orEmpty(),
                caption = documentName(body),
            )

            "sticker" -> ResolvedAttachment(
                type = AttachmentType.STICKER,
                path = largestImageUrl(body.arr("Images", "images")).orEmpty(),
            )

            "graffiti" -> ResolvedAttachment(AttachmentType.GRAFFITI, body.str("url").orEmpty())

            "link" -> ResolvedAttachment(
                type = AttachmentType.LINK,
                path = body.str("Uri", "url").orEmpty(),
                caption = body.str("Title", "title", "Caption", "caption"),
            )

            "wall" -> ResolvedAttachment(
                type = AttachmentType.WALL,
                path = permalink("wall", body),
                caption = body.str("Text", "text")?.take(CAPTION_LIMIT),
            )

            "wallreply" -> ResolvedAttachment(
                type = AttachmentType.WALL,
                path = "",
                caption = body.str("Text", "text")?.take(CAPTION_LIMIT),
            )

            // Duration only; the UI names the kind itself, so no language belongs in the parser.
            "call" -> ResolvedAttachment(
                type = AttachmentType.CALL,
                path = "",
                caption = body.long("duration")?.takeIf { it > 0 }?.let(::formatDuration),
            )

            else -> ResolvedAttachment(
                type = AttachmentType.OTHER,
                path = body.str("url", "thumb_256", "ThumbPhoto").orEmpty(),
                caption = body.str("Title", "title", "Text", "text")?.take(CAPTION_LIMIT),
            )
        }
    }

    /** `https://vk.com/{kind}{ownerId}_{id}` — how VK addresses a post or a video on the web. */
    private fun permalink(kind: String, body: JsonObject): String {
        val ownerId = body.long("owner_id", "OwnerId") ?: return ""
        val id = body.long("id", "video_id", "Id") ?: return ""
        return "https://vk.com/$kind${ownerId}_$id"
    }

    /** The export splits a document's name from its extension, and sometimes repeats it in both. */
    private fun documentName(body: JsonObject): String? {
        val title = body.str("Title", "title") ?: return null
        val extension = body.str("Ext", "ext") ?: return title
        return if (title.endsWith(".$extension", ignoreCase = true)) title else "$title.$extension"
    }

    /**
     * Every attachment of [message], including those of the messages it quotes or forwards. VK
     * keeps forwarded content in separate substructures, so a message whose only content is a
     * forwarded photo used to import with no attachment at all.
     *
     * Depth-capped because forwards nest arbitrarily: one pathological chain should not be able to
     * blow up a batch.
     */
    private fun collectAttachments(message: JsonObject, depth: Int = 0): List<JsonObject> {
        if (depth > MAX_FORWARD_DEPTH) return emptyList()
        val own = message.arr("attachments")?.mapNotNull { runCatching { it.jsonObject }.getOrNull() }.orEmpty()
        val quoted = message.obj("reply_message")?.let { collectAttachments(it, depth + 1) }.orEmpty()
        val forwarded = message.arr("fwd_messages")
            ?.mapNotNull { runCatching { it.jsonObject }.getOrNull() }
            ?.flatMap { collectAttachments(it, depth + 1) }
            .orEmpty()
        // Own attachments stay first so their indices — and the attachment ids built from them —
        // are unchanged for messages that carry no forwards.
        return own + quoted + forwarded
    }

    private fun parseProfileDetails(ownerInfo: JsonObject): ProfileDetails = ProfileDetails(
        vkId = ownerInfo["id"]?.jsonPrimitive?.contentOrNull,
        screenName = ownerInfo["screen_name"]?.jsonPrimitive?.contentOrNull,
        avatarPath = ownerInfo["photo_max_orig"]?.jsonPrimitive?.contentOrNull
            ?: ownerInfo["photo_200"]?.jsonPrimitive?.contentOrNull,
        birthDate = ownerInfo["bdate"]?.jsonPrimitive?.contentOrNull,
        sex = when (ownerInfo["sex"]?.jsonPrimitive?.intOrNull) {
            1 -> Sex.FEMALE
            2 -> Sex.MALE
            else -> Sex.UNKNOWN
        },
        country = ownerInfo["country"]?.jsonPrimitive?.contentOrNull,
        city = ownerInfo["city"]?.jsonPrimitive?.contentOrNull,
    )

    /**
     * Reads the owner's own `friends/friends.json`, absent from every export except the API-dump layout.
     * Like `messages`/`groups`/`saved.json`, the whole file is a single top-level JS array assignment
     * (`friends=[...]`), not an object wrapping a `friends` key.
     */
    private suspend fun parseFriends(contentRoot: File, profileId: String): List<Friend>? {
        val friends = readJsonArray(File(contentRoot, "friends/friends.json")) ?: return null
        return friends.mapNotNull { element ->
            val entity = element.jsonObject
            val id = entity["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            Friend(
                id = id,
                profileId = profileId,
                name = personName(entity) ?: id,
                avatarPath = entity["photo100"]?.jsonPrimitive?.contentOrNull,
            )
        }
    }

    /** Reads the owner's own `groups/groups.json`. Real exports have been observed with a capitalized `Id` key. */
    private suspend fun parseGroups(contentRoot: File, profileId: String): List<Group>? {
        val groups = readJsonArray(File(contentRoot, "groups/groups.json")) ?: return null
        return groups.mapNotNull { element ->
            val entity = element.jsonObject
            val id = (entity["id"] ?: entity["Id"])?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            Group(
                id = id,
                profileId = profileId,
                name = entity["name"]?.jsonPrimitive?.contentOrNull ?: id,
                avatarPath = entity["photo_100"]?.jsonPrimitive?.contentOrNull
                    ?: entity["photo_50"]?.jsonPrimitive?.contentOrNull,
                screenName = entity["screen_name"]?.jsonPrimitive?.contentOrNull,
            )
        }
    }

    /** Reads the owner's own `saved/saved.json`, picking the largest available size per photo. */
    private suspend fun parseSavedPhotos(contentRoot: File, profileId: String): List<SavedPhoto>? {
        val photos = readJsonArray(File(contentRoot, "saved/saved.json")) ?: return null
        return photos.mapIndexedNotNull { index, element ->
            val entity = element.jsonObject
            val url = largestImageUrl(entity.arr("sizes")) ?: return@mapIndexedNotNull null
            SavedPhoto(
                id = index.toString(),
                profileId = profileId,
                url = url,
                timestampEpoch = (entity["date"]?.jsonPrimitive?.longOrNull ?: 0L) * 1000,
            )
        }
    }

    private suspend fun readJsonObject(file: File): JsonObject? =
        readJsonFile(file) { it.jsonObject }

    private suspend fun readJsonArray(file: File): JsonArray? =
        readJsonFile(file) { it.jsonArray }

    /**
     * Reads one of the root-level payloads on the IO dispatcher and parses it on the caller's:
     * these are the only reads on the parse path not already inside an `async(ioDispatcher)`, and
     * `conversations.json` alone runs to tens of megabytes on a real dump.
     *
     * Null covers a missing file, unparseable content and a payload of the wrong shape alike —
     * every caller treats an absent section as a normal export shape, not a failure.
     */
    private suspend fun <T> readJsonFile(file: File, cast: (JsonElement) -> T): T? {
        val raw = withContext(ioDispatcher) { runCatching { file.readText() }.getOrNull() } ?: return null
        // The cast runs inside the catch because `jsonObject`/`jsonArray` throw on a mismatch.
        return runCatching { cast(json.parseToJsonElement(stripJsAssignment(raw))) }.getOrNull()
    }

    private fun personName(entity: JsonObject): String? {
        val name = listOfNotNull(
            entity["first_name"]?.jsonPrimitive?.contentOrNull,
            entity["last_name"]?.jsonPrimitive?.contentOrNull,
        ).joinToString(" ").trim()
        return name.ifBlank { entity["name"]?.jsonPrimitive?.contentOrNull }
    }

    /** These exports save each JSON payload as a JS assignment, e.g. `messages=[...]`. */
    private fun stripJsAssignment(raw: String): String {
        val trimmed = raw.trim()
        val eq = trimmed.indexOf('=')
        return if (eq in 1..40) trimmed.substring(eq + 1).trim().removeSuffix(";") else trimmed
    }

    private companion object {
        const val BATCH_SIZE = 2000
        const val PARALLELISM = 4
        const val GROUP_PEER_ID_THRESHOLD = 2_000_000_000L
        const val CAPTION_LIMIT = 200

        /**
         * Only a stop against pathological recursion, so it is set well clear of real data: one
         * export nests forwards 32 deep, and a tighter cap silently lost the attachments below it.
         */
        const val MAX_FORWARD_DEPTH = 64
        const val MAX_ATTACHMENTS_PER_MESSAGE = 64
    }
}
