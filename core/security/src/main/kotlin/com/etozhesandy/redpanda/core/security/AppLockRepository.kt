package com.etozhesandy.redpanda.core.security

import com.etozhesandy.redpanda.core.security.model.AppLockConfig
import com.etozhesandy.redpanda.core.security.model.PinCheckResult
import kotlinx.coroutines.flow.Flow

/** Stores the login PIN (as a salted hash) and the lock preferences around it. */
interface AppLockRepository {

    val config: Flow<AppLockConfig>

    /** Replaces the PIN with [pin] and turns the lock on, clearing any lockout penalty. */
    suspend fun setPin(pin: String)

    suspend fun verifyPin(pin: String): PinCheckResult

    /** Removes the PIN and every derived setting, turning login protection off. */
    suspend fun clearLock()

    suspend fun setBiometricEnabled(value: Boolean)

    suspend fun setTimeoutSeconds(value: Int)

    /** Milliseconds left of the current lockout penalty, or 0 when entry is allowed. */
    suspend fun lockoutRemainingMs(): Long
}
