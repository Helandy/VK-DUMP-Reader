package com.etozhesandy.redpanda.core.archive.di

import com.etozhesandy.redpanda.core.archive.format.probe.ArchiveLayoutProbe
import com.etozhesandy.redpanda.core.archive.format.probe.VkApiLayoutProbe
import com.etozhesandy.redpanda.core.archive.format.probe.VkHtmlHistoryLayoutProbe
import com.etozhesandy.redpanda.core.archive.format.probe.VkHtmlTorrentLayoutProbe
import com.etozhesandy.redpanda.core.archive.format.probe.VkJsonDumpLayoutProbe
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoSet
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * The complete set of export layouts the app recognises. Teaching it a new one is a new
 * [ArchiveLayoutProbe] and one line here — nothing already in this list is touched.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ArchiveProbeModule {

    @Binds
    @IntoSet
    abstract fun bindVkApiProbe(impl: VkApiLayoutProbe): ArchiveLayoutProbe

    @Binds
    @IntoSet
    abstract fun bindVkJsonDumpProbe(impl: VkJsonDumpLayoutProbe): ArchiveLayoutProbe

    @Binds
    @IntoSet
    abstract fun bindVkHtmlTorrentProbe(impl: VkHtmlTorrentLayoutProbe): ArchiveLayoutProbe

    @Binds
    @IntoSet
    abstract fun bindVkHtmlHistoryProbe(impl: VkHtmlHistoryLayoutProbe): ArchiveLayoutProbe
}
