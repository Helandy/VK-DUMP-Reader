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
import org.jsoup.nodes.TextNode

/**
 * The VK HTML export layout seen in most archives:
 * `Диалоги/{категория}/{Имя (idN)}/history_N.html`, with message blocks shaped as
 * `div.m > (div.mp img, div.mm > a.ma + span.md, div.mc > div.mt (+ div.a > a))`.
 *
 * Note the attachment block is written `<div class=a>` — unquoted — so it is matched by class, not
 * by an attribute-value selector.
 */
class VkClassicHtmlDialect @Inject constructor() : HtmlDialect {

    override val timestampPattern: String = "dd.MM.yyyy, HH:mm:ss"

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
    ): List<RawHtmlMessage> = document.select("div.m").mapNotNull { block ->
        val senderLink = block.selectFirst("a.ma") ?: return@mapNotNull null
        val senderId = HtmlParseUtils.extractVkId(senderLink.attr("href")) ?: return@mapNotNull null
        RawHtmlMessage(
            senderId = senderId,
            senderName = senderLink.text().trim(),
            isOutgoing = null,
            timestampEpoch = timestamps.parse(block.selectFirst("span.md")?.text().orEmpty()),
            text = block.selectFirst("div.mt")?.text().orEmpty(),
            peerAvatarPath = block.selectFirst("div.mp img")?.attr("src"),
            attachments = parseInlineAttachments(block),
        )
    }

    override fun parseGallery(document: Document, type: AttachmentType): List<RawHtmlAttachment> =
        document.select("a.Ae").map { RawHtmlAttachment(type, it.attr("href")) }

    /**
     * The attachment block mixes three shapes with no type markers at all, so its direct children
     * are walked rather than selected: anchors (photos, videos, documents, wall posts), bare `img`
     * elements (stickers — by far the most common attachment in real archives, and previously
     * invisible because nothing looked outside anchors), and a bare text node for voice messages.
     */
    private fun parseInlineAttachments(block: Element): List<RawHtmlAttachment> =
        block.select("div.a").flatMap { attachments ->
            attachments.childNodes().flatMap { node ->
                when (node) {
                    is Element -> listOfNotNull(elementAttachment(node))
                    is TextNode -> audioAttachments(node.text())
                    else -> emptyList()
                }
            }
        }

    private fun elementAttachment(element: Element): RawHtmlAttachment? = when (element.tagName()) {
        "img" -> element.attr("src")
            .takeIf { it.contains(STICKER_MARKER, ignoreCase = true) }
            ?.let { RawHtmlAttachment(AttachmentType.STICKER, it) }

        "a" -> anchorAttachment(element)
        else -> null
    }

    private fun anchorAttachment(link: Element): RawHtmlAttachment? {
        val href = link.attr("href")
        if (href.isBlank()) return null
        val text = link.text().trim()
        return when {
            // "Документ Отчёт.docx" — the label repeats the kind, so only the file name is kept.
            href.contains(DOC_MARKER, ignoreCase = true) ->
                RawHtmlAttachment(AttachmentType.FILE, href, text.removePrefix(DOC_LABEL).trim().ifBlank { null })

            href.contains(WALL_MARKER, ignoreCase = true) ->
                RawHtmlAttachment(AttachmentType.WALL, href, text.ifBlank { null })

            isVideoLink(link, href) -> RawHtmlAttachment(AttachmentType.VIDEO, href)
            link.selectFirst("img") != null -> RawHtmlAttachment(AttachmentType.PHOTO, href)
            else -> null
        }
    }

    /**
     * Voice messages appear as nothing but the word "Аудио", repeated once per track, with no URL
     * and no metadata anywhere in the export. The count is the only thing recoverable, so that many
     * path-less attachments are emitted — one per track that was really there.
     */
    private fun audioAttachments(text: String): List<RawHtmlAttachment> {
        val count = text.split(AUDIO_LABEL).size - 1
        return List(count) { RawHtmlAttachment(AttachmentType.AUDIO, "", AUDIO_LABEL) }
    }

    private fun isVideoLink(link: Element, href: String): Boolean =
        href.contains("/video", ignoreCase = true) ||
            href.contains("video_ext", ignoreCase = true) ||
            link.attr("title").contains("Видео", ignoreCase = true) ||
            link.text().startsWith("Видео", ignoreCase = true)

    private companion object {
        const val STICKER_MARKER = "vk.com/sticker"
        const val DOC_MARKER = "vk.com/doc"
        const val WALL_MARKER = "vk.com/wall"
        const val DOC_LABEL = "Документ "
        const val AUDIO_LABEL = "Аудио"

        val historyPageRegex = Regex("""history_(\d+)\.html""")
    }
}
