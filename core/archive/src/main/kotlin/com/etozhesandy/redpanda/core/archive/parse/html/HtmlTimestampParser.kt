package com.etozhesandy.redpanda.core.archive.parse.html

import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Reads one dialect's date format. Deliberately **not** thread-safe: [SimpleDateFormat] isn't, and
 * contacts parse concurrently, so [HtmlDialogArchiveParser] builds one of these per contact rather
 * than sharing a formatter that would corrupt results across coroutines.
 *
 * An unreadable timestamp yields 0 rather than dropping the message — a message with a wrong sort
 * position is still worth keeping.
 */
class HtmlTimestampParser(pattern: String) {

    private val format = SimpleDateFormat(pattern, Locale.forLanguageTag("ru"))

    fun parse(text: String): Long = runCatching { format.parse(text.trim())?.time }.getOrNull() ?: 0L
}
