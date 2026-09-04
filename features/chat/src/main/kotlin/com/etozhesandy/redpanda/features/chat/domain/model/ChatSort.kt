package com.etozhesandy.redpanda.features.chat.domain.model

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.model.MessageSort
import com.etozhesandy.redpanda.core.model.Attachment.Companion.sortedBy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

/**
 * Note this reorders the results the search already produced, which the DAO caps at its own limit
 * of the most recent matches — so ascending shows the oldest of *those*, not the oldest overall.
 */
fun List<Message>.sortedBy(sort: MessageSort, ascending: Boolean): List<Message> {
    val comparator: Comparator<Message> = when (sort) {
        MessageSort.DATE -> compareBy { it.timestampEpoch }
        // Same sender repeats a lot, so date decides within a name to keep runs readable.
        MessageSort.SENDER -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.senderName }
    }
    val withTiebreak = if (sort == MessageSort.SENDER) {
        comparator.thenBy { it.timestampEpoch }
    } else {
        comparator
    }
    return sortedWith(if (ascending) withTiebreak else withTiebreak.reversed())
}
/**
 * Applies a media ordering that can change while the list is on screen: the sort comes from the
 * settings until the user picks one on the screen itself, so it is a flow, not a fixed pair.
 *
 * [dispatcher] keeps the sort off the collector's thread — a dump's dialog can hold thousands of
 * attachments, and every caller here collects in `viewModelScope`, i.e. on the main thread.
 */
fun Flow<List<Attachment>>.sortedBy(
    order: Flow<Pair<MediaSort, Boolean>>,
    dispatcher: CoroutineDispatcher,
): Flow<List<Attachment>> =
    combine(order) { attachments, (sort, ascending) -> attachments.sortedBy(sort, ascending) }
        .flowOn(dispatcher)
