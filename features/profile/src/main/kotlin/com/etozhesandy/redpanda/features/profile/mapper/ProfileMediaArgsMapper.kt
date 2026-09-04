package com.etozhesandy.redpanda.features.profile.mapper

import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaArgs

fun Routes.ProfileMedia.toArgs(): ProfileMediaArgs = ProfileMediaArgs(
    profileId = profileId,
)
