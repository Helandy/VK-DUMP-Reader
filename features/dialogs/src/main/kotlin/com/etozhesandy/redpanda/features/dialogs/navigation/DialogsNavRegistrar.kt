package com.etozhesandy.redpanda.features.dialogs.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.dialogs.presentation.DialogsScreen
import com.etozhesandy.redpanda.features.dialogs.presentation.DialogsViewModel
import javax.inject.Inject

class DialogsNavRegistrar @Inject constructor() : NavRegistrar {

    override fun register(builder: NavGraphBuilder) {
        builder.composable<Routes.Dialogs> {
            val viewModel: DialogsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            DialogsScreen(
                state = state,
                effect = viewModel.effect,
                onEvent = viewModel::onEvent,
            )
        }
    }
}
