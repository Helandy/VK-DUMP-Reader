package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.messages

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.navigation.manager.PopUpTo
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import com.etozhesandy.redpanda.features.chat.domain.usecase.GetAttachmentsForMessageUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.GetMessagePositionUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveMessagesUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ToggleFavoriteUseCase
import com.etozhesandy.redpanda.features.chat.mapper.toOrderOverride
import com.etozhesandy.redpanda.features.chat.mapper.toUi
import com.etozhesandy.redpanda.features.chat.model.ChatArgs
import com.etozhesandy.redpanda.features.chat.model.MessageUi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MessagesTabViewModel @Inject constructor(
    private val nav: INavigationManager,
    private val args: ChatArgs,
    private val getAttachments: GetAttachmentsForMessageUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    observeMessages: ObserveMessagesUseCase,
    getMessagePosition: GetMessagePositionUseCase,
    settingsRepository: SettingsRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<MessagesTabState.State, MessagesTabState.Event, MessagesTabState.Effect>() {

    override fun createInitialState() = MessagesTabState.State()

    /**
     * Resolved once for the whole tab so the paging source and the order toggle can't end up
     * disagreeing if the setting changes while the chat is open.
     */
    private val reversed: Deferred<Boolean> = viewModelScope.async {
        args.orderReversed ?: settingsRepository.settings.first().defaultChatReversed
    }

    val pagingMessages: Flow<PagingData<MessageUi>> = flow {
        val isReversed = reversed.await()
        val position = args.scrollToMessageId?.let { messageId ->
            getMessagePosition(args.dialogId, messageId, isReversed)
        }
        emit(isReversed to position)
    }
        .flatMapLatest { (isReversed, initialPosition) ->
            observeMessages(args.dialogId, isReversed, initialPosition)
        }
        .map { pagingData -> pagingData.map { message -> message.toUi(getAttachments) } }
        // Joining each page with its attachments is dispatched from wherever this is collected,
        // and `cachedIn` collects in `viewModelScope` — i.e. on the main thread without this.
        .flowOn(defaultDispatcher)
        .cachedIn(viewModelScope)

    init {
        launchSafe {
            val isReversed = reversed.await()
            setState { copy(isReversed = isReversed) }
        }
    }

    override fun onEvent(event: MessagesTabState.Event) {
        when (event) {
            is MessagesTabState.Event.FavoriteToggled ->
                launchSafe { toggleFavorite(event.messageId, event.isFavorite) }
            is MessagesTabState.Event.AttachmentClicked ->
                nav.navigate(Routes.PhotoViewer(args.dialogId, event.attachmentId))
            is MessagesTabState.Event.FileClicked ->
                setEffect { MessagesTabState.Effect.OpenExternally(event.url) }
            MessagesTabState.Event.ToggleOrderReversed -> recreateChat()
        }
    }

    /**
     * Reopens the chat from scratch: the paging source is built around one order and one anchor
     * message, so changing either means a new screen rather than a new state.
     */
    private fun recreateChat() {
        nav.navigate(
            Routes.Chat(
                dialogId = args.dialogId,
                profileId = args.profileId,
                orderOverride = (!currentState.isReversed).toOrderOverride(),
            ),
            PopUpTo(Routes.Chat::class, inclusive = true),
        )
    }
}
