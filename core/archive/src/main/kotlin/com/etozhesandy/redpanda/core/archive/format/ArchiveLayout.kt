package com.etozhesandy.redpanda.core.archive.format

import java.io.File

/**
 * What [ArchiveFormatDetector] made of an extracted archive: the [format] to parse it as, and the
 * [contentRoot] the export actually starts at, which is often a few wrapper directories below the
 * extraction root.
 */
data class ArchiveLayout(val format: DetectedFormat, val contentRoot: File)
