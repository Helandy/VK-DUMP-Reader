package com.etozhesandy.redpanda.features.lock.domain.usecase

import com.etozhesandy.redpanda.core.security.AppLockRepository
import javax.inject.Inject

/** Turns unlocking by fingerprint on or off; the PIN stays available either way. */
class SetBiometricEnabledUseCase @Inject constructor(
    private val repository: AppLockRepository,
) {
    suspend operator fun invoke(value: Boolean) = repository.setBiometricEnabled(value)
}
