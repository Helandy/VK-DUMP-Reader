package com.etozhesandy.redpanda

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class RedPandaApplication : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            // Coil calls this initializer lazily, off the main thread, the first time it needs the
            // disk cache — which is what makes the blocking read below acceptable. Reading the
            // setting in `newImageLoader` instead would block the main thread during cold start.
            // The size is still resolved once per process: a changed setting takes effect on the
            // next cold start.
            .diskCache {
                val cacheSizeMb = runBlocking { settingsRepository.settings.first().coilCacheSizeMb }
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(cacheSizeMb * 1024L * 1024L)
                    .build()
            }
            .build()
}
