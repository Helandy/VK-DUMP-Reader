package com.etozhesandy.redpanda.features.lock.presentation.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.features.lock.R

/**
 * The 1-9 / backspace-0 digit pad shared by the lock screen and the PIN setup screen.
 *
 * [enabled] is false while a lockout penalty is running, so the pad stays visible but inert.
 */
@Composable
fun PinKeypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DIGIT_ROWS.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { digit ->
                    KeypadKey(enabled = enabled, onClick = { onDigit(digit) }) {
                        Text(text = digit.toString(), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(modifier = Modifier.size(KEY_SIZE))
            KeypadKey(enabled = enabled, onClick = { onDigit(0) }) {
                Text(text = "0", style = MaterialTheme.typography.headlineSmall)
            }
            KeypadKey(enabled = enabled, onClick = onBackspace) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = stringResource(R.string.pin_delete),
                )
            }
        }
    }
}

@Composable
private fun KeypadKey(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(KEY_SIZE),
    ) {
        content()
    }
}

private val KEY_SIZE = 72.dp

private val DIGIT_ROWS = listOf(
    listOf(1, 2, 3),
    listOf(4, 5, 6),
    listOf(7, 8, 9),
)
