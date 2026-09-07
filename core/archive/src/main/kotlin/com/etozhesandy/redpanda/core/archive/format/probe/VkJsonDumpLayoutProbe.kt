package com.etozhesandy.redpanda.core.archive.format.probe

import com.etozhesandy.redpanda.core.archive.format.DetectedFormat
import com.etozhesandy.redpanda.core.archive.parse.vk.VkJsonDumpPaths
import java.io.File
import javax.inject.Inject

/**
 * The exporter that ships a `json/` directory of its own data next to an `html/` viewer.
 *
 * It exists in two builds that differ only in what things are called — `json/dialogs/` and
 * `profile.json` in one, `json/conversations/` and `страница.json` in the other — and the second
 * was invisible to the previous detector. Five real exports therefore fell through to the HTML copy
 * of the same conversations, which carries no message ids, no attachment metadata and no friend
 * list, or to a media-only import when they had no HTML copy at all.
 *
 * Neither build's profile file is required: the dialogs are the export, and a dump whose profile
 * file went missing still has every message in it.
 */
class VkJsonDumpLayoutProbe @Inject constructor() : ArchiveLayoutProbe {

    override val priority: Int = 0

    override fun probe(dir: File): DetectedFormat? {
        val dialogs = VkJsonDumpPaths.dialogsDir(dir) ?: return null
        return DetectedFormat.VK_JSON_DUMP.takeIf { VkJsonDumpPaths.dialogFiles(dialogs).isNotEmpty() }
    }
}
