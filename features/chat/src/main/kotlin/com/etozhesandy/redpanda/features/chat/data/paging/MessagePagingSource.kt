package com.etozhesandy.redpanda.features.chat.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.etozhesandy.redpanda.core.storage.db.message.MessageDao
import com.etozhesandy.redpanda.core.storage.db.message.MessageEntity

/**
 * Pages one dialog by `LIMIT`/`OFFSET`, deliberately outside Room's invalidation tracker.
 *
 * An imported archive doesn't change after the import that wrote it, and nothing the reader does
 * in a chat writes to `messages` — a star goes to `favorite_messages` and is observed through
 * `ChatRepository.observeFavoriteIds`. So there is nothing here to react to, and the pages stay
 * exactly where the reader left them.
 *
 * Room's generated source is not a safe fallback for that same reason: it refreshes on every write
 * to the table, and with `enablePlaceholders = false` its refresh lands on the wrong rows. Room
 * takes the refresh key from `PagingState.anchorPosition`, which is a database offset only while
 * placeholders are on; with them off the anchor counts loaded items, so each refresh walked the
 * list back towards the start of the dialog.
 */
class MessagePagingSource(
    private val messageDao: MessageDao,
    private val dialogId: String,
    private val isReversed: Boolean,
) : PagingSource<Int, MessageEntity>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MessageEntity> {
        // Read per load rather than once: a page can be requested while an import is still
        // writing, and a stale count would cut the dialog short at the old last message.
        val itemCount = messageDao.countMessages(dialogId)
        val key = params.key ?: 0
        // Keys are offsets of the first row of a page, so a prepend counts backwards from its key
        // and shortens itself when fewer rows than a full page are left in front of it.
        val limit = when (params) {
            is LoadParams.Prepend -> minOf(key, params.loadSize)
            else -> params.loadSize
        }
        val offset = when (params) {
            is LoadParams.Prepend -> maxOf(0, key - params.loadSize)
            is LoadParams.Append -> key
            // A refresh key can outrun the table if rows were removed since it was taken; clamping
            // lands on the last full page instead of on nothing at all.
            is LoadParams.Refresh -> key.coerceAtMost(maxOf(0, itemCount - params.loadSize))
        }

        val page = if (limit == 0) emptyList() else loadPage(limit, offset)
        val loadedThrough = offset + page.size
        return LoadResult.Page(
            data = page,
            prevKey = if (offset == 0 || page.isEmpty()) null else offset,
            nextKey = if (page.isEmpty() || loadedThrough >= itemCount) null else loadedThrough,
            itemsBefore = offset,
            itemsAfter = maxOf(0, itemCount - loadedThrough),
        )
    }

    /**
     * The offset of the anchored row, recovered from the page holding it: with placeholders off
     * [PagingState.anchorPosition] counts loaded items and says nothing about where in the dialog
     * those items came from, but every page here records the offset it was loaded with.
     */
    override fun getRefreshKey(state: PagingState<Int, MessageEntity>): Int? {
        val loadedCount = state.pages.sumOf { it.data.size }
        if (loadedCount == 0) return null
        val anchor = (state.anchorPosition ?: return null).coerceIn(0, loadedCount - 1)

        var loadedBefore = 0
        for (page in state.pages) {
            if (anchor < loadedBefore + page.data.size) {
                val anchorOffset = page.itemsBefore + (anchor - loadedBefore)
                // Half a page in front of the anchor, half behind, so the reader ends up in the
                // middle of what gets loaded rather than at its edge.
                return maxOf(0, anchorOffset - state.config.initialLoadSize / 2)
            }
            loadedBefore += page.data.size
        }
        return null
    }

    private suspend fun loadPage(limit: Int, offset: Int): List<MessageEntity> =
        if (isReversed) messageDao.getMessagesPageDescending(dialogId, limit, offset)
        else messageDao.getMessagesPageAscending(dialogId, limit, offset)
}
