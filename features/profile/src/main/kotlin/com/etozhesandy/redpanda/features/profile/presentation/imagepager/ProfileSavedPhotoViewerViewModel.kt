package com.etozhesandy.redpanda.features.profile.presentation.imagepager

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.files.ImageDownloader
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.profile.R
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import com.etozhesandy.redpanda.features.profile.model.ProfileSavedPhotoViewerArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ProfileSavedPhotoViewerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nav: INavigationManager,
    private val args: ProfileSavedPhotoViewerArgs,
    private val repository: ProfileInfoRepository,
    private val imageDownloader: ImageDownloader,
) : BaseViewModel<ImagePagerState.State, ImagePagerState.Event, ImagePagerState.Effect>() {

    override fun createInitialState() = ImagePagerState.State()

    /** Folder name for saved files; the id is only a fallback until the profile loads. */
    private var profileName: String = args.profileId

    init {
        repository.observeProfile(args.profileId)
            .onEach { profile -> profile?.displayName?.takeIf { it.isNotBlank() }?.let { profileName = it } }
            .launchIn(viewModelScope)

        repository.observeSavedPhotos(args.profileId)
            .onEach { photos ->
                val startIndex = photos.indexOfFirst { it.id == args.startPhotoId }.coerceAtLeast(0)
                setState { copy(urls = photos.map { it.url }, startIndex = startIndex, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ImagePagerState.Event) {
        when (event) {
            ImagePagerState.Event.BackClicked -> nav.back()

            is ImagePagerState.Event.DownloadClicked -> launchSafe {
                val savedTo = imageDownloader.download(event.url, profileName, context.getString(R.string.download_folder_saved_photos)).getOrNull()
                setEffect { ImagePagerState.Effect.DownloadFinished(savedTo) }
            }
        }
    }
}
