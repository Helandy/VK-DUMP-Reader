package com.etozhesandy.redpanda.features.chat.domain.usecase

import androidx.paging.PagingData
import com.etozhesandy.redpanda.features.chat.domain.model.MessageWithAttachments
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Pages through a dialog's messages, with their attachments, in the requested chronological order. */
class ObserveMessagesUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    operator fun invoke(
        dialogId: String,
        isReversed: Boolean,
        initialPosition: Int?,
    ): Flow<PagingData<MessageWithAttachments>> = repository.pagingMessages(dialogId, isReversed, initialPosition)
}
