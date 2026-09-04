package com.etozhesandy.redpanda.core.security.model

/** Login-protection settings, backed by its own DataStore separate from the general app settings. */
data class AppLockConfig(
    val enabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val timeoutSeconds: Int = DEFAULT_TIMEOUT_SECONDS,
) {
    companion object {
        /** How long the app may stay in the background before it locks again. */
        const val DEFAULT_TIMEOUT_SECONDS = 30

        const val TIMEOUT_MIN_SECONDS = 0
        const val TIMEOUT_MAX_SECONDS = 300
        const val TIMEOUT_STEP_SECONDS = 15

        const val PIN_LENGTH = 4

        /** Failed fingerprint touches after which the lock screen falls back to the PIN pad. */
        const val BIOMETRIC_ATTEMPTS_BEFORE_PIN = 3

        /** Wrong PINs allowed before the keypad starts locking out. */
        const val PIN_ATTEMPTS_BEFORE_DELAY = 5

        const val FIRST_PENALTY_MS = 30_000L
        const val MAX_PENALTY_MS = 30 * 60_000L
    }
}
