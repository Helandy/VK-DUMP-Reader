package com.etozhesandy.redpanda.features.favorites.mapper

import com.etozhesandy.redpanda.core.navigation.Routes
import com.etozhesandy.redpanda.features.favorites.model.FavoritesArgs

fun Routes.Favorites.toArgs(): FavoritesArgs = FavoritesArgs(profileId = profileId)
