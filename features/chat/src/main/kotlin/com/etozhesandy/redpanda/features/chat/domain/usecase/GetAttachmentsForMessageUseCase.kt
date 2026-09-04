package com.etozhesandy.redpanda.features.chat.domain.usecase

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.features.chat.domain.repository.ChatRepository
import javax.inject.Inject

/** Reads the photos/videos/files attached to one message once, for a page of messages. */
class GetAttachmentsForMessageUseCase @Inject constructor(
    private val repository: ChatRepository,
) {
    suspend operator fun invoke(messageId: String): List<Attachment> =
        repository.getAttachmentsForMessage(messageId)
}
