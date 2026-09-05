package com.etozhesandy.redpanda.features.chat.presentation.globalsearch

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.common.mvi.sortPreference
import com.etozhesandy.redpanda.core.model.MessageSort
import com.etozhesandy.redpanda.core.model.naturalAscending
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import com.etozhesandy.redpanda.features.chat.domain.model.sortedBy
import com.etozhesandy.redpanda.features.chat.domain.usecase.SearchAllDialogsUseCase
import com.etozhesandy.redpanda.features.chat.model.GlobalSearchArgs
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
class GlobalSearchViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val nav: INavigationManager,
    private val args: GlobalSearchArgs,
    searchAllDialogs: SearchAllDialogsUseCase,
    settingsRepository: SettingsRepository,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<GlobalSearchState.State, GlobalSearchState.Event, GlobalSearchState.Effect>() {

    override fun createInitialState() = GlobalSearchState.State(query = rawQuery.value)

    private val rawQuery = MutableStateFlow(savedStateHandle.get<String>(KEY_QUERY).orEmpty())
    private val sort = savedStateHandle.sortPreference<MessageSort>(
        keyPrefix = "global_search",
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
                if (raw.isBlank()) flowOf(emptyList()) else searchAllDialogs(args.profileId, raw)
            }
            .combine(sort.flow) { results, (sort, ascending) -> results.sortedBy(sort, ascending) }
            .flowOn(defaultDispatcher)
            .onEach { results -> setState { copy(results = results) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: GlobalSearchState.Event) {
        when (event) {
            is GlobalSearchState.Event.QueryChanged -> {
                savedStateHandle[KEY_QUERY] = event.query
                setState { copy(query = event.query) }
                rawQuery.value = event.query
            }
            // Unlike the in-dialog search this screen isn't standing on the chat it opens, so it
            // stays on the stack: going back returns to the results instead of leaving the user in
            // a chat they only glanced at.
            is GlobalSearchState.Event.ResultClicked -> nav.navigate(
                Routes.Chat(
                    dialogId = event.result.message.dialogId,
                    profileId = args.profileId,
                    scrollToMessageId = event.result.message.id,
                ),
            )
            GlobalSearchState.Event.BackClicked -> nav.back()
            is GlobalSearchState.Event.SortSelected -> {
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
        const val KEY_QUERY = "global_search_query"

        /** Long enough that typing a word doesn't run a query per keystroke. */
        const val SEARCH_DEBOUNCE_MS = 250L
    }
}
