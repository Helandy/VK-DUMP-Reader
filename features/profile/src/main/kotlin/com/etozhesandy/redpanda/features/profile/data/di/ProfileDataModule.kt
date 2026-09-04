package com.etozhesandy.redpanda.features.profile.data.di

import com.etozhesandy.redpanda.features.profile.data.ProfileInfoRepositoryImpl
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileDataModule {

    @Binds
    @Singleton
    abstract fun bindProfileInfoRepository(impl: ProfileInfoRepositoryImpl): ProfileInfoRepository
}
