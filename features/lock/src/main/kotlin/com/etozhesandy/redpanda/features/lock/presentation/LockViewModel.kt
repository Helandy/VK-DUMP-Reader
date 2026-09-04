package com.etozhesandy.redpanda.features.lock.presentation

import androidx.biometric.BiometricPrompt
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.security.model.AppLockConfig
import com.etozhesandy.redpanda.core.security.model.BiometricAvailability
import com.etozhesandy.redpanda.core.security.model.LockState as AppLockState
import com.etozhesandy.redpanda.core.security.model.PinCheckResult
import com.etozhesandy.redpanda.features.lock.R
import com.etozhesandy.redpanda.features.lock.domain.usecase.GetBiometricAvailabilityUseCase
import com.etozhesandy.redpanda.features.lock.domain.usecase.GetLockoutRemainingUseCase
import com.etozhesandy.redpanda.features.lock.domain.usecase.ObserveAppLockConfigUseCase
import com.etozhesandy.redpanda.features.lock.domain.usecase.ObserveLockStateUseCase
import com.etozhesandy.redpanda.features.lock.domain.usecase.UnlockAppUseCase
import com.etozhesandy.redpanda.features.lock.domain.usecase.VerifyPinUseCase
import com.etozhesandy.redpanda.features.lock.presentation.model.LockMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@HiltViewModel
class LockViewModel @Inject constructor(
    private val observeAppLockConfig: ObserveAppLockConfigUseCase,
    private val getBiometricAvailability: GetBiometricAvailabilityUseCase,
    private val getLockoutRemaining: GetLockoutRemainingUseCase,
    observeLockState: ObserveLockStateUseCase,
    private val verifyPin: VerifyPinUseCase,
    private val unlockApp: UnlockAppUseCase,
) : BaseViewModel<LockState.State, LockState.Event, LockState.Effect>() {

    private var countdownJob: Job? = null

    override fun createInitialState() = LockState.State()

    init {
        // This ViewModel is kept by the activity, not by the gate composable, so it outlives a
        // single lock. Re-arming on every transition into Locked is what clears the previous
        // session's half-typed PIN and failure counters.
        observeLockState()
            .filter { it is AppLockState.Locked }
            .onEach { arm() }
            .launchIn(viewModelScope)
    }

    private fun arm() = launchSafe {
        countdownJob?.cancel()
        setState { LockState.State() }
        val config = observeAppLockConfig().first()
        val canUseBiometric = config.biometricEnabled &&
            getBiometricAvailability() == BiometricAvailability.AVAILABLE
        setState { copy(canUseBiometric = canUseBiometric) }
        startCountdown(getLockoutRemaining())
        if (canUseBiometric) requestBiometricPrompt()
    }

    override fun onEvent(event: LockState.Event) {
        when (event) {
            is LockState.Event.PinDigitEntered -> onDigit(event.digit)
            LockState.Event.PinBackspacePressed -> setState {
                copy(pin = pin.dropLast(1), errorRes = null, errorArg = null)
            }
            LockState.Event.UsePinClicked -> setState { copy(mode = LockMode.PIN) }
            LockState.Event.UseBiometricClicked -> requestBiometricPrompt()
            LockState.Event.BiometricSucceeded -> unlockApp()
            LockState.Event.BiometricFailed -> onBiometricFailed()
            is LockState.Event.BiometricErrored -> onBiometricErrored(event.code)
        }
    }

    private fun requestBiometricPrompt() = setState {
        copy(
            mode = LockMode.BIOMETRIC,
            errorRes = null,
            errorArg = null,
            biometricRequestId = biometricRequestId + 1,
        )
    }

    private fun onDigit(digit: Int) {
        if (!currentState.isKeypadEnabled || currentState.pin.length >= AppLockConfig.PIN_LENGTH) return
        val pin = currentState.pin + digit
        setState { copy(pin = pin, errorRes = null, errorArg = null) }
        if (pin.length == AppLockConfig.PIN_LENGTH) submit(pin)
    }

    private fun submit(pin: String) = launchSafe {
        when (val result = verifyPin(pin)) {
            PinCheckResult.Success -> unlockApp()
            is PinCheckResult.Wrong -> setState {
                copy(
                    pin = "",
                    errorRes = if (result.attemptsLeft <= 1) R.string.lock_wrong_pin_last else R.string.lock_wrong_pin,
                    errorArg = result.attemptsLeft,
                )
            }
            is PinCheckResult.LockedOut -> {
                setState { copy(pin = "", errorRes = null, errorArg = null) }
                startCountdown(result.remainingMs)
            }
        }
    }

    /** A wrong finger, not a sensor problem: after three of them the PIN pad takes over. */
    private fun onBiometricFailed() {
        val failures = currentState.biometricFailures + 1
        setState {
            copy(
                biometricFailures = failures,
                errorRes = R.string.lock_biometric_failed,
                errorArg = null,
            )
        }
        if (failures >= AppLockConfig.BIOMETRIC_ATTEMPTS_BEFORE_PIN) {
            setState { copy(mode = LockMode.PIN, errorRes = null, errorArg = null) }
        }
    }

    /**
     * Every error ends the prompt, so the only way forward is the PIN. A user cancel keeps the
     * "use fingerprint" button available; a sensor lockout does not.
     */
    private fun onBiometricErrored(code: Int) {
        val sensorLockedOut = code == BiometricPrompt.ERROR_LOCKOUT ||
            code == BiometricPrompt.ERROR_LOCKOUT_PERMANENT
        setState {
            copy(
                mode = LockMode.PIN,
                canUseBiometric = canUseBiometric && !sensorLockedOut,
            )
        }
    }

    private fun startCountdown(remainingMs: Long) {
        countdownJob?.cancel()
        setState { copy(lockoutRemainingMs = remainingMs) }
        if (remainingMs <= 0L) return
        countdownJob = viewModelScope.launch {
            var left = remainingMs
            while (left > 0L) {
                delay(TICK_MS)
                left -= TICK_MS
                setState { copy(lockoutRemainingMs = left.coerceAtLeast(0L)) }
            }
        }
    }

    private companion object {
        const val TICK_MS = 1000L
    }
}
