package com.etozhesandy.redpanda.features.settings.presentation.utils

import com.etozhesandy.redpanda.core.security.model.AppLockConfig

/** The final slider stop, reserved for the never-relock sentinel. */
const val LOCK_TIMEOUT_NEVER_INDEX =
    (AppLockConfig.TIMEOUT_MAX_SECONDS - AppLockConfig.TIMEOUT_MIN_SECONDS) /
        AppLockConfig.TIMEOUT_STEP_SECONDS + 1

/** Maps a discrete timeout-slider index to the persisted timeout value. */
fun lockTimeoutSecondsFor(index: Int): Int {
    val normalizedIndex = index.coerceIn(0, LOCK_TIMEOUT_NEVER_INDEX)
    return if (normalizedIndex == LOCK_TIMEOUT_NEVER_INDEX) {
        AppLockConfig.TIMEOUT_NEVER_SECONDS
    } else {
        AppLockConfig.TIMEOUT_MIN_SECONDS + normalizedIndex * AppLockConfig.TIMEOUT_STEP_SECONDS
    }
}

/** Maps a persisted timeout value to its closest lower discrete timeout-slider index. */
fun lockTimeoutIndexOf(seconds: Int): Int = when (seconds) {
    AppLockConfig.TIMEOUT_NEVER_SECONDS -> LOCK_TIMEOUT_NEVER_INDEX
    else -> ((seconds - AppLockConfig.TIMEOUT_MIN_SECONDS) / AppLockConfig.TIMEOUT_STEP_SECONDS)
        .coerceIn(0, LOCK_TIMEOUT_NEVER_INDEX - 1)
}
