package com.etozhesandy.redpanda.features.chat.model

/** Where a media grid was left: the first visible item and how far it was scrolled past. */
data class MediaScrollPosition(val index: Int = 0, val offset: Int = 0)
