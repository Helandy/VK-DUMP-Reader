package com.etozhesandy.redpanda.core.navigation

import android.os.Bundle
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds a navigation back stack while the lock gate removes its composition and NavController.
 *
 * This is a singleton because the controller is owned by the content below that gate; keeping its
 * saved state in the composition would discard it together with the controller on lock.
 */
@Singleton
class NavStateCache @Inject constructor() {

    private var state: Bundle? = null

    fun save(bundle: Bundle?) {
        state = bundle
    }

    fun consume(): Bundle? = state.also { state = null }
}
