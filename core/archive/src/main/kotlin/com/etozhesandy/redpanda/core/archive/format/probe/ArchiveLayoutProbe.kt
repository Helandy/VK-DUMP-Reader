package com.etozhesandy.redpanda.core.archive.format.probe

import com.etozhesandy.redpanda.core.archive.format.DetectedFormat
import java.io.File

/**
 * Recognises one export layout in one directory.
 *
 * The point of the interface is isolation. Detection used to be a single ordered `when` over
 * marker files, and every new export shape was another branch inserted into it — which is how
 * teaching the app one archive kept silently un-teaching it another: the branches competed by
 * position, so a shape that matched earlier stole directories from a shape that matched later, and
 * nothing said so.
 *
 * A probe therefore may not know about any other probe, and may not depend on the order it is
 * called in. Where two probes both recognise a directory, [priority] decides, and it is the only
 * thing that decides. Adding a shape is a new file plus one binding in `ArchiveProbeModule`; no
 * existing probe is edited, so no existing archive can change behaviour by accident.
 */
interface ArchiveLayoutProbe {

    /**
     * Lower wins. Structured JSON exports are 0 and HTML exports are 1, because a dump regularly
     * ships both — the same conversations as machine-readable JSON and as an offline HTML viewer —
     * and the JSON copy is strictly richer (493 dialogs / 517 138 messages against 486 / 515 877 on
     * the export this was measured on).
     */
    val priority: Int

    /** The format [dir] should be read as, or null when this probe does not recognise it. */
    fun probe(dir: File): DetectedFormat?
}
