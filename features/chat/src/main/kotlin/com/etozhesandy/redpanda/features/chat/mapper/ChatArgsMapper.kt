package com.etozhesandy.redpanda.features.chat.mapper

import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.chat.model.ChatArgs
import com.etozhesandy.redpanda.features.chat.model.ChatSearchArgs
import com.etozhesandy.redpanda.features.chat.model.PhotoViewerArgs

fun Routes.Chat.toArgs(): ChatArgs = ChatArgs(
    dialogId = dialogId,
    profileId = profileId,
    scrollToMessageId = scrollToMessageId,
    // Anything other than the two known values means "not specified", same as no override at all.
    orderReversed = when (orderOverride) {
        Routes.Chat.ORDER_DESCENDING -> true
        Routes.Chat.ORDER_ASCENDING -> false
        else -> null
    },
)

fun Routes.ChatSearch.toArgs(): ChatSearchArgs = ChatSearchArgs(
    dialogId = dialogId,
    profileId = profileId,
    orderOverride = orderOverride,
)

fun Routes.PhotoViewer.toArgs(): PhotoViewerArgs = PhotoViewerArgs(
    dialogId = dialogId,
    startAttachmentId = startAttachmentId,
)

/**
 * The inverse of the order mapping above: null when nobody has picked an order for this screen and
 * the default from the settings should keep applying.
 */
fun Boolean?.toOrderOverride(): String? = when (this) {
    true -> Routes.Chat.ORDER_DESCENDING
    false -> Routes.Chat.ORDER_ASCENDING
    null -> null
}
