package nl.wissels.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.wissels.app.AppVersion
import nl.wissels.app.MatchViewModel
import nl.wissels.app.R
import nl.wissels.app.UpdateChecker
import nl.wissels.app.UpdateStatus
import nl.wissels.app.model.Screen
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun AboutScreen(state: nl.wissels.app.UiState, vm: MatchViewModel) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val update by UpdateChecker.state.collectAsState()
    // Eigen versie uit de manifest (nooit hardcoden — dan klopt de vergelijking altijd).
    val currentVersion = remember(context) { AppVersion.name(context) }

    fun checkForUpdates() {
        UpdateChecker.check(context, force = true)
    }

    LaunchedEffect(Unit) { UpdateChecker.check(context) }

    // Drie vakken die samen de beschikbare ruimte verdelen (dynamisch, ook op
    // kleine schermen), met de credits gecentreerd onderaan.
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.vv_wijster_logo),
                    contentDescription = "VV Wijster logo",
                    modifier = Modifier.size(104.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(8.dp))
                Text("Wissels", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Versie $currentVersion", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Links
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Links", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LinkRow("VV Wijster website", "https://www.vvwijster.nl/")
                LinkRow("GitHub repository", "https://github.com/mistermej/Wissels")
                LinkRow("Releases / Download APK", "https://github.com/mistermej/Wissels/releases")
            }
        }

        Spacer(Modifier.height(12.dp))

        // Update check (kleiner dan de andere vakken)
        Card(modifier = Modifier.fillMaxWidth().weight(0.6f)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    when (update.status) {
                        UpdateStatus.Unknown -> Text("Nog niet gecontroleerd", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        UpdateStatus.Checking -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("Controleren op updates...")
                        }
                        UpdateStatus.UpToDate -> Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("App is up-to-date (v$currentVersion)", color = MaterialTheme.colorScheme.primary)
                        }
                        UpdateStatus.UpdateAvailable -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Update beschikbaar!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("Nieuwe versie: ${update.latestVersion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = { showUpdateDialog = true }) { Text("Installeren") }
                        }
                        UpdateStatus.Private -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Automatisch controleren lukt niet", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Het GitHub-repository is privé, dus de app mag de " +
                                                "releases niet inzien. Open de pagina hieronder om " +
                                                "handmatig te kijken of er een nieuwe versie is.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OpenReleasesButton()
                        }
                        UpdateStatus.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Kon niet controleren op updates (geen internet?)", color = MaterialTheme.colorScheme.error)
                            }
                            OpenReleasesButton()
                        }
                    }
                    // De knop staat ONDER de tekst, zoals gevraagd.
                    OutlinedButton(onClick = { checkForUpdates() }) { Text("Controleren op updates") }
                }
            }
        }

        // Credits: gecentreerd onderin
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Gemaakt door Jaimie", style = MaterialTheme.typography.bodyMedium)
            Text("Voor VV Wijster", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update beschikbaar: v${update.latestVersion}") },
            text = { Text("Druk op 'Installeren' om de GitHub releases pagina te openen.") },
            confirmButton = {
                Button(onClick = {
                    showUpdateDialog = false
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(update.releaseUrl))
                    context.startActivity(intent)
                }) { Text("Installeren") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) { Text("Later") }
            }
        )
    }
}

/** Knop die de GitHub-releasespagina opent (handmatige controle). */
@Composable
private fun OpenReleasesButton() {
    val context = LocalContext.current
    OutlinedButton(onClick = {
        context.startActivity(
            android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://github.com/mistermej/Wissels/releases")
            )
        )
    }) {
        Icon(Icons.Default.OpenInNew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("Releasespagina openen")
    }
}

@Composable
private fun LinkRow(label: String, url: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }) {
            Icon(Icons.Default.OpenInNew, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
    }
}
