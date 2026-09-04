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
 * The export produced by the torrentvk dumper, laid out as
 * `Переписки/{категория}/{Имя NNNNNNN}/{N}.html` — no parentheses around the peer id, and pages
 * numbered from zero.
 *
 * It is the only layout that never names a message's sender: direction lives in the message's own
 * CSS class (`message-right` for outgoing), and the archive owner's name appears nowhere except the
 * title of the `Диалоги.html` index beside the conversations.
 */
class TorrentVkHtmlDialect @Inject constructor() : HtmlDialect {

    override val timestampPattern: String = "dd.MM.yyyy, HH:mm:ss"

    override fun dialogsRoot(contentRoot: File): File = File(contentRoot, "Переписки")

    override fun contactFolder(dir: File): HtmlContactFolder? =
        HtmlParseUtils.parseTrailingIdFolderName(dir.name)

    override fun historyPages(contactDir: File): List<File> =
        HtmlParseUtils.numberedPages(contactDir, pageRegex)

    /** Galleries are paginated into their own subdirectories rather than being one file each. */
    override fun galleries(contactDir: File): List<HtmlGallerySpec> =
        galleryDirs.flatMap { (name, type) ->
            HtmlParseUtils.numberedPages(File(contactDir, name), pageRegex).map { HtmlGallerySpec(it, type) }
        }

    override fun parsePage(
        document: Document,
        peer: HtmlContactFolder,
        timestamps: HtmlTimestampParser,
    ): List<RawHtmlMessage> = document.select("div.message").mapNotNull { block ->
        val isOutgoing = when {
            block.hasClass(OUTGOING_CLASS) -> true
            block.hasClass(INCOMING_CLASS) -> false
            // Neither marker means the block is not a message; there is no other way to tell.
            else -> return@mapNotNull null
        }
        val body = block.selectFirst("div.column") ?: return@mapNotNull null
        RawHtmlMessage(
            // Nothing in this markup identifies the sender, so the shared parser fills both in.
            senderId = null,
            senderName = null,
            isOutgoing = isOutgoing,
            timestampEpoch = timestamps.parse(timestampText(body)),
            // The text is a bare node between the media block and the timestamp span.
            text = body.ownText().trim(),
            peerAvatarPath = block.selectFirst("img.avatar")?.attr("src"),
            attachments = block.select("img.$MEDIA_CLASS").mapNotNull { media ->
                media.attr("src").takeIf { it.isNotBlank() }?.let { RawHtmlAttachment(mediaType(it), it) }
            },
        )
    }

    override fun parseGallery(document: Document, type: AttachmentType): List<RawHtmlAttachment> =
        document.select("div.media").map { RawHtmlAttachment(type, it.attr("data-link")) }

    /**
     * The owner is named only in the index page's title, as `Диалоги | Имя Фамилия 12345`. Read
     * from the head of the file: the index lists every conversation and runs to megabytes.
     */
    override fun ownerName(contentRoot: File): String? {
        val index = File(contentRoot, INDEX_FILE).takeIf { it.isFile } ?: return null
        val head = runCatching { index.bufferedReader().use { it.readText().take(HEAD_CHARS) } }.getOrNull().orEmpty()
        val title = titleRegex.find(head)?.groupValues?.get(1)?.trim() ?: return null
        val owner = title.substringAfter(TITLE_SEPARATOR, missingDelimiterValue = "").trim().ifBlank { return null }
        return HtmlParseUtils.parseTrailingIdFolderName(owner)?.peerName ?: owner
    }

    private fun timestampText(body: Element): String =
        body.children().lastOrNull { it.tagName() == "span" }?.text().orEmpty()

    /** Inline media links straight at the file, so the extension is the only type marker. */
    private fun mediaType(url: String): AttachmentType =
        if (url.substringBefore('?').substringAfterLast('.').lowercase() in videoExtensions) {
            AttachmentType.VIDEO
        } else {
            AttachmentType.PHOTO
        }

    private companion object {
        const val OUTGOING_CLASS = "message-right"
        const val INCOMING_CLASS = "message-left"
        const val MEDIA_CLASS = "message-media"
        const val INDEX_FILE = "Диалоги.html"
        const val TITLE_SEPARATOR = "|"
        const val HEAD_CHARS = 4096

        val pageRegex = Regex("""(\d+)\.html""")
        val titleRegex = Regex("""<title>(.*?)</title>""", RegexOption.IGNORE_CASE)
        val galleryDirs = listOf("photo" to AttachmentType.PHOTO, "video" to AttachmentType.VIDEO)
        val videoExtensions = setOf("mp4", "mov", "webm", "mkv", "avi", "m4v", "3gp")
    }
}
