package com.etozhesandy.redpanda.features.profile.presentation.media

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.features.profile.domain.repository.ProfileInfoRepository
import com.etozhesandy.redpanda.features.profile.mapper.toSummaries
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ProfileMediaViewModel @Inject constructor(
    private val nav: INavigationManager,
    args: ProfileMediaArgs,
    repository: ProfileInfoRepository,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : BaseViewModel<ProfileMediaState.State, ProfileMediaState.Event, ProfileMediaState.Effect>() {

    override fun createInitialState() = ProfileMediaState.State()

    private val profileId = args.profileId

    init {
        // Counting and previewing happen in SQL: a dump can hold thousands of archive files, and
        // the list only ever shows one thumbnail and one count per folder.
        repository.observeArchiveFolders(profileId)
            .map { folders -> folders.toSummaries() }
            .flowOn(defaultDispatcher)
            .onEach { folders -> setState { copy(folders = folders, isLoading = false) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ProfileMediaState.Event) {
        when (event) {
            ProfileMediaState.Event.BackClicked -> nav.back()
            is ProfileMediaState.Event.FolderClicked ->
                nav.navigate(Routes.ProfileMediaFolder(profileId, event.folder))
        }
    }
}
