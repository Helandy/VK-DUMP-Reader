package com.etozhesandy.redpanda.core.designsystem.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.R

/**
 * Toolbar action that saves the currently shown media to the phone.
 *
 * Below API 29, writing into the public Downloads folder needs WRITE_EXTERNAL_STORAGE granted at
 * runtime; from API 29 on, MediaStore's Downloads collection needs no such permission.
 */
@Composable
fun DownloadIconButton(source: String?, onDownload: (String) -> Unit) {
    if (source == null) return

    var pendingSource by remember { mutableStateOf<String?>(null) }
    val onDownloadState = rememberUpdatedState(onDownload)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingSource
        pendingSource = null
        if (granted && pending != null) onDownloadState.value(pending)
    }

    IconButton(
        onClick = {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                pendingSource = source
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                onDownload(source)
            }
        },
    ) {
        Icon(
            imageVector = Icons.Default.Download,
            contentDescription = stringResource(R.string.action_download),
            tint = Color.White,
        )
    }
}
