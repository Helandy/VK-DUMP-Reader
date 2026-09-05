package com.etozhesandy.redpanda.features.home.domain.usecase

import com.etozhesandy.redpanda.features.home.domain.repository.HomeRepository
import javax.inject.Inject

/**
 * Schedules the deletion of a profile — its Room rows and its `profiles/$id` directory on disk.
 * Returns as soon as the work is enqueued; [ObserveDeletingProfilesUseCase] reports the progress.
 */
class DeleteProfileUseCase @Inject constructor(
    private val repository: HomeRepository,
) {
    operator fun invoke(profileId: String) = repository.deleteProfile(profileId)
}
