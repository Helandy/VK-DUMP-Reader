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
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile as Zip4jFile
import net.lingala.zip4j.model.FileHeader

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
                    extractZip(archive, destination)
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
            if (isZip) extractZip(temp, destination) else extractRar(temp, destination)
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
     * Unpacks [archive], repairing entry names that were written in a legacy codepage.
     *
     * zip4j is deliberately left on its default charset. Setting [Zip4jFile.setCharset] applies
     * that charset to *every* entry and takes precedence over each entry's own UTF-8 flag
     * (`HeaderUtil.decodeStringWithCharset` checks the charset first and never looks at the flag),
     * which destroys the archives this is meant to fix: real exports are mixed, because the
     * Windows zip tools that write them use the OEM codepage by default and only set the UTF-8
     * flag on the names that codepage cannot represent. Forcing CP866 on one such export turned
     * all 270 of its dialogs into `╨Ф╨╕╨░╨╗╨╛╨│╨╕/...`, so `Диалоги/` was not found at all and the
     * import silently fell back to media-only. On the default charset zip4j honours the flag per
     * entry, so only the unflagged names need correcting — which is done here, per entry.
     */
    private fun extractZip(archive: File, destination: File) {
        val zipFile = Zip4jFile(archive)
        val legacy = legacyNameCharset(zipFile)
        if (legacy == null) {
            zipFile.extractAll(destination.absolutePath)
            return
        }
        val root = destination.canonicalFile
        for (header in zipFile.fileHeaders) {
            val name = decodeEntryName(header, legacy)
            val outFile = File(destination, name.replace('\\', '/'))
            if (!outFile.isInside(root)) continue
            if (header.isDirectory) {
                outFile.mkdirs()
            } else {
                // Only ever called for a file entry: handed a *directory* header, zip4j extracts
                // that whole subtree and rebuilds each name with `String.replaceFirst`, whose
                // first argument is a regex — so a folder like `инцест+тройнички` or
                // `Имя (id123)` fails to match its own name and the entire subtree is written
                // under the uncorrected CP437 name instead.
                zipFile.extractFile(header, destination.absolutePath, name)
            }
        }
    }

    /**
     * Decodes [header]'s name, honouring its own UTF-8 flag before falling back to [legacy].
     *
     * An unflagged name reaches us as zip4j's CP437 decode. CP437 maps all 256 byte values to
     * distinct characters, so re-encoding recovers the original bytes exactly and they can be
     * decoded again with the codepage that was really used.
     */
    private fun decodeEntryName(header: FileHeader, legacy: Charset): String =
        if (header.isFileNameUTF8Encoded) {
            header.fileName
        } else {
            CP437?.let { String(header.fileName.toByteArray(it), legacy) } ?: header.fileName
        }

    /**
     * The charset the archive's *unflagged* entry names were really written in, or null when there
     * is nothing to repair and zip4j's own decoding can be trusted for the whole archive.
     *
     * Entries that set the UTF-8 flag are skipped: zip4j already decodes those correctly and they
     * say nothing about how the rest were encoded. For the others zip4j falls back to CP437 and
     * mangles every non-ASCII name — "Диалоги" becomes "ä¿á½«ú¿", which then fails the
     * `Name (idNNN)` folder pattern the parsers match on, so those dialogs are dropped. Two kinds
     * of archive land here, and they need opposite treatment:
     *  - `zip` on macOS/Linux writes UTF-8 bytes but never sets the flag,
     *  - older VK export tools write a legacy DOS codepage (CP866, verified against real exports).
     *
     * The CP437 round-trip above makes them distinguishable: valid UTF-8 means the first kind,
     * anything else means the second.
     */
    private fun legacyNameCharset(zipFile: Zip4jFile): Charset? {
        val cp437 = CP437 ?: return null
        var sawUnflaggedNonAscii = false
        var allValidUtf8 = true
        val decodeFailed = runCatching {
            for (header in zipFile.fileHeaders) {
                if (header.isFileNameUTF8Encoded) continue
                val name = header.fileName
                if (name.none { it.code > 127 }) continue
                sawUnflaggedNonAscii = true
                if (!isValidUtf8(name.toByteArray(cp437))) {
                    allValidUtf8 = false
                    break
                }
            }
        }.isFailure
        if (decodeFailed || !sawUnflaggedNonAscii) return null
        return if (allValidUtf8) Charsets.UTF_8 else runCatching { Charset.forName("CP866") }.getOrNull()
    }

    private fun isValidUtf8(bytes: ByteArray): Boolean = runCatching {
        Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
    }.isSuccess

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
        /**
         * Single-byte, so decoding to it and back recovers the raw entry-name bytes unchanged.
         * Neither this nor CP866 is part of the charset set every Android build is required to
         * ship, so both are resolved defensively and detection just backs off when they're missing.
         */
        val CP437: Charset? = runCatching { Charset.forName("Cp437") }.getOrNull()

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
