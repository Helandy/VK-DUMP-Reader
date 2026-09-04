package com.etozhesandy.redpanda.features.favorites.di

import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.features.favorites.navigation.FavoritesNavRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class FavoritesNavigationModule {

    @Binds
    @IntoSet
    abstract fun bindFavoritesNavRegistrar(impl: FavoritesNavRegistrar): NavRegistrar
}
