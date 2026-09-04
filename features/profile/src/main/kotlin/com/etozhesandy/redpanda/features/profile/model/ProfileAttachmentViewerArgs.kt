package com.etozhesandy.redpanda.features.profile.model

/** What the attachment viewer screen was opened with, free of the navigation types that carried it. */
data class ProfileAttachmentViewerArgs(
    val profileId: String,
    val startAttachmentId: String,
)
