package com.etozhesandy.redpanda.core.storage.db.profile

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.etozhesandy.redpanda.core.model.ProfileStatus
import com.etozhesandy.redpanda.core.model.Sex
import com.etozhesandy.redpanda.core.model.SourceType

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
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
