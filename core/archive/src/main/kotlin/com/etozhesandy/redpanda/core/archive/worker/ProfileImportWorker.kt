package com.etozhesandy.redpanda.core.archive.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.Data
import com.etozhesandy.redpanda.core.archive.R
import com.etozhesandy.redpanda.core.archive.pipeline.ProfileImportPipeline
import com.etozhesandy.redpanda.core.archive.source.ArchiveSource
import com.etozhesandy.redpanda.core.model.ImportProgress
import com.etozhesandy.redpanda.core.model.ImportStage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest

private fun Data.toArchiveSource(): ArchiveSource? {
    val type = getString(ProfileImportWorker.KEY_SOURCE_TYPE) ?: return null
    val uri = getString(ProfileImportWorker.KEY_URI)?.let(Uri::parse) ?: return null
    return when (type) {
        "ARCHIVE" -> ArchiveSource.ArchiveFile(uri)
        // Work enqueued by an older build, still in the queue across the update.
        "ZIP", "RAR" -> ArchiveSource.ArchiveFile(uri)
        "DIRECTORY" -> ArchiveSource.Directory(uri)
        else -> null
    }
}

/**
 * Runs [ProfileImportPipeline] as durable, foreground WorkManager work: it survives the user
 * leaving the import screen, backgrounding the app, or the process being killed and restarted —
 * unlike a plain `viewModelScope` coroutine, which is torn down with its screen.
 */
@HiltWorker
class ProfileImportWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pipeline: ProfileImportPipeline,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val profileId = inputData.getString(KEY_PROFILE_ID) ?: return Result.failure()
        val source = inputData.toArchiveSource() ?: return Result.failure()

        setForeground(foregroundInfo(applicationContext.getString(R.string.import_stage_preparing)))

        var lastNotifiedAt = 0L
        var failed = false
        pipeline.import(profileId, source).collectLatest { progress ->
            when (progress.stage) {
                ImportStage.ERROR -> failed = true
                else -> Unit
            }
            val now = System.currentTimeMillis()
            if (now - lastNotifiedAt >= NOTIFICATION_THROTTLE_MS) {
                lastNotifiedAt = now
                setForeground(
                    foregroundInfo(progress.label(), progress.dialogsDone, progress.dialogsTotal),
                )
            }
        }

        return if (failed) Result.failure() else Result.success()
    }

    private fun foregroundInfo(contentText: String, done: Int = 0, total: Int = 0): ForegroundInfo {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.import_notification_title))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Indeterminate until the parser has counted the dialogs.
            .setProgress(total, done, total <= 0)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = applicationContext.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.import_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    /**
     * The dialog total is only known once the parser has enumerated the archive, so until then
     * (and for the copy/extract stages) the message counter is all there is to show.
     */
    private fun ImportProgress.label(): String = when (stage) {
        ImportStage.COPYING -> applicationContext.getString(R.string.import_stage_copying)
        ImportStage.EXTRACTING -> applicationContext.getString(R.string.import_stage_extracting)
        ImportStage.PARSING -> applicationContext.getString(R.string.import_stage_parsing)
        ImportStage.SAVING -> if (dialogsTotal > 0) {
            applicationContext.getString(
                R.string.import_stage_saving_dialogs,
                dialogsDone,
                dialogsTotal,
                current,
            )
        } else {
            applicationContext.getString(R.string.import_stage_saving_messages, current)
        }
        ImportStage.DONE -> applicationContext.getString(R.string.import_stage_done)
        ImportStage.ERROR -> applicationContext.getString(R.string.import_stage_error)
    }

    companion object {
        const val KEY_PROFILE_ID = "profileId"
        const val KEY_SOURCE_TYPE = "sourceType"
        const val KEY_URI = "uri"
        private const val CHANNEL_ID = "profile_import"
        private const val NOTIFICATION_ID = 4201
        private const val NOTIFICATION_THROTTLE_MS = 1000L
    }
}
