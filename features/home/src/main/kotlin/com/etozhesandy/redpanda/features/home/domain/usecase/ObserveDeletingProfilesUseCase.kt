package com.etozhesandy.redpanda.features.home.domain.usecase

import com.etozhesandy.redpanda.features.home.domain.repository.HomeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Ids of the profiles being erased right now, so the screen can keep them from being opened. */
class ObserveDeletingProfilesUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    operator fun invoke(): Flow<Set<String>> = repository.observeDeletingProfileIds()
}
