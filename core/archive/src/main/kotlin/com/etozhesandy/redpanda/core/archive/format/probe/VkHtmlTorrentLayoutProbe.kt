package com.etozhesandy.redpanda.core.archive.format.probe

import com.etozhesandy.redpanda.core.archive.format.DetectedFormat
import com.etozhesandy.redpanda.core.archive.format.probe.DumpDirectoryNames.childNamed
import java.io.File
import javax.inject.Inject

/**
 * The torrentvk dumper: `Переписки/{категория}/{Имя NNNN}/{N}.html`.
 *
 * The marker directory floats. It sits under `html/` in most exports, directly in the export root
 * in others, and under `Диалоги/` in one — so this keys on the marker itself and never on where the
 * marker is, which is what the enclosing directory walk is for.
 */
class VkHtmlTorrentLayoutProbe @Inject constructor() : ArchiveLayoutProbe {

    override val priority: Int = 1

    override fun probe(dir: File): DetectedFormat? {
        val conversations = dir.childNamed(TORRENT_DIALOGS_DIR)?.takeIf { it.isDirectory } ?: return null
        return DetectedFormat.VK_HTML_TORRENT.takeIf { conversations.holdsContactPages() }
    }

    /**
     * Whether the marker directory really holds `{категория}/{контакт}/{N}.html`.
     *
     * An empty `Переписки/` is not evidence of this export: claiming it would import the profile
     * with zero dialogs instead of falling back to whatever else the archive holds.
     */
    private fun File.holdsContactPages(): Boolean = listFiles().orEmpty().asSequence()
        .filter { it.isDirectory }
        .take(MAX_CATEGORIES)
        .flatMap { category -> category.listFiles().orEmpty().asSequence().filter { it.isDirectory } }
        .take(MAX_CONTACTS)
        .any { contact -> contact.listFiles().orEmpty().any(::isContactPage) }

    private fun isContactPage(file: File): Boolean =
        file.isFile && file.nameWithoutExtension.toIntOrNull() != null && file.extension.equals("html", ignoreCase = true)

    private companion object {
        const val TORRENT_DIALOGS_DIR = "Переписки"
        const val MAX_CATEGORIES = 8
        const val MAX_CONTACTS = 20
    }
}
