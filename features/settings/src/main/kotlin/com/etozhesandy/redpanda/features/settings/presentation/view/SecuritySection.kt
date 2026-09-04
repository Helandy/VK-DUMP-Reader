package com.etozhesandy.redpanda.features.settings.presentation.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.security.model.AppLockConfig
import com.etozhesandy.redpanda.core.security.model.BiometricAvailability
import com.etozhesandy.redpanda.features.settings.R
import com.etozhesandy.redpanda.features.settings.presentation.SettingsState

/**
 * The login-protection block of the settings screen. The fingerprint and timeout rows only appear
 * once a PIN exists, because both are meaningless without one.
 */
@Composable
fun SecuritySection(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_security),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_app_lock)) },
            supportingContent = {
                Text(
                    if (state.appLockEnabled) {
                        stringResource(R.string.settings_app_lock_on)
                    } else {
                        stringResource(R.string.settings_app_lock_off)
                    },
                )
            },
            trailingContent = {
                Switch(
                    checked = state.appLockEnabled,
                    onCheckedChange = { onEvent(SettingsState.Event.AppLockToggled(it)) },
                )
            },
        )

        if (state.appLockEnabled) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_change_pin)) },
                modifier = Modifier.clickable { onEvent(SettingsState.Event.ChangePinClicked) },
            )
            BiometricItem(state = state, onEvent = onEvent)
            LockTimeoutSlider(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun BiometricItem(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
) {
    val isAvailable = state.biometricAvailability == BiometricAvailability.AVAILABLE
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_biometric)) },
        supportingContent = {
            Text(
                stringResource(
                    when {
                        state.biometricAvailability == BiometricAvailability.NOT_ENROLLED ->
                            R.string.settings_biometric_not_enrolled
                        !isAvailable -> R.string.settings_biometric_no_hardware
                        state.biometricEnabled -> R.string.settings_biometric_on
                        else -> R.string.settings_biometric_off
                    },
                ),
            )
        },
        trailingContent = {
            Switch(
                checked = state.biometricEnabled && isAvailable,
                enabled = isAvailable,
                onCheckedChange = { onEvent(SettingsState.Event.BiometricToggled(it)) },
            )
        },
    )
}

@Composable
private fun LockTimeoutSlider(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
) {
    var sliderValue by remember(state.lockTimeoutSeconds) {
        mutableFloatStateOf(state.lockTimeoutSeconds.toFloat())
    }
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_lock_timeout)) },
        supportingContent = {
            Text(
                if (sliderValue.toInt() == 0) {
                    stringResource(R.string.settings_lock_timeout_immediately)
                } else {
                    stringResource(R.string.value_seconds, sliderValue.toInt())
                },
            )
        },
    )
    Slider(
        value = sliderValue,
        onValueChange = { sliderValue = it },
        onValueChangeFinished = { onEvent(SettingsState.Event.LockTimeoutChanged(sliderValue.toInt())) },
        valueRange = AppLockConfig.TIMEOUT_MIN_SECONDS.toFloat()..AppLockConfig.TIMEOUT_MAX_SECONDS.toFloat(),
        steps = (AppLockConfig.TIMEOUT_MAX_SECONDS - AppLockConfig.TIMEOUT_MIN_SECONDS) /
            AppLockConfig.TIMEOUT_STEP_SECONDS - 1,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    )
}
