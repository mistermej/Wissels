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
import nl.wissels.app.UiState
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private enum class UpdateStatus { Unknown, Checking, UpToDate, UpdateAvailable, Private, Error }

/** Waarom een update-controle lukte of niet (404 = repo niet publiek zichtbaar). */
private enum class FetchOk { Ok, NotFound, Network }

@Composable
fun AboutScreen(state: UiState, vm: MatchViewModel) {
    var updateStatus by remember { mutableStateOf(UpdateStatus.Unknown) }
    var latestVersion by remember { mutableStateOf("") }
    var latestReleaseUrl by remember { mutableStateOf("") }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Eigen versie uit de manifest (nooit hardcoden — dan klopt de vergelijking altijd).
    val currentVersion = remember(context) { AppVersion.name(context) }

    fun checkForUpdates() {
        updateStatus = UpdateStatus.Checking
        scope.launch(Dispatchers.IO) {
            // 200 = release gevonden, 404 = repo bestaat niet (publiek) — dat is
            // meestal een PRIVÉ repository: zonder GitHub-token krijgen we die niet
            // te zien. Alles wat overblijft is een netwerkfout.
            val result: Pair<FetchOk, Pair<String, String>?> = try {
                val conn = URL("https://api.github.com/repos/mistermej/Wissels/releases/latest")
                    .openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                when (val code = conn.responseCode) {
                    200 -> {
                        val json = conn.inputStream.reader().use { it.readText() }
                        conn.disconnect()
                        val obj = JSONObject(json)
                        val tag = obj.getString("tag_name").removePrefix("v").removePrefix("V")
                        val url = obj.getString("html_url")
                        FetchOk.Ok to (tag to url)
                    }
                    404 -> {
                        conn.disconnect()
                        FetchOk.NotFound to null
                    }
                    else -> {
                        conn.disconnect()
                        FetchOk.Network to null
                    }
                }
            } catch (_: Exception) {
                FetchOk.Network to null
            }

            if (!isActive) return@launch
            withContext(Dispatchers.Main) {
                val (ok, payload) = result
                when (ok) {
                    FetchOk.Ok -> {
                        val (version, url) = payload!!
                        latestVersion = version
                        latestReleaseUrl = url
                        // Pas een melding als de release écht nieuwer is dan deze app.
                        updateStatus = if (AppVersion.asNumber(version) > AppVersion.asNumber(currentVersion)) {
                            UpdateStatus.UpdateAvailable
                        } else {
                            UpdateStatus.UpToDate
                        }
                    }
                    FetchOk.NotFound -> updateStatus = UpdateStatus.Private
                    FetchOk.Network -> updateStatus = UpdateStatus.Error
                }
            }
        }
    }

    LaunchedEffect(Unit) { checkForUpdates() }

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
                    modifier = Modifier.size(72.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(8.dp))
                Text("Wissels", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Versie $currentVersion", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Update check
        Card(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { checkForUpdates() }) { Text("Controleren") }
                }
                when (updateStatus) {
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
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(24.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Update beschikbaar!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            Text("Nieuwe versie: $latestVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            title = { Text("Update beschikbaar: v$latestVersion") },
            text = { Text("Druk op 'Installeren' om de GitHub releases pagina te openen.") },
            confirmButton = {
                Button(onClick = {
                    showUpdateDialog = false
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(latestReleaseUrl))
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
private fun LinkRow(label: String, url: String) {    val context = LocalContext.current
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
