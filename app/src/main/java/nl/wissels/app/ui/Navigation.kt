package nl.wissels.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import nl.wissels.app.MatchViewModel
import nl.wissels.app.model.Screen

@Composable
fun AppNavHost(vm: MatchViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    NavHost(navController = navController, startDestination = Screen.SPLASH.route) {
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
