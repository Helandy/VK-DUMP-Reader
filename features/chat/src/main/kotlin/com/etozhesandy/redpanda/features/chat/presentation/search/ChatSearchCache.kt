package com.etozhesandy.redpanda.features.chat.presentation.search

import com.etozhesandy.redpanda.core.common.mvi.SortMemory
import com.etozhesandy.redpanda.core.model.MessageSort
import javax.inject.Inject
import javax.inject.Singleton

/** Holds a dialog search's explicit query and ordering until the app process is restarted. */
@Singleton
class ChatSearchCache @Inject constructor() {

    private val queries = mutableMapOf<String, String>()
    private val sorts = mutableMapOf<String, Pair<MessageSort, Boolean>>()

    fun query(dialogId: String): String? = queries[dialogId]

    fun setQuery(dialogId: String, query: String) {
        queries[dialogId] = query
    }

    fun sortMemory(dialogId: String): SortMemory<MessageSort> = object : SortMemory<MessageSort> {
        override fun read(): Pair<MessageSort, Boolean>? = sorts[dialogId]

        override fun write(sort: MessageSort, ascending: Boolean) {
            sorts[dialogId] = sort to ascending
        }
    }
}
