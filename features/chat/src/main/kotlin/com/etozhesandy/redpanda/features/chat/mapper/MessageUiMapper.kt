package com.etozhesandy.redpanda.features.chat.mapper

import com.etozhesandy.redpanda.features.chat.domain.model.MessageWithAttachments
import com.etozhesandy.redpanda.features.chat.model.MessageUi

fun MessageWithAttachments.toUi(): MessageUi = MessageUi(
    message = message,
    attachments = attachments,
)
