package com.etozhesandy.redpanda.features.importer.presentation

import com.etozhesandy.redpanda.core.archive.worker.ProfileImportScheduler
import com.etozhesandy.redpanda.core.common.mvi.BaseViewModel
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.navigation.manager.PopUpTo
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val nav: INavigationManager,
    private val scheduler: ProfileImportScheduler,
) : BaseViewModel<ImportState.State, ImportState.Event, ImportState.Effect>() {

    override fun createInitialState() = ImportState.State

    override fun onEvent(event: ImportState.Event) {
        when (event) {
            is ImportState.Event.SourcePicked -> {
                val profileId = UUID.randomUUID().toString()
                scheduler.enqueue(profileId, event.source)
                // Home, not the import screen, is what "back" should reach once the import ran.
                nav.navigate(Routes.Dialogs(profileId), PopUpTo(Routes.Home::class))
            }
            ImportState.Event.BackClicked -> nav.back()
        }
    }
}
