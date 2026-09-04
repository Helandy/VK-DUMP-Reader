package com.etozhesandy.redpanda.features.importer.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.importer.presentation.ImportScreen
import com.etozhesandy.redpanda.features.importer.presentation.ImportViewModel
import javax.inject.Inject

class ImportNavRegistrar @Inject constructor() : NavRegistrar {

    override fun register(builder: NavGraphBuilder) {
        builder.composable<Routes.Import> {
            val viewModel: ImportViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ImportScreen(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }
    }
}
