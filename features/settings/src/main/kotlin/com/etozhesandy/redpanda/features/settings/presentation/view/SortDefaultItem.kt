package com.etozhesandy.redpanda.features.settings.presentation.view

import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.components.SortMenu
import com.etozhesandy.redpanda.core.designsystem.components.SortOption
import com.etozhesandy.redpanda.features.settings.R

/**
 * A row that both shows the stored default and edits it, reusing the same [SortMenu] the sorting
 * screens use — including its "tap the active option to flip the direction" rule, which is why the
 * current direction is spelled out in words here.
 */
@Composable
fun <T> SortDefaultItem(
    title: String,
    options: List<SortOption<T>>,
    selected: T,
    ascending: Boolean,
    onSelect: (T) -> Unit,
) {
    val label = options.firstOrNull { it.key == selected }?.let { stringResource(it.labelRes) }.orEmpty()
    val direction = stringResource(
        if (ascending) R.string.sort_direction_ascending_lower else R.string.sort_direction_descending_lower,
    )
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(stringResource(R.string.sort_default_summary, label, direction))
        },
        trailingContent = {
            SortMenu(
                options = options,
                selected = selected,
                ascending = ascending,
                onSelect = onSelect,
                showDirection = true,
            )
        },
    )
}
