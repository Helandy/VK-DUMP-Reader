package com.etozhesandy.redpanda.core.model

/** A single message parsed from an imported archive. */
data class Message(
    val id: String,
    val dialogId: String,
    val profileId: String,
    val senderId: String,
    val senderName: String,
    val timestampEpoch: Long,
    val text: String,
    val isOutgoing: Boolean,
    val isFavorite: Boolean,
    val hasAttachments: Boolean = false,
)
