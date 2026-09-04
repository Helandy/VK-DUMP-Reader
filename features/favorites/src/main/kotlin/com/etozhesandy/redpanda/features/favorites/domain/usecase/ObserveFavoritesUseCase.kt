package com.etozhesandy.redpanda.features.favorites.domain.usecase

import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.features.favorites.domain.repository.FavoritesRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Streams every message a profile has marked as favorite, most recent first. */
class ObserveFavoritesUseCase @Inject constructor(
    private val repository: FavoritesRepository,
) {
    operator fun invoke(profileId: String): Flow<List<Message>> = repository.observeFavorites(profileId)
}
