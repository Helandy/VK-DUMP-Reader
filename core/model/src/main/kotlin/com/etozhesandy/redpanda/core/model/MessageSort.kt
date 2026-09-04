package com.etozhesandy.redpanda.core.model

/** What the in-dialog search results are ordered by. */
enum class MessageSort {
    DATE,
    SENDER,
}

/** The direction a freshly picked sort starts in: newest first by date, but А→Я by sender. */
val MessageSort.naturalAscending: Boolean get() = this == MessageSort.SENDER
