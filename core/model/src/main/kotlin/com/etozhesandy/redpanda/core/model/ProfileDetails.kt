package com.etozhesandy.redpanda.core.model

/** The archive owner's own profile fields, parsed from the source archive when available. */
data class ProfileDetails(
    val vkId: String?,
    val screenName: String?,
    val avatarPath: String?,
    val birthDate: String?,
    val sex: Sex,
    val country: String?,
    val city: String?,
)
