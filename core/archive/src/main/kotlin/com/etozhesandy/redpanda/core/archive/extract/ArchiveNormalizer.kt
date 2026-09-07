package com.etozhesandy.redpanda.core.archive.extract

import com.etozhesandy.redpanda.core.archive.extract.NestedArchiveNaming.EXTRACTED_SUFFIX
import com.etozhesandy.redpanda.core.common.files.isInside
import com.github.junrar.Archive
import java.io.File
import java.io.FileOutputStream
import net.lingala.zip4j.ZipFile as Zip4jFile

/**
 * Turns whatever the user picked into a plain directory tree the format detector can read: unpacks
 * archives that were themselves files inside the import, and deletes the sidecars macOS leaves
 * behind.
 *
 * Kept free of Android for the same reason as [ZipExtraction]: this is the half of extraction whose
 * bugs only show up on real exports, and it is what the detection matrix tool runs over the whole
 * corpus without an emulator.
 */
internal object ArchiveNormalizer {

    /** Zip local file header, plus the empty-archive and spanned-archive variants. */
    private val ZIP_SIGNATURES = listOf(
        byteArrayOf(0x50, 0x4B, 0x03, 0x04),
        byteArrayOf(0x50, 0x4B, 0x05, 0x06),
        byteArrayOf(0x50, 0x4B, 0x07, 0x08),
    )

