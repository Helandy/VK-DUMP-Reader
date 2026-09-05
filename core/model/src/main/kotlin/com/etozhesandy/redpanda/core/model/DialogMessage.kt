package com.etozhesandy.redpanda.core.model

/**
 * A message together with the name of the dialog it belongs to.
 *
 * Search across all dialogs shows results out of their context, so the dialog has to travel with
 * the message — inside one dialog the name is already on screen and plain [Message] is enough.
 */
data class DialogMessage(
    val message: Message,
    val dialogName: String,
)
