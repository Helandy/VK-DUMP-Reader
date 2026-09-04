package com.etozhesandy.redpanda.features.chat.model

import com.etozhesandy.redpanda.core.model.Attachment
import com.etozhesandy.redpanda.core.model.Message

/**
 * A message together with what was attached to it.
 *
 * The two are joined while the page is being built rather than by the row that draws it: a bubble
 * collecting its own attachments meant one database query starting per composition, cancelled and
 * restarted on every scroll.
 */
data class MessageUi(
    val message: Message,
    val attachments: List<Attachment> = emptyList(),
)
