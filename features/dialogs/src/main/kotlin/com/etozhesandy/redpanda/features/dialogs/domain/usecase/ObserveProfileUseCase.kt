package com.etozhesandy.redpanda.features.dialogs.domain.usecase

import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.features.dialogs.domain.repository.DialogsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams one profile's own row — used to show an "importing…" banner while it's still filling in. */
class ObserveProfileUseCase @Inject constructor(
    private val repository: DialogsRepository,
) {
    operator fun invoke(profileId: String): Flow<Profile?> = repository.observeProfile(profileId)
}
