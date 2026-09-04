package com.etozhesandy.redpanda.core.settings.di

import com.etozhesandy.redpanda.core.settings.SettingsRepository
import com.etozhesandy.redpanda.core.settings.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBindsModule {

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
