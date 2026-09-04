package com.etozhesandy.redpanda.core.archive.format

import java.io.File
import javax.inject.Inject

/**
 * Tells the two `Диалоги/`-shaped HTML exports apart, which paths alone cannot: both write
 * `Диалоги/{категория}/{Имя (idN)}/history_N.html`, but one uses `div.m`/`a.ma` and the other
 * `div.im_in`/`a.mem_link`, with no overlap. Verified against real dumps: the classic layout
 * contains zero `im_in` occurrences and the b00m layout zero `class="m"` ones.
 *
 * Only the first [SNIFF_BYTES] bytes of one history file are read. Parsing the document would be
 * far too expensive just to classify — a single history page runs to tens of megabytes — and both
 * markers appear within the first couple of kilobytes of every file checked.
 */
class HtmlDialectSniffer @Inject constructor() {

    /**
     * Returns the dialect of the export rooted at [dialogsRoot], or null when it holds no history
     * file at all — an empty `Диалоги/` is not evidence of either dialect, and claiming one would
     * import the profile with zero dialogs instead of falling back to a media-only import.
     */
    fun sniff(dialogsRoot: File): DetectedFormat? {
        val historyFile = findHistoryFile(dialogsRoot) ?: return null
        val head = readHead(historyFile)
        return if (b00mMarkers.any { head.contains(it) }) {
            DetectedFormat.VK_HTML_B00M
        } else {
            // The classic layout is the safe default: it is what every previously working import used.
            DetectedFormat.VK_HTML_CLASSIC
        }
    }

    private fun findHistoryFile(dialogsRoot: File): File? {
        var inspected = 0
        for (categoryDir in dialogsRoot.listFiles { file -> file.isDirectory }.orEmpty()) {
            for (contactDir in categoryDir.listFiles { file -> file.isDirectory }.orEmpty()) {
                if (inspected++ >= MAX_CONTACT_DIRS) return null
                contactDir.listFiles { file -> file.isFile && isHistoryPage(file) }
                    ?.firstOrNull()
                    ?.let { return it }
            }
        }
        return null
    }

    private fun isHistoryPage(file: File): Boolean =
        file.name.startsWith("history_") && file.extension.equals("html", ignoreCase = true)

    private fun readHead(file: File): String {
        val buffer = ByteArray(SNIFF_BYTES)
        var total = 0
        runCatching {
            file.inputStream().use { stream ->
                while (total < SNIFF_BYTES) {
                    val count = stream.read(buffer, total, SNIFF_BYTES - total)
                    if (count < 0) break
                    total += count
                }
            }
        }
        // Markers are pure ASCII, so a multi-byte character clipped at the boundary cannot hide one.
        return String(buffer, 0, total, Charsets.UTF_8)
    }

    private companion object {
        const val SNIFF_BYTES = 8 * 1024
        const val MAX_CONTACT_DIRS = 20
        val b00mMarkers = listOf("im_log_author", "\"im_in\"")
    }
}
