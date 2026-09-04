package com.etozhesandy.redpanda.features.settings.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.settings.presentation.SettingsScreen
import com.etozhesandy.redpanda.features.settings.presentation.SettingsViewModel
import javax.inject.Inject

class SettingsNavRegistrar @Inject constructor() : NavRegistrar {

    override fun register(builder: NavGraphBuilder) {
        builder.composable<Routes.Settings> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            SettingsScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }
    }
}
