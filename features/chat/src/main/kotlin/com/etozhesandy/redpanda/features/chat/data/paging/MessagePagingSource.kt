package com.etozhesandy.redpanda.features.chat.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.etozhesandy.redpanda.core.storage.db.attachment.AttachmentDao
import com.etozhesandy.redpanda.core.storage.db.attachment.toDomain
import com.etozhesandy.redpanda.core.storage.db.message.MessageDao
import com.etozhesandy.redpanda.core.storage.db.message.MessageEntity
import com.etozhesandy.redpanda.core.storage.db.message.toDomain
import com.etozhesandy.redpanda.features.chat.domain.model.MessageWithAttachments

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
 *
 * Each page arrives joined with its attachments, fetched in one query for the whole page — one
 * query per message made a page of photos cost fifty round trips to the database.
 */
class MessagePagingSource(
    private val messageDao: MessageDao,
    private val attachmentDao: AttachmentDao,
    private val dialogId: String,
    private val isReversed: Boolean,
) : PagingSource<Int, MessageWithAttachments>() {

    /**
     * Counted on refresh only: `COUNT(*)` walks the dialog's index, and a long dialog paid for it
     * on every page. The end of the dialog is found by a short page instead, so a count gone stale
     * while an import is still writing no longer cuts the dialog short at its old last message.
     */
    private var itemCount: Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MessageWithAttachments> {
        val knownCount = itemCount.takeIf { params !is LoadParams.Refresh }
            ?: messageDao.countMessages(dialogId).also { itemCount = it }
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
            is LoadParams.Refresh -> key.coerceAtMost(maxOf(0, knownCount - params.loadSize))
        }

        val page = if (limit == 0) emptyList() else loadPage(limit, offset)
        val loadedThrough = offset + page.size
        val count = maxOf(knownCount, loadedThrough).also { itemCount = it }
        // A prepend is shortened on purpose near the start; anywhere else a short page means the
        // dialog ended.
        val isLastPage = params !is LoadParams.Prepend && page.size < limit
        return LoadResult.Page(
            data = page.withAttachments(),
            prevKey = if (offset == 0 || page.isEmpty()) null else offset,
            nextKey = if (page.isEmpty() || isLastPage) null else loadedThrough,
            itemsBefore = offset,
            itemsAfter = count - loadedThrough,
        )
    }

    /**
     * The offset of the anchored row, recovered from the page holding it: with placeholders off
     * [PagingState.anchorPosition] counts loaded items and says nothing about where in the dialog
     * those items came from, but every page here records the offset it was loaded with.
     */
    override fun getRefreshKey(state: PagingState<Int, MessageWithAttachments>): Int? {
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

    /**
     * Messages without attachments are the common case and are left out of the query outright —
     * `hasAttachments` is stored on the message for exactly this.
     */
    private suspend fun List<MessageEntity>.withAttachments(): List<MessageWithAttachments> {
        val withAttachments = filter { it.hasAttachments }.map { it.messageId }
        val attachments = if (withAttachments.isEmpty()) {
            emptyMap()
        } else {
            attachmentDao.getAttachmentsForMessages(withAttachments).groupBy({ it.messageId }, { it.toDomain() })
        }
        return map { entity ->
            MessageWithAttachments(
                message = entity.toDomain(),
                attachments = attachments[entity.messageId].orEmpty(),
            )
        }
    }
}
