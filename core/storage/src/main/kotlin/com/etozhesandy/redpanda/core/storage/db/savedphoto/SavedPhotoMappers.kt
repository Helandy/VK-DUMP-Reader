package com.etozhesandy.redpanda.core.storage.db.savedphoto

import com.etozhesandy.redpanda.core.model.SavedPhoto

fun SavedPhotoEntity.toDomain(): SavedPhoto = SavedPhoto(
    id = id.substringAfter(':'),
    profileId = profileId,
    url = url,
    timestampEpoch = timestampEpoch,
)

fun SavedPhoto.toEntity(): SavedPhotoEntity = SavedPhotoEntity(
    id = "$profileId:$id",
    profileId = profileId,
    url = url,
    timestampEpoch = timestampEpoch,
)
