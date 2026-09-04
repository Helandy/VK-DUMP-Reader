package com.etozhesandy.redpanda.features.profile.model

/** What the media viewer screen was opened with, free of the navigation types that carried it. */
data class ProfileMediaViewerArgs(
    val profileId: String,
    val folder: String,
    val startAttachmentId: String,
)
