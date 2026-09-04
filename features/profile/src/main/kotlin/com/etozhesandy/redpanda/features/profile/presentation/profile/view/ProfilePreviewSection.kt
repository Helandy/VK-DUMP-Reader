package com.etozhesandy.redpanda.features.profile.presentation.profile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.features.profile.R

/**
 * Shell every profile preview strip shares: a header with the total and an "all" button, over a
 * horizontal row of items. An empty section draws nothing at all, so callers can pass their slice
 * unconditionally.
 */
@Composable
fun ProfilePreviewSection(
    title: String,
    count: Int,
    onAllClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: LazyListScope.() -> Unit,
) {
    if (count == 0) return
    Column(modifier = modifier) {
        PreviewSectionHeader(title = title, count = count, onAllClick = onAllClick)
        LazyRow(
            contentPadding = PreviewContentPadding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun PreviewSectionHeader(title: String, count: Int, onAllClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onAllClick) { Text(stringResource(R.string.profile_show_all)) }
    }
}

private val PreviewContentPadding = PaddingValues(horizontal = 16.dp)
