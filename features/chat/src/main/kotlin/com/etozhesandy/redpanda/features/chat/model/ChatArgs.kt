package com.etozhesandy.redpanda.features.chat.model

/**
 * What the chat screen was opened with, free of the navigation types that carried it.
 *
 * [orderReversed] is null when the caller had no opinion, and the default from the settings
 * applies — the route spells the same thing as two magic strings, which stop at the mapper.
 */
data class ChatArgs(
    val dialogId: String,
    val profileId: String,
    val scrollToMessageId: String? = null,
    val orderReversed: Boolean? = null,
)
