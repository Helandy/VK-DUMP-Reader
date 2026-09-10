package com.etozhesandy.redpanda.core.model

/**
 * A single message parsed from an imported archive.
 *
 * Carries no favorite flag: messages are read in pages that are never re-read, so a flag copied
 * into one would go stale the moment the reader starred it. A star is a row of its own, in
 * `favorite_messages`, and is observed alongside the messages it decorates.
 */
data class Message(
    val id: String,
    val dialogId: String,
    val profileId: String,
    val senderId: String,
    val senderName: String,
    val timestampEpoch: Long,
    val text: String,
    val isOutgoing: Boolean,
    val hasAttachments: Boolean = false,
)
