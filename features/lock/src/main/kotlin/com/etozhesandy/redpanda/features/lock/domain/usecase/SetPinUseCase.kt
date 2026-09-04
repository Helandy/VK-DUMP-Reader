package com.etozhesandy.redpanda.features.lock.domain.usecase

import com.etozhesandy.redpanda.core.security.AppLockRepository
import javax.inject.Inject

/** Stores a new login PIN and turns login protection on. */
class SetPinUseCase @Inject constructor(
    private val repository: AppLockRepository,
) {
    suspend operator fun invoke(pin: String) = repository.setPin(pin)
}
