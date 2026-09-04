package com.etozhesandy.redpanda.core.archive.format

import java.io.File
import javax.inject.Inject

/**
 * Works out how to read an extracted archive, and where in it the export actually starts.
 *
 * Real content is regularly buried a few directories down — a zip whose single top-level entry is
 * the export folder, sometimes doubled when the folder gets zipped twice — so every directory down
 * to [MAX_UNWRAP_DEPTH] is considered, not just the extraction root.
 *
 * Candidates are ranked by [priority] rather than by depth alone. Depth alone picks wrong twice on
 * real dumps: one export keeps a `Вложения/` folder beside a nested archive whose contents hold all
 * the dialogs, so the shallower directory would win with no dialogs in it; another nests a complete
 * JSON export inside an HTML one, where the JSON copy is strictly richer (493 dialogs / 517 138
 * messages against 486 / 515 877). Depth is only the tie-breaker, applied by scanning breadth-first
 * so the shallowest candidate of equal rank is seen first.
 */
class ArchiveFormatDetector @Inject constructor(
    private val dialectSniffer: HtmlDialectSniffer,
) {

    fun detect(root: File): ArchiveLayout {
        val best = breadthFirstDirectories(root)
            .mapNotNull { dir -> classify(dir)?.let { format -> ArchiveLayout(format, dir) } }
            .minByOrNull { priority(it.format) }
        return best ?: ArchiveLayout(DetectedFormat.MEDIA_ONLY, mediaRoot(root))
    }

    /**
     * Directories from [root] down to [MAX_UNWRAP_DEPTH], shallowest first, so that
     * `minByOrNull` — which keeps the first of equal keys — resolves rank ties toward the top.
     */
    private fun breadthFirstDirectories(root: File): List<File> {
        val result = mutableListOf<File>()
        var level = listOf(root)
        repeat(MAX_UNWRAP_DEPTH + 1) {
            if (level.isEmpty()) return result
            result += level
            level = level.flatMap { dir -> dir.listFiles()?.filter { it.isDirectory }.orEmpty() }
        }
        return result
    }

    /** The format [dir] would be read as, or null when it holds no dialogs and is no export root. */
    private fun classify(dir: File): DetectedFormat? = when {
        File(dir, "profile.json").isFile && File(dir, "messages").isDirectory -> DetectedFormat.VK_API
        File(dir, TORRENT_DIALOGS_DIR).isDirectory -> DetectedFormat.VK_HTML_TORRENT
        File(dir, HTML_DIALOGS_DIR).isDirectory -> dialectSniffer.sniff(File(dir, HTML_DIALOGS_DIR))
        else -> null
    }

    /**
     * Where a dialog-less dump "starts". It names the profile and labels its media folders, so
     * landing on the extraction root would call every such profile "raw".
     *
     * Prefers the shallowest directory carrying an export marker; failing that, unwraps directories
     * that hold nothing but a single subdirectory, which is what a folder picked one level too high
     * looks like.
     */
    private fun mediaRoot(root: File): File {
        breadthFirstDirectories(root).firstOrNull(::hasWeakMarker)?.let { return it }
        var candidate = root
        repeat(MAX_UNWRAP_DEPTH) {
            val children = candidate.listFiles().orEmpty()
            val onlyChild = children.singleOrNull()?.takeIf { it.isDirectory } ?: return candidate
            candidate = onlyChild
        }
        return candidate
    }

    private fun hasWeakMarker(dir: File): Boolean = weakMarkers.any { File(dir, it).exists() }

    private fun priority(format: DetectedFormat): Int = when (format) {
        DetectedFormat.VK_API -> 0
        DetectedFormat.VK_HTML_TORRENT, DetectedFormat.VK_HTML_B00M, DetectedFormat.VK_HTML_CLASSIC -> 1
        DetectedFormat.MEDIA_ONLY -> 2
    }

    private companion object {
        const val MAX_UNWRAP_DEPTH = 3
        const val HTML_DIALOGS_DIR = "Диалоги"
        const val TORRENT_DIALOGS_DIR = "Переписки"

        /** Enough to recognise an export folder, but not enough to promise it holds dialogs. */
        val weakMarkers = listOf("Вложения", "Друзья", "Друзья.txt", "Диалоги.html", "profile.json")
    }
}
