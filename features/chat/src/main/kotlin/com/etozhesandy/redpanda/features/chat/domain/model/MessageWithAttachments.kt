package com.etozhesandy.redpanda.features.chat.domain.model

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.Message

/**
 * One row of a dialog's history: a message and what was attached to it, in the order they were
 * sent. Pages are built with both already joined, so drawing a message never goes back to the
 * database.
 */
data class MessageWithAttachments(
    val message: Message,
    val attachments: List<Attachment>,
)
