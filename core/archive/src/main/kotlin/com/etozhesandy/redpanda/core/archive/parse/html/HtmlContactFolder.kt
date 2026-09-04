package com.etozhesandy.redpanda.core.archive.parse.html

/** The peer a contact directory belongs to, as read from its folder name by an [HtmlDialect]. */
data class HtmlContactFolder(val peerName: String, val peerId: String)
