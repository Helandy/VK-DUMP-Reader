package com.etozhesandy.redpanda.core.security

import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.dispatcher.MainDispatcher
import com.etozhesandy.redpanda.core.security.model.AppLockConfig
import com.etozhesandy.redpanda.core.security.model.LockState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Single source of truth for whether the app is currently locked. Locks on a cold start and again
 * when the app returns from the background after more than the configured timeout.
 */
@Singleton
class AppLockManager @Inject constructor(
    repository: AppLockRepository,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
    @MainDispatcher mainDispatcher: CoroutineDispatcher,
) {

    private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)

    private val _state = MutableStateFlow<LockState>(LockState.Unknown)
    val state: StateFlow<LockState> = _state.asStateFlow()

    @Volatile
    private var config: AppLockConfig = AppLockConfig()

    /** Elapsed-realtime mark, immune to the user changing the wall clock. */
    @Volatile
    private var backgroundedAtMs: Long = 0L

    /** False until the stored config has been read once, which is what identifies a cold start. */
    @Volatile
    private var configLoaded: Boolean = false

    init {
        repository.config
            .onEach { latest ->
                val wasEnabled = config.enabled
                val wasLoaded = configLoaded
                config = latest
                configLoaded = true
                _state.value = when {
                    !latest.enabled -> LockState.Disabled
                    // A cold start with protection already on begins locked.
                    !wasLoaded -> LockState.Locked
                    // Protection was just switched on from the settings: the user is right here,
                    // so setting a PIN must not lock them out of the session they are in.
                    !wasEnabled -> LockState.Unlocked
                    // An unrelated setting changed; whatever the session was, it stays.
                    _state.value == LockState.Unlocked -> LockState.Unlocked
                    else -> LockState.Locked
                }
            }
            .launchIn(scope)

        scope.launch(mainDispatcher) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStop(owner: LifecycleOwner) {
                        backgroundedAtMs = SystemClock.elapsedRealtime()
                    }

                    override fun onStart(owner: LifecycleOwner) {
                        if (backgroundedAtMs != 0L) relockIfTimedOut()
                    }
                },
            )
        }
    }

    fun unlock() {
        if (config.enabled) _state.value = LockState.Unlocked
    }

    fun lockNow() {
        if (config.enabled) _state.value = LockState.Locked
    }

    private fun relockIfTimedOut() {
        if (!config.enabled || _state.value != LockState.Unlocked) return
        val awayMs = SystemClock.elapsedRealtime() - backgroundedAtMs
        if (awayMs >= config.timeoutSeconds * MILLIS_PER_SECOND) _state.value = LockState.Locked
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
