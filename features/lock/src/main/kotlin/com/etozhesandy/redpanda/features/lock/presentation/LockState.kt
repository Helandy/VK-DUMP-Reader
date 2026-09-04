package com.etozhesandy.redpanda.features.lock.presentation

import androidx.annotation.StringRes
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState
import com.etozhesandy.redpanda.features.lock.presentation.model.LockMode

/** MVI-контракт экрана блокировки: состояние, события и одноразовые эффекты. */
object LockState {

    data class State(
        val mode: LockMode = LockMode.PIN,
        val pin: String = "",
        val biometricFailures: Int = 0,
        val canUseBiometric: Boolean = false,
        /**
         * Bumped every time the system prompt should be shown. The request lives in the state
         * rather than in an effect because it is raised while the screen is still being composed,
         * and a one-off effect emitted then would have no collector yet.
         */
        val biometricRequestId: Int = 0,
        @param:StringRes val errorRes: Int? = null,
        val errorArg: Any? = null,
        val lockoutRemainingMs: Long = 0L,
    ) : UiState {
        val isKeypadEnabled: Boolean get() = lockoutRemainingMs == 0L
    }

    sealed interface Event : UiEvent {
        data class PinDigitEntered(val digit: Int) : Event
        data object PinBackspacePressed : Event
        data object UsePinClicked : Event
        data object UseBiometricClicked : Event
        data object BiometricSucceeded : Event
        data object BiometricFailed : Event
        /** [code] is an `androidx.biometric.BiometricPrompt` ERROR_* constant. */
        data class BiometricErrored(val code: Int) : Event
    }

    /** No one-off effects: showing the system prompt is driven by [State.biometricRequestId]. */
    sealed interface Effect : UiEffect
}
