package com.etozhesandy.redpanda.features.profile.mapper

import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.profile.model.ProfileFriendsArgs

fun Routes.ProfileFriends.toArgs(): ProfileFriendsArgs = ProfileFriendsArgs(
    profileId = profileId,
)
