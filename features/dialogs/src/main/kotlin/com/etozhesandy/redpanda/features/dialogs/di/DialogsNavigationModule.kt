package com.etozhesandy.redpanda.features.dialogs.di

import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.features.dialogs.navigation.DialogsNavRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class DialogsNavigationModule {

    @Binds
    @IntoSet
    abstract fun bindDialogsNavRegistrar(impl: DialogsNavRegistrar): NavRegistrar
}
