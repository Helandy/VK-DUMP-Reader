package com.etozhesandy.redpanda.core.designsystem.components

import androidx.compose.runtime.Composable

/**
 * The "spinner, then placeholder, then the list" progression every listing screen shows.
 *
 * [isEmpty] is only consulted once loading is over, so a list that is merely still loading does
 * not flash the empty-state text on its way in.
 */
@Composable
fun LoadableContent(
    isLoading: Boolean,
    isEmpty: Boolean,
    emptyText: String,
    content: @Composable () -> Unit,
) {
    when {
        isLoading -> LoadingState()
        isEmpty -> EmptyState(text = emptyText)
        else -> content()
    }
}
