package com.etozhesandy.redpanda.core.storage.db.attachment

import com.etozhesandy.redpanda.core.model.AttachmentType

/** One archive media folder with how many files it holds and one of them to preview it with. */
data class ArchiveFolderSummary(
    val folder: String,
    val fileCount: Int,
    val previewPath: String?,
    val previewType: AttachmentType?,
    val latestTimestampEpoch: Long,
)
