package com.etozhesandy.redpanda.core.model

/** An imported chat archive, scoped to its own directory under `filesDir/profiles/$id`. */
data class Profile(
    val id: String,
    val sourceType: SourceType,
    val displayName: String,
    val avatarPath: String?,
    val rootDirPath: String,
    val status: ProfileStatus,
    val importedAt: Long,
    val updatedAt: Long,
    val vkId: String? = null,
    val screenName: String? = null,
    val birthDate: String? = null,
    val sex: Sex = Sex.UNKNOWN,
    val country: String? = null,
    val city: String? = null,
)
