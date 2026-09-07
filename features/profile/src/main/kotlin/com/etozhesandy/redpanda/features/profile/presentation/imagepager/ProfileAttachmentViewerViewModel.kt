package com.etozhesandy.redpanda.features.profile.presentation.imagepager

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.files.ImageDownloader
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.profile.R
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import com.etozhesandy.redpanda.features.profile.model.ProfileAttachmentViewerArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ProfileAttachmentViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nav: INavigationManager,
    private val args: ProfileAttachmentViewerArgs,
    private val repository: ProfileInfoRepository,
    private val imageDownloader: ImageDownloader,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<ImagePagerState.State, ImagePagerState.Event, ImagePagerState.Effect>() {

    override fun createInitialState() = ImagePagerState.State()

    /** Folder name for saved files; the id is only a fallback until the profile loads. */
    private var profileName: String = args.profileId

    init {
        repository.observeProfile(args.profileId)
            .onEach { profile -> profile?.displayName?.takeIf { it.isNotBlank() }?.let { profileName = it } }
            .launchIn(viewModelScope)

        repository.observeAttachments(args.profileId)
            .map { attachments -> attachments.toPages() to attachments.indexOfFirst { it.id == args.startAttachmentId } }
            .flowOn(defaultDispatcher)
            .onEach { (pages, index) ->
                setState { copy(pages = pages, startIndex = index.coerceAtLeast(0), isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ImagePagerState.Event) {
        when (event) {
            ImagePagerState.Event.BackClicked -> nav.back()

            // The grid this viewer was opened from stays on the stack, so going back from the
            // dialog returns to the photo and then to the grid.
            is ImagePagerState.Event.JumpToMessageClicked -> nav.navigate(
                Routes.Chat(
                    dialogId = event.anchor.dialogId,
                    profileId = event.anchor.profileId,
                    scrollToMessageId = event.anchor.messageId,
                ),
            )

            is ImagePagerState.Event.DownloadClicked -> launchSafe {
                val savedTo = imageDownloader.download(event.url, profileName, context.getString(R.string.download_folder_attachments)).getOrNull()
                setEffect { ImagePagerState.Effect.DownloadFinished(savedTo) }
            }
        }
    }

    /**
     * Attachments from VK's flat gallery export carry no message of their own (and sometimes no
     * dialog either), but VK re-uses the same CDN URL for the copy that was sent inline — so a
     * twin sharing the [Attachment.path] is what the jump aims at.
     *
     * The twins are indexed once rather than searched per attachment: a profile's attachment list
     * is the whole archive's worth, and a scan inside the mapping would be quadratic over it.
     */
    private fun List<Attachment>.toPages(): List<ImagePagerState.Page> {
        val anchors = filter { it.messageId != null && it.dialogId.isNotBlank() && it.path.isNotBlank() }
            // Blank paths are excluded above: they are not one file, and would all collide on one key.
            .associate { it.path to ImagePagerState.MessageAnchor(it.dialogId, it.profileId, it.messageId!!) }
        return map { ImagePagerState.Page(url = it.path, anchor = anchors[it.path]) }
    }
}
