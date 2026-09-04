package com.etozhesandy.redpanda.features.favorites.data.di

import com.etozhesandy.redpanda.features.favorites.data.FavoritesRepositoryImpl
import com.etozhesandy.redpanda.features.favorites.domain.repository.FavoritesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FavoritesDataModule {

    @Binds
    @Singleton
    abstract fun bindFavoritesRepository(impl: FavoritesRepositoryImpl): FavoritesRepository
}
