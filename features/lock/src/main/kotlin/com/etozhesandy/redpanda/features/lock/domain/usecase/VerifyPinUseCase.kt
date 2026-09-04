package com.etozhesandy.redpanda.features.lock.domain.usecase

import com.etozhesandy.redpanda.core.security.AppLockRepository
import com.etozhesandy.redpanda.core.security.model.PinCheckResult
import javax.inject.Inject

/** Checks a PIN against the stored hash, counting the attempt towards the lockout penalty. */
class VerifyPinUseCase @Inject constructor(
    private val repository: AppLockRepository,
) {
    suspend operator fun invoke(pin: String): PinCheckResult = repository.verifyPin(pin)
}
