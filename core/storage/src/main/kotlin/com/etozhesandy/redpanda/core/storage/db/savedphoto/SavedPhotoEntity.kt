package com.etozhesandy.redpanda.core.storage.db.savedphoto

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profile_saved_photos",
    indices = [Index("profileId"), Index(value = ["profileId", "timestampEpoch"])],
)
data class SavedPhotoEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val url: String,
    val timestampEpoch: Long,
)
