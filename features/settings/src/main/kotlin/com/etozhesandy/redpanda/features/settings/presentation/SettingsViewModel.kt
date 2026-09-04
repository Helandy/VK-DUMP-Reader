package com.etozhesandy.redpanda.features.settings.presentation

import androidx.lifecycle.viewModelScope
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.PinSetupMode
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.model.naturalAscending
import com.etozhesandy.redpanda.core.model.nextAscending
import com.etozhesandy.redpanda.features.settings.domain.usecase.GetBiometricAvailabilityUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.GetProfilesCacheSizeUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.ObserveAppLockConfigUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.ObserveSettingsUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateBiometricEnabledUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateCoilCacheSizeUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateDefaultChatReversedUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateDefaultDialogSortUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateDefaultMediaSortUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateDefaultSearchSortUseCase
import com.etozhesandy.redpanda.features.settings.domain.usecase.UpdateLockTimeoutUseCase
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
    observeAppLockConfig: ObserveAppLockConfigUseCase,
    private val getBiometricAvailability: GetBiometricAvailabilityUseCase,
    private val updateBiometricEnabled: UpdateBiometricEnabledUseCase,
    private val updateLockTimeout: UpdateLockTimeoutUseCase,
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

        observeAppLockConfig()
            .onEach { config ->
                setState {
                    copy(
                        appLockEnabled = config.enabled,
                        biometricEnabled = config.biometricEnabled,
                        lockTimeoutSeconds = config.timeoutSeconds,
                    )
                }
            }
            .launchIn(viewModelScope)

        setState { copy(biometricAvailability = getBiometricAvailability()) }

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
            is SettingsState.Event.AppLockToggled -> nav.navigate(
                // Turning protection off still goes through the PIN screen: removing it must be
                // proven, not just toggled by whoever has the unlocked phone.
                Routes.PinSetup(if (event.value) PinSetupMode.CREATE else PinSetupMode.DISABLE),
            )
            SettingsState.Event.ChangePinClicked -> nav.navigate(Routes.PinSetup(PinSetupMode.CHANGE))
            is SettingsState.Event.BiometricToggled -> launchSafe { updateBiometricEnabled(event.value) }
            is SettingsState.Event.LockTimeoutChanged -> launchSafe { updateLockTimeout(event.seconds) }
            SettingsState.Event.BackClicked -> nav.back()
        }
    }
}
