package com.etozhesandy.redpanda.features.home.domain.usecase

import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.features.home.domain.repository.HomeRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams every imported profile, most recently imported first. */
class ObserveProfilesUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    operator fun invoke(): Flow<List<Profile>> = repository.observeProfiles()
}
