package com.etozhesandy.redpanda.core.storage.db.message

import com.etozhesandy.redpanda.core.model.Message

fun MessageEntity.toDomain(): Message = Message(
    id = messageId,
    dialogId = dialogId,
    profileId = profileId,
    senderId = senderId,
    senderName = senderName,
    timestampEpoch = timestampEpoch,
    text = text,
    isOutgoing = isOutgoing,
    isFavorite = isFavorite,
    hasAttachments = hasAttachments,
)

fun Message.toEntity(): MessageEntity = MessageEntity(
    messageId = id,
    dialogId = dialogId,
    profileId = profileId,
    senderId = senderId,
    senderName = senderName,
    timestampEpoch = timestampEpoch,
    text = text,
    isOutgoing = isOutgoing,
    isFavorite = isFavorite,
    hasAttachments = hasAttachments,
)
