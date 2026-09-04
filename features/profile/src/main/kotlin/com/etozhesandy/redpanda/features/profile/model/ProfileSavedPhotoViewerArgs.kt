package com.etozhesandy.redpanda.features.profile.model

/** What the saved photo viewer screen was opened with, free of the navigation types that carried it. */
data class ProfileSavedPhotoViewerArgs(
    val profileId: String,
    val startPhotoId: String,
)
