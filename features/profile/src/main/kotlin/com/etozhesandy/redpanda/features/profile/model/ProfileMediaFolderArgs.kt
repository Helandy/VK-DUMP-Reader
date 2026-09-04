package com.etozhesandy.redpanda.features.profile.model

/** What one media folder screen was opened with, free of the navigation types that carried it. */
data class ProfileMediaFolderArgs(
    val profileId: String,
    val folder: String,
)
