package com.etozhesandy.redpanda.features.chat.di

import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.features.chat.navigation.ChatNavRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatNavigationModule {

    @Binds
    @IntoSet
    abstract fun bindChatNavRegistrar(impl: ChatNavRegistrar): NavRegistrar
}
