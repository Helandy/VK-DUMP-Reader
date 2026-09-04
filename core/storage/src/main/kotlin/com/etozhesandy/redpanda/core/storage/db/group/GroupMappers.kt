package com.etozhesandy.redpanda.core.storage.db.group

import com.etozhesandy.redpanda.core.model.Group

fun GroupEntity.toDomain(): Group = Group(
    id = id.substringAfter(':'),
    profileId = profileId,
    name = name,
    avatarPath = avatarPath,
    screenName = screenName,
)

fun Group.toEntity(): GroupEntity = GroupEntity(
    id = "$profileId:$id",
    profileId = profileId,
    name = name,
    avatarPath = avatarPath,
    screenName = screenName,
)
