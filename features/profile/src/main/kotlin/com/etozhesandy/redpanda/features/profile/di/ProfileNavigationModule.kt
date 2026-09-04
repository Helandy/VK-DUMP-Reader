package com.etozhesandy.redpanda.features.profile.di

import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.features.profile.navigation.ProfileNavRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileNavigationModule {

    @Binds
    @IntoSet
    abstract fun bindProfileNavRegistrar(impl: ProfileNavRegistrar): NavRegistrar
}
