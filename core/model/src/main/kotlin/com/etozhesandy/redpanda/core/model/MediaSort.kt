package com.etozhesandy.redpanda.core.model

/** What a dialog's photo / video / audio tab is ordered by. */
enum class MediaSort {
    DATE,
    NAME,
}

/**
 * Oldest first is the natural default here: the media tabs read as a timeline of the dialog,
 * matching the order attachments were sent in.
 */
@Suppress("UnusedReceiverParameter")
val MediaSort.naturalAscending: Boolean get() = true
