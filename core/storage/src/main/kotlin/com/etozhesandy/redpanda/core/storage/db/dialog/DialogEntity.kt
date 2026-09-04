package com.etozhesandy.redpanda.core.storage.db.dialog

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.etozhesandy.redpanda.core.model.DialogKind

@Entity(
    tableName = "dialogs",
    indices = [Index("profileId"), Index("peerName")],
)
data class DialogEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val peerId: String,
    val peerName: String,
    val peerAvatarPath: String?,
    val kind: DialogKind,
    val category: String?,
    val lastMessageAt: Long,
    val messageCount: Int,
)
