package com.etozhesandy.redpanda.features.settings.presentation

import com.etozhesandy.redpanda.core.model.DialogSort
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.model.MessageSort
import com.etozhesandy.redpanda.core.security.model.AppLockConfig
import com.etozhesandy.redpanda.core.security.model.BiometricAvailability
import com.etozhesandy.redpanda.core.settings.AppSettings
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState
import java.io.File

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object SettingsState {

    data class State(
        val coilCacheSizeMb: Int = AppSettings.DEFAULT_COIL_CACHE_SIZE_MB,
        val mediaImageWidthDp: Int = AppSettings.DEFAULT_MEDIA_IMAGE_WIDTH_DP,
        val defaultDialogSort: DialogSort = AppSettings.DEFAULT_DIALOG_SORT,
        val defaultDialogSortAscending: Boolean = AppSettings.DEFAULT_DIALOG_SORT_ASCENDING,
        val defaultChatReversed: Boolean = AppSettings.DEFAULT_CHAT_REVERSED,
        val defaultMediaSort: MediaSort = AppSettings.DEFAULT_MEDIA_SORT,
        val defaultMediaSortAscending: Boolean = AppSettings.DEFAULT_MEDIA_SORT_ASCENDING,
        val defaultSearchSort: MessageSort = AppSettings.DEFAULT_SEARCH_SORT,
        val defaultSearchSortAscending: Boolean = AppSettings.DEFAULT_SEARCH_SORT_ASCENDING,
        val loggingEnabled: Boolean = AppSettings.DEFAULT_LOGGING_ENABLED,
        val profilesCacheBytes: Long? = null,
        val isCacheSizeLoading: Boolean = true,
        val appLockEnabled: Boolean = false,
        val biometricEnabled: Boolean = false,
        val biometricAvailability: BiometricAvailability = BiometricAvailability.UNAVAILABLE,
        val lockTimeoutSeconds: Int = AppLockConfig.DEFAULT_TIMEOUT_SECONDS,
    ) : UiState

    sealed interface Event : UiEvent {
        data class CoilCacheSizeChanged(val valueMb: Int) : Event
        data class MediaImageWidthChanged(val widthDp: Int) : Event
        data class DefaultDialogSortSelected(val sort: DialogSort) : Event
        data class DefaultChatReversedChanged(val value: Boolean) : Event
        data class DefaultMediaSortSelected(val sort: MediaSort) : Event
        data class DefaultSearchSortSelected(val sort: MessageSort) : Event
        data class AppLockToggled(val value: Boolean) : Event
        data object ChangePinClicked : Event
        data class BiometricToggled(val value: Boolean) : Event
        data class LockTimeoutChanged(val seconds: Int) : Event
        data class LoggingToggled(val value: Boolean) : Event
        data object ShareLogClicked : Event
        data object ClearLogClicked : Event
        data object BackClicked : Event
    }

    sealed interface Effect : UiEffect {
        data class ShareLog(val files: List<File>) : Effect
        data object LogEmpty : Effect
        data object LogCleared : Effect
    }
}
