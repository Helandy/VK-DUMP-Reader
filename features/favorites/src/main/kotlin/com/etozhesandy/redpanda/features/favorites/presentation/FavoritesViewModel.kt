package com.etozhesandy.redpanda.features.favorites.presentation

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.favorites.domain.usecase.ObserveFavoritesUseCase
import com.etozhesandy.redpanda.features.favorites.model.FavoritesArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val nav: INavigationManager,
    args: FavoritesArgs,
    observeFavorites: ObserveFavoritesUseCase,
) : BaseViewModel<FavoritesState.State, FavoritesState.Event, FavoritesState.Effect>() {

    override fun createInitialState() = FavoritesState.State()

    init {
        observeFavorites(args.profileId)
            .onEach { messages -> setState { copy(messages = messages, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: FavoritesState.Event) {
        when (event) {
            is FavoritesState.Event.MessageClicked ->
                nav.navigate(Routes.Chat(event.message.dialogId, event.message.profileId, event.message.id))
            FavoritesState.Event.BackClicked -> nav.back()
        }
    }
}
