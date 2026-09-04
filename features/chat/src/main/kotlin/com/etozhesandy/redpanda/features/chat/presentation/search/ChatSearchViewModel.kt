package com.etozhesandy.redpanda.features.chat.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.common.mvi.sortPreference
import com.etozhesandy.redpanda.core.model.MessageSort
import com.etozhesandy.redpanda.core.model.naturalAscending
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.navigation.manager.PopUpTo
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import com.etozhesandy.redpanda.features.chat.domain.model.sortedBy
import com.etozhesandy.redpanda.features.chat.domain.usecase.SearchMessagesUseCase
import com.etozhesandy.redpanda.features.chat.model.ChatSearchArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ChatSearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val nav: INavigationManager,
    private val args: ChatSearchArgs,
    searchMessages: SearchMessagesUseCase,
    settingsRepository: SettingsRepository,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<ChatSearchState.State, ChatSearchState.Event, ChatSearchState.Effect>() {

    override fun createInitialState() = ChatSearchState.State(query = rawQuery.value)

    private val rawQuery = MutableStateFlow(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
    private val sort = savedStateHandle.sortPreference<MessageSort>(
        keyPrefix = "search",
        defaults = settingsRepository.settings.map { it.defaultSearchSort to it.defaultSearchSortAscending },
        naturalAscending = { it.naturalAscending },
    )

    init {
        sort.flow
            .onEach { (sort, ascending) -> setState { copy(sort = sort, sortAscending = ascending) } }
            .launchIn(viewModelScope)

        rawQuery
            .debounce(SEARCH_DEBOUNCE_MS)
            .flatMapLatest { raw ->
                if (raw.isBlank()) flowOf(emptyList()) else searchMessages(args.profileId, raw, args.dialogId)
            }
            .combine(sort.flow) { messages, (sort, ascending) -> messages.sortedBy(sort, ascending) }
            .flowOn(defaultDispatcher)
            .onEach { results -> setState { copy(results = results) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ChatSearchState.Event) {
        when (event) {
            is ChatSearchState.Event.QueryChanged -> {
                savedStateHandle[KEY_QUERY] = event.query
                setState { copy(query = event.query) }
                rawQuery.value = event.query
            }
            // Opening a result rebuilds the chat around that message, which takes the search
            // screen off the stack with it — the order the user was reading in is handed back
            // exactly as it arrived.
            is ChatSearchState.Event.ResultClicked -> nav.navigate(
                Routes.Chat(
                    dialogId = args.dialogId,
                    profileId = args.profileId,
                    scrollToMessageId = event.message.id,
                    orderOverride = args.orderOverride,
                ),
                PopUpTo(Routes.Chat::class, inclusive = true),
            )
            ChatSearchState.Event.BackClicked -> nav.back()
            is ChatSearchState.Event.SortSelected -> {
                val ascending = sort.select(
                    picked = event.sort,
                    current = currentState.sort,
                    currentAscending = currentState.sortAscending,
                )
                setState { copy(sort = event.sort, sortAscending = ascending) }
            }
        }
    }

    private companion object {
        const val KEY_QUERY = "search_query"

        /** Long enough that typing a word doesn't run a query per keystroke. */
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
