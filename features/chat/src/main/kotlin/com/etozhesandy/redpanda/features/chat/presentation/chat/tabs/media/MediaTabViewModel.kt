package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.media

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.common.mvi.sortPreference
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.model.naturalAscending
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import com.etozhesandy.redpanda.features.chat.domain.model.sortedBy
import com.etozhesandy.redpanda.features.chat.model.ChatArgs
import com.etozhesandy.redpanda.features.chat.model.ChatMediaTab
import com.etozhesandy.redpanda.features.chat.presentation.chat.ChatMediaScrollCache
import com.etozhesandy.redpanda.features.chat.presentation.chat.MediaScrollSlot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * The half of a media tab that doesn't depend on what it shows: its own ordering, the thumbnail
 * size from the settings, and the cell of the scroll cache it restores from.
 *
 * The ordering is per-tab: each subclass builds its own [sortPreference] over its own
 * [SavedStateHandle], so sorting the photos leaves the videos as the user left them.
 */
abstract class MediaTabViewModel(
    savedStateHandle: SavedStateHandle,
    settingsRepository: SettingsRepository,
    private val nav: INavigationManager,
    private val args: ChatArgs,
    scrollCache: ChatMediaScrollCache,
    tab: ChatMediaTab,
    attachments: Flow<List<Attachment>>,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<MediaTabState.State, MediaTabState.Event, MediaTabState.Effect>() {

    override fun createInitialState() = MediaTabState.State()

    /** Read once by the grid on its first composition, then written to as the user scrolls. */
    val scrollSlot: MediaScrollSlot = scrollCache.slot(args.dialogId, tab)

    private val sort = savedStateHandle.sortPreference<MediaSort>(
        keyPrefix = "media",
        defaults = settingsRepository.settings.map { it.defaultMediaSort to it.defaultMediaSortAscending },
        naturalAscending = { it.naturalAscending },
    )

    init {
        sort.flow
            .onEach { (sort, ascending) -> setState { copy(sort = sort, sortAscending = ascending) } }
            .launchIn(viewModelScope)

        settingsRepository.settings
            .onEach { settings -> setState { copy(imageWidthDp = settings.mediaImageWidthDp) } }
            .launchIn(viewModelScope)

        attachments.sortedBy(sort.flow, defaultDispatcher)
            .onEach { sorted -> setState { copy(attachments = sorted) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: MediaTabState.Event) {
        when (event) {
            is MediaTabState.Event.AttachmentClicked ->
                nav.navigate(Routes.PhotoViewer(args.dialogId, event.attachmentId))
            is MediaTabState.Event.SortSelected -> {
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
