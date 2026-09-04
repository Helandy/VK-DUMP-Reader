package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import javax.inject.Inject

class UpdateDefaultMediaSortUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(sort: MediaSort, ascending: Boolean) =
        repository.setDefaultMediaSort(sort, ascending)
}
