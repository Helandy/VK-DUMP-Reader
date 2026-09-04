package com.etozhesandy.redpanda.features.profile.mapper

import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.profile.model.ProfileGroupsArgs

fun Routes.ProfileGroups.toArgs(): ProfileGroupsArgs = ProfileGroupsArgs(
    profileId = profileId,
)
