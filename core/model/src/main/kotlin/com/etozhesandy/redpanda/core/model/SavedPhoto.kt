package com.etozhesandy.redpanda.core.model

/** One entry in the archive owner's saved-photos album. */
data class SavedPhoto(
    val id: String,
    val profileId: String,
    val url: String,
    val timestampEpoch: Long,
)
