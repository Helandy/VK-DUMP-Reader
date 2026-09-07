package com.etozhesandy.redpanda.core.archive.format

import java.io.File

/**
 * What [ArchiveFormatDetector] made of an extracted archive.
 *
 * [contentRoot] is where the parser starts reading — the directory the chosen format's markers sit
 * in. [dumpRoot] is the directory the whole export belongs to, which is regularly higher up: one
 * dump keeps its dialogs in a subfolder and its media beside that subfolder, so the profile's name,
 * its media labels and the directory it gets moved into all key on [dumpRoot], while parsing keys
 * on [contentRoot].
 */
data class ArchiveLayout(
    val format: DetectedFormat,
    val contentRoot: File,
    val dumpRoot: File = contentRoot,
)
