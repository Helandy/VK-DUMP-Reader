package com.etozhesandy.redpanda.features.lock.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.lock.presentation.PinSetupScreen
import com.etozhesandy.redpanda.features.lock.presentation.PinSetupViewModel
import javax.inject.Inject

class LockNavRegistrar @Inject constructor() : NavRegistrar {

    override fun register(builder: NavGraphBuilder) {
        builder.composable<Routes.PinSetup> {
            val viewModel: PinSetupViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            PinSetupScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }
    }
}
