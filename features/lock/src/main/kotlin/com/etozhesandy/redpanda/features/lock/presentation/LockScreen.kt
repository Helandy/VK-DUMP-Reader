package com.etozhesandy.redpanda.features.lock.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.features.lock.R
import com.etozhesandy.redpanda.features.lock.presentation.handler.rememberBiometricLauncher
import com.etozhesandy.redpanda.features.lock.presentation.model.LockMode
import com.etozhesandy.redpanda.features.lock.presentation.utils.formatCountdown
import com.etozhesandy.redpanda.features.lock.presentation.view.PinDots
import com.etozhesandy.redpanda.features.lock.presentation.view.PinKeypad
import com.etozhesandy.redpanda.core.security.model.AppLockConfig

/**
 * The full-screen gate shown instead of the app content while it is locked. It has no top bar and
 * no way back: the only way out is a matching fingerprint or PIN.
 */
@Composable
fun LockScreen(
    state: LockState.State,
    onEvent: (LockState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val biometricLauncher = rememberBiometricLauncher(
        onSucceeded = { onEvent(LockState.Event.BiometricSucceeded) },
        onFailed = { onEvent(LockState.Event.BiometricFailed) },
        onError = { code -> onEvent(LockState.Event.BiometricErrored(code)) },
    )

    LaunchedEffect(state.biometricRequestId) {
        if (state.biometricRequestId > 0 && state.mode == LockMode.BIOMETRIC) biometricLauncher.launch()
    }

    LaunchedEffect(state.mode) {
        if (state.mode == LockMode.PIN) biometricLauncher.cancel()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = if (state.mode == LockMode.BIOMETRIC) Icons.Default.Fingerprint else Icons.Default.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.lock_title), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(
                if (state.mode == LockMode.BIOMETRIC) R.string.lock_touch_sensor else R.string.lock_enter_pin,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(24.dp))
        LockMessage(state = state)
        Spacer(modifier = Modifier.height(24.dp))

        if (state.mode == LockMode.PIN) {
            PinDots(
                filled = state.pin.length,
                length = AppLockConfig.PIN_LENGTH,
                isError = state.errorRes != null,
            )
            Spacer(modifier = Modifier.height(24.dp))
            PinKeypad(
                onDigit = { onEvent(LockState.Event.PinDigitEntered(it)) },
                onBackspace = { onEvent(LockState.Event.PinBackspacePressed) },
                enabled = state.isKeypadEnabled,
            )
            if (state.canUseBiometric) {
                TextButton(onClick = { onEvent(LockState.Event.UseBiometricClicked) }) {
                    Text(stringResource(R.string.lock_use_fingerprint))
                }
            }
        } else {
            TextButton(onClick = { onEvent(LockState.Event.UsePinClicked) }) {
                Text(stringResource(R.string.lock_use_pin))
            }
        }
    }
}

/** The countdown takes priority: while it runs, no other error is worth reading. */
@Composable
private fun LockMessage(state: LockState.State) {
    val text = when {
        state.lockoutRemainingMs > 0L ->
            stringResource(R.string.lock_locked_out, formatCountdown(state.lockoutRemainingMs))
        state.errorRes == null -> null
        state.errorArg == null -> stringResource(state.errorRes)
        else -> stringResource(state.errorRes, state.errorArg)
    }
    Text(
        text = text.orEmpty(),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
}
