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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import nl.wissels.app.MatchViewModel
import nl.wissels.app.UiState
import nl.wissels.app.model.ASK_FIELD_SIZE
import nl.wissels.app.model.Half
import nl.wissels.app.model.Screen

/**
 * Voorbereiding van een wedstrijd: team kiezen, tegenstander invullen,
 * deelnemende spelers aanvinken, aantal spelers in het veld en de keepers
 * bepalen. Pas als hier alles staat klinkt "Wedstrijd starten".
 */
@Composable
fun PrepareScreen(state: UiState, vm: MatchViewModel, navController: NavController) {
    val prep = state.prep
    var started by remember { mutableStateOf(false) }

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
    val fieldSize = prep.fieldSize
    // Het team heeft een vaste keeper als er één speler voor beide helften
    // is aangewezen → dan volstaat één keuzelijst (vooraf ingevuld).
    val hasFixedKeeper = team.players.any { it.keeperFirst && it.keeperSecond }
    // Bank = deelnemers − veldformaat: méér tegelijk wisselen kan niet.
    val bankSize = fieldSize?.let { (selected.size - it).coerceAtLeast(0) } ?: 0
    // De keeper wordt niet automatisch gewisseld, dus er kunnen maximaal
    // `veldspelers − 1` spelers tegelijk van plek wisselen.
    val maxPerWindow = fieldSize
        ?.let { minOf(bankSize, (it - 1).coerceAtLeast(0)) }
        ?.coerceAtMost(8)
        ?: 0
    val keepersReady = prep.keeperFirstId != null

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
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                SettingLine("${settings.halfMinutes} minuten per helft")
                SettingLine(if (settings.timeoutEnabled) "Time-out: toegestaan" else "Time-out: uit")
                SettingLine(if (settings.cardsEnabled) "Kaarten (geel/rood): aan" else "Kaarten (geel/rood): uit")
                SettingLine(
                    if (settings.autoApply) "Wissels automatisch toepassen"
                    else "Wissels op bevestiging"
                )
                HorizontalDivider()
                OutlinedButton(
                    onClick = { navController.navigate(Screen.GAMESETTINGS.route) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Wedstrijdinstellingen aanpassen") }
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
                team.players.forEach { player ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = player.id in prep.selectedIds,
                            onCheckedChange = { vm.togglePrepParticipant(player.id) }
                        )
                        Text(
                            player.label,
                            modifier = Modifier.weight(1f),
                            fontWeight = if (player.id in prep.selectedIds) FontWeight.SemiBold
                            else FontWeight.Normal,
                            color = if (player.id in prep.selectedIds) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --------------------------------------------- veld & wissels
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Veld en wissels",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IntDropdown(
                    label = "In het veld (incl. keeper)",
                    value = fieldSize ?: ASK_FIELD_SIZE,
                    options = listOf(ASK_FIELD_SIZE) + (2..20).toList(),
                    suffix = { if (it == ASK_FIELD_SIZE) "Nog niet gekozen" else "$it spelers" }
                ) { v ->
                    vm.setPrepFieldSize(if (v == ASK_FIELD_SIZE) null else v)
                }
                when {
                    fieldSize == null -> Text(
                        "Kies hoeveel spelers er in het veld staan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    fieldSize > selected.size -> Text(
                        "Meer in het veld dan deelnemers: iedereen die meedoet staat in het veld.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> Text(
                        "${(selected.size - fieldSize).coerceAtLeast(0)} wissel(s) op de bank",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IntDropdown(
                    label = "Wissels per moment",
                    value = prep.subsPerWindow,
                    options = listOf(0) + (1..maxPerWindow).toList(),
                    // 0 = alle tegelijk, maar als er helemaal niets kan:
                    // liever "Geen wissels" dan "Alle tegelijk".
                    suffix = { v ->
                        when {
                            maxPerWindow == 0 -> "Geen wissels"
                            v == 0 -> "Alle tegelijk"
                            v == 1 -> "1 speler"
                            else -> "$v spelers"
                        }
                    }
                ) { v -> vm.setPrepSubsPerWindow(v) }
                Text(
                    when {
                        maxPerWindow == 0 ->
                            "Er kan nog niets gewisseld worden: er is geen bank (of alleen " +
                                    "een keeper in het veld)."
                        bankSize == 0 ->
                            "Zodra er wissels op de bank zitten kun je hier kiezen hoeveel er " +
                                    "tegelijk wisselen — nooit meer dan er tegelijk kan wisselen."
                        else ->
                            "Hoeveel spelers er op een wisselmoment tegelijk worden gewisseld — " +
                                    "maximaal $maxPerWindow (de keeper wordt niet gewisseld). " +
                                    "Tijdens de wedstrijd kun je dit nog bijstellen."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                if (hasFixedKeeper) {
                    PlayerDropdown(
                        label = "Keeper (hele wedstrijd)",
                        value = prep.keeperFirstId?.takeIf { id -> selected.any { it.id == id } },
                        players = selected,
                        emptyLabel = "Kies de keeper"
                    ) { id -> vm.setPrepFixedKeeper(id) }
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
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(vertical = 8.dp),
            enabled = selected.isNotEmpty() && fieldSize != null && keepersReady,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                when {
                    selected.isEmpty() -> "Kies eerst de deelnemende spelers"
                    fieldSize == null -> "Kies het aantal spelers in het veld"
                    !keepersReady -> "Kies de keeper"
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
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) { Text("Annuleren") }

        Spacer(Modifier.padding(bottom = 16.dp))
    }
}

@Composable
private fun SettingLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
