package com.etozhesandy.redpanda.core.archive.format

import com.etozhesandy.redpanda.core.archive.format.probe.ArchiveLayoutProbe
import com.etozhesandy.redpanda.core.common.files.isInside
import java.io.File
import javax.inject.Inject

/**
 * Works out how to read an extracted archive, and where in it the export actually starts.
 *
 * Every directory is offered to every [ArchiveLayoutProbe]; none of them knows about the others.
 * That is deliberate. Detection used to be one ordered `when` over marker files, and each export
 * shape added to it competed with the shapes already there by position alone — so teaching the app
 * one archive quietly stopped it recognising another, with no failure anywhere to point at.
 *
 * Depth is not bounded. Real exports bury their content anywhere from the extraction root to four
 * levels down, and a nested archive adds a level of its own, so any fixed limit is a shape the app
 * simply cannot read. The walk is bounded by [MAX_DIRECTORIES_VISITED] and by not descending into
 * directories that are the body of an export rather than a place another export could hide.
 */
class ArchiveFormatDetector @Inject constructor(
    private val probes: Set<@JvmSuppressWildcards ArchiveLayoutProbe>,
) {

    /** The export to read [root] as — the best of [detectAll], or a media-only import when it finds none. */
    fun detect(root: File): ArchiveLayout =
        detectAll(root).firstOrNull() ?: mediaRoot(root).let { ArchiveLayout(DetectedFormat.MEDIA_ONLY, it, it) }

    /**
     * Every independent export inside [root], best first, or empty when it holds none.
     *
     * More than one is a real shape, not a corner case: one archive ships three unrelated dumps
     * side by side, each in its own nested rar. They are separate profiles and each has to be
     * imported as one, so all of them are returned rather than just the richest.
     *
     * Candidates that nest are one export in several shapes, not several exports, and only the best
     * of each nest survives. Two real cases drive this: a dump that keeps a complete JSON export and
     * an HTML copy of the same conversations under one profile folder, and a dump whose `Диалоги/`
     * sits at the profile folder while a richer API export sits in a subfolder beside it. Grouping
     * is by containment and is **transitive**, so a folder that contains two others ties all three
     * into one group even though those two contain neither each other nor anything else.
     */
    fun detectAll(root: File): List<ArchiveLayout> =
        group(scan(root))
            .map { candidates ->
                val best = candidates.min()
                ArchiveLayout(
                    format = best.format,
                    contentRoot = best.directory,
                    // The shallowest member, not the best one: the export owns everything from
                    // there down, including media kept beside the folder the parser reads.
                    dumpRoot = candidates.minBy { it.depth }.directory,
                )
            }
            .sortedWith(compareBy({ priority(it.format) }, { it.contentRoot.path }))

    /**
     * Every directory of [root] that a probe recognised, breadth-first.
     *
     * Descent stops below a match only when that match is the richest kind there is. Stopping at
     * *any* match reads one real export wrong: its `Диалоги/` folder matches at the profile
     * directory while a complete API export sits one level below in a sibling folder, and pruning
     * there would have thrown away every message id and attachment in it.
     */
    private fun scan(root: File): List<Candidate> {
        val found = mutableListOf<Candidate>()
        var level = listOf(root)
        var depth = 0
        var visited = 0
        while (level.isNotEmpty() && visited < MAX_DIRECTORIES_VISITED) {
            val next = mutableListOf<File>()
            for (directory in level) {
                if (visited++ >= MAX_DIRECTORIES_VISITED) break
                val format = classify(directory)
                if (format != null) found += Candidate(format, directory, depth)
                if (format != null && priority(format) == RICHEST_PRIORITY) continue
                if (directory != root && !directory.holdsExportBody()) continue
                next += directory.listFiles().orEmpty().filter { it.isDirectory }
            }
            level = next
            depth++
        }
        return found
    }

    /** The best format any probe reports for [dir], or null when none recognises it. */
    private fun classify(dir: File): DetectedFormat? = probes
        .sortedBy { it.priority }
        .firstNotNullOfOrNull { probe -> runCatching { probe.probe(dir) }.getOrNull() }

    /**
     * Whether descending into [this] could turn up another export.
     *
     * Category and per-peer folders cannot: they are the inside of an export, and walking them
     * costs thousands of directories on a large dump for nothing. A folder named after a peer id is
     * treated the same way, which is what `messages/{peerId}/` and its per-page files are.
     */
    private fun File.holdsExportBody(): Boolean =
        name !in EXPORT_BODY_DIRS && name.toLongOrNull() == null

    /**
     * Ties candidates that nest into one group, transitively.
     *
     * A candidate touching several groups merges them: containment is transitive, and the
     * enclosing folder is regularly the last one found.
     */
    private fun group(candidates: List<Candidate>): List<List<Candidate>> {
        val groups = mutableListOf<MutableList<Candidate>>()
        for (candidate in candidates) {
            val (touching, separate) = groups.partition { group ->
                group.any { it.directory.overlaps(candidate.directory) }
            }
            val merged = touching.flatten().toMutableList().apply { add(candidate) }
            groups.clear()
            groups += separate
            groups += merged
        }
        return groups
    }

    /** Whether one directory is the other, or contains it: either way they are one export, not two. */
    private fun File.overlaps(other: File): Boolean =
        this == other || isInside(other) || other.isInside(this)

    /**
     * Where a dialog-less dump "starts". It names the profile and labels its media folders, so
     * landing on the extraction root would call every such profile "raw".
     *
     * Prefers the shallowest directory carrying an export marker; failing that, unwraps directories
     * that hold nothing but a single subdirectory, which is what a folder picked one level too high
     * looks like.
     */
    private fun mediaRoot(root: File): File {
        var level = listOf(root)
        repeat(MAX_MEDIA_UNWRAP_DEPTH + 1) {
            level.firstOrNull(::hasWeakMarker)?.let { return it }
            level = level.flatMap { dir -> dir.listFiles().orEmpty().filter { it.isDirectory } }
        }
        var candidate = root
        repeat(MAX_MEDIA_UNWRAP_DEPTH) {
            val onlyChild = candidate.listFiles().orEmpty().singleOrNull()?.takeIf { it.isDirectory } ?: return candidate
            candidate = onlyChild
        }
        return candidate
    }

    private fun hasWeakMarker(dir: File): Boolean = WEAK_MARKERS.any { File(dir, it).exists() }

    private fun priority(format: DetectedFormat): Int = when (format) {
        DetectedFormat.VK_API, DetectedFormat.VK_JSON_DUMP -> 0
        DetectedFormat.VK_HTML_TORRENT, DetectedFormat.VK_HTML_B00M, DetectedFormat.VK_HTML_CLASSIC -> 1
        DetectedFormat.MEDIA_ONLY -> 2
    }

    /** Ranked by how rich the format is first, then by how near the top of the archive it sits. */
    private inner class Candidate(
        val format: DetectedFormat,
        val directory: File,
        val depth: Int,
    ) : Comparable<Candidate> {
        override fun compareTo(other: Candidate): Int =
            compareValuesBy(this, other, { priority(it.format) }, { it.depth })
    }

    private companion object {
        const val MAX_DIRECTORIES_VISITED = 20_000
        const val MAX_MEDIA_UNWRAP_DEPTH = 3
        const val RICHEST_PRIORITY = 0

        /** Directories that are the inside of an export, so no second export can be hiding in them. */
        val EXPORT_BODY_DIRS = setOf(
            "messages", "Диалоги", "Переписки", "attachments", "Вложения", "Исходящие вложения",
            "photo", "photos", "video", "videos", "img", "css", "js", "fonts", "saved",
            "friends", "groups",
        )

        /** Enough to recognise an export folder, but not enough to promise it holds dialogs. */
        val WEAK_MARKERS = listOf(
            "Вложения",
            "Друзья",
            "Друзья.txt",
            "Диалоги.html",
            "profile.json",
            "json/profile.json",
        )
    }
}
