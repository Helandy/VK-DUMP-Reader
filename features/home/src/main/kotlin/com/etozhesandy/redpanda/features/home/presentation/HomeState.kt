package com.etozhesandy.redpanda.features.home.presentation

import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.core.update.domain.model.AppRelease

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object HomeState {

    data class State(
        val profiles: List<Profile> = emptyList(),
        val isLoading: Boolean = true,
        /** An import is already in flight; starting a second one breaks both. */
        val isImportRunning: Boolean = false,
        /**
         * Profiles whose erase is still running. Their rows leave the database early on, but the
         * on-disk data does not, so opening one of them would show a profile that is coming apart.
         */
        val deletingProfileIds: Set<String> = emptySet(),
        /**
         * A published release newer than the installed one, or null while the check is still
         * running, has failed, or has found nothing: the banner is the only thing that depends
         * on it, so «not yet known» and «nothing to show» are the same state here.
         */
        val availableUpdate: AppRelease? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object ImportClicked : Event
        data object SettingsClicked : Event
        data class ProfileClicked(val profileId: String) : Event
        data class DeleteProfileClicked(val profileId: String) : Event
        data object UpdateBannerClicked : Event
    }

    sealed interface Effect : UiEffect {
        /**
         * The release page opens in a browser rather than in-app: it is a GitHub page, not a
         * screen of this app, so it does not go through INavigationManager.
         */
        data class OpenUrl(val url: String) : Effect
    }
}
