package com.etozhesandy.redpanda.features.dialogs.di

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.dialogs.mapper.toArgs
import com.etozhesandy.redpanda.features.dialogs.model.DialogsArgs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * The one place the dialogs screen's route is read, so the ViewModel can take plain arguments and
 * know nothing about [Routes].
 */
@Module
@InstallIn(ViewModelComponent::class)
object DialogsArgsModule {

    @Provides
    fun provideDialogsArgs(savedStateHandle: SavedStateHandle): DialogsArgs =
        savedStateHandle.toRoute<Routes.Dialogs>().toArgs()
}
