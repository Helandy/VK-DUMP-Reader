package com.etozhesandy.redpanda.core.designsystem.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.R

/** One entry of a [SortMenu]: the key the caller sorts by, plus its user-facing label. */
data class SortOption<T>(val key: T, @StringRes val labelRes: Int)

/**
 * Top-bar control that picks both what to sort by and which direction, in one menu.
 *
 * Picking the option that's already selected is how the direction is flipped — the caller decides
 * that in [onSelect] (it gets the tapped key and compares it to [selected]), so this stays a dumb
 * control. The active option carries an arrow showing the current direction.
 *
 * Set [showDirection] where the button is the only thing on screen telling the direction apart
 * (the settings rows); a screen that already shows the sorted content keeps the neutral icon.
 */
@Composable
fun <T> SortMenu(
    options: List<SortOption<T>>,
    selected: T,
    ascending: Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    showDirection: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }, modifier = modifier) {
        if (showDirection) {
            Icon(
                imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = stringResource(
                    if (ascending) R.string.action_sort_ascending else R.string.action_sort_descending,
                ),
            )
        } else {
            Icon(Icons.Default.Sort, contentDescription = stringResource(R.string.action_sort))
        }
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            val isSelected = option.key == selected
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(option.labelRes),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                },
                trailingIcon = {
                    if (isSelected) {
                        Icon(
                            imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = stringResource(
                                if (ascending) {
                                    R.string.sort_direction_ascending
                                } else {
                                    R.string.sort_direction_descending
                                },
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onSelect(option.key)
                },
            )
        }
    }
}
