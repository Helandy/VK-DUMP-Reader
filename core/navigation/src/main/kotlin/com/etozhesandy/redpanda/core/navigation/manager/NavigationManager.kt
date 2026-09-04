package com.etozhesandy.redpanda.core.navigation.manager

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Queues navigation requests for `AppNavHost` to apply to its `NavController`.
 *
 * A queue rather than the stack itself: a `NavController` holds an Activity context and is created
 * in the composition, so a singleton must not keep one. What survives here is only the intent.
 *
 * The channel is buffered because a ViewModel may navigate from its `init`, before the host starts
 * collecting; [commands] is a `receiveAsFlow`, so every request is applied exactly once rather than
 * replayed on recomposition.
 */
@Singleton
class NavigationManager @Inject constructor() : INavigationManager {

    private val _commands = Channel<NavCommand>(Channel.BUFFERED)
    val commands: Flow<NavCommand> = _commands.receiveAsFlow()

    override fun navigate(dest: Any, popUpTo: PopUpTo?) {
        _commands.trySend(NavCommand.To(dest, popUpTo))
    }

    override fun back() {
        _commands.trySend(NavCommand.Back)
    }
}
