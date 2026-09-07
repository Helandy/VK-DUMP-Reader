package com.etozhesandy.redpanda.core.archive.parse.vk

import com.etozhesandy.redpanda.core.model.AttachmentType

/**
 * One VK attachment object turned into something storable.
 *
 * [path] is empty for kinds the export describes but does not link — a track it only names, a call,
 * a video the source has removed — and [caption] then carries whatever it did say about it.
 *
 * [sourceFolder] is set only when [path] points at a file the dump itself downloaded: it is the
 * folder holding that file, relative to the archive's content root, and it is what keeps the file
 * listed among the archive's own media as well as on the message it arrived with.
 */
data class ResolvedAttachment(
    val type: AttachmentType,
    val path: String,
    val caption: String? = null,
    val sourceFolder: String? = null,
)
