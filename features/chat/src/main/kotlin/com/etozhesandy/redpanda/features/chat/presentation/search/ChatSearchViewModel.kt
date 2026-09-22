package com.etozhesandy.redpanda.features.chat.presentation.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class ChatSearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val nav: INavigationManager,
    private val args: ChatSearchArgs,
    private val cache: ChatSearchCache,
    private val searchMessages: SearchMessagesUseCase,
    settingsRepository: SettingsRepository,
) : BaseViewModel<ChatSearchState.State, ChatSearchState.Event, ChatSearchState.Effect>() {

    private val rawQuery = savedStateHandle.get<String>(KEY_QUERY) ?: cache.query(args.dialogId).orEmpty()
    private val sort = savedStateHandle.sortPreference<MessageSort>(
        keyPrefix = "search",
        defaults = settingsRepository.settings.map { it.defaultSearchSort to it.defaultSearchSortAscending },
        naturalAscending = { it.naturalAscending },
        memory = cache.sortMemory(args.dialogId),
    )
    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null

    override fun createInitialState() = ChatSearchState.State(
        query = rawQuery,
        isSearching = rawQuery.trim().length >= MIN_QUERY_LENGTH,
    )

    init {
        sort.flow
            .onEach { (sort, ascending) -> setState { copy(sort = sort, sortAscending = ascending) } }
            .launchIn(viewModelScope)
        if (rawQuery.trim().length >= MIN_QUERY_LENGTH) startSearch(rawQuery)
    }

    override fun onEvent(event: ChatSearchState.Event) {
        when (event) {
            is ChatSearchState.Event.QueryChanged -> {
                savedStateHandle[KEY_QUERY] = event.query
                cache.setQuery(args.dialogId, event.query)
                searchJob?.cancel()
                loadMoreJob?.cancel()
                val searchable = event.query.trim().length >= MIN_QUERY_LENGTH
                setState {
                    copy(
                        query = event.query,
                        results = emptyList(),
                        isSearching = searchable,
                        isLoadingMore = false,
                        hasMoreResults = false,
                    )
                }
                if (searchable) startSearch(event.query)
            }
            ChatSearchState.Event.LoadMore -> loadMore()
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
                setState {
                    copy(sort = event.sort, sortAscending = ascending, results = results.sortedBy(event.sort, ascending))
                }
            }
        }
    }

    private fun startSearch(query: String) {
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            val page = searchMessages.searchPage(args.profileId, query, args.dialogId, PAGE_SIZE, 0)
            setState {
                copy(
                    results = page.sortedBy(sort, sortAscending),
                    isSearching = false,
                    hasMoreResults = page.size == PAGE_SIZE,
                )
            }
        }
    }

    private fun loadMore() {
        if (currentState.isSearching || currentState.isLoadingMore || !currentState.hasMoreResults) return
        if (currentState.query.trim().length < MIN_QUERY_LENGTH) return
        val query = currentState.query
        val offset = currentState.results.size
        loadMoreJob = viewModelScope.launch {
            setState { copy(isLoadingMore = true) }
            val page = searchMessages.searchPage(args.profileId, query, args.dialogId, PAGE_SIZE, offset)
            setState {
                copy(
                    results = (results + page).sortedBy(sort, sortAscending),
                    isLoadingMore = false,
                    hasMoreResults = page.size == PAGE_SIZE,
                )
            }
        }
    }

    private companion object {
        const val KEY_QUERY = "search_query"
        const val SEARCH_DEBOUNCE_MS = 250L
        const val MIN_QUERY_LENGTH = 3
        const val PAGE_SIZE = 50
    }
}
