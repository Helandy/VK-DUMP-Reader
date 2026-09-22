package com.etozhesandy.redpanda.features.chat.presentation.globalsearch

import androidx.lifecycle.SavedStateHandle
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.DialogMessage
import com.etozhesandy.redpanda.core.model.DialogSort
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.model.MessageSort
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.core.navigation.manager.INavigationManager
import com.etozhesandy.redpanda.core.settings.AppSettings
import com.etozhesandy.redpanda.core.settings.SettingsRepository
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import com.etozhesandy.redpanda.features.chat.domain.usecase.SearchAllDialogsUseCase
import com.etozhesandy.redpanda.features.chat.model.GlobalSearchArgs
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GlobalSearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: RecordingChatRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = RecordingChatRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `query shorter than three characters does not search`() = runTest(dispatcher.scheduler) {
        val viewModel = createViewModel()

        viewModel.onEvent(GlobalSearchState.Event.QueryChanged("ab"))
        advanceUntilIdle()

        assertTrue(repository.calls.isEmpty())
        assertFalse(viewModel.state.value.isSearching)
        assertTrue(viewModel.state.value.results.isEmpty())
    }

    @Test
    fun `search waits for debounce and loads first page of fifty`() = runTest(dispatcher.scheduler) {
        repository.pages[0] = page(50, 0)
        val viewModel = createViewModel()

        viewModel.onEvent(GlobalSearchState.Event.QueryChanged("arch"))
        advanceTimeBy(249)
        assertTrue(repository.calls.isEmpty())
        assertTrue(viewModel.state.value.isSearching)

        advanceTimeBy(1)
        advanceUntilIdle()

        assertEquals(listOf(Call("arch*", 50, 0)), repository.calls)
        assertEquals(50, viewModel.state.value.results.size)
        assertTrue(viewModel.state.value.hasMoreResults)
        assertFalse(viewModel.state.value.isSearching)
    }

    @Test
    fun `load more appends the next page with offset`() = runTest(dispatcher.scheduler) {
        repository.pages[0] = page(50, 0)
        repository.pages[50] = page(2, 50)
        val viewModel = createViewModel()
        viewModel.onEvent(GlobalSearchState.Event.QueryChanged("arch"))
        advanceUntilIdle()

        viewModel.onEvent(GlobalSearchState.Event.LoadMore)
        advanceUntilIdle()

        assertEquals(listOf(Call("arch*", 50, 0), Call("arch*", 50, 50)), repository.calls)
        assertEquals(52, viewModel.state.value.results.size)
        assertFalse(viewModel.state.value.hasMoreResults)
        assertFalse(viewModel.state.value.isLoadingMore)
    }

    @Test
    fun `changing query cancels the pending older search`() = runTest(dispatcher.scheduler) {
        repository.pages[0] = page(1, 0)
        val viewModel = createViewModel()

        viewModel.onEvent(GlobalSearchState.Event.QueryChanged("old"))
        advanceTimeBy(100)
        viewModel.onEvent(GlobalSearchState.Event.QueryChanged("new"))
        advanceUntilIdle()

        assertEquals(listOf(Call("new*", 50, 0)), repository.calls)
    }

    private fun createViewModel(): GlobalSearchViewModel = GlobalSearchViewModel(
        savedStateHandle = SavedStateHandle(),
        nav = NoOpNavigation,
        args = GlobalSearchArgs(profileId = "profile"),
        searchAllDialogs = SearchAllDialogsUseCase(repository),
        settingsRepository = FakeSettingsRepository,
    )

    private fun page(size: Int, offset: Int): List<DialogMessage> = (offset until offset + size).map { index ->
        DialogMessage(
            message = Message(
                id = "message-$index",
                dialogId = "dialog-$index",
                profileId = "profile",
                senderId = "sender",
                senderName = "Sender",
                timestampEpoch = (size - index).toLong(),
                text = "message $index",
                isOutgoing = false,
            ),
            dialogName = "Dialog $index",
        )
    }

    private data class Call(val query: String, val limit: Int, val offset: Int)

    private class RecordingChatRepository : ChatRepository {
        val calls = CopyOnWriteArrayList<Call>()
        val pages = mutableMapOf<Int, List<DialogMessage>>()

        override suspend fun searchAllDialogs(
            profileId: String,
            ftsQuery: String,
            limit: Int,
            offset: Int,
        ): List<DialogMessage> {
            calls += Call(ftsQuery, limit, offset)
            return pages[offset].orEmpty()
        }

        override fun searchMessages(profileId: String, ftsQuery: String, dialogId: String?): Flow<List<Message>> = unused()
        override suspend fun searchMessagesPage(
            profileId: String,
            ftsQuery: String,
            dialogId: String,
            limit: Int,
            offset: Int,
        ): List<Message> = unused()
        override fun observeDialog(dialogId: String): Flow<ChatDialog?> = unused()
        override fun observeProfile(profileId: String): Flow<Profile?> = unused()
        override fun pagingMessages(dialogId: String, isReversed: Boolean, initialPosition: Int?): kotlinx.coroutines.flow.Flow<androidx.paging.PagingData<Message>> = unused()
        override suspend fun getAttachmentsForMessage(messageId: String): List<Attachment> = unused()
        override fun observeMediaForDialog(dialogId: String): Flow<List<Attachment>> = unused()
        override fun observePhotosForDialog(dialogId: String): Flow<List<Attachment>> = unused()
        override fun observeVideosForDialog(dialogId: String): Flow<List<Attachment>> = unused()
        override fun observeAudioForDialog(dialogId: String): Flow<List<Attachment>> = unused()
        override fun observeFilesForDialog(dialogId: String): Flow<List<Attachment>> = unused()
        override suspend fun setFavorite(messageId: String, isFavorite: Boolean) = Unit
        override fun observeFavoriteIds(dialogId: String): Flow<Set<String>> = unused()
        override suspend fun getMessagePosition(dialogId: String, messageId: String, isReversed: Boolean): Int = unused()

        private fun <T> unused(): T = error("Not used by this test")
    }

    private object NoOpNavigation : INavigationManager {
        override fun navigate(dest: Any, popUpTo: com.etozhesandy.redpanda.core.navigation.manager.PopUpTo?) = Unit
        override fun back() = Unit
    }

    private object FakeSettingsRepository : SettingsRepository {
        override val settings: Flow<AppSettings> = flowOf(AppSettings())
        override suspend fun setCoilCacheSizeMb(value: Int) = Unit
        override suspend fun setMediaImageWidthDp(value: Int) = Unit
        override suspend fun setDefaultDialogSort(sort: DialogSort, ascending: Boolean) = Unit
        override suspend fun setDefaultChatReversed(value: Boolean) = Unit
        override suspend fun setDefaultMediaSort(sort: MediaSort, ascending: Boolean) = Unit
        override suspend fun setDefaultSearchSort(sort: MessageSort, ascending: Boolean) = Unit
    }
}
