package com.etozhesandy.redpanda.core.security.model

/** Whether the app content may be shown right now. */
sealed interface LockState {

    /** The stored configuration has not been read yet; show nothing rather than flashing content. */
    data object Unknown : LockState

    /** The user has not set up a login password. */
    data object Disabled : LockState

    data object Locked : LockState

    data object Unlocked : LockState
}
