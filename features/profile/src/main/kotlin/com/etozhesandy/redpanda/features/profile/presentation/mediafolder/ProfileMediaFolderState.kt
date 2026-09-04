package com.etozhesandy.redpanda.features.profile.presentation.mediafolder

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.settings.AppSettings
import com.etozhesandy.redpanda.core.common.mvi.UiEffect
import com.etozhesandy.redpanda.core.common.mvi.UiEvent
import com.etozhesandy.redpanda.core.common.mvi.UiState

/** MVI-контракт экрана: состояние, события и одноразовые эффекты. */
object ProfileMediaFolderState {

    data class State(
        val folderName: String = "",
        val attachments: List<Attachment> = emptyList(),
        val isLoading: Boolean = true,
        val imageWidthDp: Int = AppSettings.DEFAULT_MEDIA_IMAGE_WIDTH_DP,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackClicked : Event
        data class AttachmentClicked(val attachment: Attachment) : Event
    }

    /** No one-off effects: the only outward action is navigation, and that goes through
     * INavigationManager. */
    sealed interface Effect : UiEffect
}
