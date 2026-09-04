package com.etozhesandy.redpanda.features.favorites.data

import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.storage.db.message.MessageDao
import com.etozhesandy.redpanda.core.storage.db.message.toDomain
import com.etozhesandy.redpanda.features.favorites.domain.repository.FavoritesRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class FavoritesRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : FavoritesRepository {

    override fun observeFavorites(profileId: String): Flow<List<Message>> =
        messageDao.observeFavorites(profileId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(defaultDispatcher)
}
