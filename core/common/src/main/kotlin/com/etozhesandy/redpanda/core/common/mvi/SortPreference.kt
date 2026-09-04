package com.etozhesandy.redpanda.core.common.mvi

import androidx.lifecycle.SavedStateHandle
import com.etozhesandy.redpanda.core.model.nextAscending
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/**
 * A sort key plus its direction, picked on one screen on top of an app-wide default.
 *
 * Nothing is chosen until the user picks something: until then [flow] follows [defaults], so a
 * change to the setting still reaches a screen that is already open. A pick is deliberately
 * one-off — it lives in the [SavedStateHandle] for the screen's lifetime and never rewrites the
 * default.
 */
class SortPreference<T : Enum<T>> @PublishedApi internal constructor(
    private val savedStateHandle: SavedStateHandle,
    private val sortKey: String,
    private val ascendingKey: String,
    private val naturalAscending: (T) -> Boolean,
    defaults: Flow<Pair<T, Boolean>>,
    restoredSort: T?,
    restoredAscending: Boolean?,
) {

    private val sortOverride = MutableStateFlow(restoredSort)
    private val ascendingOverride = MutableStateFlow(restoredAscending)

    val flow: Flow<Pair<T, Boolean>> =
        combine(sortOverride, ascendingOverride, defaults) { sort, ascending, (defaultSort, defaultAscending) ->
            (sort ?: defaultSort) to (ascending ?: defaultAscending)
        }

    /**
     * Records the user picking [picked] while [current]/[currentAscending] are on screen, and
     * returns the direction that pick lands on — picking the active key flips it.
     */
    fun select(picked: T, current: T, currentAscending: Boolean): Boolean {
        val ascending = nextAscending(
            picked = picked,
            current = current,
            currentAscending = currentAscending,
            natural = naturalAscending(picked),
        )
        savedStateHandle.putEnum(sortKey, picked)
        savedStateHandle[ascendingKey] = ascending
        sortOverride.value = picked
        ascendingOverride.value = ascending
        return ascending
    }
}

/**
 * Builds a [SortPreference] restored from this handle under `"<keyPrefix>_sort"` and
 * `"<keyPrefix>_sort_ascending"`.
 *
 * An extension rather than a constructor because reading the enum back needs [getEnum], which is
 * inline and reified.
 */
inline fun <reified T : Enum<T>> SavedStateHandle.sortPreference(
    keyPrefix: String,
    defaults: Flow<Pair<T, Boolean>>,
    noinline naturalAscending: (T) -> Boolean,
): SortPreference<T> {
    val sortKey = "${keyPrefix}_sort"
    val ascendingKey = "${keyPrefix}_sort_ascending"
    return SortPreference(
        savedStateHandle = this,
        sortKey = sortKey,
        ascendingKey = ascendingKey,
        naturalAscending = naturalAscending,
        defaults = defaults,
        restoredSort = getEnum<T>(sortKey),
        restoredAscending = get<Boolean>(ascendingKey),
    )
}
