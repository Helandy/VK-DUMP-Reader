package com.etozhesandy.redpanda.features.chat.model

/** Where the loaded track is, for the one audio row the player is on. */
data class AudioProgress(
    val positionMs: Long,
    val durationMs: Long,
)
