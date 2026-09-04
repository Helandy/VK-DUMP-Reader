package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.settings.cache.ProfileCacheSizeCalculator
import javax.inject.Inject

class GetProfilesCacheSizeUseCase @Inject constructor(
    private val calculator: ProfileCacheSizeCalculator,
) {
    suspend operator fun invoke(): Long = calculator.calculateTotalBytes()
}
