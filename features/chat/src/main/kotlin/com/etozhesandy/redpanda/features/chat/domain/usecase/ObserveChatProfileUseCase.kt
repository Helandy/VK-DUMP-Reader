package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams the profile a dialog belongs to — its display name names the folder downloads go into. */
class ObserveChatProfileUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    operator fun invoke(profileId: String): Flow<Profile?> = repository.observeProfile(profileId)
}
