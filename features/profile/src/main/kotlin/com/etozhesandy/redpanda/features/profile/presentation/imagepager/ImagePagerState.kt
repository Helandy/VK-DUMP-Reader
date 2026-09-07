package com.etozhesandy.redpanda.features.profile.presentation.imagepager

import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** Shared state for a full-screen swipeable image viewer, backed by either saved photos or attachments. */

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object ImagePagerState {

    /** The message a pictured item was sent in, and the dialog to open at it. */
    data class MessageAnchor(
        val dialogId: String,
        val profileId: String,
        val messageId: String,
    )

    /**
     * [anchor] is null when nothing links the image back to a conversation: a saved photo never
     * was in one, and VK's flat gallery export carries no message of its own.
     */
    data class Page(val url: String, val anchor: MessageAnchor? = null)

    data class State(
        val pages: List<Page> = emptyList(),
        val startIndex: Int = 0,
        val isLoading: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackClicked : Event
        data class DownloadClicked(val url: String) : Event
        data class JumpToMessageClicked(val anchor: MessageAnchor) : Event
    }

    sealed interface Effect : UiEffect {
        /** [savedTo] is the folder under Download/ the file landed in; null means it did not. */
        data class DownloadFinished(val savedTo: String?) : Effect
    }
}
