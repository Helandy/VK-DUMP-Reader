package com.etozhesandy.redpanda.features.lock.presentation

import androidx.lifecycle.ViewModel
import com.etozhesandy.redpanda.core.security.model.LockState as AppLockState
import com.etozhesandy.redpanda.features.lock.domain.usecase.ObserveLockStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds nothing of its own: it exists so the gate composable can read the process-wide lock state
 * without reaching into the domain layer itself.
 */
@HiltViewModel
class AppLockGateViewModel @Inject constructor(
    observeLockState: ObserveLockStateUseCase,
) : ViewModel() {

    val lockState: StateFlow<AppLockState> = observeLockState()
}
