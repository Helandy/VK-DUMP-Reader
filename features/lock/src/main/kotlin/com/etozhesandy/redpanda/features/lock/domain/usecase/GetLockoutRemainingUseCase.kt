package com.etozhesandy.redpanda.features.lock.domain.usecase

import com.etozhesandy.redpanda.core.security.AppLockRepository
import javax.inject.Inject

/** Milliseconds left of the current wrong-PIN lockout penalty, or 0 when entry is allowed. */
class GetLockoutRemainingUseCase @Inject constructor(
    private val repository: AppLockRepository,
) {
    suspend operator fun invoke(): Long = repository.lockoutRemainingMs()
}
