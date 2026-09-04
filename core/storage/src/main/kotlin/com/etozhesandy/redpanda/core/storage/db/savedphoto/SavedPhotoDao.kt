package com.etozhesandy.redpanda.core.storage.db.savedphoto

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedPhotoDao {

    @Query("SELECT * FROM profile_saved_photos WHERE profileId = :profileId ORDER BY timestampEpoch DESC")
    fun observeSavedPhotos(profileId: String): Flow<List<SavedPhotoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(photos: List<SavedPhotoEntity>)

    @Query("DELETE FROM profile_saved_photos WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: String)
}
