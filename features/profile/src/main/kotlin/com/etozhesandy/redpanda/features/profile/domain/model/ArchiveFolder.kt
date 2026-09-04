package com.etozhesandy.redpanda.features.profile.domain.model

import com.etozhesandy.redpanda.core.model.AttachmentType

/**
 * One folder of archive media, summarised. Counted and previewed by the database rather than by
 * loading every file of the profile — a dump can hold thousands, and the list only needs a count
 * and one thumbnail per folder.
 */
data class ArchiveFolder(
    val path: String,
    val fileCount: Int,
    val previewPath: String?,
    val previewType: AttachmentType?,
)
