package com.etozhesandy.redpanda.features.chat.presentation.chat

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveDialogUseCase
import com.etozhesandy.redpanda.features.chat.mapper.toOrderOverride
import com.etozhesandy.redpanda.features.chat.model.ChatArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ChatTopBarViewModel @Inject constructor(
    private val nav: INavigationManager,
    private val args: ChatArgs,
    observeDialog: ObserveDialogUseCase,
) : BaseViewModel<ChatTopBarState.State, ChatTopBarState.Event, ChatTopBarState.Effect>() {

    override fun createInitialState() = ChatTopBarState.State()

    init {
        observeDialog(args.dialogId)
            .onEach { dialog -> setState { copy(dialog = dialog) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ChatTopBarState.Event) {
        when (event) {
            ChatTopBarState.Event.BackClicked -> nav.back()
            // The order travels as the route spelled it: null means nobody has picked one, and
            // reopening the chat from a result then falls back to the same setting it does now.
            ChatTopBarState.Event.SearchClicked -> nav.navigate(
                Routes.ChatSearch(
                    dialogId = args.dialogId,
                    profileId = args.profileId,
                    orderOverride = args.orderReversed.toOrderOverride(),
                ),
            )
        }
    }
}
