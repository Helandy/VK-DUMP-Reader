package com.etozhesandy.redpanda.core.storage.db.dialog

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DialogDao {

    @Query(
        """
        SELECT * FROM dialogs
        WHERE profileId = :profileId
        AND (:query IS NULL OR peerName LIKE '%' || :query || '%')
        AND (:category IS NULL OR category = :category)
        ORDER BY lastMessageAt DESC
        """,
    )
    fun observeDialogs(profileId: String, query: String?, category: String?): Flow<List<DialogEntity>>

    @Query("SELECT * FROM dialogs WHERE id = :id")
    fun observeDialog(id: String): Flow<DialogEntity?>

    @Query("SELECT DISTINCT category FROM dialogs WHERE profileId = :profileId AND category IS NOT NULL")
    fun observeCategories(profileId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(dialogs: List<DialogEntity>)

    @Query("DELETE FROM dialogs WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)
}
