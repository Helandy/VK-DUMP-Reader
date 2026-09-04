package com.etozhesandy.redpanda.features.profile.domain.model

import com.etozhesandy.redpanda.core.model.Group

/** A blank query matches everything: the search field starts empty and is not a filter yet. */
fun List<Group>.matching(query: String): List<Group> =
    if (query.isBlank()) this else filter { it.name.contains(query, ignoreCase = true) }
