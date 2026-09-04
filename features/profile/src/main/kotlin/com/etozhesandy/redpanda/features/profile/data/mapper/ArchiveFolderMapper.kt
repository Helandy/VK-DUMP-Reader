package com.etozhesandy.redpanda.features.profile.data.mapper

import com.etozhesandy.redpanda.core.storage.db.attachment.ArchiveFolderSummary
import com.etozhesandy.redpanda.features.profile.domain.model.ArchiveFolder

fun ArchiveFolderSummary.toDomain(): ArchiveFolder = ArchiveFolder(
    path = folder,
    fileCount = fileCount,
    previewPath = previewPath,
    previewType = previewType,
)
