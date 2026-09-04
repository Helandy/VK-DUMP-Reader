package com.etozhesandy.redpanda.core.security.model

/** Outcome of verifying a PIN entered on the lock screen. */
sealed interface PinCheckResult {

    data object Success : PinCheckResult

    data class Wrong(val attemptsLeft: Int) : PinCheckResult

    /** Too many wrong PINs: entry is refused until [remainingMs] elapses. */
    data class LockedOut(val remainingMs: Long) : PinCheckResult
}
