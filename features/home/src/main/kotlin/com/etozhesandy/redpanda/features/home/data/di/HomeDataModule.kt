package com.etozhesandy.redpanda.features.home.data.di

import com.etozhesandy.redpanda.features.home.data.HomeRepositoryImpl
import com.etozhesandy.redpanda.features.home.domain.repository.HomeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeDataModule {

    @Binds
    @Singleton
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository
}
