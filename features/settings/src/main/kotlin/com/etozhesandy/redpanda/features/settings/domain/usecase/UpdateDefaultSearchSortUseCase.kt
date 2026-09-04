package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.model.MessageSort
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import javax.inject.Inject

class UpdateDefaultSearchSortUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(sort: MessageSort, ascending: Boolean) =
        repository.setDefaultSearchSort(sort, ascending)
}
