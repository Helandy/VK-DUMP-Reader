package com.etozhesandy.redpanda.features.home.di

import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.features.home.navigation.HomeNavRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeNavigationModule {

    @Binds
    @IntoSet
    abstract fun bindHomeNavRegistrar(impl: HomeNavRegistrar): NavRegistrar
}
