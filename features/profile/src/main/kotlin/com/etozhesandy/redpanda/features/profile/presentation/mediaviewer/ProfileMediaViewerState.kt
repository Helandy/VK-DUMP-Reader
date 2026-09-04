package com.etozhesandy.redpanda.features.profile.presentation.mediaviewer

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object ProfileMediaViewerState {

    /** Full-screen swipeable photo/video viewer for a profile's media (unlike [ImagePagerState], keeps
     * [Attachment.type] so video entries can be played instead of rendered as a static image). */
    data class State(
        val attachments: List<Attachment> = emptyList(),
        val startIndex: Int = 0,
        val isLoading: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackClicked : Event
        data class DownloadClicked(val source: String) : Event
    }

    sealed interface Effect : UiEffect {
        /** [savedTo] is the folder under Download/ the file landed in; null means it did not. */
        data class DownloadFinished(val savedTo: String?) : Effect
    }
}
