package com.etozhesandy.redpanda.core.storage.db.attachment

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.etozhesandy.redpanda.core.model.AttachmentType

@Entity(
    tableName = "attachments",
    indices = [
        Index("messageId"),
        Index("dialogId"),
        Index("profileId"),
        Index(value = ["dialogId", "timestampEpoch"]),
        // Every media screen filters by type within one dialog or profile and orders by time;
        // without these the type filters degrade to a full scan of a profile's attachments.
        Index(value = ["dialogId", "type", "timestampEpoch"]),
        Index(value = ["profileId", "type", "timestampEpoch"]),
    ],
)
data class AttachmentEntity(
    @PrimaryKey val id: String,
    val messageId: String?,
    val dialogId: String,
    val profileId: String,
    val type: AttachmentType,
    val path: String,
    val orderInMessage: Int,
    val timestampEpoch: Long,
    val caption: String? = null,
    val sourceFolder: String? = null,
)
