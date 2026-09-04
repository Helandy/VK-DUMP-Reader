package com.etozhesandy.redpanda.features.lock.domain.usecase

import com.etozhesandy.redpanda.core.security.BiometricAvailabilityChecker
import com.etozhesandy.redpanda.core.security.model.BiometricAvailability
import javax.inject.Inject

/** Reports whether this device can authenticate the user biometrically right now. */
class GetBiometricAvailabilityUseCase @Inject constructor(
    private val checker: BiometricAvailabilityChecker,
) {
    operator fun invoke(): BiometricAvailability = checker.availability()
}
