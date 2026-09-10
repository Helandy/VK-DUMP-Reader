package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Streams the ids of the messages a dialog has marked as favorite.
 *
 * Separate from the paged messages on purpose: a page is a snapshot of the rows as they were when
 * it was read, so the only way a bubble can show a star that is still true is to watch the
 * favorites themselves.
 */
class ObserveFavoriteMessageIdsUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    operator fun invoke(dialogId: String): Flow<Set<String>> = repository.observeFavoriteIds(dialogId)
}
