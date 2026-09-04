package com.etozhesandy.redpanda.features.profile.mapper

import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.profile.model.ProfileSavedPhotosArgs

fun Routes.ProfileSavedPhotos.toArgs(): ProfileSavedPhotosArgs = ProfileSavedPhotosArgs(
    profileId = profileId,
)
