package com.etozhesandy.redpanda.core.archive.tools

import com.etozhesandy.redpanda.core.archive.extract.ArchiveNormalizer
import com.etozhesandy.redpanda.core.archive.extract.EntryPolicy
import com.etozhesandy.redpanda.core.archive.format.ArchiveFormatDetector
import com.etozhesandy.redpanda.core.archive.format.ArchiveLayout
import com.etozhesandy.redpanda.core.archive.format.DetectedFormat
import com.etozhesandy.redpanda.core.archive.format.HtmlDialectSniffer
import com.etozhesandy.redpanda.core.archive.format.probe.VkApiLayoutProbe
import com.etozhesandy.redpanda.core.archive.format.probe.VkHtmlHistoryLayoutProbe
import com.etozhesandy.redpanda.core.archive.format.probe.VkHtmlTorrentLayoutProbe
import com.etozhesandy.redpanda.core.archive.format.probe.VkJsonDumpLayoutProbe
import java.io.File
import kotlin.system.measureTimeMillis
import org.junit.Test

/**
 * Developer tool, not a test: runs the real extraction and detection over a folder of real archives
 * and prints what each one was recognised as.
 *
 * It exists because the failure this project keeps hitting is invisible from any single archive —
 * a change that makes one dump import correctly quietly turns another into a media-only profile,
 * and nobody notices until that profile is opened. Running the whole corpus takes a minute and
 * makes the trade explicit.
 *
 *     ./gradlew :core:archive:detectionMatrix -Parchives="/path/to/exampl"
 *
 * Media entries are unpacked as empty files ([EntryPolicy.SkipContent]): detection never reads a
 * photo, and skipping the bytes is what turns a 20 GB corpus into something that fits on a laptop
 * and finishes while you watch. Every HTML and JSON file — everything detection actually looks at —
 * is extracted for real.
 *
 * Without `-Parchives` the task passes immediately, so it is harmless on CI.
 */
class ArchiveDetectionMatrix {

    @Test
    fun printMatrix() {
        val root = System.getProperty(ARCHIVES_PROPERTY).orEmpty().trim()
        if (root.isEmpty()) {
            println("[detectionMatrix] no -Parchives=<dir> given, nothing to do")
            return
        }
        val directory = File(root)
        require(directory.isDirectory) { "Not a directory: $root" }

        val archives = directory.listFiles()
            .orEmpty()
            .filter { it.isFile && it.extension.lowercase() in ARCHIVE_EXTENSIONS }
            .sortedBy { it.name.lowercase() }
        println("[detectionMatrix] ${archives.size} archives in $root")

        val detector = ArchiveFormatDetector(
            setOf(
                VkApiLayoutProbe(),
                VkJsonDumpLayoutProbe(),
                VkHtmlTorrentLayoutProbe(),
                VkHtmlHistoryLayoutProbe(HtmlDialectSniffer()),
            ),
        )
        var emptyCount = 0
        var multiCount = 0
        for (archive in archives) {
            val staging = createTempDirectory()
            try {
                val elapsed = measureTimeMillis { unpackShape(archive, staging) }
                val layouts = detector.detectAll(staging)
                if (layouts.isEmpty()) emptyCount++
                if (layouts.size > 1) multiCount++
                report(archive, staging, layouts, detector, elapsed)
            } catch (t: Throwable) {
                println("%-52s FAILED  %s".format(archive.name.take(52), t.message))
            } finally {
                staging.deleteRecursively()
            }
        }
        println(
            "[detectionMatrix] done: ${archives.size} archives, " +
                "$emptyCount with no dump, $multiCount with more than one",
        )
    }

    private fun report(
        archive: File,
        staging: File,
        layouts: List<ArchiveLayout>,
        detector: ArchiveFormatDetector,
        elapsedMs: Long,
    ) {
        val effective = layouts.ifEmpty { listOf(detector.detect(staging)) }
        val header = "%-52s %d dump(s) %5ds".format(archive.name.take(52), layouts.size, elapsedMs / 1000)
        println(header)
        for (layout in effective) {
            val content = layout.contentRoot.relativeTo(staging).path.ifEmpty { "." }
            val dump = layout.dumpRoot.relativeTo(staging).path.ifEmpty { "." }
            val where = if (dump == content) content else "$content  [dump: $dump]"
            println("%-52s   %-16s %s".format("", layout.format.label(), where))
        }
    }

    private fun DetectedFormat.label(): String = name

    /** Unpacks [archive] into [destination] keeping every byte detection reads and no media. */
    private fun unpackShape(archive: File, destination: File) {
        val policy = EntryPolicy.SkipContent { name, _ ->
            name.substringAfterLast('.', "").lowercase() in MEDIA_EXTENSIONS
        }
        require(ArchiveNormalizer.fitsBudget(archive)) { "over budget or unreadable" }
        ArchiveNormalizer.unpack(archive, destination, policy)
        ArchiveNormalizer.normalize(destination, policy)
    }

    private fun createTempDirectory(): File =
        File.createTempFile("detection-matrix", "").let { file ->
            file.delete()
            file.mkdirs()
            file
        }

    private companion object {
        const val ARCHIVES_PROPERTY = "redpanda.archives"
        val ARCHIVE_EXTENSIONS = setOf("zip", "rar")
        val MEDIA_EXTENSIONS = setOf(
            "jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif",
            "mp4", "mov", "webm", "mkv", "avi", "m4v", "3gp",
            "mp3", "ogg", "m4a", "wav", "opus",
        )
    }
}
