package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.messages

import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/**
 * MVI-контракт таба «Сообщения».
 *
 * Самих сообщений в состоянии нет: `PagingData` — это поток, который Compose собирает сам, и
 * снимок такого потока был бы новым списком на каждой рекомпозиции.
 */
object MessagesTabState {

    /**
     * [favoriteIds] rather than a flag on each message: the messages arrive as `PagingData`, whose
     * pages are read once and never re-read, so the stars have to be watched separately.
     */
    data class State(
        val isReversed: Boolean = false,
        val favoriteIds: Set<String> = emptySet(),
    ) : UiState

    sealed interface Event : UiEvent {
        data class FavoriteToggled(val messageId: String, val isFavorite: Boolean) : Event
        data class AttachmentClicked(val attachmentId: String) : Event
        data class FileClicked(val url: String) : Event

        /** Flips the reading order, which re-anchors paging rather than reordering a loaded list. */
        data object ToggleOrderReversed : Event
    }

    sealed interface Effect : UiEffect {
        /** Documents live on VK's servers, so the only thing to do with one is hand it to the browser. */
        data class OpenExternally(val url: String) : Effect
    }
}
