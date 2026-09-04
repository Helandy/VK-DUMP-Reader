package com.etozhesandy.redpanda.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.etozhesandy.redpanda.core.navigation.manager.NavCommand
import com.etozhesandy.redpanda.core.navigation.manager.NavigationManager

@Composable
fun AppNavHost(
    navController: NavHostController,
    navManager: NavigationManager,
    registrars: Set<NavRegistrar>,
) {
    // The one place the queued requests meet the back stack; everywhere else navigation is asked
    // for through INavigationManager.
    LaunchedEffect(navController) {
        navManager.commands.collect { command ->
            when (command) {
                is NavCommand.To -> navController.navigate(command.dest) {
                    command.popUpTo?.let { popUpTo(it.route) { inclusive = it.inclusive } }
                }
                NavCommand.Back -> navController.popBackStack()
            }
        }
    }

    NavHost(navController = navController, startDestination = Routes.Home) {
        registrars.forEach { it.register(this) }
    }
}
