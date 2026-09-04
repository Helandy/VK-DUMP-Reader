package com.etozhesandy.redpanda.core.archive.di

import com.etozhesandy.redpanda.core.archive.extract.ArchiveExtractor
import com.etozhesandy.redpanda.core.archive.extract.ArchiveExtractorImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ArchiveModule {

    @Binds
    @Singleton
    abstract fun bindArchiveExtractor(impl: ArchiveExtractorImpl): ArchiveExtractor
}
