package com.etozhesandy.redpanda.core.archive.extract

import com.etozhesandy.redpanda.core.archive.source.ArchiveSource
import java.io.File

/** Copies/unpacks an [ArchiveSource] into [destination], which lives under `profiles/$id/raw`. */
interface ArchiveExtractor {
    suspend fun extract(source: ArchiveSource, destination: File)
}
