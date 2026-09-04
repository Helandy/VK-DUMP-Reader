package com.etozhesandy.redpanda.features.chat.presentation.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.etozhesandy.redpanda.core.designsystem.components.BaseScreen
import com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.audio.AudioTabScreen
import com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.audio.AudioTabViewModel
import com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.files.FilesTabScreen
import com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.files.FilesTabViewModel
import com.etozhesandy.redpanda.features.chat.R
import com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.media.MediaTabScreen
import com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.media.PhotosTabViewModel
import com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.media.VideosTabViewModel
import com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.messages.MessagesTabScreen
import com.etozhesandy.redpanda.features.chat.presentation.chat.tabs.messages.MessagesTabViewModel
import com.etozhesandy.redpanda.features.chat.presentation.chat.view.ChatTopBar
import kotlinx.coroutines.launch

private val TAB_TITLES = listOf(
    R.string.chat_tab_messages,
    R.string.chat_tab_photos,
    R.string.chat_tab_videos,
    R.string.chat_tab_audio,
    R.string.chat_tab_files,
)

/**
 * The frame around the chat's five tabs: a bar, a row of tabs, and a pager.
 *
 * It holds no state of its own — every tab is a screen with its own ViewModel, reached through
 * [hiltViewModel] inside its own page. The pager only composes the page it is showing, so a tab
 * the user never opens never subscribes to anything.
 */
@Composable
fun ChatScreen(modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { TAB_TITLES.size })
    val coroutineScope = rememberCoroutineScope()

    // Hoisted out of the pager: the media grids can restore themselves from the scroll cache,
    // but the messages list has no such backstop, so its position is kept where the pager can't
    // dispose it.
    val messagesListState = rememberLazyListState()

    val topBarViewModel: ChatTopBarViewModel = hiltViewModel()
    val topBarState by topBarViewModel.state.collectAsStateWithLifecycle()

    BaseScreen(
        topBar = { ChatTopBar(state = topBarState, onEvent = topBarViewModel::onEvent) },
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                edgePadding = 0.dp,
            ) {
                TAB_TITLES.forEachIndexed { index, titleRes ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(stringResource(titleRes)) },
                    )
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                when (page) {
                    0 -> MessagesPage(listState = messagesListState)
                    1 -> PhotosPage()
                    2 -> VideosPage()
                    3 -> AudioPage()
                    4 -> FilesPage()
                }
            }
        }
    }
}

@Composable
private fun MessagesPage(listState: LazyListState) {
    val viewModel: MessagesTabViewModel = hiltViewModel()
    MessagesTabScreen(
        pagingItems = viewModel.pagingMessages.collectAsLazyPagingItems(),
        listState = listState,
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun PhotosPage() {
    val viewModel: PhotosTabViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    MediaTabScreen(
        state = state,
        scrollSlot = viewModel.scrollSlot,
        emptyText = stringResource(R.string.chat_empty_photos),
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun VideosPage() {
    val viewModel: VideosTabViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    MediaTabScreen(
        state = state,
        scrollSlot = viewModel.scrollSlot,
        emptyText = stringResource(R.string.chat_empty_videos),
        onEvent = viewModel::onEvent,
    )
}

@Composable
private fun AudioPage() {
    val viewModel: AudioTabViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    AudioTabScreen(state = state, onEvent = viewModel::onEvent)
}

@Composable
private fun FilesPage() {
    val viewModel: FilesTabViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    FilesTabScreen(state = state, effect = viewModel.effect, onEvent = viewModel::onEvent)
}
