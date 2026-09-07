package com.etozhesandy.redpanda.core.archive.parse.html.dialect

import com.etozhesandy.redpanda.core.archive.parse.html.HtmlContactFolder
import com.etozhesandy.redpanda.core.archive.parse.html.HtmlDialect
import com.etozhesandy.redpanda.core.archive.parse.html.HtmlGallerySpec
import com.etozhesandy.redpanda.core.archive.parse.html.HtmlParseUtils
import com.etozhesandy.redpanda.core.archive.parse.html.HtmlTimestampParser
import com.etozhesandy.redpanda.core.archive.parse.html.RawHtmlAttachment
import com.etozhesandy.redpanda.core.archive.parse.html.RawHtmlMessage
import com.etozhesandy.redpanda.core.model.AttachmentType
import java.io.File
import javax.inject.Inject
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

/**
 * The export produced by the b00m.site dumper. It writes the same
 * `Диалоги/{категория}/{Имя (idN)}/history_N.html` tree as [VkClassicHtmlDialect] but shares none of
 * its markup, which is why the two are told apart by sniffing a page rather than by paths.
 *
 * Richer than the classic layout in one respect: voice messages are embedded as real `<audio>`
 * elements with playable `.ogg` URLs, where the classic export writes only the word "Аудио".
 *
 * Two versions of this dumper are in the wild and are read by this one dialect, because they share
 * every class name — `im_in`, `im_log_author_chat_name`, `im_date_link`, `wrapped` — and differ
 * only in what carries them. The newer one lays each message out as a `div`, keeps its attachments
 * in a `div.gallery`, and wraps the categories in `Диалоги/`. The older one renders the log as a
 * table (`tr.im_in`), drops attachments straight into the message body, writes `.htm` pages, and
 * puts the category folders in the export root. Forwarded messages are the one place the table
 * version needs real care: it nests a whole `tr.im_in` inside the body of the message forwarding
 * it, so [parseInlineAttachments] has to keep a forward's own attachments off its host.
 */
class B00mHtmlDialect @Inject constructor() : HtmlDialect {

    // No seconds, and a single-digit hour for times before 10 — `H` accepts both widths.
    override val timestampPattern: String = "dd.MM.yyyy H:mm"

    override fun dialogsRoot(contentRoot: File): File = HtmlParseUtils.dialogsRoot(contentRoot, "Диалоги")

    override fun contactFolder(dir: File): HtmlContactFolder? = HtmlParseUtils.parseVkFolderName(dir.name)

    override fun historyPages(contactDir: File): List<File> =
        HtmlParseUtils.numberedPages(contactDir, historyPageRegex)

    /**
     * Both namings this dumper has produced — `photos.html`/`videos.html` and `images.htm`/
     * `videos.htm`. Listing all four is safe: a gallery file that is not there is skipped.
     */
    override fun galleries(contactDir: File): List<HtmlGallerySpec> = listOf(
        HtmlGallerySpec(File(contactDir, "photos.html"), AttachmentType.PHOTO),
        HtmlGallerySpec(File(contactDir, "images.htm"), AttachmentType.PHOTO),
        HtmlGallerySpec(File(contactDir, "videos.html"), AttachmentType.VIDEO),
        HtmlGallerySpec(File(contactDir, "videos.htm"), AttachmentType.VIDEO),
    )

    override fun parsePage(
        document: Document,
        peer: HtmlContactFolder,
        timestamps: HtmlTimestampParser,
    ): List<RawHtmlMessage> = document.select(MESSAGE_SELECTOR).mapNotNull { block ->
        val senderLink = block.selectFirst("div.im_log_author_chat_name a.mem_link") ?: return@mapNotNull null
        val senderId = HtmlParseUtils.extractVkId(senderLink.attr("href")) ?: return@mapNotNull null
        val body = block.selectFirst("div.wrapped")
        RawHtmlMessage(
            senderId = senderId,
            senderName = senderLink.text().trim(),
            isOutgoing = null,
            timestampEpoch = timestamps.parse(block.selectFirst("a.im_date_link")?.text().orEmpty()),
            // The message text is a bare text node between the author div and the attachment block,
            // so only the element's own text belongs to it.
            text = body?.ownText()?.trim().orEmpty(),
            peerAvatarPath = block.selectFirst("div.im_log_author_chat_thumb img")?.attr("src"),
            attachments = parseInlineAttachments(block),
        )
    }

    override fun parseGallery(document: Document, type: AttachmentType): List<RawHtmlAttachment> =
        document.select(GALLERY_SELECTOR).map { RawHtmlAttachment(type, it.attr("href")) }

