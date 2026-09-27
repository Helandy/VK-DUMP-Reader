package com.etozhesandy.redpanda.features.settings.presentation.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.features.settings.R
import com.etozhesandy.redpanda.features.settings.presentation.SettingsState

/**
 * The diagnostics block of the settings screen: the opt-in app log and the actions to send or
 * delete what it recorded. The actions stay available after recording is turned off, so a log
 * captured earlier can still be sent.
 */
@Composable
fun DiagnosticsSection(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_diagnostics),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_logging)) },
            supportingContent = { Text(stringResource(R.string.settings_logging_description)) },
            trailingContent = {
                Switch(
                    checked = state.loggingEnabled,
                    onCheckedChange = { onEvent(SettingsState.Event.LoggingToggled(it)) },
                )
            },
            modifier = Modifier.clickable { onEvent(SettingsState.Event.LoggingToggled(!state.loggingEnabled)) },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_log_share)) },
            modifier = Modifier.clickable { onEvent(SettingsState.Event.ShareLogClicked) },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_log_clear)) },
            modifier = Modifier.clickable { onEvent(SettingsState.Event.ClearLogClicked) },
        )
    }
}
