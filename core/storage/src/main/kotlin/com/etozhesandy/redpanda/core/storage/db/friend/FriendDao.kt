package com.etozhesandy.redpanda.core.storage.db.friend

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FriendDao {

    @Query("SELECT * FROM profile_friends WHERE profileId = :profileId ORDER BY name ASC")
    fun observeFriends(profileId: String): Flow<List<FriendEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(friends: List<FriendEntity>)

    @Query("DELETE FROM profile_friends WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)
}
