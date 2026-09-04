package com.etozhesandy.redpanda.core.security.di

import com.etozhesandy.redpanda.core.security.AppLockRepository
import com.etozhesandy.redpanda.core.security.AppLockRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityBindsModule {

    @Binds
    abstract fun bindAppLockRepository(impl: AppLockRepositoryImpl): AppLockRepository
}
