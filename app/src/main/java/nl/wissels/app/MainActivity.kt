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

// Kleuren VV Wijster: groen en wit. Alles wat eerst paars (Material-defaults)
// of grijs was, is hier groen of wit geworden.
private val Green = androidx.compose.ui.graphics.Color(0xFF1B5E20)
private val GreenLight = androidx.compose.ui.graphics.Color(0xFF4C8C4A)
private val GreenPale = androidx.compose.ui.graphics.Color(0xFFE8F5E9)
private val White = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val Ink = androidx.compose.ui.graphics.Color(0xFF1A1C19)
private val InkSoft = androidx.compose.ui.graphics.Color(0xFF465046)

/** Het thema van de app: groen/wit (VV Wijster), geen paars en geen grijs. */
private val WisselsColors = lightColorScheme(
    primary = Green,
    onPrimary = White,
    primaryContainer = Green,          // banners (balk bovenaan) groen
    onPrimaryContainer = White,
    inversePrimary = GreenPale,
    secondary = GreenLight,
    onSecondary = White,
    secondaryContainer = GreenPale,    // groen i.p.v. het paarsgrijze default
    onSecondaryContainer = Green,
    tertiary = GreenLight,             // geen paarse accentkleur
    onTertiary = White,
    tertiaryContainer = GreenPale,
    onTertiaryContainer = Green,
    background = White,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = White,            // het grijze vlak → wit
    onSurfaceVariant = InkSoft,
    inverseSurface = Green,
    inverseOnSurface = White,
    outline = GreenLight,              // groene randen op alles wat een rand heeft
    outlineVariant = GreenPale,        // groene scheidingslijnen
    surfaceBright = White,
    surfaceDim = White,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = White,
    surfaceContainerHigh = White,
    surfaceContainerHighest = White    // kaarten wit (met groene rand)
)

class MainActivity : ComponentActivity() {

    /** Van de statusbalk-melding: direct naar het wedstrijdscherm. */
    private var openMatch by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openMatch = intent?.getBooleanExtra(Notifier.EXTRA_OPEN_MATCH, false) == true
        // Bij het openen van de app meteen kijken of er een nieuwe versie is
        // (de status zie je onder "Over" en — als er iets is — in het menu).
        UpdateChecker.check(this)
        setContent {
            // Themakeuze staat voorlopig vast op de dagstand; donkere thema's
            // komen later (zie TODO.md).
            MaterialTheme(colorScheme = WisselsColors) {
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