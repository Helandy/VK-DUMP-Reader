package com.etozhesandy.redpanda.core.archive.extract

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.etozhesandy.redpanda.core.archive.R
import com.etozhesandy.redpanda.core.archive.source.ArchiveSource
import com.etozhesandy.redpanda.core.common.dispatcher.IoDispatcher
import com.etozhesandy.redpanda.core.common.files.isInside
import com.github.junrar.Archive
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile as Zip4jFile

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
        expandNestedArchives(destination)
    }

    /**
     * Unpacks archives that were themselves files inside the import. One real export keeps its
     * entire dialog history in a `Диалоги.rar` sitting in the picked folder: without this the
     * import "succeeds" with zero dialogs and no error at all.
     *
     * Bounded to [MAX_NESTED_PASSES] pass so a crafted chain of archives cannot recurse; each
     * archive is also size-capped before a single byte is written (see [fitsBudget]).
     */
    private fun expandNestedArchives(root: File) {
        repeat(MAX_NESTED_PASSES) {
            val candidates = root.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in NESTED_EXTENSIONS }
                .toList()
            if (candidates.none(::expandNested)) return
        }
    }

    /** Returns whether [archive] was unpacked; a failure leaves the original in place and is skipped. */
    private fun expandNested(archive: File): Boolean {
        // Deliberately NOT named after the archive's base name: a `Диалоги.rar` unpacked into
        // `Диалоги/` would fabricate the very marker directory format detection keys on, and the
        // classic HTML parser would then walk it, find no contacts, and report no dialogs.
        val destination = File(archive.parentFile, archive.name + EXTRACTED_SUFFIX)
        if (destination.exists()) return false
        val unpacked = runCatching {
            when {
                // A file can carry an archive extension and be nothing of the sort — one real dump
                // ships a corrupt `.zip` whose header is garbage. Skipping it is the right outcome.
                !fitsBudget(archive) -> false
                archive.startsWithAny(ZIP_SIGNATURES) -> {
                    destination.mkdirs()
                    ZipExtraction.extract(archive, destination)
                    true
                }
                archive.startsWithAny(RAR_SIGNATURES) -> {
                    destination.mkdirs()
                    extractRar(archive, destination)
                    true
                }
                else -> false
            }
        }.getOrDefault(false)
        if (!unpacked) {
            destination.deleteRecursively()
            return false
        }
        // Drop the archive now its contents are on disk: the next pass must not rediscover it, and
        // keeping both copies would double the space a large export needs.
        archive.delete()
        return true
    }

    /**
     * Rejects an archive that claims to expand past [MAX_ARCHIVE_UNCOMPRESSED_BYTES] or
     * [MAX_ARCHIVE_ENTRIES]. Both are read from the headers, so a decompression bomb is refused
     * before anything is written. Applies to the archive the user picked and to the nested ones
     * alike. Generous on purpose — real exports reach hundreds of megabytes.
     */
    private fun fitsBudget(archive: File): Boolean = runCatching {
        when {
            archive.startsWithAny(ZIP_SIGNATURES) -> {
                val headers = Zip4jFile(archive).fileHeaders
                headers.size <= MAX_ARCHIVE_ENTRIES &&
                    headers.sumOf { it.uncompressedSize } <= MAX_ARCHIVE_UNCOMPRESSED_BYTES
            }
            archive.startsWithAny(RAR_SIGNATURES) -> Archive(archive).use { rar ->
                var entries = 0
                var bytes = 0L
                var header = rar.nextFileHeader()
                while (header != null) {
                    entries++
                    bytes += header.fullUnpackSize
                    if (entries > MAX_ARCHIVE_ENTRIES || bytes > MAX_ARCHIVE_UNCOMPRESSED_BYTES) return false
                    header = rar.nextFileHeader()
                }
                true
            }
            else -> false
        }
    }.getOrDefault(false)

    /**
     * Unpacks a user-picked archive without asking them what kind it is.
     *
     * Neither the picker nor the file name can be trusted to say: SAF reports plenty of rar files
     * as `application/octet-stream`, and an extension is just as easy to get wrong. Both libraries
     * need a real [File] anyway, so the stream is copied out once and the leading bytes of the
     * result decide who unpacks it.
     */
    private fun extractArchive(uri: android.net.Uri, destination: File) {
        val temp = copyToTempFile(uri, "import", ".archive")
        try {
            val isZip = temp.startsWithAny(ZIP_SIGNATURES)
            val isRar = temp.startsWithAny(RAR_SIGNATURES)
            if (!isZip && !isRar) error(context.getString(R.string.archive_format_unknown))
            // The picked archive gets the same header-read budget as a nested one: without it a
            // zip bomb fills the device before anything notices it is not an export.
            if (!fitsBudget(temp)) error(context.getString(R.string.archive_too_large_or_broken))
            if (isZip) ZipExtraction.extract(temp, destination) else extractRar(temp, destination)
        } finally {
            temp.delete()
        }
    }

    private fun File.startsWithAny(signatures: List<ByteArray>): Boolean {
        val length = signatures.maxOf { it.size }
        val head = ByteArray(length)
        val read = inputStream().use { stream ->
            var total = 0
            while (total < length) {
                val count = stream.read(head, total, length - total)
                if (count < 0) break
                total += count
            }
            total
        }
        return signatures.any { signature ->
            read >= signature.size && signature.indices.all { head[it] == signature[it] }
        }
    }

    /**
     * zip4j refuses entry names that escape the output directory on its own; junrar does not, and
     * nested archives ([expandNestedArchives]) mean entry names now come from content the user
     * never picked, so the containment check is done here.
     */
    private fun extractRar(archive: File, destination: File) {
        val root = destination.canonicalFile
        Archive(archive).use { rar ->
            var header = rar.nextFileHeader()
            while (header != null) {
                if (!header.isDirectory) {
                    val relativePath = header.fileName.replace('\\', '/')
                    val outFile = File(destination, relativePath)
                    if (outFile.isInside(root)) {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { out -> rar.extractFile(header, out) }
                    }
                }
                header = rar.nextFileHeader()
            }
        }
    }

    private fun copyToTempFile(uri: android.net.Uri, prefix: String, suffix: String): File {
        val temp = File.createTempFile(prefix, suffix, context.cacheDir)
        val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open $uri")
        input.use { source -> temp.outputStream().use { output -> source.copyTo(output) } }
        return temp
    }

    private fun copyDirectory(uri: android.net.Uri, destination: File) {
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
        /** Local file header, plus the empty-archive and spanned-archive variants. */
        val ZIP_SIGNATURES = listOf(
            byteArrayOf(0x50, 0x4B, 0x03, 0x04),
            byteArrayOf(0x50, 0x4B, 0x05, 0x06),
            byteArrayOf(0x50, 0x4B, 0x07, 0x08),
        )

        /**
         * One pass is enough for every layout seen in the wild (an export folder holding a single
         * `Диалоги.rar`), and refusing to recurse is what keeps a chain of archives bounded.
         */
        const val MAX_NESTED_PASSES = 1
        const val MAX_ARCHIVE_ENTRIES = 500_000
        const val MAX_ARCHIVE_UNCOMPRESSED_BYTES = 8L * 1024 * 1024 * 1024

        /** Suffix that cannot collide with a format marker directory — see [expandNested]. */
        const val EXTRACTED_SUFFIX = ".extracted"
        val NESTED_EXTENSIONS = setOf("zip", "rar")

        /** "Rar!" then the format version: 0x00 for RAR4, 0x01 0x00 for RAR5. */
        val RAR_SIGNATURES = listOf(
            byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00),
            byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00),
        )
    }
}
