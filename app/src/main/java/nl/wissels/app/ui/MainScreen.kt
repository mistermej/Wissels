package nl.wissels.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import nl.wissels.app.AppVersion
import nl.wissels.app.MatchViewModel
import nl.wissels.app.model.Screen
import nl.wissels.app.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: MatchViewModel,
    navController: NavController,
    currentRoute: String,
    content: @Composable (UiState) -> Unit
) {
    val state by vm.ui.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = androidx.compose.runtime.remember(context) { AppVersion.name(context) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    currentRoute = currentRoute,
                    version = version,
                    onItemClick = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(getTitleForRoute(currentRoute)) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        ) { innerPadding ->
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                content(state)
            }
        }
    }
}

@Composable
fun DrawerContent(
    currentRoute: String,
    version: String,
    onItemClick: (Screen) -> Unit
) {
    val items = listOf(
        Triple(Screen.SETUP, "Start wedstrijd", Icons.Default.PlayArrow),
        Triple(Screen.MATCH, "Wedstrijd", Icons.Default.SportsSoccer),
        Triple(Screen.SUMMARY, "Overzicht", Icons.Default.Assessment),
        Triple(Screen.GAMESETTINGS, "Wedstrijdinstellingen", Icons.Default.Tune),
        Triple(Screen.TEAMS, "Teams", Icons.Default.Group),
        Triple(Screen.SETTINGS, "App-instellingen", Icons.Default.Settings),
        Triple(Screen.ABOUT, "Over", Icons.Default.Info),
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Wissels",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(16.dp)
        )
        Text(
            "v$version",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.width(8.dp))
        HorizontalDivider()
        items.forEach { (screen, label, icon) ->
            val selected = currentRoute == screen.route
            NavigationDrawerItem(
                label = { Text(label) },
                icon = { Icon(icon, contentDescription = null) },
                selected = selected,
                onClick = { onItemClick(screen) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedContainerColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

private fun getTitleForRoute(route: String): String = when (route) {
    Screen.SETUP.route -> "Wedstrijd starten"
    Screen.PREPARE.route -> "Voorbereiding"
    Screen.MATCH.route -> "Wedstrijd"
    Screen.SUMMARY.route -> "Wedstrijdoverzicht"
    Screen.SETTINGS.route -> "App-instellingen"
    Screen.GAMESETTINGS.route -> "Wedstrijdinstellingen"
    Screen.TEAMS.route -> "Teams beheren"
    Screen.ABOUT.route -> "Over Wissels"
    else -> "Wissels"
}
