package com.etozhesandy.redpanda.features.dialogs.domain.model

import com.etozhesandy.redpanda.core.model.ChatDialog
import com.etozhesandy.redpanda.core.model.DialogSort

fun List<ChatDialog>.sortedBy(sort: DialogSort, ascending: Boolean): List<ChatDialog> {
    val comparator: Comparator<ChatDialog> = when (sort) {
        DialogSort.DATE -> compareBy { it.lastMessageAt }
        DialogSort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.peerName }
        DialogSort.MESSAGE_COUNT -> compareBy { it.messageCount }
    }
    return sortedWith(if (ascending) comparator else comparator.reversed())
}
