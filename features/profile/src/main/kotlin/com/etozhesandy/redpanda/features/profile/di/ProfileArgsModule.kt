package com.etozhesandy.redpanda.features.profile.di

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.profile.mapper.toArgs
import com.etozhesandy.redpanda.features.profile.model.ProfileArgs
import com.etozhesandy.redpanda.features.profile.model.ProfileAttachmentViewerArgs
import com.etozhesandy.redpanda.features.profile.model.ProfileAttachmentsArgs
import com.etozhesandy.redpanda.features.profile.model.ProfileFriendsArgs
import com.etozhesandy.redpanda.features.profile.model.ProfileGroupsArgs
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaArgs
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaFolderArgs
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaViewerArgs
import com.etozhesandy.redpanda.features.profile.model.ProfileSavedPhotoViewerArgs
import com.etozhesandy.redpanda.features.profile.model.ProfileSavedPhotosArgs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * The one place this feature's routes are read. Every screen here shares a module rather than
 * getting one each: they are one navigation graph, and splitting it would only spread the same
 * three lines across ten files.
 *
 * A distinct Args type per screen is also what keeps these bindings apart — several `@Provides`
 * returning a bare `String profileId` would collide in the same component.
 */
@Module
@InstallIn(ViewModelComponent::class)
object ProfileArgsModule {

    @Provides
    fun provideProfileArgs(savedStateHandle: SavedStateHandle): ProfileArgs =
        savedStateHandle.toRoute<Routes.Profile>().toArgs()

    @Provides
    fun provideProfileFriendsArgs(savedStateHandle: SavedStateHandle): ProfileFriendsArgs =
        savedStateHandle.toRoute<Routes.ProfileFriends>().toArgs()

    @Provides
    fun provideProfileGroupsArgs(savedStateHandle: SavedStateHandle): ProfileGroupsArgs =
        savedStateHandle.toRoute<Routes.ProfileGroups>().toArgs()

    @Provides
    fun provideProfileSavedPhotosArgs(savedStateHandle: SavedStateHandle): ProfileSavedPhotosArgs =
        savedStateHandle.toRoute<Routes.ProfileSavedPhotos>().toArgs()

    @Provides
    fun provideProfileAttachmentsArgs(savedStateHandle: SavedStateHandle): ProfileAttachmentsArgs =
        savedStateHandle.toRoute<Routes.ProfileAttachments>().toArgs()

    @Provides
    fun provideProfileMediaArgs(savedStateHandle: SavedStateHandle): ProfileMediaArgs =
        savedStateHandle.toRoute<Routes.ProfileMedia>().toArgs()

    @Provides
    fun provideProfileMediaFolderArgs(savedStateHandle: SavedStateHandle): ProfileMediaFolderArgs =
        savedStateHandle.toRoute<Routes.ProfileMediaFolder>().toArgs()

    @Provides
    fun provideProfileMediaViewerArgs(savedStateHandle: SavedStateHandle): ProfileMediaViewerArgs =
        savedStateHandle.toRoute<Routes.ProfileMediaViewer>().toArgs()

    @Provides
    fun provideProfileSavedPhotoViewerArgs(savedStateHandle: SavedStateHandle): ProfileSavedPhotoViewerArgs =
        savedStateHandle.toRoute<Routes.ProfileSavedPhotoViewer>().toArgs()

    @Provides
    fun provideProfileAttachmentViewerArgs(savedStateHandle: SavedStateHandle): ProfileAttachmentViewerArgs =
        savedStateHandle.toRoute<Routes.ProfileAttachmentViewer>().toArgs()
}
