package com.etozhesandy.redpanda.features.chat.domain.usecase

import androidx.paging.PagingData
import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Pages through a dialog's messages in the requested chronological order. */
class ObserveMessagesUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    operator fun invoke(
        dialogId: String,
        isReversed: Boolean,
        initialPosition: Int?,
    ): Flow<PagingData<Message>> = repository.pagingMessages(dialogId, isReversed, initialPosition)
}
