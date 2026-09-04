package com.etozhesandy.redpanda.core.settings

import com.etozhesandy.redpanda.core.model.DialogSort
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.model.MessageSort

/** User-configurable app-wide settings, backed by DataStore. */
data class AppSettings(
    val coilCacheSizeMb: Int = DEFAULT_COIL_CACHE_SIZE_MB,
    val mediaImageWidthDp: Int = DEFAULT_MEDIA_IMAGE_WIDTH_DP,
    val defaultDialogSort: DialogSort = DEFAULT_DIALOG_SORT,
    val defaultDialogSortAscending: Boolean = DEFAULT_DIALOG_SORT_ASCENDING,
    val defaultChatReversed: Boolean = DEFAULT_CHAT_REVERSED,
    val defaultMediaSort: MediaSort = DEFAULT_MEDIA_SORT,
    val defaultMediaSortAscending: Boolean = DEFAULT_MEDIA_SORT_ASCENDING,
    val defaultSearchSort: MessageSort = DEFAULT_SEARCH_SORT,
    val defaultSearchSortAscending: Boolean = DEFAULT_SEARCH_SORT_ASCENDING,
) {
    companion object {
        const val DEFAULT_COIL_CACHE_SIZE_MB = 500
        const val DEFAULT_MEDIA_IMAGE_WIDTH_DP = 120

        const val COIL_CACHE_MIN_MB = 100
        const val COIL_CACHE_MAX_MB = 2000
        const val COIL_CACHE_STEP_MB = 100

        const val MEDIA_IMAGE_MIN_WIDTH_DP = 60
        const val MEDIA_IMAGE_MAX_WIDTH_DP = 200
        const val MEDIA_IMAGE_WIDTH_STEP_DP = 10

        // The order each screen used before it became configurable, so a fresh install behaves
        // exactly as it did.
        val DEFAULT_DIALOG_SORT = DialogSort.DATE
        const val DEFAULT_DIALOG_SORT_ASCENDING = false

        /** true shows the newest messages at the top of a dialog. */
        const val DEFAULT_CHAT_REVERSED = false

        val DEFAULT_MEDIA_SORT = MediaSort.DATE
        const val DEFAULT_MEDIA_SORT_ASCENDING = true

        val DEFAULT_SEARCH_SORT = MessageSort.DATE
        const val DEFAULT_SEARCH_SORT_ASCENDING = false
    }
}
