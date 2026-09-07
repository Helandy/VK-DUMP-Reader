package com.etozhesandy.redpanda.core.archive.format.probe

import com.etozhesandy.redpanda.core.archive.format.DetectedFormat
import com.etozhesandy.redpanda.core.archive.format.probe.DumpDirectoryNames.childNamed
import com.etozhesandy.redpanda.core.archive.format.probe.DumpDirectoryNames.hasPeerIdSubdirectory
import java.io.File
import javax.inject.Inject

/**
 * The exporter that saves raw `messages.getHistory` responses: `messages/{peerId}/{1..N}.json`
 * beside the small offline viewer that reads them.
 *
 * Keyed on the per-peer directories rather than on `profile.json`, which the previous detector also
 * required. That extra condition cost real imports: an export split across nested archives can land
 * its `messages/` tree in one of them and its profile file in another, and demanding both in the
 * same directory sent the whole dump to a media-only import with no dialogs and no error.
 */
class VkApiLayoutProbe @Inject constructor() : ArchiveLayoutProbe {

    override val priority: Int = 0

    override fun probe(dir: File): DetectedFormat? {
        val messages = dir.childNamed(MESSAGES_DIR)?.takeIf { it.isDirectory } ?: return null
        return DetectedFormat.VK_API.takeIf { messages.hasPeerIdSubdirectory() }
    }

    private companion object {
        const val MESSAGES_DIR = "messages"
    }
}
