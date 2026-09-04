package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.media

import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.settings.AppSettings

/** MVI-контракт таба-сетки. Общий для «Фото» и «Видео» — они различаются только источником данных. */
object MediaTabState {

    data class State(
        val attachments: List<Attachment> = emptyList(),
        val imageWidthDp: Int = AppSettings.DEFAULT_MEDIA_IMAGE_WIDTH_DP,
        val sort: MediaSort = MediaSort.DATE,
        val sortAscending: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data class AttachmentClicked(val attachmentId: String) : Event

        /** Picking the sort that's already active flips the direction instead of changing the key. */
        data class SortSelected(val sort: MediaSort) : Event
    }

    sealed interface Effect : UiEffect
}
