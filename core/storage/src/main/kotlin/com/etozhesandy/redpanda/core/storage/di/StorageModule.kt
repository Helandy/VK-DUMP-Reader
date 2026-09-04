package com.etozhesandy.redpanda.core.storage.di

import android.content.Context
import androidx.room.Room
import com.etozhesandy.redpanda.core.storage.db.AppDatabase
import com.etozhesandy.redpanda.core.storage.db.attachment.AttachmentDao
import com.etozhesandy.redpanda.core.storage.db.dialog.DialogDao
import com.etozhesandy.redpanda.core.storage.db.friend.FriendDao
import com.etozhesandy.redpanda.core.storage.db.group.GroupDao
import com.etozhesandy.redpanda.core.storage.db.message.MessageDao
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
            // The schema is not migrated: the app is unreleased, so a schema change simply
            // rebuilds the database and the user re-imports their dumps. No migration needs
            // writing — but the version in @Database must still be bumped on every schema change,
            // otherwise Room finds the old schema under the current version and fails the identity
            // check ("Room cannot verify the data integrity") instead of rebuilding.
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun provideDialogDao(db: AppDatabase): DialogDao = db.dialogDao()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()

    @Provides
    fun provideFriendDao(db: AppDatabase): FriendDao = db.friendDao()

    @Provides
    fun provideGroupDao(db: AppDatabase): GroupDao = db.groupDao()

    @Provides
    fun provideSavedPhotoDao(db: AppDatabase): SavedPhotoDao = db.savedPhotoDao()
}
