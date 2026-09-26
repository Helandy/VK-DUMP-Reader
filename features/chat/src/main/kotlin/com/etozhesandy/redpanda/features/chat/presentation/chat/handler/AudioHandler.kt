package com.etozhesandy.redpanda.features.chat.presentation.chat.handler

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.chat.domain.model.PreparedAudio
import com.etozhesandy.redpanda.features.chat.domain.usecase.PrepareAudioUseCase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The audio rows a screen has shown, prepared for playback, keyed by attachment id.
 *
 * Asked for row by row as they scroll into view rather than for the whole dialog up front: each is
 * a download, and a long chat holds thousands of voice messages nobody scrolls to.
 */
class AudioHandler @Inject constructor(
    private val prepareAudio: PrepareAudioUseCase,
) {
    private val _prepared = MutableStateFlow<Map<String, PreparedAudio>>(emptyMap())
    val prepared: StateFlow<Map<String, PreparedAudio>> = _prepared.asStateFlow()

    private val requested = mutableSetOf<String>()

    fun request(scope: CoroutineScope, attachment: Attachment) {
        if (!requested.add(attachment.id)) return
        scope.launch {
            val audio = prepareAudio(attachment) ?: return@launch
            _prepared.update { it + (attachment.id to audio) }
        }
    }
}
