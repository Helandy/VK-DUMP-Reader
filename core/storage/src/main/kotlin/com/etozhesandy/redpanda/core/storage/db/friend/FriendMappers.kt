package com.etozhesandy.redpanda.core.storage.db.friend

import com.etozhesandy.redpanda.core.model.Friend

fun FriendEntity.toDomain(): Friend = Friend(
    id = id.substringAfter(':'),
    profileId = profileId,
    name = name,
    avatarPath = avatarPath,
)

fun Friend.toEntity(): FriendEntity = FriendEntity(
    id = "$profileId:$id",
    profileId = profileId,
    name = name,
    avatarPath = avatarPath,
)
