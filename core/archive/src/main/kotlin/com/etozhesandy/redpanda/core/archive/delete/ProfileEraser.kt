package com.etozhesandy.redpanda.core.archive.delete

import com.etozhesandy.redpanda.core.common.dispatcher.IoDispatcher
import com.etozhesandy.redpanda.core.common.files.ProfileDirectories
import com.etozhesandy.redpanda.core.storage.db.attachment.AttachmentDao
import com.etozhesandy.redpanda.core.storage.db.dialog.DialogDao
import com.etozhesandy.redpanda.core.storage.db.friend.FriendDao
import com.etozhesandy.redpanda.core.storage.db.group.GroupDao
import com.etozhesandy.redpanda.core.storage.db.message.MessageDao
import com.etozhesandy.redpanda.core.storage.db.profile.ProfileDao
import com.etozhesandy.redpanda.core.storage.db.savedphoto.SavedPhotoDao
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Drops everything one imported profile owns: its Room rows and its `profiles/$id` directory.
 *
 * The counterpart of the import pipeline, and just as long-running — the directory walk covers the
 * whole extracted archive, which is gigabytes on a real dump — so it is run from a worker rather
 * than from a screen's scope. Idempotent: re-running it on an already-erased profile is a no-op,
 * which is what makes a retry safe.
 */
class ProfileEraser @Inject constructor(
    private val profileDao: ProfileDao,
    private val dialogDao: DialogDao,
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val friendDao: FriendDao,
    private val groupDao: GroupDao,
    private val savedPhotoDao: SavedPhotoDao,
    private val directories: ProfileDirectories,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun erase(profileId: String) {
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
