package com.etozhesandy.redpanda.features.lock.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.navigation.PinSetupMode
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.security.model.AppLockConfig
import com.etozhesandy.redpanda.core.security.model.PinCheckResult
import com.etozhesandy.redpanda.features.lock.R
import com.etozhesandy.redpanda.features.lock.domain.usecase.ClearAppLockUseCase
import com.etozhesandy.redpanda.features.lock.domain.usecase.GetLockoutRemainingUseCase
import com.etozhesandy.redpanda.features.lock.domain.usecase.SetPinUseCase
import com.etozhesandy.redpanda.features.lock.domain.usecase.VerifyPinUseCase
import com.etozhesandy.redpanda.features.lock.presentation.model.PinSetupStep
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class PinSetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val nav: INavigationManager,
    private val verifyPin: VerifyPinUseCase,
    private val setPin: SetPinUseCase,
    private val clearAppLock: ClearAppLockUseCase,
    private val getLockoutRemaining: GetLockoutRemainingUseCase,
) : BaseViewModel<PinSetupState.State, PinSetupState.Event, PinSetupState.Effect>() {

    private val mode = savedStateHandle.toRoute<Routes.PinSetup>().mode

    /** The new PIN typed on the NEW step, held until the CONFIRM step matches it. */
    private var pendingPin: String = ""

    private var countdownJob: Job? = null

    override fun createInitialState() = PinSetupState.State(
        mode = mode,
        // Changing or removing a PIN starts by proving the current one.
        step = if (mode == PinSetupMode.CREATE) PinSetupStep.NEW else PinSetupStep.CURRENT,
    )

    init {
        launchSafe { startCountdown(getLockoutRemaining()) }
    }

    override fun onEvent(event: PinSetupState.Event) {
        when (event) {
            is PinSetupState.Event.PinDigitEntered -> onDigit(event.digit)
            PinSetupState.Event.PinBackspacePressed -> setState {
                copy(pin = pin.dropLast(1), errorRes = null, errorArg = null)
            }
            PinSetupState.Event.BackClicked -> nav.back()
        }
    }

    private fun onDigit(digit: Int) {
        if (!currentState.isKeypadEnabled || currentState.pin.length >= AppLockConfig.PIN_LENGTH) return
        val pin = currentState.pin + digit
        setState { copy(pin = pin, errorRes = null, errorArg = null) }
        if (pin.length == AppLockConfig.PIN_LENGTH) submit(pin)
    }

    private fun submit(pin: String) = launchSafe {
        when (currentState.step) {
            PinSetupStep.CURRENT -> onCurrentEntered(pin)
            PinSetupStep.NEW -> {
                pendingPin = pin
                setState { copy(step = PinSetupStep.CONFIRM, pin = "") }
            }
            PinSetupStep.CONFIRM -> onConfirmEntered(pin)
        }
    }

    private suspend fun onCurrentEntered(pin: String) {
        when (val result = verifyPin(pin)) {
            PinCheckResult.Success -> when (mode) {
                PinSetupMode.DISABLE -> {
                    clearAppLock()
                    nav.back()
                }
                else -> setState { copy(step = PinSetupStep.NEW, pin = "") }
            }
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

    private suspend fun onConfirmEntered(pin: String) {
        if (pin == pendingPin) {
            setPin(pin)
            nav.back()
        } else {
            pendingPin = ""
            setState {
                copy(step = PinSetupStep.NEW, pin = "", errorRes = R.string.pin_setup_mismatch, errorArg = null)
            }
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
