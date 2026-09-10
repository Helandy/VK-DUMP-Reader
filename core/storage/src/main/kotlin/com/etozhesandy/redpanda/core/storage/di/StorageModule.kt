package com.etozhesandy.redpanda.core.storage.di

import android.content.Context
import androidx.room.Room
import com.etozhesandy.redpanda.core.storage.db.AppDatabase
import com.etozhesandy.redpanda.core.storage.db.attachment.AttachmentDao
import com.etozhesandy.redpanda.core.storage.db.dialog.DialogDao
import com.etozhesandy.redpanda.core.storage.db.favorite.FavoriteMessageDao
import com.etozhesandy.redpanda.core.storage.db.friend.FriendDao
import com.etozhesandy.redpanda.core.storage.db.group.GroupDao
import com.etozhesandy.redpanda.core.storage.db.message.MessageDao
import com.etozhesandy.redpanda.core.storage.db.migration.MIGRATION_2_3
import com.etozhesandy.redpanda.core.storage.db.profile.ProfileDao
import com.etozhesandy.redpanda.core.storage.db.savedphoto.SavedPhotoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "redpanda.db")
            // No destructive fallback: an imported dump is gigabytes and hours of the reader's
            // time, so a schema change owes them a migration. Every change therefore needs both a
            // bumped version in @Database and a Migration listed here — without one Room refuses
            // to open the database rather than quietly rebuilding it.
            .addMigrations(MIGRATION_2_3)
            .build()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideDialogDao(db: AppDatabase): DialogDao = db.dialogDao()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideFavoriteMessageDao(db: AppDatabase): FavoriteMessageDao = db.favoriteMessageDao()

    @Provides
    fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()

    @Provides
    fun provideFriendDao(db: AppDatabase): FriendDao = db.friendDao()

    @Provides
    fun provideGroupDao(db: AppDatabase): GroupDao = db.groupDao()

    @Provides
    fun provideSavedPhotoDao(db: AppDatabase): SavedPhotoDao = db.savedPhotoDao()
}
