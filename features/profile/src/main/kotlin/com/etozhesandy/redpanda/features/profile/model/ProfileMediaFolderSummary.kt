package com.etozhesandy.redpanda.features.profile.model

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.AttachmentType

/** One archive folder that orphan-scanned files were grouped under. [path] is the raw relative
 * path used to filter/navigate (matches [Attachment.sourceFolder]); [displayName] is just its
 * last path segment, shown in the UI. The preview is carried as a path and a kind rather than a
 * whole [Attachment] because the database summarises the folder without loading its files. */
data class ProfileMediaFolderSummary(
    val path: String,
    val displayName: String,
    val previewPath: String?,
    val previewType: AttachmentType?,
    val count: Int,
)
