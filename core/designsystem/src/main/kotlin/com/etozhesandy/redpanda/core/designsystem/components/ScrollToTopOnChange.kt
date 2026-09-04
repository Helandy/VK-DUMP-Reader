package com.etozhesandy.redpanda.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Runs [scroll] whenever [key] changes — but never on the first composition.
 *
 * Reordering a keyed lazy list keeps whichever item was on screen in view, so after a re-sort the
 * user is left somewhere in the middle of the new order instead of at its start. Skipping the
 * first run matters just as much: these screens restore a saved scroll position when they open,
 * and an unconditional effect would throw it away.
 */
@Composable
fun ScrollToTopOnChange(key: Any, scroll: suspend () -> Unit) {
    var previousKey by remember { mutableStateOf(key) }
    LaunchedEffect(key) {
        if (key != previousKey) {
            previousKey = key
            scroll()
        }
    }
}
