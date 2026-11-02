package nl.wissels.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import nl.wissels.app.MatchViewModel
import nl.wissels.app.UiState
import nl.wissels.app.model.Half
import nl.wissels.app.model.Screen

/**
 * Voorbereiding van een wedstrijd: team kiezen, tegenstander invullen,
 * deelnemende spelers aanvinken, de keepers bepalen en de startopstelling
 * kiezen. De startopstelling bepaalt de veldgrootte. Pas als hier alles
 * staat klinkt "Wedstrijd starten".
 */
@Composable
fun PrepareScreen(state: UiState, vm: MatchViewModel, navController: NavController) {
    val prep = state.prep
    var started by remember { mutableStateOf(false) }
    var showKeeperReplace by remember { mutableStateOf(false) }

    val btnH = ResponsiveSizes.buttonHeight(LocalConfiguration.current.screenHeightDp)

    // Start gelukt (het concept is weg) → naar het wedstrijdscherm.
    LaunchedEffect(started, prep) {
        if (started && prep == null) {
            navController.navigate(Screen.MATCH.route) {
                popUpTo(Screen.PREPARE.route) { inclusive = true }
            }
        }
    }

    if (prep == null) {
        EmptyState(
            title = "Geen voorbereiding actief",
            message = "Er is geen wedstrijd in voorbereiding.\n" +
                    "Ga terug en start opnieuw met de voorbereiding.",
            actionLabel = "Terug naar start",
            onAction = { navController.navigate(Screen.SETUP.route) }
        )
        return
    }

    val team = state.prepTeam
    val settings = team.settings
    val selected = team.players.filter { it.id in prep.selectedIds }
    // Het team heeft een vaste keeper als er één speler voor beide helften
    // is aangewezen → dan volstaat één keuzelijst (vooraf ingevuld).
    val hasFixedKeeper = team.players.any { it.keeperFirst && it.keeperSecond }
    val keepersReady = prep.keeperFirstId != null
    // Startopstelling: de keeper staat altijd in het veld; de coach kiest de
    // overige veldspelers. Dat aantal bepaalt de veldgrootte.
    val fieldCount = prep.starterIds.size + if (keepersReady) 1 else 0
    val benchCount = (selected.size - fieldCount).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Wedstrijdvoorbereiding",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )

        // ------------------------------------------------------------- team
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Team", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IntDropdown(
                    label = "Welk team speelt",
                    value = prep.teamId.toInt(),
                    options = state.teams.map { it.id.toInt() },
                    suffix = { id -> state.teams.firstOrNull { it.id.toInt() == id }?.name ?: "?" }
                ) { id ->
                    val newId = state.teams.firstOrNull { it.id.toInt() == id }?.id ?: team.id
                    vm.setPrepTeam(newId)
                }
            }
        }

        // ------------------------------------------------------ tegenstander
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tegenstander", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = prep.awayTeam,
                    onValueChange = { v -> vm.setPrepAwayTeam(v) },
                    label = { Text("Naam van de tegenstander") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }
        }

        // ---------------------------------------------- wedstrijdinstellingen
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Wedstrijdinstellingen",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    val settingLines = listOf(
                        "${settings.halfMinutes} minuten per helft",
                        if (settings.timeoutEnabled) "Time-out: toegestaan" else "Time-out: uit",
                        if (settings.cardsEnabled) "Kaarten (geel/rood): aan" else "Kaarten (geel/rood): uit",
                        if (settings.autoApply) "Wissels automatisch toepassen" else "Wissels op bevestiging"
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        settingLines.chunked(2).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { line ->
                                    Text(
                                        line,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (row.size < 2) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { navController.navigate(Screen.GAMESETTINGS.route) },
                    modifier = Modifier.height(btnH * 0.85f),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Aanpassen") }
            }
        }

        // -------------------------------------------------- deelnemende spelers
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Deelnemende spelers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${selected.size} van de ${team.players.size} spelers doen mee",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                if (team.players.isEmpty()) {
                    Text(
                        "Nog geen spelers. Voeg ze toe bij Menu → Teams.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                PlayerSelectionGrid(
                    players = team.players,
                    selected = prep.selectedIds,
                    onToggle = { vm.togglePrepParticipant(it) },
                    enabled = true
                )
            }
        }

        // -------------------------------------------------------------- keepers
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Keepers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (hasFixedKeeper)
                        "Vaste keeper: deze staat al ingevuld. Wijzig hem hier als het " +
                                "toch iemand anders wordt."
                    else
                        "Kies per helft de keeper — zonder keeper kan de wedstrijd niet " +
                                "starten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selected.isEmpty()) {
                    Text(
                        "Vink eerst deelnemende spelers aan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val wide = LocalConfiguration.current.screenWidthDp >= 360
                if (hasFixedKeeper) {
                    PlayerDropdown(
                        label = "Keeper (hele wedstrijd)",
                        value = prep.keeperFirstId?.takeIf { id -> selected.any { it.id == id } },
                        players = selected,
                        emptyLabel = "Kies de keeper"
                    ) { id -> vm.setPrepFixedKeeper(id) }
                } else if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlayerDropdown(
                            label = "Keeper 1e helft",
                            value = prep.keeperFirstId?.takeIf { id -> selected.any { it.id == id } },
                            players = selected,
                            emptyLabel = "Kies de keeper",
                            modifier = Modifier.weight(1f)
                        ) { id -> vm.setPrepKeeper(id, Half.FIRST) }
                        PlayerDropdown(
                            label = "Keeper 2e helft",
                            value = prep.keeperSecondId?.takeIf { id -> selected.any { it.id == id } },
                            players = selected,
                            emptyLabel = "Wordt later bepaald",
                            modifier = Modifier.weight(1f)
                        ) { id -> vm.setPrepKeeper(id, Half.SECOND) }
                    }
                } else {
                    PlayerDropdown(
                        label = "Keeper 1e helft",
                        value = prep.keeperFirstId?.takeIf { id -> selected.any { it.id == id } },
                        players = selected,
                        emptyLabel = "Kies de keeper"
                    ) { id -> vm.setPrepKeeper(id, Half.FIRST) }
                    PlayerDropdown(
                        label = "Keeper 2e helft",
                        value = prep.keeperSecondId?.takeIf { id -> selected.any { it.id == id } },
                        players = selected,
                        emptyLabel = "Wordt later bepaald"
                    ) { id -> vm.setPrepKeeper(id, Half.SECOND) }
                }
            }
        }

        // ------------------------------------------------------ startopstelling
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Startopstelling", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Wie beginnen er in het veld? De rest begint op de bank en wisselt automatisch.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                when {
                    selected.isEmpty() -> Text(
                        "Vink eerst deelnemende spelers aan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (prep.starterIds.isEmpty())
                                "Nog geen veldspelers gekozen — kies er minstens één."
                            else
                                "${prep.starterIds.size} veldspeler(s) + keeper = " +
                                        "$fieldCount in het veld · $benchCount op de bank",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (prep.starterIds.isEmpty()) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                        PlayerSelectionGrid(
                            players = selected,
                            selected = prep.starterIds + setOfNotNull(prep.keeperFirstId),
                            onToggle = { id ->
                                if (id == prep.keeperFirstId) {
                                    showKeeperReplace = true
                                } else {
                                    vm.togglePrepStarter(id)
                                }
                            },
                            locked = emptySet(),
                            keeperId = prep.keeperFirstId,
                            enabled = true
                        )
                    }
                }
            }
        }

        // --------------------------------------------------------------- fouten
        state.setupError?.let { err ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(
                    err,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // ----------------------------------------------------------- startknop
        Button(
            onClick = {
                started = true
                vm.startMatch()
            },
            modifier = Modifier.fillMaxWidth().height(btnH),
            enabled = selected.isNotEmpty() && keepersReady && prep.starterIds.isNotEmpty(),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                when {
                    selected.isEmpty() -> "Kies eerst de deelnemende spelers"
                    !keepersReady -> "Kies de keeper"
                    prep.starterIds.isEmpty() -> "Kies de startopstelling"
                    else -> "Wedstrijd starten"
                }
            )
        }

        OutlinedButton(
            onClick = {
                vm.cancelPreparation()
                navController.navigate(Screen.SETUP.route) {
                    popUpTo(Screen.PREPARE.route) { inclusive = true }
                }
            },
            modifier = Modifier.fillMaxWidth().height(btnH),
            shape = MaterialTheme.shapes.medium
        ) { Text("Annuleren") }

        Spacer(Modifier.padding(bottom = 16.dp))
    }

    if (showKeeperReplace) {
        val currentKeeperId = prep.keeperFirstId
        val candidates = selected.filter { it.id != currentKeeperId }
        AlertDialog(
            onDismissRequest = { showKeeperReplace = false },
            title = { Text("Nieuwe keeper kiezen") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Kies wie de nieuwe keeper wordt. De huidige keeper " +
                                "gaat het veld in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PlayerSelectionGrid(
                        players = candidates,
                        selected = emptySet(),
                        onToggle = { newId ->
                            if (hasFixedKeeper) vm.setPrepFixedKeeper(newId)
                            else vm.setPrepKeeper(newId, Half.FIRST)
                            if (currentKeeperId != null) vm.togglePrepStarter(currentKeeperId)
                            showKeeperReplace = false
                        },
                        showCheckbox = false,
                        columns = ResponsiveSizes.selectionGridColumns(LocalConfiguration.current.screenWidthDp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showKeeperReplace = false }) { Text("Klaar") }
            },
            dismissButton = {
                TextButton(onClick = { showKeeperReplace = false }) { Text("Annuleren") }
            }
        )
    }
}

@Composable
private fun SettingLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
