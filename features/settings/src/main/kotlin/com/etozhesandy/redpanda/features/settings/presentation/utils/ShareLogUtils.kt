package com.etozhesandy.redpanda.features.settings.presentation.utils

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Opens the system share sheet with [files] attached. They go out through the app's FileProvider,
 * which exposes only the log directory, with read access granted to the chosen app alone.
 */
fun Context.shareLogFiles(files: List<File>, chooserTitle: String) {
    val authority = "$packageName.fileprovider"
    val uris = ArrayList<Uri>(files.map { FileProvider.getUriForFile(this, authority, it) })
    val send = Intent(Intent.ACTION_SEND_MULTIPLE)
        .setType("text/plain")
        .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    // The grant flag only reaches the target through ClipData when the intent is wrapped in a chooser.
    send.clipData = ClipData.newRawUri(null, uris.first()).apply {
        uris.drop(1).forEach { addItem(ClipData.Item(it)) }
    }
    runCatching {
        startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
