package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import com.etozhesandy.redpanda.features.chat.utils.asPrefixQuery
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Full-text searches messages; optionally limited to one dialog. Each term in the query is a prefix. */
class SearchMessagesUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    operator fun invoke(
        profileId: String,
        rawQuery: String,
        dialogId: String? = null,
    ): Flow<List<Message>> {
        val ftsQuery = rawQuery.asPrefixQuery()
        if (ftsQuery.isBlank()) return flowOf(emptyList())
        return repository.searchMessages(profileId, ftsQuery, dialogId)
    }
}
