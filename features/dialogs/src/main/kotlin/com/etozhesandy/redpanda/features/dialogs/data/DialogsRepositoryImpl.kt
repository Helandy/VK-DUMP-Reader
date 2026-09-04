package com.etozhesandy.redpanda.features.dialogs.data

import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.core.storage.db.dialog.DialogDao
import com.etozhesandy.redpanda.core.storage.db.dialog.toDomain
import com.etozhesandy.redpanda.core.storage.db.profile.ProfileDao
import com.etozhesandy.redpanda.core.storage.db.profile.toDomain
import com.etozhesandy.redpanda.features.dialogs.domain.repository.DialogsRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class DialogsRepositoryImpl @Inject constructor(
    private val dialogDao: DialogDao,
    private val profileDao: ProfileDao,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : DialogsRepository {

    override fun observeDialogs(profileId: String, query: String?, category: String?): Flow<List<ChatDialog>> =
        dialogDao.observeDialogs(profileId, query, category)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(defaultDispatcher)

    override fun observeCategories(profileId: String): Flow<List<String>> =
        dialogDao.observeCategories(profileId)

    override fun observeProfile(profileId: String): Flow<Profile?> =
        profileDao.observeProfile(profileId).map { it?.toDomain() }.flowOn(defaultDispatcher)
}
