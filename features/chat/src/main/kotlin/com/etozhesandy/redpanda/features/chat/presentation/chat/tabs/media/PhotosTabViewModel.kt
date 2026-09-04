package com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.media

import androidx.lifecycle.SavedStateHandle
import com.etozhesandy.redpanda.core.common.dispatcher.DefaultDispatcher
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import com.etozhesandy.redpanda.features.chat.domain.usecase.ObserveDialogPhotosUseCase
import com.etozhesandy.redpanda.features.chat.model.ChatArgs
import com.etozhesandy.redpanda.features.chat.model.ChatMediaTab
import com.etozhesandy.redpanda.features.chat.presentation.chat.ChatMediaScrollCache
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher

@HiltViewModel
class PhotosTabViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    settingsRepository: SettingsRepository,
    nav: INavigationManager,
    args: ChatArgs,
    scrollCache: ChatMediaScrollCache,
    observeDialogPhotos: ObserveDialogPhotosUseCase,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : MediaTabViewModel(
    savedStateHandle = savedStateHandle,
    settingsRepository = settingsRepository,
    nav = nav,
    args = args,
    scrollCache = scrollCache,
    tab = ChatMediaTab.PHOTOS,
    attachments = observeDialogPhotos(args.dialogId),
    defaultDispatcher = defaultDispatcher,
)
