package com.etozhesandy.redpanda.core.storage.db.message

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * Room only supports FTS3/FTS4, so the query built by the search use case has to obey the FTS4
 * syntax: a prefix query works on a bare token (`архив*`) and not on a quoted phrase.
 *
 * [FtsOptions.TOKENIZER_UNICODE61] replaces the default `simple` tokenizer, which case-folds ASCII
 * only — with it «Архив» would not match a search for «архив». unicode61 also defines token
 * boundaries by Unicode character class, which is what the use case mirrors when it strips FTS
 * syntax out of a search term.
 */
@Fts4(contentEntity = MessageEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "messages_fts")
data class MessageFtsEntity(
    val text: String,
)
