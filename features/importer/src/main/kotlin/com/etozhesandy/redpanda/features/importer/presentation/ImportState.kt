package com.etozhesandy.redpanda.features.importer.presentation

import com.etozhesandy.redpanda.core.archive.source.ArchiveSource
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object ImportState {

    /** The import itself runs as background work (see `ProfileImportScheduler`), so this screen is
     * just a source picker — nothing to track once the pick has been dispatched. */
    data object State : UiState

    sealed interface Event : UiEvent {
        data class SourcePicked(val source: ArchiveSource) : Event
        data object BackClicked : Event
    }

    /** No one-off effects: the only outward action is navigation, and that goes through
     * INavigationManager. */
    sealed interface Effect : UiEffect
}
