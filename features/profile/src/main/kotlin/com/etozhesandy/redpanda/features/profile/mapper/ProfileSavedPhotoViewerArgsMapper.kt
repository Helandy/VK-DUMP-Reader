package com.etozhesandy.redpanda.features.profile.mapper

import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.profile.model.ProfileSavedPhotoViewerArgs

fun Routes.ProfileSavedPhotoViewer.toArgs(): ProfileSavedPhotoViewerArgs = ProfileSavedPhotoViewerArgs(
    profileId = profileId,
    startPhotoId = startPhotoId,
)
