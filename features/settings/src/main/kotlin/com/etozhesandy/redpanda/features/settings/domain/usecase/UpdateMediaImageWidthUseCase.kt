package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.settings.SettingsRepository
import javax.inject.Inject

class UpdateMediaImageWidthUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(widthDp: Int) = repository.setMediaImageWidthDp(widthDp)
}
