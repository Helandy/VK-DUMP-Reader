package com.etozhesandy.redpanda.features.dialogs.domain.usecase

import com.etozhesandy.redpanda.features.dialogs.domain.repository.DialogsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the distinct dialog categories a source archive defined (e.g. VK's "Девушки"/"Парни"). */
class ObserveCategoriesUseCase @Inject constructor(
    private val repository: DialogsRepository,
) {
    operator fun invoke(profileId: String): Flow<List<String>> = repository.observeCategories(profileId)
}
