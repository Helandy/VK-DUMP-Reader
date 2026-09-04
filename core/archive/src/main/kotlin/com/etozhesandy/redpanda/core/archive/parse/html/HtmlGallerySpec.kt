package com.etozhesandy.redpanda.core.archive.parse.html

import com.etozhesandy.redpanda.core.model.AttachmentType
import java.io.File

/**
 * One flat attachment gallery page and the kind of media it lists. Exports differ in how many of
 * these a contact has — a single `photos.html`, or a paginated `photo/1.html`, `photo/2.html` — so
 * [HtmlDialect.galleries] returns one spec per file.
 */
data class HtmlGallerySpec(val file: File, val type: AttachmentType)
