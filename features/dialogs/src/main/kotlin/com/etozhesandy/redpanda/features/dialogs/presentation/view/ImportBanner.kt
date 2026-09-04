package com.etozhesandy.redpanda.features.dialogs.presentation.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.model.ImportProgress
import com.etozhesandy.redpanda.features.dialogs.R

/**
 * Live import status. The dialog counters only exist while the import is running in this process
 * (see `ImportProgressStore`), so a spinner and a plain caption stand in whenever they are absent —
 * on a cold start into an import already in flight, for instance.
 */
@Composable
fun ImportBanner(progress: ImportProgress?, modifier: Modifier = Modifier) {
    val total = progress?.dialogsTotal ?: 0
    val done = progress?.dialogsDone ?: 0
    Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    text = if (total > 0) {
                        stringResource(R.string.dialogs_import_banner_progress, done, total)
                    } else {
                        stringResource(R.string.dialogs_import_banner_indeterminate)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            if (total > 0) {
                LinearProgressIndicator(
                    progress = { done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}
