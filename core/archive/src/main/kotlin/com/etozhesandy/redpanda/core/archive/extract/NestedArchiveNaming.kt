package com.etozhesandy.redpanda.core.archive.extract

import java.io.File

/**
 * Names the directories [ArchiveNormalizer] creates when it unpacks an archive that was itself a
 * file inside the import.
 *
 * Shared with format detection rather than kept private to the extractor: the wrapper directory is
 * an artefact of unpacking, not part of the export, so detection has to see through it to work out
 * which candidates belong to the same dump.
 */
internal object NestedArchiveNaming {

    /** Suffix that cannot collide with a format marker directory — see [ArchiveNormalizer]. */
    const val EXTRACTED_SUFFIX = ".extracted"

    fun File.isExtractedWrapper(): Boolean = name.endsWith(EXTRACTED_SUFFIX)

    /**
     * The base name of the archive this wrapper came from — `json.zip.extracted` gives `json` —
     * or null when this is not a wrapper directory.
     */
    fun File.nestedArchiveBaseName(): String? =
        if (isExtractedWrapper()) {
            name.removeSuffix(EXTRACTED_SUFFIX).substringBeforeLast('.')
        } else {
            null
        }
}
