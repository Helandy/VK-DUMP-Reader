package com.etozhesandy.redpanda.features.favorites.di

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.toRoute
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.favorites.mapper.toArgs
import com.etozhesandy.redpanda.features.favorites.model.FavoritesArgs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

/**
 * The one place the favorites screen's route is read. Keeping it here is what lets the ViewModel
 * take plain arguments and know nothing about [Routes] or [SavedStateHandle].
 */
@Module
@InstallIn(ViewModelComponent::class)
object FavoritesArgsModule {

    @Provides
    fun provideFavoritesArgs(savedStateHandle: SavedStateHandle): FavoritesArgs =
        savedStateHandle.toRoute<Routes.Favorites>().toArgs()
}
