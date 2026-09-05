package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.DialogMessage
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import com.etozhesandy.redpanda.features.chat.utils.asPrefixQuery
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Full-text searches the messages of every dialog of one profile, each result carrying the name of
 * the dialog it belongs to. Each term in the query is a prefix, exactly as in the single-dialog
 * search.
 */
class SearchAllDialogsUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    operator fun invoke(profileId: String, rawQuery: String): Flow<List<DialogMessage>> {
        val ftsQuery = rawQuery.asPrefixQuery()
        if (ftsQuery.isBlank()) return flowOf(emptyList())
        return repository.searchAllDialogs(profileId, ftsQuery)
    }
}
