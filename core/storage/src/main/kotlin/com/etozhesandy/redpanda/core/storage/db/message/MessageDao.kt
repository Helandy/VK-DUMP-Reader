package com.etozhesandy.redpanda.core.storage.db.message

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE dialogId = :dialogId ORDER BY timestampEpoch ASC, rowId ASC")
    fun pagingMessagesAscending(dialogId: String): PagingSource<Int, MessageEntity>

    @Query("SELECT * FROM messages WHERE dialogId = :dialogId ORDER BY timestampEpoch DESC, rowId DESC")
    fun pagingMessagesDescending(dialogId: String): PagingSource<Int, MessageEntity>

    @Query(
        """
        SELECT messages.* FROM messages
        JOIN messages_fts ON messages.rowId = messages_fts.rowid
        WHERE messages.profileId = :profileId
        AND (:dialogId IS NULL OR messages.dialogId = :dialogId)
        AND messages_fts MATCH :ftsQuery
        ORDER BY messages.timestampEpoch DESC
        LIMIT :limit
        """,
    )
    fun searchMessages(
        profileId: String,
        ftsQuery: String,
        dialogId: String? = null,
        limit: Int = SEARCH_RESULT_LIMIT,
    ): Flow<List<MessageEntity>>

    companion object {
        const val SEARCH_RESULT_LIMIT = 200
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(messages: List<MessageEntity>)

    @Query("UPDATE messages SET isFavorite = :isFavorite WHERE messageId = :messageId")
    suspend fun setFavorite(messageId: String, isFavorite: Boolean)

    @Query("SELECT * FROM messages WHERE profileId = :profileId AND isFavorite = 1 ORDER BY timestampEpoch DESC")
    fun observeFavorites(profileId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE messageId = :messageId")
    suspend fun getMessage(messageId: String): MessageEntity?

    /**
     * Ties on [MessageEntity.timestampEpoch] (VK timestamps are second-precision and common in
     * photo bursts) are broken by [MessageEntity.rowId], matching the tiebreak used by
     * [pagingMessagesAscending]/[pagingMessagesDescending] — otherwise the offset this returns can
     * point at a neighboring message instead of the requested one whenever ties exist.
     */
    @Query(
        """
        SELECT COUNT(*) FROM messages
        WHERE dialogId = :dialogId AND (
            timestampEpoch < (SELECT timestampEpoch FROM messages WHERE messageId = :messageId)
            OR (
                timestampEpoch = (SELECT timestampEpoch FROM messages WHERE messageId = :messageId)
                AND rowId <= (SELECT rowId FROM messages WHERE messageId = :messageId)
            )
        )
        """,
    )
    suspend fun getMessagePositionAscending(dialogId: String, messageId: String): Int

    @Query(
        """
        SELECT COUNT(*) FROM messages
        WHERE dialogId = :dialogId AND (
            timestampEpoch > (SELECT timestampEpoch FROM messages WHERE messageId = :messageId)
            OR (
                timestampEpoch = (SELECT timestampEpoch FROM messages WHERE messageId = :messageId)
                AND rowId >= (SELECT rowId FROM messages WHERE messageId = :messageId)
            )
        )
        """,
    )
    suspend fun getMessagePositionDescending(dialogId: String, messageId: String): Int

    @Query("DELETE FROM messages WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)

    /**
     * Backfills the denormalized [MessageEntity.hasAttachments] flag from the `attachments` table
     * for one profile's messages. Run once after an import finishes writing all attachment
     * batches, since attachments for a message can arrive in a later batch than the message
     * itself.
     */
    @Query(
        """
        UPDATE messages SET hasAttachments = 1
        WHERE profileId = :profileId AND messageId IN (
            SELECT DISTINCT messageId FROM attachments WHERE profileId = :profileId AND messageId IS NOT NULL
        )
        """,
    )
    suspend fun markMessagesWithAttachments(profileId: String)
}
