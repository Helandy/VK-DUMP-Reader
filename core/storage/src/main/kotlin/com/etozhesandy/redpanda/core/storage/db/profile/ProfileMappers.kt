package com.etozhesandy.redpanda.core.storage.db.profile

import com.etozhesandy.redpanda.core.model.Profile

fun ProfileEntity.toDomain(): Profile = Profile(
    id = id,
    sourceType = sourceType,
    displayName = displayName,
    avatarPath = avatarPath,
    rootDirPath = rootDirPath,
    status = status,
    importedAt = importedAt,
    updatedAt = updatedAt,
    vkId = vkId,
    screenName = screenName,
    birthDate = birthDate,
    sex = sex,
    country = country,
    city = city,
)

fun Profile.toEntity(): ProfileEntity = ProfileEntity(
    id = id,
    sourceType = sourceType,
    displayName = displayName,
    avatarPath = avatarPath,
    rootDirPath = rootDirPath,
    status = status,
    importedAt = importedAt,
    updatedAt = updatedAt,
    vkId = vkId,
    screenName = screenName,
    birthDate = birthDate,
    sex = sex,
    country = country,
    city = city,
)
