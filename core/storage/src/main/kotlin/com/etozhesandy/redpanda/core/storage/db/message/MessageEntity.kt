package com.etozhesandy.redpanda.core.storage.db.message

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * [rowId] is the Room/SQLite autoincrement primary key required so [MessageFtsEntity] can link
 * to this table as its FTS `contentEntity` (external-content FTS tables need an integer rowid).
 * [messageId] is the stable business id used everywhere else (favorites, attachments, navigation).
 */
@Entity(
    tableName = "messages",
    indices = [
        Index("dialogId"),
        Index("profileId"),
        Index("isFavorite"),
        Index(value = ["messageId"], unique = true),
        Index(value = ["dialogId", "timestampEpoch"]),
    ],
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val messageId: String,
    val dialogId: String,
    val profileId: String,
    val senderId: String,
    val senderName: String,
    val timestampEpoch: Long,
    val text: String,
    val isOutgoing: Boolean,
    val isFavorite: Boolean,
    val hasAttachments: Boolean = false,
)
