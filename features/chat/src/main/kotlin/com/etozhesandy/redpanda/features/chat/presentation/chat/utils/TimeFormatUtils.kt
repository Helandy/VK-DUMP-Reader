package com.etozhesandy.redpanda.features.chat.presentation.chat.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

fun formatMessageTime(epochMillis: Long): String = timeFormat.format(Date(epochMillis))

fun formatMessageDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/**
 * Runs for every row of a chat on every composition, so it compares local day numbers instead of
 * building two `Calendar`s each time. `java.time` would say it more plainly, but `minSdk` 24 has
 * no `java.time` without desugaring.
 */
fun isSameDay(epochMillisA: Long, epochMillisB: Long): Boolean {
    val timeZone = TimeZone.getDefault()
    return Math.floorDiv(epochMillisA + timeZone.getOffset(epochMillisA), MILLIS_PER_DAY) ==
        Math.floorDiv(epochMillisB + timeZone.getOffset(epochMillisB), MILLIS_PER_DAY)
}

/** `m:ss`, or `h:mm:ss` past an hour — the way a player shows a track's length. */
fun formatDuration(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}
