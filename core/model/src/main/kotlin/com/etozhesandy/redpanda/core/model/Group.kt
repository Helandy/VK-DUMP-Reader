package com.etozhesandy.redpanda.core.model

/** One entry in the archive owner's joined-groups list. */
data class Group(
    val id: String,
    val profileId: String,
    val name: String,
    val avatarPath: String?,
    val screenName: String?,
)
