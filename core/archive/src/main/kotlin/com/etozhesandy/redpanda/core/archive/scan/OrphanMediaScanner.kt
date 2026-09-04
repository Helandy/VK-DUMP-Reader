package com.etozhesandy.redpanda.core.archive.scan

import com.etozhesandy.redpanda.core.model.AttachmentType
import java.io.File

/**
 * VK export folders often contain photo/video files that no parser's HTML/JSON walk ever
 * reads — e.g. VK exports can include arbitrary per-contact dump folders (renamed "Видео"/"Видс",
 * or a custom folder the export tool created) full of raw `.mp4`/`.jpg` files not referenced by any
 * `photos.html`/`videos.html`/dialog history. Since the whole archive is already copied byte-for-byte
 * into app storage on import, walking it by file extension is enough to surface this media instead of
 * silently dropping it.
 */
/** One orphan file found by [OrphanMediaScanner.scan], with [folder] — the path of the directory
 * it was found in, relative to the scan's label root — so callers can group results by origin folder. */
data class OrphanMediaFile(val file: File, val type: AttachmentType, val folder: String)

object OrphanMediaScanner {

    private val photoExtensions = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
    private val videoExtensions = setOf("mp4", "mov", "webm", "mkv", "avi", "m4v", "3gp")

    /**
     * Walks all of [root] — which should be the whole extracted archive, not just the detected
     * content root: real exports keep media *outside* the folder detection settles on (one dump
     * has ~430 photos and videos sitting one level above it, another has them as siblings of the
     * export dir), and scanning only the content root drops every one of them.
     *
     * [labelRoot] is what [OrphanMediaFile.folder] is reported relative to — pass the content root
     * so folder labels stay the short, meaningful ones the media screen groups by, instead of
     * gaining the wrapper-directory prefix that scanning from [root] would otherwise add. Files
     * found outside [labelRoot] fall back to being labelled relative to [root].
     */
    fun scan(root: File, labelRoot: File = root): List<OrphanMediaFile> {
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown()
            .filter { it.isFile }
            .mapNotNull { file ->
                val type = when (file.extension.lowercase()) {
                    in photoExtensions -> AttachmentType.PHOTO
                    in videoExtensions -> AttachmentType.VIDEO
                    else -> null
                } ?: return@mapNotNull null
                OrphanMediaFile(file, type, folderLabel(file, root, labelRoot))
            }
            .toList()
    }

    private fun folderLabel(file: File, root: File, labelRoot: File): String {
        val parent = file.parentFile ?: return root.name
        // relativeTo happily walks upwards, so a file *above* the label root comes back as "..",
        // which is no kind of folder name. Anything that escapes is labelled from the scan root.
        val relative = parent.relativeToOrNull(labelRoot)?.path?.takeIf { !it.startsWith("..") }
            ?: parent.relativeToOrNull(root)?.path
        return relative.orEmpty().ifBlank { labelRoot.name }
    }
}
