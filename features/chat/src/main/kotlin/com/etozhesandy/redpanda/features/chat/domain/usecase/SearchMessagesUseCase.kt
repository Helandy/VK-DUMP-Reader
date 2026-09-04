package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
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

    /**
     * Turns raw user input into an FTS4 prefix query: `архив стар` becomes `архив* стар*`, which
     * matches messages containing a word starting with each term.
     *
     * The prefix operator `*` only applies to a bare token in FTS3/FTS4 — quoting the term as
     * `"архив"*` makes it an exact-word match instead, so the term cannot be protected from FTS
     * syntax (`NEAR(`, `^`, `-`, `"` and the like fail the whole query with a syntax error) by
     * quoting it. Instead each run of letters and digits is taken as one token, mirroring how the
     * `unicode61` tokenizer of the `messages_fts` table splits the indexed text; everything else
     * is dropped, so no character of the input can reach the FTS parser as syntax. Input that holds no token at all yields an empty query.
     */
    private fun String.asPrefixQuery(): String =
        split(TOKEN_SEPARATORS).filter { it.isNotEmpty() }.joinToString(" ") { "$it*" }

    private companion object {
        /** Anything that is not a letter or a digit is a token boundary, as it is for `unicode61`. */
        val TOKEN_SEPARATORS = Regex("[^\\p{L}\\p{N}]+")
    }
}
