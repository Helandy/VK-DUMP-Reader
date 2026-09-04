package com.etozhesandy.redpanda.core.storage.db.attachment

import com.etozhesandy.redpanda.core.model.Attachment

fun AttachmentEntity.toDomain(): Attachment = Attachment(
    id = id,
    messageId = messageId,
    dialogId = dialogId,
    profileId = profileId,
    type = type,
    path = path,
    orderInMessage = orderInMessage,
    timestampEpoch = timestampEpoch,
    caption = caption,
    sourceFolder = sourceFolder,
)

fun Attachment.toEntity(): AttachmentEntity = AttachmentEntity(
    id = id,
    messageId = messageId,
    dialogId = dialogId,
    profileId = profileId,
    type = type,
    path = path,
    orderInMessage = orderInMessage,
    timestampEpoch = timestampEpoch,
    caption = caption,
    sourceFolder = sourceFolder,
)
