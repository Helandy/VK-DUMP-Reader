package com.etozhesandy.redpanda.features.lock.domain.usecase

import com.etozhesandy.redpanda.core.security.AppLockManager
import com.etozhesandy.redpanda.core.security.model.LockState
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Streams whether the app content may be shown right now. */
class ObserveLockStateUseCase @Inject constructor(
    private val manager: AppLockManager,
) {
    operator fun invoke(): StateFlow<LockState> = manager.state
}
