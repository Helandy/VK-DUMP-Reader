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
