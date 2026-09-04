package com.etozhesandy.redpanda.core.storage.db.friend

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** [id] is `"$profileId:$vkId"`, following the same composite-id convention as [com.etozhesandy.redpanda.core.storage.db.dialog.DialogEntity]. */
@Entity(
    tableName = "profile_friends",
    indices = [Index("profileId")],
)
data class FriendEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val name: String,
    val avatarPath: String?,
)
