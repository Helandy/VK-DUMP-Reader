package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.media

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import com.etozhesandy.redpanda.features.chat.model.MediaScrollPosition
import com.etozhesandy.redpanda.features.chat.presentation.chat.MediaScrollSlot

/**
 * A grid state that opens where [slot] left off and keeps writing back to it.
 *
 * `rememberSaveable` covers configuration changes on its own; the slot is what survives the chat
 * being rebuilt around another message, which destroys the back stack entry entirely.
 */
@Composable
fun rememberCachedGridState(slot: MediaScrollSlot): LazyGridState {
    val gridState = rememberSaveable(saver = LazyGridState.Saver) {
        val restored = slot.read()
        LazyGridState(restored.index, restored.offset)
    }
    LaunchedEffect(gridState, slot) {
        snapshotFlow {
            MediaScrollPosition(
                gridState.firstVisibleItemIndex,
                gridState.firstVisibleItemScrollOffset,
            )
        }.collect(slot::write)
    }
    return gridState
}
