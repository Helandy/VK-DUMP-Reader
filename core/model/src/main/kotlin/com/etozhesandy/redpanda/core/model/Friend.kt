package com.etozhesandy.redpanda.core.model

/** One entry in the archive owner's friends list. */
data class Friend(
    val id: String,
    val profileId: String,
    val name: String,
    val avatarPath: String?,
)
