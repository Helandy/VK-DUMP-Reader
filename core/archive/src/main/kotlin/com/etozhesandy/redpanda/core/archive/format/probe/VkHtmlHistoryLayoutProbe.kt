package com.etozhesandy.redpanda.core.archive.format.probe

import com.etozhesandy.redpanda.core.archive.format.DetectedFormat
import com.etozhesandy.redpanda.core.archive.format.HtmlDialectSniffer
import com.etozhesandy.redpanda.core.archive.format.probe.DumpDirectoryNames.childNamed
import java.io.File
import javax.inject.Inject

/**
 * The two `history_N.htm(l)` exporters, which share a folder tree and share no markup at all — see
 * [HtmlDialectSniffer], which reads a page to tell them apart.
 *
 * The `Диалоги/` wrapper is optional. Several real exports drop it and leave the category folders
 * (`Девушки/`, `telki/`, `parni/`) sitting directly in the export root, and there is no marker
 * directory to key on in that case. The sniffer decides both ways: it only answers for a directory
 * that really holds `{категория}/{контакт}/history_N.htm(l)`, and returns null for everything else.
 */
class VkHtmlHistoryLayoutProbe @Inject constructor(
    private val dialectSniffer: HtmlDialectSniffer,
) : ArchiveLayoutProbe {

    override val priority: Int = 1

    override fun probe(dir: File): DetectedFormat? {
        val wrapper = dir.childNamed(HTML_DIALOGS_DIR)?.takeIf { it.isDirectory }
        return dialectSniffer.sniff(wrapper ?: dir)
    }

    private companion object {
        const val HTML_DIALOGS_DIR = "Диалоги"
    }
}
