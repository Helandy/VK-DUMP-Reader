package com.etozhesandy.redpanda.core.designsystem.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SubdirectoryArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.R

/**
 * Toolbar action that opens the dialog the shown media was sent in.
 *
 * Draws nothing when [enabled] is false: not every attachment can be traced back to a message —
 * gallery exports are flat and unlinked — and an action that would land nowhere is worse than no
 * action at all.
 */
@Composable
fun GoToMessageIconButton(enabled: Boolean, onClick: () -> Unit) {
    if (!enabled) return
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.SubdirectoryArrowLeft,
            contentDescription = stringResource(R.string.action_go_to_message),
            tint = Color.White,
        )
    }
}
