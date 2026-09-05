package com.etozhesandy.redpanda.features.home.data

import com.etozhesandy.redpanda.core.archive.worker.ProfileDeleteScheduler
import com.etozhesandy.redpanda.core.archive.worker.ProfileImportScheduler
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.core.storage.db.profile.ProfileDao
import com.etozhesandy.redpanda.core.storage.db.profile.toDomain
import com.etozhesandy.redpanda.features.home.domain.repository.HomeRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class HomeRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val importScheduler: ProfileImportScheduler,
    private val deleteScheduler: ProfileDeleteScheduler,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : HomeRepository {

    override fun observeProfiles(): Flow<List<Profile>> =
        profileDao.observeProfiles()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(defaultDispatcher)

    override fun observeImportRunning(): Flow<Boolean> = importScheduler.observeImportRunning()

    override fun observeDeletingProfileIds(): Flow<Set<String>> =
        deleteScheduler.observeDeletingProfileIds()

    /**
     * Fire-and-forget: the erase walks gigabytes of extracted archive, so it runs as background
     * work that outlives this screen rather than inside the caller's scope.
     */
    override fun deleteProfile(profileId: String) = deleteScheduler.enqueue(profileId)
}
