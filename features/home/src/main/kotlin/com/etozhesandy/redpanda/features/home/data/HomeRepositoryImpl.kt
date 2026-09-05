package com.etozhesandy.redpanda.features.home.data

import com.etozhesandy.redpanda.core.archive.worker.ProfileImportScheduler
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.dispatcher.IoDispatcher
import com.etozhesandy.redpanda.core.common.files.ProfileDirectories
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.core.storage.db.attachment.AttachmentDao
import com.etozhesandy.redpanda.core.storage.db.dialog.DialogDao
import com.etozhesandy.redpanda.core.storage.db.friend.FriendDao
import com.etozhesandy.redpanda.core.storage.db.group.GroupDao
import com.etozhesandy.redpanda.core.storage.db.message.MessageDao
import com.etozhesandy.redpanda.core.storage.db.profile.ProfileDao
import com.etozhesandy.redpanda.core.storage.db.profile.toDomain
import com.etozhesandy.redpanda.core.storage.db.savedphoto.SavedPhotoDao
import com.etozhesandy.redpanda.features.home.domain.repository.HomeRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class HomeRepositoryImpl @Inject constructor(
    private val profileDao: ProfileDao,
    private val dialogDao: DialogDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val friendDao: FriendDao,
    private val groupDao: GroupDao,
    private val savedPhotoDao: SavedPhotoDao,
    private val directories: ProfileDirectories,
    private val importScheduler: ProfileImportScheduler,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : HomeRepository {

    override fun observeProfiles(): Flow<List<Profile>> =
        profileDao.observeProfiles()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(defaultDispatcher)

    override fun observeImportRunning(): Flow<Boolean> = importScheduler.observeImportRunning()

    /**
     * Deleting the profile's directory walks the whole extracted archive, which is gigabytes on a
     * real dump — the caller is a `viewModelScope` coroutine, so this has to leave the main thread.
     */
    override suspend fun deleteProfile(profileId: String) {
        withContext(ioDispatcher) {
            attachmentDao.deleteForProfile(profileId)
            messageDao.deleteForProfile(profileId)
            dialogDao.deleteForProfile(profileId)
            friendDao.deleteForProfile(profileId)
            groupDao.deleteForProfile(profileId)
            savedPhotoDao.deleteForProfile(profileId)
            profileDao.delete(profileId)
            directories.deleteProfileDir(profileId)
        }
    }
}
