package com.etozhesandy.redpanda.features.chat.model

/**
 * What the search screen was opened with.
 *
 * [orderOverride] stays the raw route value: search only hands it back when opening a result, so
 * translating it here would be work undone a moment later.
 */
data class ChatSearchArgs(
    val dialogId: String,
    val profileId: String,
    val orderOverride: String? = null,
)
