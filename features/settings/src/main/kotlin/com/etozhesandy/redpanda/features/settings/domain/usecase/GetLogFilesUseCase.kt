package com.etozhesandy.redpanda.features.settings.domain.usecase

import com.etozhesandy.redpanda.core.common.dispatcher.IoDispatcher
import com.etozhesandy.redpanda.core.common.logging.AppLogger
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

/** Returns the recorded diagnostic log files (logcat output and crash reports), empty if none. */
class GetLogFilesUseCase @Inject constructor(
    private val logger: AppLogger,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(): List<File> = withContext(ioDispatcher) { logger.logFiles() }
}