    /** "Rar!" then the format version: 0x00 for RAR4, 0x01 0x00 for RAR5. */
    private val RAR_SIGNATURES = listOf(
        byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00),
        byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00),
    )

    /**
     * Real exports nest more than one level deep: one archive in the corpus is nothing but three
     * `.rar` files, each holding a complete dump, and another splits a single dump across a
     * `html.zip` and a `json.zip` that then hold folders of their own. One pass left those imports
     * with no dialogs and no error at all.
     *
     * Recursion stays bounded three ways: a pass limit, a total limit on how many archives one
     * import may unpack, and a header-read size budget applied to each one before a byte is written.
     */
    private const val MAX_NESTED_PASSES = 3
    private const val MAX_NESTED_ARCHIVES = 64
    private const val MAX_ARCHIVE_ENTRIES = 500_000
    private const val MAX_ARCHIVE_UNCOMPRESSED_BYTES = 8L * 1024 * 1024 * 1024

    private const val MACOS_METADATA_DIR = "__MACOSX"
    private const val APPLE_DOUBLE_PREFIX = "._"
    private const val DS_STORE_NAME = ".DS_Store"
    private val NESTED_EXTENSIONS = setOf("zip", "rar")

    /**
     * Unpacks nested archives and clears macOS metadata.
     *
     * The metadata sweep runs on both sides of the expansion. Before, because an AppleDouble keeps
     * the name of the file it shadows — a `._json.zip` sitting beside a real `json.zip` reads as an
     * archive to anything matching on extension, and every pass then tried to unpack 4 KB of
     * resource fork. After, because the archives just unpacked bring their own `__MACOSX/` with them.
     */
    fun normalize(root: File, policy: EntryPolicy = EntryPolicy.Full) {
        removeMacOsMetadata(root)
        expandNestedArchives(root, policy)
        removeMacOsMetadata(root)
    }

    fun isZip(file: File): Boolean = file.startsWithAny(ZIP_SIGNATURES)

    fun isRar(file: File): Boolean = file.startsWithAny(RAR_SIGNATURES)

    /**
     * Unpacks [archive] by what its leading bytes say it is, ignoring its name.
     *
     * Neither a picker nor a file name can be trusted to say which it is: SAF reports plenty of rar
     * files as `application/octet-stream`, and an extension is just as easy to get wrong.
     */
    fun unpack(archive: File, destination: File, policy: EntryPolicy = EntryPolicy.Full) {
        when {
            isRar(archive) -> extractRar(archive, destination, policy)
            isZip(archive) || opensAsZip(archive) -> ZipExtraction.extract(archive, destination, policy)
            else -> error("Unsupported archive: ${archive.name}")
        }
    }

    /**
     * Whether zip4j can read [archive] even though it carries no zip signature.
     *
     * That is what the last volume of a split zip looks like: the `.z01` part starts with the
     * signature and holds the entries, while the `.zip` part starts with raw compressed data and
     * ends with the central directory. zip4j finds its way in from the end, so gating on the
     * leading bytes alone skipped one real export's dialogs entirely.
     */
    private fun opensAsZip(archive: File): Boolean =
        runCatching { Zip4jFile(archive).fileHeaders.isNotEmpty() }.getOrDefault(false)

    /**
     * Rejects an archive that claims to expand past [MAX_ARCHIVE_UNCOMPRESSED_BYTES] or
     * [MAX_ARCHIVE_ENTRIES]. Both are read from the headers, so a decompression bomb is refused
     * before anything is written. Applies to the archive the user picked and to the nested ones
     * alike. Generous on purpose — real exports reach hundreds of megabytes.
     */
    fun fitsBudget(archive: File): Boolean = runCatching {
        when {
            isRar(archive) -> Archive(archive).use { rar ->
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
            else -> {
                val headers = Zip4jFile(archive).fileHeaders
                headers.isNotEmpty() &&
                    headers.size <= MAX_ARCHIVE_ENTRIES &&
                    headers.sumOf { it.uncompressedSize } <= MAX_ARCHIVE_UNCOMPRESSED_BYTES
            }
        }
    }.getOrDefault(false)

    /**
     * Unpacks archives that were themselves files inside the import. One real export keeps its
     * entire dialog history in a `Диалоги.rar` sitting in the picked folder: without this the
     * import "succeeds" with zero dialogs and no error at all.
     */
    private fun expandNestedArchives(root: File, policy: EntryPolicy) {
        var budget = MAX_NESTED_ARCHIVES
        // A file that carried an archive extension and turned out not to be one is remembered, so
        // the remaining passes do not keep retrying it. Without this a corrupt `.zip` is opened,
        // rejected and cleaned up once per pass, for as many passes as there are.
        val failed = mutableSetOf<String>()
        repeat(MAX_NESTED_PASSES) {
            if (budget <= 0) return
            val candidates = root.walkTopDown()
                .filter { it.isFile && it.extension.lowercase() in NESTED_EXTENSIONS }
                .filterNot { it.canonicalPath in failed }
                .toList()
            // Every candidate is unpacked, then the results are folded — `none { expandNested(it) }`
            // stops at the first archive it unpacks, and one real export splits itself across two:
            // a `html.zip` beside a `json.zip`, of which only whichever came first survived, so the
            // dialogs in the other one were never seen and the import came out media-only.
            val expanded = candidates.map { archive ->
                if (budget <= 0) {
                    false
                } else {
                    val path = archive.canonicalPath
                    expandNested(archive, policy).also { if (it) budget-- else failed += path }
                }
            }
            if (expanded.none { it }) return
        }
    }

    /** Returns whether [archive] was unpacked; a failure leaves the original in place and is skipped. */
    private fun expandNested(archive: File, policy: EntryPolicy): Boolean {
        // Deliberately NOT named after the archive's base name: a `Диалоги.rar` unpacked into
        // `Диалоги/` would fabricate the very marker directory format detection keys on, and the
        // classic HTML parser would then walk it, find no contacts, and report no dialogs.
        val destination = File(archive.parentFile, archive.name + EXTRACTED_SUFFIX)
        if (destination.exists()) return false
        val unpacked = runCatching {
            // A file can carry an archive extension and be nothing of the sort — one real dump
            // ships a corrupt `.zip` whose header is garbage. Skipping it is the right outcome.
            if (!fitsBudget(archive)) {
                false
            } else {
                destination.mkdirs()
                unpack(archive, destination, policy)
                true
            }
        }.getOrDefault(false)
        if (!unpacked) {
            destination.deleteRecursively()
            return false
        }
        // Drop the archive now its contents are on disk: the next pass must not rediscover it, and
        // keeping both copies would double the space a large export needs.
        archive.delete()
        ArchiveFlattening.flatten(destination)
        return true
    }

    /**
     * zip4j refuses entry names that escape the output directory on its own; junrar does not, and
     * nested archives ([expandNestedArchives]) mean entry names now come from content the user
     * never picked, so the containment check is done here.
     */
    private fun extractRar(archive: File, destination: File, policy: EntryPolicy) {
        val root = destination.canonicalFile
        Archive(archive).use { rar ->
            var header = rar.nextFileHeader()
            while (header != null) {
                if (!header.isDirectory) {
                    val relativePath = header.fileName.replace('\\', '/')
                    val outFile = File(destination, relativePath)
                    if (outFile.isInside(root)) {
                        outFile.parentFile?.mkdirs()
                        if (policy.skips(relativePath, header.fullUnpackSize)) {
                            outFile.createNewFile()
                        } else {
                            FileOutputStream(outFile).use { out -> rar.extractFile(header, out) }
                        }
                    }
                }
                header = rar.nextFileHeader()
            }
        }
    }

    /**
     * Deletes the sidecar files macOS writes into an archive it creates, which carry no content of
     * their own: a `__MACOSX/` mirror of the whole tree, an AppleDouble `._name` beside each real
     * `name`, and `.DS_Store`.
     *
     * They are not merely wasted space. An AppleDouble keeps the extension of the file it shadows,
     * so `._photo.jpg` reads as a photo to anything scanning by extension — one real export
     * produced 209 of them, and every one showed up in the archive's media folders as a file that
     * cannot be displayed, alongside a duplicate `__MACOSX/…` copy of each folder in the export.
     *
     * Runs after extraction rather than inside it so that zip, rar and picked-folder imports are
     * all covered, nested archives included.
     */
    private fun removeMacOsMetadata(root: File) {
        val junk = mutableListOf<File>()
        root.walkTopDown()
            // `onEnter` is where the mirror has to be caught. It is called *before* the directory
            // is yielded, and returning false skips the directory itself as well as its contents —
            // so the previous `filter { it.name == MACOS_METADATA_DIR }` never saw a single one and
            // no `__MACOSX/` was ever deleted. The detection matrix caught it: one export was being
            // recognised twice, the second "dump" being its own `__MACOSX/…/Переписки/` mirror.
            .onEnter { dir ->
                if (dir != root && dir.name == MACOS_METADATA_DIR) {
                    junk += dir
                    false
                } else {
                    true
                }
            }
            .forEach { file -> if (file.isMacOsSidecar()) junk += file }
        junk.forEach { it.deleteRecursively() }
    }

    /**
     * Whether this is a macOS sidecar rather than a file of the export.
     *
     * An AppleDouble is only recognised as one when the file it shadows is actually there beside
     * it: `._` is a legal start to a name, and deleting export content on a naming coincidence
     * would be far worse than leaving a stray sidecar behind. The ones inside [MACOS_METADATA_DIR]
     * have no such neighbour, but that whole directory goes anyway.
     */
    private fun File.isMacOsSidecar(): Boolean = isFile && when {
        name == DS_STORE_NAME -> true
        name.startsWith(APPLE_DOUBLE_PREFIX) -> File(parentFile, name.removePrefix(APPLE_DOUBLE_PREFIX)).exists()
        else -> false
    }

    private fun File.startsWithAny(signatures: List<ByteArray>): Boolean = runCatching {
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
        signatures.any { signature ->
            read >= signature.size && signature.indices.all { head[it] == signature[it] }
        }
    }.getOrDefault(false)
}
