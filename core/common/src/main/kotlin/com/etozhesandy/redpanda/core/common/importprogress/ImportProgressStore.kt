package com.etozhesandy.redpanda.core.common.importprogress

import com.etozhesandy.redpanda.core.model.ImportProgress
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-process mirror of the running import's progress, keyed by profile id.
 *
 * The import itself runs in a WorkManager worker (same process), and its progress flow is consumed
 * there; screens can't collect that flow directly, so the pipeline also publishes each snapshot
 * here for whatever UI happens to be on screen. Purely in-memory: after a process restart the
 * counters are unknown until the worker emits again, and the UI falls back to a plain
 * "importing" banner.
 */
@Singleton
class ImportProgressStore @Inject constructor() {

    private val state = MutableStateFlow<Map<String, ImportProgress>>(emptyMap())

    val progress: StateFlow<Map<String, ImportProgress>> = state.asStateFlow()

    fun observe(profileId: String) = progress.map { it[profileId] }

    fun publish(profileId: String, progress: ImportProgress) {
        state.value = state.value + (profileId to progress)
    }

    fun clear(profileId: String) {
        state.value = state.value - profileId
    }
}
