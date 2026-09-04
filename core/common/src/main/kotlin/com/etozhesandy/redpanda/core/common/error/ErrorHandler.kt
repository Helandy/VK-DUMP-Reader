package com.etozhesandy.redpanda.core.common.error

/** Maps a caught [Throwable] to a user-facing message. */
interface ErrorHandler {
    fun messageFor(throwable: Throwable): String
}
