package com.etozhesandy.redpanda.features.chat.presentation.chat.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

fun formatMessageTime(epochMillis: Long): String = timeFormat.format(Date(epochMillis))

fun formatMessageDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

fun isSameDay(epochMillisA: Long, epochMillisB: Long): Boolean {
    val calendarA = Calendar.getInstance().apply { timeInMillis = epochMillisA }
    val calendarB = Calendar.getInstance().apply { timeInMillis = epochMillisB }
    return calendarA.get(Calendar.YEAR) == calendarB.get(Calendar.YEAR) &&
        calendarA.get(Calendar.DAY_OF_YEAR) == calendarB.get(Calendar.DAY_OF_YEAR)
}
