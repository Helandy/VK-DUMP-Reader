package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.files

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.common.mvi.sortPreference
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.model.naturalAscending
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import com.etozhesandy.redpanda.features.chat.domain.model.sortedBy
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveDialogFilesUseCase
import com.etozhesandy.redpanda.features.chat.model.ChatArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class FilesTabViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    settingsRepository: SettingsRepository,
    args: ChatArgs,
    observeDialogFiles: ObserveDialogFilesUseCase,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<FilesTabState.State, FilesTabState.Event, FilesTabState.Effect>() {

    override fun createInitialState() = FilesTabState.State()

    private val sort = savedStateHandle.sortPreference<MediaSort>(
        keyPrefix = "media",
        defaults = settingsRepository.settings.map { it.defaultMediaSort to it.defaultMediaSortAscending },
        naturalAscending = { it.naturalAscending },
    )

    init {
        sort.flow
            .onEach { (sort, ascending) -> setState { copy(sort = sort, sortAscending = ascending) } }
            .launchIn(viewModelScope)

        observeDialogFiles(args.dialogId).sortedBy(sort.flow, defaultDispatcher)
            .onEach { files -> setState { copy(attachments = files) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: FilesTabState.Event) {
        when (event) {
            is FilesTabState.Event.FileClicked ->
                setEffect { FilesTabState.Effect.OpenExternally(event.url) }
            is FilesTabState.Event.SortSelected -> {
                val ascending = sort.select(
                    picked = event.sort,
                    current = currentState.sort,
                    currentAscending = currentState.sortAscending,
                )
                setState { copy(sort = event.sort, sortAscending = ascending) }
            }
        }
    }
}
