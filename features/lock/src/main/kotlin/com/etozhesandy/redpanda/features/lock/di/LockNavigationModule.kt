package com.etozhesandy.redpanda.features.lock.di

import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.features.lock.navigation.LockNavRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class LockNavigationModule {

    @Binds
    @IntoSet
    abstract fun bindLockNavRegistrar(impl: LockNavRegistrar): NavRegistrar
}
