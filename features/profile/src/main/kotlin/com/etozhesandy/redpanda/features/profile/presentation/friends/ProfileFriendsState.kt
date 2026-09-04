package com.etozhesandy.redpanda.features.profile.presentation.friends

import com.etozhesandy.redpanda.core.model.Friend
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object ProfileFriendsState {

    data class State(
        val friends: List<Friend> = emptyList(),
        val query: String = "",
        val isLoading: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data class QueryChanged(val query: String) : Event
        data object BackClicked : Event
        data class FriendClicked(val friend: Friend) : Event
    }

    sealed interface Effect : UiEffect {
        /** Only for links that leave the app; vk.com ones are a destination and go through
         * INavigationManager. */
        data class OpenLink(val url: String) : Effect
    }
}
