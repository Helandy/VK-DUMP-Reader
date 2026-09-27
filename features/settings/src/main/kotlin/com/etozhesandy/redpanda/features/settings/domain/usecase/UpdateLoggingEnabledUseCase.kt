package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.settings.SettingsRepository
import javax.inject.Inject

/** Turns the opt-in diagnostic log on or off; the running logger follows the stored setting. */
class UpdateLoggingEnabledUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(value: Boolean) = repository.setLoggingEnabled(value)
}
