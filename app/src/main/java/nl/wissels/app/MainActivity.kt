package nl.wissels.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import nl.wissels.app.notify.Notifier
import nl.wissels.app.ui.AppNavHost

private val Green = androidx.compose.ui.graphics.Color(0xFF1B5E20)
private val GreenLight = androidx.compose.ui.graphics.Color(0xFF4C8C4A)

class MainActivity : ComponentActivity() {

    /** Van de statusbalk-melding: direct naar het wedstrijdscherm. */
    private var openMatch by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openMatch = intent?.getBooleanExtra(Notifier.EXTRA_OPEN_MATCH, false) == true
        setContent {
            // Themakeuze staat voorlopig vast op de dagstand; donkere thema's
            // en andere kleuren komen later (zie TODO.md).
            MaterialTheme(
                colorScheme = lightColorScheme(primary = Green, secondary = GreenLight)
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: MatchViewModel = viewModel()

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

                    AppNavHost(vm = vm, openMatch = openMatch)
                }
            }
        }
    }

    /** App al open (op de achtergrond) en op de statusbalk-melding getikt. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(Notifier.EXTRA_OPEN_MATCH, false)) {
            openMatch = true
        }
    }
}