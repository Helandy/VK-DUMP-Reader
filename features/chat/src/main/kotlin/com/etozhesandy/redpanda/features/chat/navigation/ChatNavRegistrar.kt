package com.etozhesandy.redpanda.features.chat.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.etozhesandy.redpanda.core.navigation.NavRegistrar
import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.chat.presentation.chat.ChatScreen
import com.etozhesandy.redpanda.features.chat.presentation.globalsearch.GlobalSearchScreen
import com.etozhesandy.redpanda.features.chat.presentation.globalsearch.GlobalSearchViewModel
import com.etozhesandy.redpanda.features.chat.presentation.search.ChatSearchScreen
import com.etozhesandy.redpanda.features.chat.presentation.search.ChatSearchViewModel
import com.etozhesandy.redpanda.features.chat.presentation.photo.PhotoViewerScreen
import com.etozhesandy.redpanda.features.chat.presentation.photo.PhotoViewerViewModel
import javax.inject.Inject

class ChatNavRegistrar @Inject constructor() : NavRegistrar {

    override fun register(builder: NavGraphBuilder) {
        // The chat screen resolves its own ViewModels: it is five independent tabs behind one
        // route, not one screen with one state.
        builder.composable<Routes.Chat> { ChatScreen() }

        builder.composable<Routes.ChatSearch> {
            val viewModel: ChatSearchViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ChatSearchScreen(state = state, onEvent = viewModel::onEvent)
        }

        builder.composable<Routes.GlobalSearch> {
            val viewModel: GlobalSearchViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            GlobalSearchScreen(state = state, onEvent = viewModel::onEvent)
        }

        builder.composable<Routes.PhotoViewer> {
            val viewModel: PhotoViewerViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            PhotoViewerScreen(
                state = state,
                effect = viewModel.effect,
                onEvent = viewModel::onEvent,
            )
        }
    }
}
