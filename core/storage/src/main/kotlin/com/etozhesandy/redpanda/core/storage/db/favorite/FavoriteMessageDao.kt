package com.etozhesandy.redpanda.core.storage.db.favorite

import androidx.room.Dao
import androidx.room.Query
import com.etozhesandy.redpanda.core.storage.db.message.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteMessageDao {

    /**
     * Stars a message by id alone: the owning profile and dialog are read off the message in the
     * same statement, so a star can never end up on a message that isn't there.
     */
    @Query(
        """
        INSERT OR REPLACE INTO favorite_messages (messageId, profileId, dialogId)
        SELECT messageId, profileId, dialogId FROM messages WHERE messageId = :messageId
        """,
    )
    suspend fun add(messageId: String)

    @Query("DELETE FROM favorite_messages WHERE messageId = :messageId")
    suspend fun remove(messageId: String)

    /** Only the ids: a chat draws its stars over messages it has already paged in. */
    @Query("SELECT messageId FROM favorite_messages WHERE dialogId = :dialogId")
    fun observeIdsForDialog(dialogId: String): Flow<List<String>>

    @Query(
        """
        SELECT messages.* FROM messages
        JOIN favorite_messages ON favorite_messages.messageId = messages.messageId
        WHERE favorite_messages.profileId = :profileId
        ORDER BY messages.timestampEpoch DESC
        """,
    )
    fun observeFavoriteMessages(profileId: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM favorite_messages WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)
}
