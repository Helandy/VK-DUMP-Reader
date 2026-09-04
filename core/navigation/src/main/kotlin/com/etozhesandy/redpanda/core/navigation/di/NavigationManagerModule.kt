package com.etozhesandy.redpanda.core.navigation.di

import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.navigation.manager.NavigationManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NavigationManagerModule {

    @Provides
    @Singleton
    fun provideINavigationManager(navigationManager: NavigationManager): INavigationManager = navigationManager
}
