package com.etozhesandy.redpanda.features.home.domain.usecase

import com.etozhesandy.redpanda.features.home.domain.repository.HomeRepository
import javax.inject.Inject

/** Deletes a profile: its Room rows and its `profiles/$id` directory on disk. */
class DeleteProfileUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    suspend operator fun invoke(profileId: String) = repository.deleteProfile(profileId)
}
