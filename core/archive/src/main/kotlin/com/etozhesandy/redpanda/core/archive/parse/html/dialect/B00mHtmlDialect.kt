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
 */
class B00mHtmlDialect @Inject constructor() : HtmlDialect {

    // No seconds, and a single-digit hour for times before 10 — `H` accepts both widths.
    override val timestampPattern: String = "dd.MM.yyyy H:mm"

    override fun dialogsRoot(contentRoot: File): File = File(contentRoot, "Диалоги")

    override fun contactFolder(dir: File): HtmlContactFolder? = HtmlParseUtils.parseVkFolderName(dir.name)

    override fun historyPages(contactDir: File): List<File> =
        HtmlParseUtils.numberedPages(contactDir, historyPageRegex)

    override fun galleries(contactDir: File): List<HtmlGallerySpec> = listOf(
        HtmlGallerySpec(File(contactDir, "photos.html"), AttachmentType.PHOTO),
        HtmlGallerySpec(File(contactDir, "videos.html"), AttachmentType.VIDEO),
    )

    override fun parsePage(
        document: Document,
        peer: HtmlContactFolder,
        timestamps: HtmlTimestampParser,
    ): List<RawHtmlMessage> = document.select("div.im_in").mapNotNull { block ->
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
        document.select(MEDIA_SELECTOR).map { RawHtmlAttachment(type, it.attr("href")) }

    /**
     * Everything in `div.gallery.attachment`, matched in one pass so document order — and with it
     * the order attachments appear under the message — is preserved.
     */
    private fun parseInlineAttachments(block: Element): List<RawHtmlAttachment> =
        block.select("div.gallery").select(INLINE_SELECTOR).mapNotNull { element ->
            val href = element.attr("href")
            when {
                element.hasClass(MEDIA_CLASS) -> href.toAttachment(mediaType(href))
                element.tagName() == "source" -> element.attr("src").toAttachment(AttachmentType.AUDIO)
                element.tagName() == "img" -> element.attr("src").toAttachment(AttachmentType.STICKER)
                href.contains(WALL_MARKER, ignoreCase = true) ->
                    href.toAttachment(AttachmentType.WALL, element.text().trim().ifBlank { null })

                element.tagName() == "a" ->
                    href.toAttachment(AttachmentType.FILE, element.text().trim().ifBlank { null })

                else -> null
            }
        }

    private fun String.toAttachment(type: AttachmentType, caption: String? = null): RawHtmlAttachment? =
        takeIf { it.isNotBlank() }?.let { RawHtmlAttachment(type, it, caption) }

    /** Galleries link straight at the media file, so the extension is the only type marker there is. */
    private fun mediaType(url: String): AttachmentType =
        if (url.substringBefore('?').substringAfterLast('.').lowercase() in videoExtensions) {
            AttachmentType.VIDEO
        } else {
            AttachmentType.PHOTO
        }

    private companion object {
        const val MEDIA_CLASS = "download_photo_type"
        const val MEDIA_SELECTOR = "a.$MEDIA_CLASS"

        const val WALL_MARKER = "vk.com/wall"

        /**
         * Everything the attachment block can hold: photos and videos, voice message sources (this
         * export embeds real playable `.ogg` URLs, unlike the classic one), stickers as bare
         * images, and links to documents and wall posts.
         */
        const val INLINE_SELECTOR =
            "$MEDIA_SELECTOR, audio source[src], img[src*=vk.com/sticker], " +
                "a[href*=vk.com/doc], a[href*=vk.com/wall]"

        val historyPageRegex = Regex("""history_(\d+)\.html""")
        val videoExtensions = setOf("mp4", "mov", "webm", "mkv", "avi", "m4v", "3gp")
    }
}
