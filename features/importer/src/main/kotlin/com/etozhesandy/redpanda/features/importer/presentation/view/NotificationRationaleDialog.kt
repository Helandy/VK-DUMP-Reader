package com.etozhesandy.redpanda.features.importer.presentation.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.features.importer.R

/**
 * The system permission sheet says nothing about what the notification is for, so the reason comes
 * first: the import runs in a background worker and the notification is the only place its progress
 * and completion are visible once the screen is left.
 */
@Composable
fun NotificationRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notification_rationale_title)) },
        text = {
            Text(stringResource(R.string.notification_rationale_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.notification_rationale_allow)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.notification_rationale_later)) }
        },
    )
}
