package com.etozhesandy.redpanda.features.chat.di

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.chat.mapper.toArgs
import com.etozhesandy.redpanda.features.chat.model.ChatArgs
import com.etozhesandy.redpanda.features.chat.model.ChatSearchArgs
import com.etozhesandy.redpanda.features.chat.model.GlobalSearchArgs
import com.etozhesandy.redpanda.features.chat.model.PhotoViewerArgs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * The one place this feature's routes are read, so the ViewModels can take plain arguments and
 * know nothing about [Routes].
 */
@Module
@InstallIn(ViewModelComponent::class)
object ChatArgsModule {

    @Provides
    fun provideChatArgs(savedStateHandle: SavedStateHandle): ChatArgs =
        savedStateHandle.toRoute<Routes.Chat>().toArgs()

    @Provides
    fun provideChatSearchArgs(savedStateHandle: SavedStateHandle): ChatSearchArgs =
        savedStateHandle.toRoute<Routes.ChatSearch>().toArgs()

    @Provides
    fun provideGlobalSearchArgs(savedStateHandle: SavedStateHandle): GlobalSearchArgs =
        savedStateHandle.toRoute<Routes.GlobalSearch>().toArgs()

    @Provides
    fun providePhotoViewerArgs(savedStateHandle: SavedStateHandle): PhotoViewerArgs =
        savedStateHandle.toRoute<Routes.PhotoViewer>().toArgs()
}
