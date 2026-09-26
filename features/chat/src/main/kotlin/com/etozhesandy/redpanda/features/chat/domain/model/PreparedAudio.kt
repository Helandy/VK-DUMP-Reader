package com.etozhesandy.redpanda.features.chat.domain.model

/**
 * An audio attachment ready to play: a file on the device and how long it runs.
 *
 * [localPath] rather than the source link, because a voice message's CDN serves it with no
 * length and no range support, and a player can neither show the length of such a stream nor seek
 * in it.
 */
data class PreparedAudio(
    val localPath: String,
    val durationMs: Long?,
)
