package com.etozhesandy.redpanda.core.archive.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.etozhesandy.redpanda.core.archive.source.ArchiveSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Entry point for kicking off a profile import as durable background work. */
class ProfileImportScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /**
     * Whether an import is enqueued or running right now. Imports are one-at-a-time — two of them
     * share the same foreground notification and hammer the same disk, and the second one ends up
     * broken — so the UI uses this to keep the import entry points shut until the current one is
     * done. WorkManager, not the profile rows, is the source of truth here: a profile left in
     * `IMPORTING` by a killed process would otherwise block imports forever.
     */
    fun observeImportRunning(): Flow<Boolean> =
        workManager.getWorkInfosByTagFlow(TAG_IMPORT)
            .map { infos -> infos.any { !it.state.isFinished } }
            .distinctUntilChanged()

    fun enqueue(profileId: String, source: ArchiveSource) {
        val (sourceType, uri) = when (source) {
            is ArchiveSource.ArchiveFile -> "ARCHIVE" to source.uri
            is ArchiveSource.Directory -> "DIRECTORY" to source.uri
        }
        val request = OneTimeWorkRequestBuilder<ProfileImportWorker>()
            .addTag(TAG_IMPORT)
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

    private companion object {
        const val TAG_IMPORT = "profile_import"
    }
}
