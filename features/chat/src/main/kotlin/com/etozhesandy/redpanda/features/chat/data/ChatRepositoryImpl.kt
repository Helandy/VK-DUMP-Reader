package com.etozhesandy.redpanda.features.chat.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
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
import com.etozhesandy.redpanda.core.storage.db.message.MessageDao
import com.etozhesandy.redpanda.core.storage.db.message.toDomain
import com.etozhesandy.redpanda.core.storage.db.profile.ProfileDao
import com.etozhesandy.redpanda.core.storage.db.profile.toDomain
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
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
    ): Flow<PagingData<Message>> =
        Pager(
            PagingConfig(
                pageSize = 50,
                initialLoadSize = 50,
                enablePlaceholders = false,
            ),
            initialKey = initialPosition,
        ) {
            if (isReversed) messageDao.pagingMessagesDescending(dialogId)
            else messageDao.pagingMessagesAscending(dialogId)
        }
            .flow
            .map { pagingData -> pagingData.map { it.toDomain() } }
            .flowOn(defaultDispatcher)

    override fun searchMessages(profileId: String, ftsQuery: String, dialogId: String?): Flow<List<Message>> =
        messageDao.searchMessages(profileId, ftsQuery, dialogId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(defaultDispatcher)

    override fun searchAllDialogs(profileId: String, ftsQuery: String): Flow<List<DialogMessage>> =
        messageDao.searchAllDialogs(profileId, ftsQuery)
            .map { rows -> rows.map { it.toDomain() } }
            .flowOn(defaultDispatcher)

    override suspend fun getAttachmentsForMessage(messageId: String): List<Attachment> =
        attachmentDao.getAttachmentsForMessage(messageId).map { it.toDomain() }

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

    private fun observeTypes(dialogId: String, types: List<AttachmentType>): Flow<List<Attachment>> =
        attachmentDao.observeByTypesForDialog(dialogId, types)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(defaultDispatcher)

    override suspend fun setFavorite(messageId: String, isFavorite: Boolean) =
        messageDao.setFavorite(messageId, isFavorite)

    override suspend fun getMessagePosition(dialogId: String, messageId: String, isReversed: Boolean): Int =
        if (isReversed) messageDao.getMessagePositionDescending(dialogId, messageId)
        else messageDao.getMessagePositionAscending(dialogId, messageId)
}
