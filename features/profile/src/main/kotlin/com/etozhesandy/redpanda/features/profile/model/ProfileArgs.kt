package com.etozhesandy.redpanda.features.profile.model

/** What the profile overview screen was opened with, free of the navigation types that carried it. */
data class ProfileArgs(
    val profileId: String,
)
