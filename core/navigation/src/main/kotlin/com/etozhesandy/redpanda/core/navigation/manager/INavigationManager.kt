package com.etozhesandy.redpanda.core.navigation.manager

/**
 * How the rest of the app asks to navigate — injected into ViewModels, so a screen's Composable
 * never needs an `onNavigateTo...` lambda.
 *
 * Deliberately narrow: the back stack itself belongs to the `NavController` living in the
 * composition, and nothing outside the navigation host may touch it.
 */
interface INavigationManager {

    fun navigate(dest: Any, popUpTo: PopUpTo? = null)

    fun back()
}
