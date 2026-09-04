package com.etozhesandy.redpanda.features.profile.presentation.groups

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.common.net.UrlGuard
import com.etozhesandy.redpanda.features.profile.domain.model.matching
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import com.etozhesandy.redpanda.features.profile.model.ProfileGroupsArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ProfileGroupsViewModel @Inject constructor(
    private val nav: INavigationManager,
    args: ProfileGroupsArgs,
    repository: ProfileInfoRepository,
) : BaseViewModel<ProfileGroupsState.State, ProfileGroupsState.Event, ProfileGroupsState.Effect>() {

    override fun createInitialState() = ProfileGroupsState.State()

    private val profileId = args.profileId
    private val query = MutableStateFlow("")

    init {
        combine(repository.observeGroups(profileId), query) { groups, q -> groups.matching(q) }
            .onEach { groups -> setState { copy(groups = groups, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ProfileGroupsState.Event) {
        when (event) {
            ProfileGroupsState.Event.BackClicked -> nav.back()

            is ProfileGroupsState.Event.QueryChanged -> {
                query.value = event.query
                setState { copy(query = event.query) }
            }
            is ProfileGroupsState.Event.GroupClicked -> {
                // Both parts come from the archive, so both are validated — see [UrlGuard].
                val url = UrlGuard.vkProfileUrl(event.group.screenName)
                    ?: UrlGuard.vkIdUrl("club", event.group.id)
                    ?: return
                openLink(url)
            }
        }
    }

    /** vk.com stays inside the app's WebView; anything else is the browser's business. */
    private fun openLink(url: String) {
        if (UrlGuard.isVkUrl(url)) nav.navigate(Routes.WebView(url))
        else setEffect { ProfileGroupsState.Effect.OpenLink(url) }
    }
}
