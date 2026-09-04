package com.etozhesandy.redpanda.core.storage.db.dialog

import com.etozhesandy.redpanda.core.model.ChatDialog

fun DialogEntity.toDomain(): ChatDialog = ChatDialog(
    id = id,
    profileId = profileId,
    peerId = peerId,
    peerName = peerName,
    peerAvatarPath = peerAvatarPath,
    kind = kind,
    category = category,
    lastMessageAt = lastMessageAt,
    messageCount = messageCount,
)

fun ChatDialog.toEntity(): DialogEntity = DialogEntity(
    id = id,
    profileId = profileId,
    peerId = peerId,
    peerName = peerName,
    peerAvatarPath = peerAvatarPath,
    kind = kind,
    category = category,
    lastMessageAt = lastMessageAt,
    messageCount = messageCount,
)
