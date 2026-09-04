package com.etozhesandy.redpanda.features.lock.domain.usecase

import com.etozhesandy.redpanda.core.security.AppLockRepository
import com.etozhesandy.redpanda.core.security.model.AppLockConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the current login-protection settings. */
class ObserveAppLockConfigUseCase @Inject constructor(
    private val repository: AppLockRepository,
) {
    operator fun invoke(): Flow<AppLockConfig> = repository.config
}
