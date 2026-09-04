package com.etozhesandy.redpanda.core.model

/**
 * A conversation with one peer (or group) inside a [Profile].
 *
 * [category] carries the source archive's own grouping when one exists (e.g. VK's
 * "Девушки"/"Парни" dialog folders) and is null when the source has no such concept.
 */
data class ChatDialog(
    val id: String,
    val profileId: String,
    val peerId: String,
    val peerName: String,
    val peerAvatarPath: String?,
    val kind: DialogKind,
    val category: String?,
    val lastMessageAt: Long,
    val messageCount: Int,
)
