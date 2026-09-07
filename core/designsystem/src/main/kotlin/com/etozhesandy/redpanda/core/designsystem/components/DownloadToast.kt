package com.etozhesandy.redpanda.core.designsystem.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.etozhesandy.redpanda.core.designsystem.R

/**
 * Reports the outcome of saving media from a viewer.
 *
 * A download lands in a folder the user cannot see from a full-screen viewer, so the outcome is a
 * Toast rather than something in the layout — there is nothing on those screens for it to change.
 * [savedTo] is the folder under Download/ the file landed in; null means it did not.
 */
@Composable
fun rememberDownloadToast(): (savedTo: String?) -> Unit {
    val context = LocalContext.current
    val savedToTemplate = stringResource(R.string.download_saved_to, "%s")
    val failed = stringResource(R.string.download_failed)
    return { savedTo ->
        val message = savedTo?.let { savedToTemplate.format(it) } ?: failed
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
