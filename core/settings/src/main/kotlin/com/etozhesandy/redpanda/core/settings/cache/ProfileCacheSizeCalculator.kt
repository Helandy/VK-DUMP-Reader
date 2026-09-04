package com.etozhesandy.redpanda.core.settings.cache

import com.etozhesandy.redpanda.core.common.dispatcher.IoDispatcher
import com.etozhesandy.redpanda.core.common.files.ProfileDirectories
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Computes the total on-disk size of every imported profile's raw data. */
@Singleton
class ProfileCacheSizeCalculator @Inject constructor(
    private val profileDirectories: ProfileDirectories,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun calculateTotalBytes(): Long = withContext(ioDispatcher) {
        profileDirectories.rootDir()
            .walkBottomUp()
            .filter { it.isFile }
            .sumOf { it.length() }
    }
}
