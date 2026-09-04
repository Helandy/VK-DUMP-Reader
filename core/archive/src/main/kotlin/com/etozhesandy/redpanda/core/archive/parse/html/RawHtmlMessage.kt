package com.etozhesandy.redpanda.core.archive.parse.html

/**
 * One message as an [HtmlDialect] read it. The three exports disagree on how a message says who
 * sent it, so both models are carried and [HtmlDialogArchiveParser] reconciles them in one place:
 *
 *  - the two `Диалоги/` layouts name the sender ([senderId]) and leave [isOutgoing] null, because
 *    direction is only knowable by comparing that id to the dialog's peer;
 *  - the `Переписки/` layout states direction in the message's own CSS class and carries no sender
 *    id anywhere at all, so it leaves [senderId] null.
 */
data class RawHtmlMessage(
    val senderId: String?,
    val senderName: String?,
    val isOutgoing: Boolean?,
    val timestampEpoch: Long,
    val text: String,
    val peerAvatarPath: String?,
    val attachments: List<RawHtmlAttachment>,
)
