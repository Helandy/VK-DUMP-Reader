package com.etozhesandy.redpanda.core.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.etozhesandy.redpanda.core.storage.db.attachment.AttachmentDao
import com.etozhesandy.redpanda.core.storage.db.attachment.AttachmentEntity
import com.etozhesandy.redpanda.core.storage.db.dialog.DialogDao
import com.etozhesandy.redpanda.core.storage.db.dialog.DialogEntity
import com.etozhesandy.redpanda.core.storage.db.favorite.FavoriteMessageDao
import com.etozhesandy.redpanda.core.storage.db.favorite.FavoriteMessageEntity
import com.etozhesandy.redpanda.core.storage.db.friend.FriendDao
import com.etozhesandy.redpanda.core.storage.db.friend.FriendEntity
import com.etozhesandy.redpanda.core.storage.db.group.GroupDao
import com.etozhesandy.redpanda.core.storage.db.group.GroupEntity
import com.etozhesandy.redpanda.core.storage.db.message.MessageDao
import com.etozhesandy.redpanda.core.storage.db.message.MessageEntity
import com.etozhesandy.redpanda.core.storage.db.message.MessageFtsEntity
import com.etozhesandy.redpanda.core.storage.db.profile.ProfileDao
import com.etozhesandy.redpanda.core.storage.db.profile.ProfileEntity
import com.etozhesandy.redpanda.core.storage.db.savedphoto.SavedPhotoDao
import com.etozhesandy.redpanda.core.storage.db.savedphoto.SavedPhotoEntity

@Database(
    entities = [
        ProfileEntity::class,
        DialogEntity::class,
        MessageEntity::class,
        MessageFtsEntity::class,
        FavoriteMessageEntity::class,
        AttachmentEntity::class,
        FriendEntity::class,
        GroupEntity::class,
        SavedPhotoEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(StorageTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun dialogDao(): DialogDao
    abstract fun messageDao(): MessageDao
    abstract fun favoriteMessageDao(): FavoriteMessageDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun friendDao(): FriendDao
    abstract fun groupDao(): GroupDao
    abstract fun savedPhotoDao(): SavedPhotoDao
}
