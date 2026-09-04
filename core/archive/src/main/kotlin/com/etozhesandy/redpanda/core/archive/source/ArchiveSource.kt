package com.etozhesandy.redpanda.core.archive.source

import android.net.Uri

/** Where a profile archive is coming from, as picked by the user via SAF. */
sealed interface ArchiveSource {
    /** A compressed archive; zip vs rar is told apart by its signature at extraction time. */
    data class ArchiveFile(val uri: Uri) : ArchiveSource
    data class Directory(val uri: Uri) : ArchiveSource
}
