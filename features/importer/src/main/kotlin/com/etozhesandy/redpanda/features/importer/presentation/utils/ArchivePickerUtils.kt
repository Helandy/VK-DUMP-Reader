package com.etozhesandy.redpanda.features.importer.presentation.utils

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.etozhesandy.redpanda.core.archive.source.ArchiveSource

/**
 * Deliberately unfiltered: document providers report rar under half a dozen different types
 * ("application/vnd.rar", "application/x-rar-compressed", "application/octet-stream", and on some
 * vendor file managers nothing at all), and anything missing from a MIME filter comes up greyed
 * out and unpickable. The extractor sniffs the real signature and rejects what it cannot read.
 */
private val ARCHIVE_MIME_TYPES = arrayOf("*/*")

/** Opens the document picker and reports the pick as [ArchiveSource.ArchiveFile]. */
@Composable
fun rememberArchivePicker(onPicked: (ArchiveSource) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(OpenPersistableDocument()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermissionSafely(it)
            onPicked(ArchiveSource.ArchiveFile(it))
        }
    }
    return remember(launcher) { { launcher.launch(ARCHIVE_MIME_TYPES) } }
}

/** Opens the directory picker and reports the pick as [ArchiveSource.Directory]. */
@Composable
fun rememberDirectoryPicker(onPicked: (ArchiveSource) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            context.contentResolver.takePersistableUriPermissionSafely(it)
            onPicked(ArchiveSource.Directory(it))
        }
    }
    return remember(launcher) { { launcher.launch(null) } }
}

/**
 * [ActivityResultContracts.OpenDocument] asks only for a read grant, which dies with the process —
 * the import runs in a worker that may outlive it, so the persistable flag has to go on the intent
 * before the picker sees it, otherwise [ContentResolver.takePersistableUriPermission] throws.
 */
private class OpenPersistableDocument : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input)
            .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
}

/** Some providers hand back a uri they refuse to persist; the import still works for this run. */
private fun ContentResolver.takePersistableUriPermissionSafely(uri: Uri) {
    runCatching { takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
}
