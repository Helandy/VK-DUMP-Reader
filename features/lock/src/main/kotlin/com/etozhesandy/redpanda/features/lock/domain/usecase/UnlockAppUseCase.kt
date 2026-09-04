package com.etozhesandy.redpanda.features.lock.domain.usecase

import com.etozhesandy.redpanda.core.security.AppLockManager
import javax.inject.Inject

/** Opens the app for this session after a successful authentication. */
class UnlockAppUseCase @Inject constructor(
    private val manager: AppLockManager,
) {
    operator fun invoke() = manager.unlock()
}
