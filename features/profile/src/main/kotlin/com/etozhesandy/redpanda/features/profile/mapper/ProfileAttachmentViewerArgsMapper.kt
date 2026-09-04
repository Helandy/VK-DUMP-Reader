package com.etozhesandy.redpanda.features.profile.mapper

import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.profile.model.ProfileAttachmentViewerArgs

fun Routes.ProfileAttachmentViewer.toArgs(): ProfileAttachmentViewerArgs = ProfileAttachmentViewerArgs(
    profileId = profileId,
    startAttachmentId = startAttachmentId,
)
