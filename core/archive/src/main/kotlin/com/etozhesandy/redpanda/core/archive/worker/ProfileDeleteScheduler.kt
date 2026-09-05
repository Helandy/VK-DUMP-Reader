package com.etozhesandy.redpanda.core.archive.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Entry point for erasing a profile as durable background work. */
class ProfileDeleteScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val workManager: WorkManager get() = WorkManager.getInstance(context)

    /**
     * Ids of the profiles whose deletion is enqueued or running right now. The rows vanish from the
     * database early in the erase, but the directory walk keeps going for a while after that, so the
     * UI uses this to keep a profile that is on its way out from being opened.
     */
    fun observeDeletingProfileIds(): Flow<Set<String>> =
        workManager.getWorkInfosByTagFlow(TAG_DELETE)
            .map { infos ->
                infos.filterNot { it.state.isFinished }
                    .mapNotNullTo(mutableSetOf()) { info ->
                        info.tags.firstOrNull { it.startsWith(PROFILE_TAG_PREFIX) }
                            ?.removePrefix(PROFILE_TAG_PREFIX)
                    }
            }
            .distinctUntilChanged()

    /**
     * Deletion shares the import's unique work name — the profile id — and replaces it: deleting a
     * profile that is still importing has to stop the import first, otherwise the pipeline keeps
     * writing rows behind the erase.
     */
    fun enqueue(profileId: String) {
        val request = OneTimeWorkRequestBuilder<ProfileDeleteWorker>()
            .addTag(TAG_DELETE)
            .addTag(PROFILE_TAG_PREFIX + profileId)
            .setInputData(workDataOf(ProfileDeleteWorker.KEY_PROFILE_ID to profileId))
            .build()
        workManager.enqueueUniqueWork(profileId, ExistingWorkPolicy.REPLACE, request)
    }

    private companion object {
        const val TAG_DELETE = "profile_delete"
        const val PROFILE_TAG_PREFIX = "profile_delete:"
    }
}
