package com.etozhesandy.redpanda.features.importer.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.features.importer.R
import com.etozhesandy.redpanda.features.importer.presentation.utils.rememberArchivePicker
import com.etozhesandy.redpanda.features.importer.presentation.utils.rememberDirectoryPicker
import com.etozhesandy.redpanda.features.importer.presentation.view.ImportSourceCard
import com.etozhesandy.redpanda.features.importer.presentation.view.NotificationPermissionGate

/** [state] is [ImportState.State], which carries nothing — the screen is only a source picker. */
@Composable
fun ImportScreen(
    state: ImportState.State,
    onEvent: (ImportState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickArchive = rememberArchivePicker { onEvent(ImportState.Event.SourcePicked(it)) }
    val pickDirectory = rememberDirectoryPicker { onEvent(ImportState.Event.SourcePicked(it)) }

    BaseScreen(
        title = stringResource(R.string.import_title),
        modifier = modifier,
        onBack = { onEvent(ImportState.Event.BackClicked) },
    ) {
        NotificationPermissionGate()

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.import_headline),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            ImportSourceCard(
                icon = Icons.Default.FolderZip,
                title = stringResource(R.string.import_pick_archive),
                description = stringResource(R.string.import_pick_archive_description),
                onClick = pickArchive,
            )
            ImportSourceCard(
                icon = Icons.Default.FolderOpen,
                title = stringResource(R.string.import_pick_folder),
                description = stringResource(R.string.import_pick_folder_description),
                onClick = pickDirectory,
            )
            Text(
                text = stringResource(R.string.import_background_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
