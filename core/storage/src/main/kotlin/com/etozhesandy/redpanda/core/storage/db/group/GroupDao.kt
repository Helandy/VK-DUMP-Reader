package com.etozhesandy.redpanda.core.storage.db.group

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {

    @Query("SELECT * FROM profile_groups WHERE profileId = :profileId ORDER BY name ASC")
    fun observeGroups(profileId: String): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(groups: List<GroupEntity>)

    @Query("DELETE FROM profile_groups WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)
}
