package com.etozhesandy.redpanda.features.profile.domain.model

import com.etozhesandy.redpanda.core.model.Friend

/** A blank query matches everything: the search field starts empty and is not a filter yet. */
fun List<Friend>.matching(query: String): List<Friend> =
    if (query.isBlank()) this else filter { it.name.contains(query, ignoreCase = true) }
