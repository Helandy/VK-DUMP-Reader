package com.etozhesandy.redpanda.core.navigation

import kotlinx.serialization.Serializable

/** Which step the PIN setup screen opens in. */
@Serializable
enum class PinSetupMode {
    /** No PIN yet: enter a new one, then confirm it. */
    CREATE,

    /** Confirm the current PIN, then enter and confirm a new one. */
    CHANGE,

    /** Confirm the current PIN, then turn login protection off. */
    DISABLE,
}
