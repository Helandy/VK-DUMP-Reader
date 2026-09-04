package com.etozhesandy.redpanda.features.favorites.domain.repository

import com.etozhesandy.redpanda.core.model.Message
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeFavorites(profileId: String): Flow<List<Message>>
}
