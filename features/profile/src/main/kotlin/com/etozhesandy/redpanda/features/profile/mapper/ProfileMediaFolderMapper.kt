package com.etozhesandy.redpanda.features.profile.mapper

import com.etozhesandy.redpanda.features.profile.domain.model.ArchiveFolder
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaFolderSummary
import com.etozhesandy.redpanda.features.profile.utils.folderDisplayName

/** Ordered by the name the user actually sees, not by the raw path it was grouped under. */
fun List<ArchiveFolder>.toSummaries(): List<ProfileMediaFolderSummary> =
    map { folder ->
        ProfileMediaFolderSummary(
            path = folder.path,
            displayName = folderDisplayName(folder.path),
            previewPath = folder.previewPath,
            previewType = folder.previewType,
            count = folder.fileCount,
        )
    }.sortedBy { it.displayName }
