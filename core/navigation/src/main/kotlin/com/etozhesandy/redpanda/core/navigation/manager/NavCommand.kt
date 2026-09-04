package com.etozhesandy.redpanda.core.navigation.manager

import kotlin.reflect.KClass

/** One navigation request, handed to the host that owns the back stack. */
sealed interface NavCommand {

    data class To(val dest: Any, val popUpTo: PopUpTo? = null) : NavCommand

    data object Back : NavCommand
}

/**
 * Destinations to drop before landing on the new one.
 *
 * [route] is the route's class rather than an instance, because a destination is popped by type:
 * "back to the chat, whichever chat is on the stack".
 */
data class PopUpTo(val route: KClass<*>, val inclusive: Boolean = false)
