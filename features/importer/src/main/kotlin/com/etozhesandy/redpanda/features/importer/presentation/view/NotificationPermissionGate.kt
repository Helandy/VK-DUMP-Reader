package com.etozhesandy.redpanda.features.importer.presentation.view

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.etozhesandy.redpanda.features.importer.presentation.utils.needsNotificationPermission

/**
 * Draws nothing of its own: it only decides whether [NotificationRationaleDialog] is due and, once
 * the user agrees, hands the request over to the system sheet. Whether the permission is granted is
 * not tracked — the import proceeds either way and the notification is just a nice-to-have.
 */
@Composable
fun NotificationPermissionGate() {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* nothing to do: the import does not depend on the answer */ }

    LaunchedEffect(Unit) {
        showRationale = context.needsNotificationPermission()
    }

    if (showRationale) {
        NotificationRationaleDialog(
            onConfirm = {
                showRationale = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = { showRationale = false },
        )
    }
}
