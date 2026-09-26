package com.etozhesandy.redpanda.features.chat.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.AttachmentType
import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.DialogMessage
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.core.storage.db.attachment.AttachmentDao
import com.etozhesandy.redpanda.core.storage.db.attachment.toDomain
import com.etozhesandy.redpanda.core.storage.db.dialog.DialogDao
import com.etozhesandy.redpanda.core.storage.db.dialog.toDomain
import com.etozhesandy.redpanda.core.storage.db.favorite.FavoriteMessageDao
import com.etozhesandy.redpanda.core.storage.db.message.MessageDao
import com.etozhesandy.redpanda.core.storage.db.message.toDomain
import com.etozhesandy.redpanda.core.storage.db.profile.ProfileDao
import com.etozhesandy.redpanda.core.storage.db.profile.toDomain
import com.etozhesandy.redpanda.features.chat.data.paging.MessagePagingSource
import com.etozhesandy.redpanda.features.chat.domain.model.MessageWithAttachments
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val favoriteMessageDao: FavoriteMessageDao,
    private val dialogDao: DialogDao,
    private val attachmentDao: AttachmentDao,
    private val profileDao: ProfileDao,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ChatRepository {

    override fun observeDialog(dialogId: String): Flow<ChatDialog?> =
        dialogDao.observeDialog(dialogId).map { it?.toDomain() }.flowOn(defaultDispatcher)

    override fun observeProfile(profileId: String): Flow<Profile?> =
        profileDao.observeProfile(profileId).map { it?.toDomain() }.flowOn(defaultDispatcher)

    override fun pagingMessages(
        dialogId: String,
        isReversed: Boolean,
        initialPosition: Int?,
    ): Flow<PagingData<MessageWithAttachments>> =
        Pager(
            PagingConfig(
                pageSize = 50,
                initialLoadSize = 50,
                enablePlaceholders = false,
            ),
            initialKey = initialPosition,
        ) {
            MessagePagingSource(messageDao, attachmentDao, dialogId, isReversed)
        }
            .flow
            .flowOn(defaultDispatcher)

    override fun searchMessages(profileId: String, ftsQuery: String, dialogId: String?): Flow<List<Message>> =
        messageDao.searchMessages(profileId, ftsQuery, dialogId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(defaultDispatcher)

    override suspend fun searchMessagesPage(
        profileId: String,
        ftsQuery: String,
        dialogId: String,
        limit: Int,
        offset: Int,
    ): List<Message> = withContext(defaultDispatcher) {
        messageDao.searchMessagesInDialog(profileId, dialogId, ftsQuery, limit, offset)
            .map { it.toDomain() }
    }

    override suspend fun searchAllDialogs(
        profileId: String,
        ftsQuery: String,
        limit: Int,
        offset: Int,
    ): List<DialogMessage> = withContext(defaultDispatcher) {
        messageDao.searchAllDialogs(profileId, ftsQuery, limit, offset)
            .map { it.toDomain() }
    }

    // One query rather than combining two flows and re-sorting in memory on every emission: the
    // composite (dialogId, type, timestampEpoch) index lets the database merge and order these.
    override fun observeMediaForDialog(dialogId: String): Flow<List<Attachment>> =
        observeTypes(dialogId, listOf(AttachmentType.PHOTO, AttachmentType.VIDEO))

    override fun observePhotosForDialog(dialogId: String): Flow<List<Attachment>> =
        observeTypes(dialogId, listOf(AttachmentType.PHOTO))

    override fun observeVideosForDialog(dialogId: String): Flow<List<Attachment>> =
        observeTypes(dialogId, listOf(AttachmentType.VIDEO))

    override fun observeAudioForDialog(dialogId: String): Flow<List<Attachment>> =
        observeTypes(dialogId, listOf(AttachmentType.AUDIO))

    override fun observeFilesForDialog(dialogId: String): Flow<List<Attachment>> =
        observeTypes(dialogId, listOf(AttachmentType.FILE))

    // Room re-runs the query on every write to `attachments`, so while an import is running the
    // same list came back after each batch and redrew the whole grid; only real changes pass.
    private fun observeTypes(dialogId: String, types: List<AttachmentType>): Flow<List<Attachment>> =
        attachmentDao.observeByTypesForDialog(dialogId, types)
            .map { entities -> entities.map { it.toDomain() } }
            .distinctUntilChanged()
            .flowOn(defaultDispatcher)

    override suspend fun setFavorite(messageId: String, isFavorite: Boolean) =
        if (isFavorite) favoriteMessageDao.add(messageId) else favoriteMessageDao.remove(messageId)

    override fun observeFavoriteIds(dialogId: String): Flow<Set<String>> =
        favoriteMessageDao.observeIdsForDialog(dialogId)
            .map { ids -> ids.toSet() }
            .flowOn(defaultDispatcher)

    override suspend fun getMessagePosition(dialogId: String, messageId: String, isReversed: Boolean): Int =
        if (isReversed) messageDao.getMessagePositionDescending(dialogId, messageId)
        else messageDao.getMessagePositionAscending(dialogId, messageId)
}
