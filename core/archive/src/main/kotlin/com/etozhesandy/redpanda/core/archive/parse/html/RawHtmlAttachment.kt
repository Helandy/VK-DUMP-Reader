package com.etozhesandy.redpanda.core.archive.parse.html

import com.etozhesandy.redpanda.core.model.AttachmentType

/**
 * One attachment as an [HtmlDialect] read it, before the shared parser assigns it ids and ordering.
 *
 * [path] is blank for attachments the export names but does not link — the classic layout writes
 * the bare word "Аудио" for a voice message, with no URL anywhere. [caption] carries whatever the
 * markup did say about it, such as a document's file name.
 */
data class RawHtmlAttachment(
    val type: AttachmentType,
    val path: String,
    val caption: String? = null,
)
