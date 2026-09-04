package com.etozhesandy.redpanda.core.navigation

import androidx.navigation.NavGraphBuilder

/** Implemented once per feature to register its destinations into the shared [AppNavHost] graph. */
fun interface NavRegistrar {
    fun register(builder: NavGraphBuilder)
}
