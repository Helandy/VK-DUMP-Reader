package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.security.AppLockRepository
import javax.inject.Inject

/** Sets how long the app may stay in the background before it asks for the PIN again. */
class UpdateLockTimeoutUseCase @Inject constructor(
    private val repository: AppLockRepository,
) {
    suspend operator fun invoke(seconds: Int) = repository.setTimeoutSeconds(seconds)
}
