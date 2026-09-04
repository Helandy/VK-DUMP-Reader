package com.etozhesandy.redpanda.features.profile.mapper

import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.profile.model.ProfileArgs

fun Routes.Profile.toArgs(): ProfileArgs = ProfileArgs(
    profileId = profileId,
)
