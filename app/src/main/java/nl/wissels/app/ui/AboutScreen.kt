package nl.wissels.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.wissels.app.AppUpdater
import nl.wissels.app.AppVersion
import nl.wissels.app.MatchViewModel
import nl.wissels.app.R
import nl.wissels.app.UpdateChecker
import nl.wissels.app.UpdateStatus

@Composable
fun AboutScreen(state: nl.wissels.app.UiState, vm: MatchViewModel) {
    var showUpdateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val scope = rememberCoroutineScope()
    val update by UpdateChecker.state.collectAsState()
    val installState by AppUpdater.state.collectAsState()
    val currentVersion = remember(context) { AppVersion.name(context) }

    LaunchedEffect(Unit) { UpdateChecker.check(context) }

    // Outer Box zodat "Gemaakt door Jaimie" altijd onderaan staat,
    // ongeacht hoeveel er in de scrollbare sectie staat.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // ── scrollbare inhoud ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 80.dp), // ruimte voor credits
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ---- App-identiteit ----------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.vv_wijster_logo),
                        contentDescription = "VV Wijster logo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ResponsiveSizes.logoSize(LocalConfiguration.current.screenWidthDp)),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        "Wissels",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Versie $currentVersion",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ---- Updates -----------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Updates",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (update.status) {
                            UpdateStatus.Unknown -> Text(
                                "Nog niet gecontroleerd",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            UpdateStatus.Checking -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                Text("Controleren op updates…")
                            }

                            UpdateStatus.UpToDate -> Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    "App is up-to-date (v$currentVersion)",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            UpdateStatus.UpdateAvailable -> Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning, null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            "Update beschikbaar!",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "Nieuwe versie: ${update.latestVersion}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        if (update.apkDownloadUrl.isNotBlank()) {
                                            AppUpdater.downloadAndInstall(context, update.apkDownloadUrl)
                                        } else {
                                            // Fallback: open de releasespagina.
                                            context.startActivity(
                                                android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse(update.releaseUrl)
                                                )
                                            )
                                        }
                                    },
                                    shape = MaterialTheme.shapes.medium
                                ) { Text("Installeren") }
                            }

                            UpdateStatus.Private -> Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock, null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        "Repository is privé — open de releasespagina.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                OpenReleasesButton()
                            }

                            UpdateStatus.Error -> Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning, null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        "Kon niet controleren (geen internet?)",
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                OpenReleasesButton()
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { UpdateChecker.check(context, force = true) },
                        shape = MaterialTheme.shapes.medium
                    ) { Text("Controleren op updates") }
                }
            }

            // ---- Links -------------------------------------------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Links",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    LinkRow("VV Wijster website", "https://www.vvwijster.nl/")
                    LinkRow("GitHub repository", "https://github.com/mistermej/Wissels")
                    LinkRow("Releases / Download APK", "https://github.com/mistermej/Wissels/releases")
                }
            }
        }

        // ── credits — altijd onderaan, niet mee-scrollend ───────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "Gemaakt door Jaimie",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                "Voor VV Wijster",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = {
                if (installState !is nl.wissels.app.UpdateInstallState.Downloading) {
                    showUpdateDialog = false
                }
            },
            title = { Text("Update beschikbaar: v${update.latestVersion}") },
            text = {
                when (val s = installState) {
                    is nl.wissels.app.UpdateInstallState.Idle ->
                        Text("De update wordt automatisch gedownload en geïnstalleerd.")
                    is nl.wissels.app.UpdateInstallState.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Downloaden…")
                            if (s.progress != null) {
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { s.progress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                androidx.compose.material3.LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    is nl.wissels.app.UpdateInstallState.Installing ->
                        Text("Installer wordt geopend…")
                    is nl.wissels.app.UpdateInstallState.Error ->
                        Text("Fout: ${s.message}")
                }
            },
            confirmButton = {
                when (installState) {
                    is nl.wissels.app.UpdateInstallState.Idle,
                    is nl.wissels.app.UpdateInstallState.Error -> {
                        Button(
                            shape = MaterialTheme.shapes.medium,
                            onClick = {
                                if (!AppUpdater.canInstall(context)) {
                                    AppUpdater.openInstallPermissionSettings(context)
                                    return@Button
                                }
                                if (update.apkDownloadUrl.isNotBlank()) {
                                    AppUpdater.downloadAndInstall(context, update.apkDownloadUrl)
                                } else {
                                    context.startActivity(
                                        android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(update.releaseUrl)
                                        )
                                    )
                                    showUpdateDialog = false
                                }
                            }
                        ) { Text(if (installState is nl.wissels.app.UpdateInstallState.Error) "Opnieuw proberen" else "Installeren") }
                    }
                    else -> {}
                }
            },
            dismissButton = {
                if (installState is nl.wissels.app.UpdateInstallState.Idle ||
                    installState is nl.wissels.app.UpdateInstallState.Error) {
                    TextButton(onClick = {
                        AppUpdater.reset()
                        showUpdateDialog = false
                    }) { Text("Annuleren") }
                }
            }
        )
    }
}

@Composable
private fun OpenReleasesButton() {
    val context = LocalContext.current
    OutlinedButton(
        shape = MaterialTheme.shapes.medium,
        onClick = {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://github.com/mistermej/Wissels/releases")
                )
            )
        }
    ) {
        Icon(
            Icons.Default.OpenInNew, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text("Releasespagina openen")
    }
}

@Composable
private fun LinkRow(label: String, url: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            shape = MaterialTheme.shapes.medium,
            onClick = {
                context.startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(url)
                    )
                )
            }
        ) {
            Icon(
                Icons.Default.OpenInNew, null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
