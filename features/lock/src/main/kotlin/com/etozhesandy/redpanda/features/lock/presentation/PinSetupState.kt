package com.etozhesandy.redpanda.features.lock.presentation

import androidx.annotation.StringRes
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState
import com.etozhesandy.redpanda.core.navigation.PinSetupMode
import com.etozhesandy.redpanda.features.lock.presentation.model.PinSetupStep

/** MVI-контракт экрана задания PIN-кода: состояние, события и одноразовые эффекты. */
object PinSetupState {

    data class State(
        val mode: PinSetupMode = PinSetupMode.CREATE,
        val step: PinSetupStep = PinSetupStep.NEW,
        val pin: String = "",
        @param:StringRes val errorRes: Int? = null,
        val errorArg: Any? = null,
        val lockoutRemainingMs: Long = 0L,
    ) : UiState {
        val isKeypadEnabled: Boolean get() = lockoutRemainingMs == 0L
    }

    sealed interface Event : UiEvent {
        data class PinDigitEntered(val digit: Int) : Event
        data object PinBackspacePressed : Event
        data object BackClicked : Event
    }

    /** No one-off effects: the screen's only outward action is navigation. */
    sealed interface Effect : UiEffect
}
