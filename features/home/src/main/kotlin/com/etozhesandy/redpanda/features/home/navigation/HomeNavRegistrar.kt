package com.etozhesandy.redpanda.features.home.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.home.presentation.HomeScreen
import com.etozhesandy.redpanda.features.home.presentation.HomeViewModel
import javax.inject.Inject

class HomeNavRegistrar @Inject constructor() : NavRegistrar {

    override fun register(builder: NavGraphBuilder) {
        builder.composable<Routes.Home> {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                effect = viewModel.effect,
                onEvent = viewModel::onEvent,
            )
        }
    }
}
