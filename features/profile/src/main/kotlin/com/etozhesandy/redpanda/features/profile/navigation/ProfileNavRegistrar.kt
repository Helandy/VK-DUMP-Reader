package com.etozhesandy.redpanda.features.profile.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.profile.presentation.imagepager.ProfileAttachmentViewerViewModel
import com.etozhesandy.redpanda.features.profile.presentation.imagepager.ProfileSavedPhotoViewerViewModel
import com.etozhesandy.redpanda.features.profile.presentation.attachments.ProfileAttachmentsScreen
import com.etozhesandy.redpanda.features.profile.presentation.attachments.ProfileAttachmentsViewModel
import com.etozhesandy.redpanda.features.profile.presentation.friends.ProfileFriendsScreen
import com.etozhesandy.redpanda.features.profile.presentation.friends.ProfileFriendsViewModel
import com.etozhesandy.redpanda.features.profile.presentation.groups.ProfileGroupsScreen
import com.etozhesandy.redpanda.features.profile.presentation.groups.ProfileGroupsViewModel
import com.etozhesandy.redpanda.features.profile.presentation.imagepager.ImagePagerScreen
import com.etozhesandy.redpanda.features.profile.presentation.media.ProfileMediaScreen
import com.etozhesandy.redpanda.features.profile.presentation.media.ProfileMediaViewModel
import com.etozhesandy.redpanda.features.profile.presentation.mediafolder.ProfileMediaFolderScreen
import com.etozhesandy.redpanda.features.profile.presentation.mediafolder.ProfileMediaFolderViewModel
import com.etozhesandy.redpanda.features.profile.presentation.mediaviewer.ProfileMediaViewerScreen
import com.etozhesandy.redpanda.features.profile.presentation.mediaviewer.ProfileMediaViewerViewModel
import com.etozhesandy.redpanda.features.profile.presentation.profile.ProfileScreen
import com.etozhesandy.redpanda.features.profile.presentation.profile.ProfileViewModel
import com.etozhesandy.redpanda.features.profile.presentation.savedphotos.ProfileSavedPhotosScreen
import com.etozhesandy.redpanda.features.profile.presentation.savedphotos.ProfileSavedPhotosViewModel
import com.etozhesandy.redpanda.features.profile.presentation.webview.WebViewScreen
import javax.inject.Inject

class ProfileNavRegistrar @Inject constructor(
    /** Only for [Routes.WebView], the one destination here with no ViewModel of its own. */
    private val nav: INavigationManager,
) : NavRegistrar {

    override fun register(builder: NavGraphBuilder) {
        builder.composable<Routes.Profile> {
            val viewModel: ProfileViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ProfileScreen(state = state, effect = viewModel.effect, onEvent = viewModel::onEvent)
        }

        builder.composable<Routes.ProfileFriends> {
            val viewModel: ProfileFriendsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ProfileFriendsScreen(state = state, effect = viewModel.effect, onEvent = viewModel::onEvent)
        }

        builder.composable<Routes.ProfileGroups> {
            val viewModel: ProfileGroupsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ProfileGroupsScreen(state = state, effect = viewModel.effect, onEvent = viewModel::onEvent)
        }

        builder.composable<Routes.ProfileSavedPhotos> {
            val viewModel: ProfileSavedPhotosViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ProfileSavedPhotosScreen(state = state, onEvent = viewModel::onEvent)
        }

        builder.composable<Routes.ProfileAttachments> {
            val viewModel: ProfileAttachmentsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ProfileAttachmentsScreen(state = state, onEvent = viewModel::onEvent)
        }

        builder.composable<Routes.ProfileMedia> {
            val viewModel: ProfileMediaViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ProfileMediaScreen(state = state, onEvent = viewModel::onEvent)
        }

        builder.composable<Routes.ProfileMediaFolder> {
            val viewModel: ProfileMediaFolderViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ProfileMediaFolderScreen(state = state, onEvent = viewModel::onEvent)
        }

        builder.composable<Routes.ProfileMediaViewer> {
            val viewModel: ProfileMediaViewerViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ProfileMediaViewerScreen(state = state, effect = viewModel.effect, onEvent = viewModel::onEvent)
        }

        // Two routes, one screen: both viewers drive the same ImagePagerState contract, and only
        // the ViewModel behind them differs.
        builder.composable<Routes.ProfileSavedPhotoViewer> {
            val viewModel: ProfileSavedPhotoViewerViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ImagePagerScreen(state = state, effect = viewModel.effect, onEvent = viewModel::onEvent)
        }

        builder.composable<Routes.ProfileAttachmentViewer> {
            val viewModel: ProfileAttachmentViewerViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ImagePagerScreen(state = state, effect = viewModel.effect, onEvent = viewModel::onEvent)
        }

        builder.composable<Routes.WebView> { backStackEntry ->
            WebViewScreen(
                url = backStackEntry.toRoute<Routes.WebView>().url,
                onBack = { nav.back() },
            )
        }
    }
}
