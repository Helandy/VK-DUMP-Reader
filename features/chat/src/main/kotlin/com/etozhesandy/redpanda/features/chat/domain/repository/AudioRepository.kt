package com.etozhesandy.redpanda.features.chat.domain.repository

import com.etozhesandy.redpanda.features.chat.domain.model.PreparedAudio

interface AudioRepository {
    /** A local copy of the audio at [path] with its length, or null when it can't be had (a dead link). */
    suspend fun prepare(path: String): PreparedAudio?
}
