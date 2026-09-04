package com.etozhesandy.redpanda.features.lock.domain.usecase

import com.etozhesandy.redpanda.core.security.AppLockRepository
import javax.inject.Inject

/** Removes the login PIN, turning login protection off entirely. */
class ClearAppLockUseCase @Inject constructor(
    private val repository: AppLockRepository,
) {
    suspend operator fun invoke() = repository.clearLock()
}
