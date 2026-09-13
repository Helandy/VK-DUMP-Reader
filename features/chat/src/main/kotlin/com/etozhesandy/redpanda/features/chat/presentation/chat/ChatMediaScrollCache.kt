package com.etozhesandy.redpanda.features.chat.presentation.chat

import com.etozhesandy.redpanda.core.common.mvi.SortMemory
import com.etozhesandy.redpanda.core.model.MediaSort
import com.etozhesandy.redpanda.features.chat.model.ChatMediaTab
import com.etozhesandy.redpanda.features.chat.model.MediaScrollPosition
import javax.inject.Inject
import javax.inject.Singleton

/** One tab's cell in [ChatMediaScrollCache], handed to the grid that owns it. */
interface MediaScrollSlot {
    fun read(): MediaScrollPosition
    fun write(position: MediaScrollPosition)
}

/**
 * Хранит позиции скрола и выбранную сортировку медиа-табов поверх пересоздания экрана чата.
 *
 * Переход к сообщению из просмотрщика фото и из поиска делает
 * `nav.navigate(Routes.Chat(...), PopUpTo(Routes.Chat::class, inclusive = true))`, то есть старая
 * запись бэкстека уничтожается вместе со своим `SaveableStateHolder` — и вместе со всеми
 * привязанными к ней ViewModel. Поэтому ни `rememberSaveable`, ни `SavedStateHandle` такой переход
 * пережить не могут, и позицию нужно держать снаружи навигации.
 *
 * Наружу для каждой пары диалог/таб отдаётся только её [MediaScrollSlot] и [SortMemory].
 */
@Singleton
class ChatMediaScrollCache @Inject constructor() {

    private val positions = mutableMapOf<String, MediaScrollPosition>()
    private val sorts = mutableMapOf<String, Pair<MediaSort, Boolean>>()

    fun slot(dialogId: String, tab: ChatMediaTab): MediaScrollSlot {
        val key = cacheKey(dialogId, tab)
        return object : MediaScrollSlot {
            override fun read(): MediaScrollPosition = positions[key] ?: MediaScrollPosition()

            override fun write(position: MediaScrollPosition) {
                positions[key] = position
            }
        }
    }

    fun sortMemory(dialogId: String, tab: ChatMediaTab): SortMemory<MediaSort> {
        val key = cacheKey(dialogId, tab)
        return object : SortMemory<MediaSort> {
            override fun read(): Pair<MediaSort, Boolean>? = sorts[key]

            override fun write(sort: MediaSort, ascending: Boolean) {
                sorts[key] = sort to ascending
            }
        }
    }

    private fun cacheKey(dialogId: String, tab: ChatMediaTab): String = "$dialogId:${tab.name}"
}
