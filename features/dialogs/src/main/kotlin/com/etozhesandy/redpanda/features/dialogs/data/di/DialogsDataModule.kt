package com.etozhesandy.redpanda.features.dialogs.data.di

import com.etozhesandy.redpanda.features.dialogs.data.DialogsRepositoryImpl
import com.etozhesandy.redpanda.features.dialogs.domain.repository.DialogsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DialogsDataModule {

    @Binds
    @Singleton
    abstract fun bindDialogsRepository(impl: DialogsRepositoryImpl): DialogsRepository
}
