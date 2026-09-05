package com.etozhesandy.redpanda.core.archive.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.etozhesandy.redpanda.core.archive.R
import com.etozhesandy.redpanda.core.archive.delete.ProfileEraser
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs [ProfileEraser] as durable, foreground WorkManager work. Erasing a profile deletes gigabytes
 * of extracted archive, so like the import it has to outlive the screen that started it: a
 * `viewModelScope` coroutine dies with Home and leaves half a profile on disk.
 */
@HiltWorker
class ProfileDeleteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val eraser: ProfileEraser,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val profileId = inputData.getString(KEY_PROFILE_ID) ?: return Result.failure()

        // The notification is a courtesy, not a precondition: the system refuses a foreground start
        // when the app went to the background between enqueueing and running, and the erase itself
        // is still worth doing.
        runCatching { setForeground(foregroundInfo()) }

        return runCatching { eraser.erase(profileId) }.fold(
            // The erase is idempotent, so a transient I/O failure can simply be repeated.
            onSuccess = { Result.success() },
            onFailure = { if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure() },
        )
    }

    private fun foregroundInfo(): ForegroundInfo {
        ensureNotificationChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(applicationContext.getString(R.string.delete_notification_title))
            .setContentText(applicationContext.getString(R.string.delete_notification_text))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
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
                applicationContext.getString(R.string.delete_notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
    }

    companion object {
        const val KEY_PROFILE_ID = "profileId"
        private const val CHANNEL_ID = "profile_delete"
        private const val NOTIFICATION_ID = 4202
        private const val MAX_ATTEMPTS = 3
    }
}
