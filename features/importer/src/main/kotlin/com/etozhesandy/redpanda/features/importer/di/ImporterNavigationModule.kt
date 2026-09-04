package com.etozhesandy.redpanda.features.importer.di

import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.features.importer.navigation.ImportNavRegistrar
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ImporterNavigationModule {

    @Binds
    @IntoSet
    abstract fun bindImportNavRegistrar(impl: ImportNavRegistrar): NavRegistrar
}
