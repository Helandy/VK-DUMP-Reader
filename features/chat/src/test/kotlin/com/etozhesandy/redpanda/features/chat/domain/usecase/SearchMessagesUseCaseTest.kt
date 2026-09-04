package com.etozhesandy.redpanda.features.chat.domain.usecase

import androidx.paging.PagingData
import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.core.model.Profile
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import java.sql.DriverManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchMessagesUseCaseTest {

    private val repository = RecordingRepository()
    private val useCase = SearchMessagesUseCase(repository)

    @Test
    fun `each term becomes a bare prefix token`() {
        useCase("p", "привет мир")
        assertEquals("привет* мир*", repository.lastQuery)
    }

    /** Left in, these are FTS syntax and fail the whole query rather than matching nothing. */
    @Test
    fun `fts syntax is stripped out of the terms`() {
        useCase("p", "NEAR( ^a -b \"c\" d:e")
        assertEquals("NEAR* a* b* c* d* e*", repository.lastQuery)
    }

    @Test
    fun `a query with no letters or digits never reaches the repository`() {
        useCase("p", " *^\" ")
        assertNull(repository.lastQuery)
    }

    @Test
    fun `a blank query never reaches the repository`() {
        useCase("p", "   ")
        assertNull(repository.lastQuery)
    }

    /**
     * The regression these cover: as a quoted phrase (`"архив"*`) the `*` is not a prefix operator
     * in FTS3/FTS4, so «архив» stopped finding «архива». The query the use case builds is run
     * against a real SQLite FTS4 table here, so prefix search is checked end to end rather than as
     * a string shape.
     */
    @Test
    fun `a term matches longer words starting with it`() {
        assertEquals(listOf("сообщение из архива", "Архив Старый"), matching("архив"))
    }

    @Test
    fun `all terms must match, in any order`() {
        assertEquals(listOf("сообщение из архива"), matching("архива сообщение"))
        assertEquals(emptyList<String>(), matching("архив отсутствующее"))
    }

    /** The `unicode61` tokenizer case-folds beyond ASCII; the default `simple` one would not. */
    @Test
    fun `search is case insensitive for cyrillic`() {
        assertEquals(listOf("Архив Старый"), matching("АРХИВ СТАР"))
    }

    /** Syntax-looking input must find nothing rather than fail the query with a syntax error. */
    @Test
    fun `fts syntax in the query matches nothing instead of throwing`() {
        assertEquals(emptyList<String>(), matching("NEAR( ^отсутствующее"))
    }

    /** Runs the query the use case builds for [rawQuery] against an in-memory FTS4 table. */
    private fun matching(rawQuery: String): List<String> {
        useCase("p", rawQuery)
        val ftsQuery = requireNotNull(repository.lastQuery)
        DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("CREATE VIRTUAL TABLE messages_fts USING fts4(text, tokenize=unicode61)")
            }
            connection.prepareStatement("INSERT INTO messages_fts(text) VALUES (?)").use { insert ->
                CORPUS.forEach { text ->
                    insert.setString(1, text)
                    insert.executeUpdate()
                }
            }
            connection.prepareStatement("SELECT text FROM messages_fts WHERE messages_fts MATCH ?").use { select ->
                select.setString(1, ftsQuery)
                select.executeQuery().use { rows ->
                    return buildList { while (rows.next()) add(rows.getString(1)) }
                }
            }
        }
    }

    /** Records the query the use case hands to FTS; nothing else here is exercised. */
    private class RecordingRepository : ChatRepository {
        var lastQuery: String? = null

        override fun searchMessages(profileId: String, ftsQuery: String, dialogId: String?): Flow<List<Message>> {
            lastQuery = ftsQuery
            return flowOf(emptyList())
        }

        override fun observeDialog(dialogId: String): Flow<ChatDialog?> = unused()
        override fun observeProfile(profileId: String): Flow<Profile?> = unused()
        override fun pagingMessages(
            dialogId: String,
            isReversed: Boolean,
            initialPosition: Int?,
        ): Flow<PagingData<Message>> = unused()
        override suspend fun getAttachmentsForMessage(messageId: String): List<Attachment> = unused()
        override fun observeMediaForDialog(dialogId: String): Flow<List<Attachment>> = unused()
        override fun observePhotosForDialog(dialogId: String): Flow<List<Attachment>> = unused()
        override fun observeVideosForDialog(dialogId: String): Flow<List<Attachment>> = unused()
        override fun observeAudioForDialog(dialogId: String): Flow<List<Attachment>> = unused()
        override fun observeFilesForDialog(dialogId: String): Flow<List<Attachment>> = unused()
        override suspend fun setFavorite(messageId: String, isFavorite: Boolean) = unused()
        override suspend fun getMessagePosition(dialogId: String, messageId: String, isReversed: Boolean): Int =
            unused()

        private fun unused(): Nothing = error("Not used by SearchMessagesUseCase")
    }

    private companion object {
        val CORPUS = listOf("сообщение из архива", "Архив Старый", "unrelated text")
    }
}
