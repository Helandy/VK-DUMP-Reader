package com.etozhesandy.redpanda.core.storage.db.favorite

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A star the reader put on one message.
 *
 * A table of its own rather than a column on `messages`, because the two have opposite lifetimes:
 * `messages` is written once by an import and only read afterwards — a chat pages straight out of
 * it — while a star is written whenever the reader taps one. Kept together, every tap counted as a
 * write to the table the open dialog was paging, and the list was rebuilt underneath the reader.
 *
 * [profileId] and [dialogId] are copied off the message so the favorites screen and a chat's stars
 * can each be read without joining, and so both stay filterable by their own owner.
 */
@Entity(
    tableName = "favorite_messages",
    indices = [Index("profileId"), Index("dialogId")],
)
data class FavoriteMessageEntity(
    @PrimaryKey val messageId: String,
    val profileId: String,
    val dialogId: String,
)
