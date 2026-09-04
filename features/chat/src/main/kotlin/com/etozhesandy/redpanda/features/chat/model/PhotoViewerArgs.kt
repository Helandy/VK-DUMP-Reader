package com.etozhesandy.redpanda.features.chat.model

/** What the photo viewer was opened with, free of the navigation types that carried it. */
data class PhotoViewerArgs(
    val dialogId: String,
    val startAttachmentId: String,
)
