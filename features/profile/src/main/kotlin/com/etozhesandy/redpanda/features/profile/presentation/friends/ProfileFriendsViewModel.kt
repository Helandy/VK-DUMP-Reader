package com.etozhesandy.redpanda.features.profile.presentation.friends

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.common.net.UrlGuard
import com.etozhesandy.redpanda.features.profile.domain.model.matching
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import com.etozhesandy.redpanda.features.profile.model.ProfileFriendsArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ProfileFriendsViewModel @Inject constructor(
    private val nav: INavigationManager,
    args: ProfileFriendsArgs,
    repository: ProfileInfoRepository,
) : BaseViewModel<ProfileFriendsState.State, ProfileFriendsState.Event, ProfileFriendsState.Effect>() {

    override fun createInitialState() = ProfileFriendsState.State()

    private val profileId = args.profileId
    private val query = MutableStateFlow("")

    init {
        combine(repository.observeFriends(profileId), query) { friends, q -> friends.matching(q) }
            .onEach { friends -> setState { copy(friends = friends, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ProfileFriendsState.Event) {
        when (event) {
            ProfileFriendsState.Event.BackClicked -> nav.back()

            is ProfileFriendsState.Event.QueryChanged -> {
                query.value = event.query
                setState { copy(query = event.query) }
            }
            is ProfileFriendsState.Event.FriendClicked -> {
                // The id is archive data like everything else, so it is checked to be numeric.
                val url = UrlGuard.vkIdUrl("id", event.friend.id) ?: return
                openLink(url)
            }
        }
    }

    /** vk.com stays inside the app's WebView; anything else is the browser's business. */
    private fun openLink(url: String) {
        if (UrlGuard.isVkUrl(url)) nav.navigate(Routes.WebView(url))
        else setEffect { ProfileFriendsState.Effect.OpenLink(url) }
    }
}
