package com.etozhesandy.redpanda.core.archive.parse.vk

import com.etozhesandy.redpanda.core.model.AttachmentType

/**
 * One VK API attachment object turned into something storable.
 *
 * [path] is empty for kinds the export describes but does not link — a track it only names, a call,
 * a video the source has removed — and [caption] then carries whatever it did say about it.
 */
data class ResolvedAttachment(
    val type: AttachmentType,
    val path: String,
    val caption: String? = null,
)
