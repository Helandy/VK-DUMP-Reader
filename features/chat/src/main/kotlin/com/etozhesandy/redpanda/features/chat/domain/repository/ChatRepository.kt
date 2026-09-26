package com.etozhesandy.redpanda.features.chat.domain.repository

import androidx.paging.PagingData
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.DialogMessage
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.features.chat.domain.model.MessageWithAttachments
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeDialog(dialogId: String): Flow<ChatDialog?>
    fun observeProfile(profileId: String): Flow<Profile?>
    fun pagingMessages(
        dialogId: String,
        isReversed: Boolean,
        initialPosition: Int?,
    ): Flow<PagingData<MessageWithAttachments>>
    fun searchMessages(profileId: String, ftsQuery: String, dialogId: String?): Flow<List<Message>>
    suspend fun searchMessagesPage(
        profileId: String,
        ftsQuery: String,
        dialogId: String,
        limit: Int,
        offset: Int,
    ): List<Message>
    suspend fun searchAllDialogs(
        profileId: String,
        ftsQuery: String,
        limit: Int,
        offset: Int,
    ): List<DialogMessage>
    fun observeMediaForDialog(dialogId: String): Flow<List<Attachment>>
    fun observePhotosForDialog(dialogId: String): Flow<List<Attachment>>
    fun observeVideosForDialog(dialogId: String): Flow<List<Attachment>>
    fun observeAudioForDialog(dialogId: String): Flow<List<Attachment>>
    fun observeFilesForDialog(dialogId: String): Flow<List<Attachment>>
    suspend fun setFavorite(messageId: String, isFavorite: Boolean)
    fun observeFavoriteIds(dialogId: String): Flow<Set<String>>
    suspend fun getMessagePosition(dialogId: String, messageId: String, isReversed: Boolean): Int
}
