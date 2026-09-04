package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject

/** Returns the 0-based index of a message in the dialog's current chronological order. */
class GetMessagePositionUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(dialogId: String, messageId: String, isReversed: Boolean): Int =
        (repository.getMessagePosition(dialogId, messageId, isReversed) - 1).coerceAtLeast(0)
}
