package com.etozhesandy.redpanda.features.chat.domain.repository

import androidx.paging.PagingData
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.model.Profile
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeDialog(dialogId: String): Flow<ChatDialog?>
    fun observeProfile(profileId: String): Flow<Profile?>
    fun pagingMessages(
        dialogId: String,
        isReversed: Boolean,
        initialPosition: Int?,
    ): Flow<PagingData<Message>>
    fun searchMessages(profileId: String, ftsQuery: String, dialogId: String?): Flow<List<Message>>
    suspend fun getAttachmentsForMessage(messageId: String): List<Attachment>
    fun observeMediaForDialog(dialogId: String): Flow<List<Attachment>>
    fun observePhotosForDialog(dialogId: String): Flow<List<Attachment>>
    fun observeVideosForDialog(dialogId: String): Flow<List<Attachment>>
    fun observeAudioForDialog(dialogId: String): Flow<List<Attachment>>
    fun observeFilesForDialog(dialogId: String): Flow<List<Attachment>>
    suspend fun setFavorite(messageId: String, isFavorite: Boolean)
    suspend fun getMessagePosition(dialogId: String, messageId: String, isReversed: Boolean): Int
}