    /**
     * Everything attached to one message, matched in one pass so document order — and with it the
     * order attachments appear under the message — is preserved.
     *
     * The search is confined to `div.gallery` where the export has one and to the message body
     * otherwise, and in both cases anything sitting inside a nested message is left to that
     * message: a forwarded photo belongs to the forward, not to the message carrying it, and
     * counting it twice would put a second copy in the media grid.
     */
    private fun parseInlineAttachments(block: Element): List<RawHtmlAttachment> {
        val container = block.selectFirst("div.gallery") ?: block.selectFirst("div.wrapped") ?: return emptyList()
        return container.select(INLINE_SELECTOR).filterNot { it.isInsideNestedMessage(container) }.mapNotNull { element ->
            val href = element.attr("href")
            when {
                element.hasClass(MEDIA_CLASS) -> href.toAttachment(mediaType(href))
                element.tagName() == "source" -> element.attr("src").toAttachment(AttachmentType.AUDIO)
                element.tagName() == "img" -> element.attr("src").toAttachment(AttachmentType.STICKER)
                href.contains(WALL_MARKER, ignoreCase = true) ->
                    href.toAttachment(AttachmentType.WALL, element.text().trim().ifBlank { null })

                // A link wrapping a thumbnail is the full-size media behind it, not a file to
                // download. Typing it as a document would be worse than cosmetic: the flat gallery
                // lists these same URLs, and only an entry already imported under its real type is
                // recognised as the duplicate it is, so every photo in the dialog ended up filed
                // as a document and then dropped from the gallery for good measure.
                element.selectFirst("img") != null -> href.toAttachment(mediaType(href))

                element.tagName() == "a" ->
                    href.toAttachment(AttachmentType.FILE, element.text().trim().ifBlank { null })

                else -> null
            }
        }
    }

    /** Whether [this] belongs to a message nested inside [container] rather than to its own. */
    private fun Element.isInsideNestedMessage(container: Element): Boolean =
        generateSequence(parent()) { it.parent() }
            .takeWhile { it !== container }
            .any { it.hasClass(MESSAGE_CLASS) }

    private fun String.toAttachment(type: AttachmentType, caption: String? = null): RawHtmlAttachment? =
        takeIf { it.isNotBlank() }?.let { RawHtmlAttachment(type, it, caption) }

    /**
     * Galleries link straight at the media file, so the extension carries the type — except for a
     * video the export links to its watch page instead (a `vk.com/video…` or a YouTube URL), which
     * has no extension at all and would otherwise be filed as a photo.
     */
    private fun mediaType(url: String): AttachmentType {
        val path = url.substringBefore('?')
        return when {
            path.substringAfterLast('.').lowercase() in videoExtensions -> AttachmentType.VIDEO
            videoHosts.any { url.contains(it, ignoreCase = true) } -> AttachmentType.VIDEO
            else -> AttachmentType.PHOTO
        }
    }

    private companion object {
        const val MEDIA_CLASS = "download_photo_type"
        const val MEDIA_SELECTOR = "a.$MEDIA_CLASS"

        const val MESSAGE_CLASS = "im_in"

        /** A message is a `div` in one version of this export and a table row in the other. */
        const val MESSAGE_SELECTOR = "div.$MESSAGE_CLASS, tr.$MESSAGE_CLASS"

        /**
         * Gallery entries: classed links in the newer export, and in the older one bare
         * `<a href="…full.jpg"><img src="…thumb.jpg"></a>` pairs, where the link — not the
         * thumbnail it wraps — is the full-size media worth keeping.
         */
        const val GALLERY_SELECTOR = "a.$MEDIA_CLASS, a[href][target=_blank]:has(img)"

        const val WALL_MARKER = "vk.com/wall"

        /**
         * Everything the attachment block can hold: photos and videos, voice message sources (this
         * export embeds real playable `.ogg` URLs, unlike the classic one), stickers as bare
         * images, and links to documents and wall posts.
         */
        const val INLINE_SELECTOR =
            "$MEDIA_SELECTOR, audio source[src], img[src*=vk.com/sticker], " +
                "a[href*=vk.com/doc], a[href*=vk.com/wall], a[href][target=_blank]:has(img)"

        /** `.htm` and `.html` both occur, from the same dumper. */
        val historyPageRegex = Regex("""history_(\d+)\.html?""")
        val videoExtensions = setOf("mp4", "mov", "webm", "mkv", "avi", "m4v", "3gp")

        /** Watch-page URLs an export links instead of a file, which carry no extension. */
        val videoHosts = listOf("vk.com/video", "youtube.com/", "youtu.be/")
    }
}
