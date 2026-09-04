package com.etozhesandy.redpanda.core.security.model

/** Whether this device can authenticate the user biometrically right now. */
enum class BiometricAvailability {
    AVAILABLE,
    NO_HARDWARE,
    NOT_ENROLLED,
    UNAVAILABLE,
}
