package com.etozhesandy.redpanda.features.lock.presentation.model

/** The step the PIN setup screen is on; which steps apply depends on the requested mode. */
enum class PinSetupStep {
    /** Confirm the PIN already in use, before changing or removing it. */
    CURRENT,
    NEW,
    CONFIRM,
}
