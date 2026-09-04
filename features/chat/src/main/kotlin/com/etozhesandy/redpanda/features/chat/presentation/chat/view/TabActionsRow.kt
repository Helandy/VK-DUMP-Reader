package com.etozhesandy.redpanda.features.chat.presentation.chat.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The strip of controls a tab draws above its own list.
 *
 * Ordering belongs to the tab that is ordered, not to the chat's top bar: the bar is shared by
 * five independent tabs and would otherwise have to know which page the pager has settled on.
 */
@Composable
fun TabActionsRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
