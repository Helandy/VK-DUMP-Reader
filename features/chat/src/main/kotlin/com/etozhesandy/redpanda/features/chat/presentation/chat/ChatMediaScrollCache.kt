package com.etozhesandy.redpanda.features.chat.presentation.chat

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
 * Хранит позиции скрола медиа-табов поверх пересоздания экрана чата.
 *
 * Переход к сообщению из просмотрщика фото и из поиска делает
 * `nav.navigate(Routes.Chat(...), PopUpTo(Routes.Chat::class, inclusive = true))`, то есть старая
 * запись бэкстека уничтожается вместе со своим `SaveableStateHolder` — и вместе со всеми
 * привязанными к ней ViewModel. Поэтому ни `rememberSaveable`, ни `SavedStateHandle` такой переход
 * пережить не могут, и позицию нужно держать снаружи навигации.
 *
 * Наружу отдаётся [MediaScrollSlot], а не весь кэш: таб знает только свою ячейку и не может
 * прочитать чужую.
 */
@Singleton
class ChatMediaScrollCache @Inject constructor() {

    private val positions = mutableMapOf<String, MediaScrollPosition>()

    fun slot(dialogId: String, tab: ChatMediaTab): MediaScrollSlot {
        val key = "$dialogId:${tab.name}"
        return object : MediaScrollSlot {
            override fun read(): MediaScrollPosition = positions[key] ?: MediaScrollPosition()

            override fun write(position: MediaScrollPosition) {
                positions[key] = position
            }
        }
    }
}
