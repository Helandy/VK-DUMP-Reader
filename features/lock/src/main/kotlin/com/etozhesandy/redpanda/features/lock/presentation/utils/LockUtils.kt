package com.etozhesandy.redpanda.features.lock.presentation.utils

import java.util.Locale

/** Formats a lockout countdown as mm:ss, rounding a partial second up so it never reads 0:00. */
fun formatCountdown(remainingMs: Long): String {
    val totalSeconds = (remainingMs + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
    return String.format(Locale.US, "%d:%02d", totalSeconds / SECONDS_PER_MINUTE, totalSeconds % SECONDS_PER_MINUTE)
}

private const val MILLIS_PER_SECOND = 1000L
private const val SECONDS_PER_MINUTE = 60L
