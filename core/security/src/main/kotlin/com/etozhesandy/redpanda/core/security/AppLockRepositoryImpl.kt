package com.etozhesandy.redpanda.core.security

import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.security.di.AppLockDataStore
import com.etozhesandy.redpanda.core.security.model.AppLockConfig
import com.etozhesandy.redpanda.core.security.model.PinCheckResult
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class AppLockRepositoryImpl @Inject constructor(
    @AppLockDataStore private val dataStore: DataStore<Preferences>,
    private val pinHasher: PinHasher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : AppLockRepository {

    override val config: Flow<AppLockConfig> = dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { preferences ->
            AppLockConfig(
                // A stored hash is what actually gates the app: without one there is nothing to
                // check, so the flag alone must never lock the user out.
                enabled = preferences[ENABLED_KEY] == true && preferences[PIN_HASH_KEY] != null,
                biometricEnabled = preferences[BIOMETRIC_ENABLED_KEY] ?: false,
                timeoutSeconds = preferences[TIMEOUT_SECONDS_KEY] ?: AppLockConfig.DEFAULT_TIMEOUT_SECONDS,
            )
        }

    override suspend fun setPin(pin: String) {
        val salt = pinHasher.newSalt()
        val hash = withContext(defaultDispatcher) { pinHasher.hash(pin, salt) }
        dataStore.edit { preferences ->
            preferences[PIN_SALT_KEY] = salt.encode()
            preferences[PIN_HASH_KEY] = hash.encode()
            preferences[ENABLED_KEY] = true
            preferences[FAILED_ATTEMPTS_KEY] = 0
            preferences[LOCKED_UNTIL_KEY] = 0L
        }
    }

    override suspend fun verifyPin(pin: String): PinCheckResult {
        val preferences = dataStore.data.first()
        val remaining = preferences.lockoutRemaining()
        if (remaining > 0) return PinCheckResult.LockedOut(remaining)

        val salt = preferences[PIN_SALT_KEY]?.decode()
        val hash = preferences[PIN_HASH_KEY]?.decode()
        if (salt == null || hash == null) return PinCheckResult.Wrong(attemptsLeft = 0)

        val matches = withContext(defaultDispatcher) { pinHasher.verify(pin, salt, hash) }
        if (matches) {
            dataStore.edit {
                it[FAILED_ATTEMPTS_KEY] = 0
                it[LOCKED_UNTIL_KEY] = 0L
            }
            return PinCheckResult.Success
        }

        val failed = (preferences[FAILED_ATTEMPTS_KEY] ?: 0) + 1
        val penalty = penaltyFor(failed)
        dataStore.edit {
            it[FAILED_ATTEMPTS_KEY] = failed
            if (penalty > 0) it[LOCKED_UNTIL_KEY] = System.currentTimeMillis() + penalty
        }
        return if (penalty > 0) {
            PinCheckResult.LockedOut(penalty)
        } else {
            PinCheckResult.Wrong(attemptsLeft = AppLockConfig.PIN_ATTEMPTS_BEFORE_DELAY - failed)
        }
    }

    override suspend fun clearLock() {
        dataStore.edit { preferences -> preferences.clear() }
    }

    override suspend fun setBiometricEnabled(value: Boolean) {
        dataStore.edit { preferences -> preferences[BIOMETRIC_ENABLED_KEY] = value }
    }

    override suspend fun setTimeoutSeconds(value: Int) {
        dataStore.edit { preferences -> preferences[TIMEOUT_SECONDS_KEY] = value }
    }

    override suspend fun lockoutRemainingMs(): Long = dataStore.data.first().lockoutRemaining()

    /**
     * Capped at the longest penalty we ever hand out, so moving the system clock forward and back
     * cannot strand the user behind a multi-year countdown.
     */
    private fun Preferences.lockoutRemaining(): Long {
        val until = this[LOCKED_UNTIL_KEY] ?: 0L
        return (until - System.currentTimeMillis()).coerceIn(0L, AppLockConfig.MAX_PENALTY_MS)
    }

    /** Doubles with every wrong PIN past the free attempts; 0 while attempts are still free. */
    private fun penaltyFor(failedAttempts: Int): Long {
        val overshoot = failedAttempts - AppLockConfig.PIN_ATTEMPTS_BEFORE_DELAY
        if (overshoot < 0) return 0L
        val doublings = min(overshoot, MAX_DOUBLINGS)
        return min(AppLockConfig.FIRST_PENALTY_MS shl doublings, AppLockConfig.MAX_PENALTY_MS)
    }

    private fun ByteArray.encode(): String = Base64.encodeToString(this, Base64.NO_WRAP)

    private fun String.decode(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

    private companion object {
        /** Guards the shift in [penaltyFor] from overflowing before MAX_PENALTY_MS clamps it. */
        const val MAX_DOUBLINGS = 16

        val ENABLED_KEY = booleanPreferencesKey("lock_enabled")
        val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        val TIMEOUT_SECONDS_KEY = intPreferencesKey("timeout_seconds")
        val PIN_HASH_KEY = stringPreferencesKey("pin_hash")
        val PIN_SALT_KEY = stringPreferencesKey("pin_salt")
        val FAILED_ATTEMPTS_KEY = intPreferencesKey("failed_pin_attempts")
        val LOCKED_UNTIL_KEY = longPreferencesKey("locked_until_ms")
    }
}
