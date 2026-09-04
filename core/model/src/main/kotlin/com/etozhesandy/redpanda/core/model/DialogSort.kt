package com.etozhesandy.redpanda.core.model

/**
 * What the dialog list is ordered by. [DATE] descending is the default the DAO already returns.
 *
 * Peer names come from the archive in mixed scripts and cases (Cyrillic, Latin, leading "!!"
 * markers people put in VK names), so [NAME] compares case-insensitively to keep the order close
 * to what a reader expects rather than to raw code points.
 */
enum class DialogSort {
    DATE,
    NAME,
    MESSAGE_COUNT,
}

/**
 * The direction a freshly picked sort starts in: newest / most messages first, but names А→Я.
 */
val DialogSort.naturalAscending: Boolean get() = this == DialogSort.NAME
