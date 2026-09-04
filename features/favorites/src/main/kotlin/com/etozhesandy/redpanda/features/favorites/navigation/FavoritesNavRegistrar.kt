package com.etozhesandy.redpanda.features.favorites.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.favorites.presentation.FavoritesScreen
import com.etozhesandy.redpanda.features.favorites.presentation.FavoritesViewModel
import javax.inject.Inject

class FavoritesNavRegistrar @Inject constructor() : NavRegistrar {

    override fun register(builder: NavGraphBuilder) {
        builder.composable<Routes.Favorites> {
            val viewModel: FavoritesViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            FavoritesScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }
    }
}
