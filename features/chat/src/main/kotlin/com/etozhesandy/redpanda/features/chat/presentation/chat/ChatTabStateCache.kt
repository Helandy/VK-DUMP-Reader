package com.etozhesandy.redpanda.features.chat.presentation.chat

import com.etozhesandy.redpanda.features.chat.model.MediaScrollPosition
import javax.inject.Inject
import javax.inject.Singleton

/** One chat's cell in [ChatTabStateCache], handed to the pager and messages list that own it. */
interface ChatTabSlot {
    fun readTab(): Int
    fun writeTab(tab: Int)
    fun readMessagesPosition(): MediaScrollPosition
    fun writeMessagesPosition(position: MediaScrollPosition)
}

/**
 * Holds the selected tab and messages-list position while the lock gate rebuilds a chat screen.
 *
 * The navigation entry and its saveable state disappear with the gated composition, so this
 * singleton keeps the small Compose-owned states outside that lifecycle.
 */
@Singleton
class ChatTabStateCache @Inject constructor() {

    private val tabs = mutableMapOf<String, Int>()
    private val messagePositions = mutableMapOf<String, MediaScrollPosition>()

    fun slot(dialogId: String): ChatTabSlot = object : ChatTabSlot {
        override fun readTab(): Int = tabs[dialogId] ?: 0

        override fun writeTab(tab: Int) {
            tabs[dialogId] = tab
        }

        override fun readMessagesPosition(): MediaScrollPosition =
            messagePositions[dialogId] ?: MediaScrollPosition()

        override fun writeMessagesPosition(position: MediaScrollPosition) {
            messagePositions[dialogId] = position
        }
    }
}
