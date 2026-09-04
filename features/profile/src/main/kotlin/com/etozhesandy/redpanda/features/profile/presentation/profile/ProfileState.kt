package com.etozhesandy.redpanda.features.profile.presentation.profile

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.Friend
import com.etozhesandy.redpanda.core.model.Group
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.core.model.SavedPhoto
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object ProfileState {

    /** Only the first few entries of each preview list are kept in state — full lists live on their own screens. */
    data class State(
        val profile: Profile? = null,
        val friendsPreview: List<Friend> = emptyList(),
        val friendsCount: Int = 0,
        val groupsPreview: List<Group> = emptyList(),
        val groupsCount: Int = 0,
        val savedPhotosPreview: List<SavedPhoto> = emptyList(),
        val savedPhotosCount: Int = 0,
        val attachmentsPreview: List<Attachment> = emptyList(),
        val attachmentsCount: Int = 0,
        val mediaPreview: List<Attachment> = emptyList(),
        val mediaCount: Int = 0,
        val isLoading: Boolean = true,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackClicked : Event
        data object DialogsClicked : Event
        data object LinkClicked : Event
        data object FriendsAllClicked : Event
        data object GroupsAllClicked : Event
        data object SavedPhotosAllClicked : Event
        data object AttachmentsAllClicked : Event
        data object MediaAllClicked : Event
    }

    sealed interface Effect : UiEffect {
        /** The profile's own vk.com page — an outside link, not a destination of this app. */
        data class OpenLink(val url: String) : Effect
    }
}
