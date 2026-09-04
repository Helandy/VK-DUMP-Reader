package com.etozhesandy.redpanda.features.chat.mapper

import com.etozhesandy.redpanda.core.model.Message
import com.etozhesandy.redpanda.features.chat.domain.usecase.GetAttachmentsForMessageUseCase
import com.etozhesandy.redpanda.features.chat.model.MessageUi

/**
 * Joins a message with its attachments while the page is built.
 *
 * Messages without attachments are the common case and skip the query outright — `hasAttachments`
 * is stored on the message for exactly this.
 */
suspend fun Message.toUi(getAttachments: GetAttachmentsForMessageUseCase): MessageUi = MessageUi(
    message = this,
    attachments = if (hasAttachments) getAttachments(id) else emptyList(),
)
