package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.settings.SettingsRepository
import javax.inject.Inject

class UpdateCoilCacheSizeUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(valueMb: Int) = repository.setCoilCacheSizeMb(valueMb)
}
