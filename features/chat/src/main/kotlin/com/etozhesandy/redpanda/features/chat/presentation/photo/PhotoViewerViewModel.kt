package com.etozhesandy.redpanda.features.chat.presentation.photo

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.files.ImageDownloader
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.navigation.manager.PopUpTo
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveChatProfileUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveDialogMediaUseCase
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveDialogUseCase
import com.etozhesandy.redpanda.features.chat.model.PhotoViewerArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class PhotoViewerViewModel @Inject constructor(
    args: PhotoViewerArgs,
    private val nav: INavigationManager,
    observeDialogMedia: ObserveDialogMediaUseCase,
    observeDialog: ObserveDialogUseCase,
    private val observeProfile: ObserveChatProfileUseCase,
    private val imageDownloader: ImageDownloader,
) : BaseViewModel<PhotoViewerState.State, PhotoViewerState.Event, PhotoViewerState.Effect>() {

    override fun createInitialState() = PhotoViewerState.State()

    // Folder names for saved files; ids are only a fallback until the dialog and profile load.
    private var profileName: String = ""
    private var dialogName: String = args.dialogId
    private var isProfileObserved = false

    init {
        observeDialog(args.dialogId)
            .onEach { dialog ->
                if (dialog == null) return@onEach
                dialog.peerName.takeIf { it.isNotBlank() }?.let { dialogName = it }
                observeProfileName(dialog.profileId)
            }
            .launchIn(viewModelScope)

        observeDialogMedia(args.dialogId)
            .onEach { attachments ->
                val index = attachments.indexOfFirst { it.id == args.startAttachmentId }.coerceAtLeast(0)
                setState { copy(attachments = attachments, startIndex = index) }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: PhotoViewerState.Event) {
        when (event) {
            is PhotoViewerState.Event.JumpToMessageClicked -> {
                // Gallery-sourced attachments (VK's flat photos.html/videos.html export) carry no
                // messageId of their own — fall back to another attachment with the same path that
                // was also sent inline in a message, since VK re-uses the same CDN URL for both.
                val messageId = event.attachment.messageId
                    ?: currentState.attachments.firstOrNull { it.path == event.attachment.path && it.messageId != null }?.messageId
                    ?: return
                // The chat is rebuilt around the target message, so the old chat entry goes with
                // this viewer rather than staying behind it.
                nav.navigate(
                    Routes.Chat(
                        dialogId = event.attachment.dialogId,
                        profileId = event.attachment.profileId,
                        scrollToMessageId = messageId,
                    ),
                    PopUpTo(Routes.Chat::class, inclusive = true),
                )
            }

            PhotoViewerState.Event.BackClicked -> nav.back()

            is PhotoViewerState.Event.DownloadClicked -> launchSafe {
                val savedTo = imageDownloader.download(event.source, profileName, dialogName).getOrNull()
                setEffect { PhotoViewerState.Effect.DownloadFinished(savedTo) }
            }
        }
    }

    /** The route carries only a dialogId, so the profile is reached through the dialog. */
    private fun observeProfileName(profileId: String) {
        if (isProfileObserved) return
        isProfileObserved = true
        observeProfile(profileId)
            .onEach { profile -> profile?.displayName?.takeIf { it.isNotBlank() }?.let { profileName = it } }
            .launchIn(viewModelScope)
    }
}
