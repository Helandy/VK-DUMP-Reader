package com.etozhesandy.redpanda.features.lock.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.core.security.model.AppLockConfig
import com.etozhesandy.redpanda.features.lock.R
import com.etozhesandy.redpanda.features.lock.presentation.model.PinSetupStep
import com.etozhesandy.redpanda.features.lock.presentation.utils.formatCountdown
import com.etozhesandy.redpanda.features.lock.presentation.view.PinDots
import com.etozhesandy.redpanda.features.lock.presentation.view.PinKeypad

/** Sets, changes or removes the login PIN, one four-digit step at a time. */
@Composable
fun PinSetupScreen(
    state: PinSetupState.State,
    onEvent: (PinSetupState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseScreen(
        title = stringResource(R.string.pin_setup_title),
        modifier = modifier,
        onBack = { onEvent(PinSetupState.Event.BackClicked) },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(state.step.promptRes()),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            PinDots(
                filled = state.pin.length,
                length = AppLockConfig.PIN_LENGTH,
                isError = state.errorRes != null,
            )
            Spacer(modifier = Modifier.height(16.dp))
            PinSetupMessage(state = state)
            Spacer(modifier = Modifier.height(16.dp))
            PinKeypad(
                onDigit = { onEvent(PinSetupState.Event.PinDigitEntered(it)) },
                onBackspace = { onEvent(PinSetupState.Event.PinBackspacePressed) },
                enabled = state.isKeypadEnabled,
            )
        }
    }
}

/** The countdown takes priority: while it runs, no other error is worth reading. */
@Composable
private fun PinSetupMessage(state: PinSetupState.State) {
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

private fun PinSetupStep.promptRes(): Int = when (this) {
    PinSetupStep.CURRENT -> R.string.pin_setup_enter_current
    PinSetupStep.NEW -> R.string.pin_setup_enter_new
    PinSetupStep.CONFIRM -> R.string.pin_setup_confirm_new
}
