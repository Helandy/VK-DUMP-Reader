package com.etozhesandy.redpanda.features.profile.mapper

import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaViewerArgs

fun Routes.ProfileMediaViewer.toArgs(): ProfileMediaViewerArgs = ProfileMediaViewerArgs(
    profileId = profileId,
    folder = folder,
    startAttachmentId = startAttachmentId,
)
