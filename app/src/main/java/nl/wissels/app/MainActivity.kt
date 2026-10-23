package nl.wissels.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import nl.wissels.app.model.Screen
import nl.wissels.app.ui.MatchScreen
import nl.wissels.app.ui.SetupScreen
import nl.wissels.app.ui.SummaryScreen

private val Green = androidx.compose.ui.graphics.Color(0xFF1B5E20)
private val GreenLight = androidx.compose.ui.graphics.Color(0xFF4C8C4A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (dark) {
                    darkColorScheme(primary = GreenLight, secondary = GreenLight)
                } else {
                    lightColorScheme(primary = Green, secondary = GreenLight)
                }
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: MatchViewModel = viewModel()
                    val state by vm.ui.collectAsState()

                    // Notificaties toestaan (Android 13+)
                    var permissionAsked by remember { mutableStateOf(false) }
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { /* negeren: app werkt ook zonder meldingen */ }
                    LaunchedEffect(Unit) {
                        if (!permissionAsked &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionAsked = true
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }

                    when (state.screen) {
                        Screen.SETUP -> SetupScreen(state, vm)
                        Screen.MATCH -> MatchScreen(state, vm)
                        Screen.SUMMARY -> SummaryScreen(state, vm)
                    }
                }
            }
        }
    }
}
