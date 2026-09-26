package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.chat.domain.model.PreparedAudio
import com.etozhesandy.redpanda.features.chat.domain.repository.AudioRepository
import javax.inject.Inject

/**
 * Gets an audio attachment ready to play and finds out how long it is. No export records the
 * length of a voice message it only links to, so both come from fetching the audio itself; null
 * when there is nothing to fetch — no path, or a link the source has since removed.
 */
class PrepareAudioUseCase @Inject constructor(
    private val repository: AudioRepository,
) {
    suspend operator fun invoke(attachment: Attachment): PreparedAudio? =
        attachment.path.takeIf { it.isNotBlank() }?.let { repository.prepare(it) }
}
