package com.etozhesandy.redpanda.core.archive.parse

import java.io.File

/** Turns an extracted archive rooted at [contentRoot] into domain models for one [profileId]. */
interface ChatArchiveParser {
    suspend fun parse(contentRoot: File, profileId: String, sink: ParseSink)
}
