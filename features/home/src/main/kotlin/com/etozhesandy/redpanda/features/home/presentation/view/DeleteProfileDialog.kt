package com.etozhesandy.redpanda.features.home.presentation.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.features.home.R

/** Deleting a profile takes its dialogs, messages and attachments with it, so it is confirmed. */
@Composable
fun DeleteProfileDialog(
    profile: Profile,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_profile_title)) },
        text = { Text(stringResource(R.string.delete_profile_message, profile.displayName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
