package com.etozhesandy.redpanda.features.dialogs.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.features.dialogs.R

@Composable
fun CategoryChips(
    categories: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return
    LazyRow(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelected(null) },
                label = { Text(stringResource(R.string.dialogs_category_all)) },
            )
        }
        items(categories) { category ->
            FilterChip(selected = selected == category, onClick = { onSelected(category) }, label = { Text(category) })
        }
    }
}
