package com.etozhesandy.redpanda.core.archive.extract

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.etozhesandy.redpanda.core.archive.R
import com.etozhesandy.redpanda.core.archive.source.ArchiveSource
import com.etozhesandy.redpanda.core.common.dispatcher.IoDispatcher
import com.etozhesandy.redpanda.core.common.files.isInside
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * The Android half of extraction: SAF sources, free-space checks and user-facing error strings.
 * Everything that is only files and bytes lives in [ArchiveNormalizer] and [ZipExtraction].
 */
class ArchiveExtractorImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ArchiveExtractor {

    override suspend fun extract(source: ArchiveSource, destination: File) = withContext(ioDispatcher) {
        destination.mkdirs()
        when (source) {
            is ArchiveSource.ArchiveFile -> extractArchive(source.uri, destination)
            is ArchiveSource.Directory -> copyDirectory(source.uri, destination)
        }
        ArchiveNormalizer.normalize(destination)
    }

    /**
     * Unpacks a user-picked archive without asking them what kind it is.
     *
     * Neither the picker nor the file name can be trusted to say: SAF reports plenty of rar files
     * as `application/octet-stream`, and an extension is just as easy to get wrong. Both libraries
     * need a real [File] anyway, so the stream is copied out once and the leading bytes of the
     * result decide who unpacks it.
     */
    private fun extractArchive(uri: Uri, destination: File) {
        requireSpaceFor(uri, destination)
        val temp = copyToTempFile(uri, "import", ".archive")
        try {
            if (!ArchiveNormalizer.isZip(temp) && !ArchiveNormalizer.isRar(temp)) {
                error(context.getString(R.string.archive_format_unknown))
            }
            // The picked archive gets the same header-read budget as a nested one: without it a
            // zip bomb fills the device before anything notices it is not an export.
            if (!ArchiveNormalizer.fitsBudget(temp)) {
                error(context.getString(R.string.archive_too_large_or_broken))
            }
            ArchiveNormalizer.unpack(temp, destination)
        } finally {
            temp.delete()
        }
    }

    /**
     * Refuses an import that cannot fit before a byte of it is written.
     *
     * The peak is roughly [SPACE_HEADROOM] times the archive: the picked archive is copied out
     * whole (neither zip4j nor junrar reads a SAF stream), and its contents are then written beside
     * that copy, which only goes away once the unpacking is done. Without this check the import
     * runs for minutes and dies mid-extraction on whatever write happens to hit the full disk,
     * reporting an ENOSPC no one can act on — one real 1.5 GB export needed 3.4 GB against the
     * 2.6 GB free and failed exactly that way.
     *
     * A source whose size cannot be read is let through: refusing an import over a missing number
     * would be worse than trying it.
     */
    private fun requireSpaceFor(uri: Uri, destination: File) {
        val archiveBytes = runCatching {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize }
        }.getOrNull()?.takeIf { it > 0 } ?: return
        val required = (archiveBytes * SPACE_HEADROOM).toLong()
        val available = destination.usableSpace
        if (available < required) {
            error(
                context.getString(
                    R.string.archive_not_enough_space,
                    android.text.format.Formatter.formatShortFileSize(context, required),
                    android.text.format.Formatter.formatShortFileSize(context, available),
                ),
            )
        }
    }

    private fun copyToTempFile(uri: Uri, prefix: String, suffix: String): File {
        val temp = File.createTempFile(prefix, suffix, context.cacheDir)
        val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open $uri")
        input.use { source -> temp.outputStream().use { output -> source.copyTo(output) } }
        return temp
    }

    private fun copyDirectory(uri: Uri, destination: File) {
        val tree = DocumentFile.fromTreeUri(context, uri) ?: error("Cannot open directory $uri")
        copyDocumentTree(tree, destination, destination.canonicalFile)
    }

    /**
     * [root] is the import directory every copied file has to stay under: a display name is
     * whatever the DocumentsProvider says it is, `..` segments included, so it is treated like an
     * archive entry name rather than like a trusted file name.
     */
    private fun copyDocumentTree(doc: DocumentFile, destDir: File, root: File) {
        destDir.mkdirs()
        for (child in doc.listFiles()) {
            val name = child.name ?: continue
            val target = File(destDir, name)
            if (!target.isInside(root)) continue
            if (child.isDirectory) {
                copyDocumentTree(child, target, root)
            } else {
                context.contentResolver.openInputStream(child.uri)?.use { input ->
                    target.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    private companion object {
        /**
         * Archive size to free space needed. The copy and the unpacked result coexist, and an
         * export is mostly photos and video that barely compress, so the unpacked side lands close
         * to the archive's own size — measured at 1.18x on one real export and 1.16x on another.
         */
        const val SPACE_HEADROOM = 2.3
    }
}
