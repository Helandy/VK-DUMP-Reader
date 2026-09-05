package com.etozhesandy.redpanda.core.storage.db.message

import androidx.room.Embedded

/** A row of [MessageDao.searchAllDialogs]: a message plus the name of the dialog it came from. */
data class MessageWithDialogEntity(
    @Embedded val message: MessageEntity,
    val peerName: String,
)
