package com.etozhesandy.redpanda.features.settings.presentation

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.model.naturalAscending
import com.etozhesandy.redpanda.core.model.nextAscending
import com.etozhesandy.redpanda.features.settings.domain.usecase.GetProfilesCacheSizeUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.ObserveSettingsUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateCoilCacheSizeUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateDefaultChatReversedUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateDefaultDialogSortUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateDefaultMediaSortUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateDefaultSearchSortUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateMediaImageWidthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val nav: INavigationManager,
    observeSettings: ObserveSettingsUseCase,
    private val updateCoilCacheSize: UpdateCoilCacheSizeUseCase,
    private val updateMediaImageWidth: UpdateMediaImageWidthUseCase,
    private val updateDefaultDialogSort: UpdateDefaultDialogSortUseCase,
    private val updateDefaultChatReversed: UpdateDefaultChatReversedUseCase,
    private val updateDefaultMediaSort: UpdateDefaultMediaSortUseCase,
    private val updateDefaultSearchSort: UpdateDefaultSearchSortUseCase,
    private val getProfilesCacheSize: GetProfilesCacheSizeUseCase,
) : BaseViewModel<SettingsState.State, SettingsState.Event, SettingsState.Effect>() {

    override fun createInitialState() = SettingsState.State()

    init {
        observeSettings()
            .onEach { settings ->
                setState {
                    copy(
                        coilCacheSizeMb = settings.coilCacheSizeMb,
                        mediaImageWidthDp = settings.mediaImageWidthDp,
                        defaultDialogSort = settings.defaultDialogSort,
                        defaultDialogSortAscending = settings.defaultDialogSortAscending,
                        defaultChatReversed = settings.defaultChatReversed,
                        defaultMediaSort = settings.defaultMediaSort,
                        defaultMediaSortAscending = settings.defaultMediaSortAscending,
                        defaultSearchSort = settings.defaultSearchSort,
                        defaultSearchSortAscending = settings.defaultSearchSortAscending,
                    )
                }
            }
            .launchIn(viewModelScope)

        launchSafe {
            val bytes = getProfilesCacheSize()
            setState { copy(profilesCacheBytes = bytes, isCacheSizeLoading = false) }
        }
    }

    override fun onEvent(event: SettingsState.Event) {
        when (event) {
            is SettingsState.Event.CoilCacheSizeChanged -> launchSafe { updateCoilCacheSize(event.valueMb) }
            is SettingsState.Event.MediaImageWidthChanged -> launchSafe { updateMediaImageWidth(event.widthDp) }
            is SettingsState.Event.DefaultDialogSortSelected -> launchSafe {
                updateDefaultDialogSort(
                    sort = event.sort,
                    ascending = nextAscending(
                        picked = event.sort,
                        current = currentState.defaultDialogSort,
                        currentAscending = currentState.defaultDialogSortAscending,
                        natural = event.sort.naturalAscending,
                    ),
                )
            }
            is SettingsState.Event.DefaultChatReversedChanged -> launchSafe { updateDefaultChatReversed(event.value) }
            is SettingsState.Event.DefaultMediaSortSelected -> launchSafe {
                updateDefaultMediaSort(
                    sort = event.sort,
                    ascending = nextAscending(
                        picked = event.sort,
                        current = currentState.defaultMediaSort,
                        currentAscending = currentState.defaultMediaSortAscending,
                        natural = event.sort.naturalAscending,
                    ),
                )
            }
            is SettingsState.Event.DefaultSearchSortSelected -> launchSafe {
                updateDefaultSearchSort(
                    sort = event.sort,
                    ascending = nextAscending(
                        picked = event.sort,
                        current = currentState.defaultSearchSort,
                        currentAscending = currentState.defaultSearchSortAscending,
                        natural = event.sort.naturalAscending,
                    ),
                )
            }
            SettingsState.Event.BackClicked -> nav.back()
        }
    }
}
