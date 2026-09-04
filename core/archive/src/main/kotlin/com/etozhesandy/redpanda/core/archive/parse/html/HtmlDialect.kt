package com.etozhesandy.redpanda.core.archive.parse.html

import com.etozhesandy.redpanda.core.model.AttachmentType
import java.io.File
import org.jsoup.nodes.Document

/**
 * The parts of an HTML export that actually differ between dumper tools — folder naming, page
 * naming, markup, date format. Everything expensive and easy to get wrong (walking contacts,
 * concurrency, batching into the sink, deterministic ids, gallery/inline de-duplication) lives once
 * in [HtmlDialogArchiveParser], so a new dumper's layout costs one implementation of this and
 * nothing else.
 *
 * Implementations must be stateless: one instance parses every contact of an archive concurrently.
 */
interface HtmlDialect {

    /** Date pattern for this dialect, used to build a per-contact [HtmlTimestampParser]. */
    val timestampPattern: String

    /** Directory holding the `{категория}/{контакт}` tree, e.g. `Диалоги` or `Переписки`. */
    fun dialogsRoot(contentRoot: File): File

    /** Peer read out of a contact directory's name, or null when the name doesn't fit the layout. */
    fun contactFolder(dir: File): HtmlContactFolder?

    /** History pages of one contact, in reading order. */
    fun historyPages(contactDir: File): List<File>

    /** Flat attachment galleries of one contact; empty when the dialect has none. */
    fun galleries(contactDir: File): List<HtmlGallerySpec>

    /** Every message on one history page. A block this dialect cannot read is simply not emitted. */
    fun parsePage(
        document: Document,
        peer: HtmlContactFolder,
        timestamps: HtmlTimestampParser,
    ): List<RawHtmlMessage>

    /** Entries of one gallery page. */
    fun parseGallery(document: Document, type: AttachmentType): List<RawHtmlAttachment>

    /**
     * The archive owner's own display name when the export states it outright. Defaults to null,
     * for the dialects where it can only be inferred from who sent the outgoing messages.
     */
    fun ownerName(contentRoot: File): String? = null
}
