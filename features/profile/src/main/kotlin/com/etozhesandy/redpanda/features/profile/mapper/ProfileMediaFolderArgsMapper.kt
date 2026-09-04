package com.etozhesandy.redpanda.features.profile.mapper

import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.profile.model.ProfileMediaFolderArgs

fun Routes.ProfileMediaFolder.toArgs(): ProfileMediaFolderArgs = ProfileMediaFolderArgs(
    profileId = profileId,
    folder = folder,
)
