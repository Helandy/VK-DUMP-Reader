package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.settings.SettingsRepository
import javax.inject.Inject

class UpdateDefaultChatReversedUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(value: Boolean) = repository.setDefaultChatReversed(value)
}
