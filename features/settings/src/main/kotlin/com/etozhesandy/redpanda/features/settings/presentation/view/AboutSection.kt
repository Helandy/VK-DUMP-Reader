package com.etozhesandy.redpanda.features.settings.presentation.view

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.etozhesandy.redpanda.core.common.net.openExternally
import com.etozhesandy.redpanda.features.settings.R

private const val PROJECT_URL = "https://github.com/Helandy/VK-DUMP-Reader"

/**
 * The closing block of the settings screen: which build is installed, and where to send anything
 * about it. The version is read from the installed package, so it always matches the running APK.
 */
@Composable
fun AboutSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionName = remember(context) { context.appVersionName() }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_about),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_app_version)) },
            supportingContent = { Text(versionName) },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_feedback)) },
            supportingContent = { Text(PROJECT_URL) },
            modifier = Modifier.clickable { context.openExternally(PROJECT_URL) },
        )
    }
}

private fun Context.appVersionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }
        .getOrNull()
        .orEmpty()
        .ifEmpty { "—" }
