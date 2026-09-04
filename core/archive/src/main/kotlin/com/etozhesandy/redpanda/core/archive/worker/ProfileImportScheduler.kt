package com.etozhesandy.redpanda.core.archive.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.etozhesandy.redpanda.core.archive.source.ArchiveSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Entry point for kicking off a profile import as durable background work. */
class ProfileImportScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun enqueue(profileId: String, source: ArchiveSource) {
        val (sourceType, uri) = when (source) {
            is ArchiveSource.ArchiveFile -> "ARCHIVE" to source.uri
            is ArchiveSource.Directory -> "DIRECTORY" to source.uri
        }
        val request = OneTimeWorkRequestBuilder<ProfileImportWorker>()
            .setInputData(
                Data.Builder()
                    .putString(ProfileImportWorker.KEY_PROFILE_ID, profileId)
                    .putString(ProfileImportWorker.KEY_SOURCE_TYPE, sourceType)
                    .putString(ProfileImportWorker.KEY_URI, uri.toString())
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(profileId, ExistingWorkPolicy.KEEP, request)
    }
}
