package com.etozhesandy.redpanda.features.chat.presentation.chat.view

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The vertical space a tab list reserves for the floating [TabActionsRow]. */
val TabActionsHeight: Dp = 56.dp

/**
 * The floating controls a tab draws over its own content.
 *
 * Ordering belongs to the tab that is ordered, not to the chat's top bar: the bar is shared by
 * five independent tabs and would otherwise have to know which page the pager has settled on.
 * The tab list must reserve [TabActionsHeight] in its top `contentPadding` so its first item does
 * not appear beneath this overlay.
 */
@Composable
fun TabActionsRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Surface(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
