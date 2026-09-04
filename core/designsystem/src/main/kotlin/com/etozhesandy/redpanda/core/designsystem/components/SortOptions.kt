package com.etozhesandy.redpanda.core.designsystem.components

import com.etozhesandy.redpanda.core.designsystem.R
import com.etozhesandy.redpanda.core.model.DialogSort
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.model.MessageSort

/**
 * The user-facing labels for every sort key, shared by the screens that sort and by the settings
 * screen that picks their defaults — so a renamed option can't read differently in the two places.
 */
val DIALOG_SORT_OPTIONS = listOf(
    SortOption(DialogSort.DATE, R.string.sort_option_date),
    SortOption(DialogSort.NAME, R.string.sort_option_name),
    SortOption(DialogSort.MESSAGE_COUNT, R.string.sort_option_message_count),
)

val MESSAGE_SORT_OPTIONS = listOf(
    SortOption(MessageSort.DATE, R.string.sort_option_date),
    SortOption(MessageSort.SENDER, R.string.sort_option_sender),
)

val MEDIA_SORT_OPTIONS = listOf(
    SortOption(MediaSort.DATE, R.string.sort_option_date),
    SortOption(MediaSort.NAME, R.string.sort_option_title),
)
