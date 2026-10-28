package nl.wissels.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import nl.wissels.app.MatchViewModel
import nl.wissels.app.model.Screen

@Composable
fun AppNavHost(vm: MatchViewModel, openMatch: Boolean = false) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    // Rechtstreeks openen vanuit de statusbalk-melding: meteen naar de
    // wedstrijd in plaats van het startscherm (als er nog een loopt).
    val match = vm.ui.value.match
    val openableMatch = openMatch && match != null && !match.finished
    val startDestination = if (openableMatch) Screen.MATCH.route else Screen.SPLASH.route

    LaunchedEffect(openMatch) {
        if (openableMatch) {
            navController.navigate(Screen.MATCH.route) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.SPLASH.route) {
            SplashScreen(onFinished = {
                navController.navigate(Screen.SETUP.route) {
                    popUpTo(Screen.SPLASH.route) { inclusive = true }
                }
            })
        }
        composable(Screen.SETUP.route) {
            MainScreen(vm, navController, currentRoute) { SetupScreen(it, vm, navController) }
        }
        composable(Screen.PREPARE.route) {
            MainScreen(vm, navController, currentRoute) { PrepareScreen(it, vm, navController) }
        }
        composable(Screen.MATCH.route) {
            MainScreen(vm, navController, currentRoute) { MatchScreen(it, vm, navController) }
        }
        composable(Screen.SUMMARY.route) {
            MainScreen(vm, navController, currentRoute) { SummaryScreen(it, vm, navController) }
        }
        composable(Screen.SETTINGS.route) {
            MainScreen(vm, navController, currentRoute) { SettingsScreen(it, vm) }
        }
        composable(Screen.GAMESETTINGS.route) {
            MainScreen(vm, navController, currentRoute) { GameSettingsScreen(it, vm) }
        }
        composable(Screen.TEAMS.route) {
            MainScreen(vm, navController, currentRoute) { TeamsScreen(it, vm) }
        }
        composable(Screen.ABOUT.route) {
            MainScreen(vm, navController, currentRoute) { AboutScreen(it, vm) }
        }
    }
}
