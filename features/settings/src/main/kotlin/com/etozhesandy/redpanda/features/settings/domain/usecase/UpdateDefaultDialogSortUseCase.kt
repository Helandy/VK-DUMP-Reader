package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.model.DialogSort
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import javax.inject.Inject

class UpdateDefaultDialogSortUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(sort: DialogSort, ascending: Boolean) =
        repository.setDefaultDialogSort(sort, ascending)
}
