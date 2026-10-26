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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nl.wissels.app.MatchViewModel
import nl.wissels.app.UiState
import nl.wissels.app.model.Player
import nl.wissels.app.model.Team

/**
 * Teams beheren: teams toevoegen/wijzigen/verwijderen/actief kiezen,
 * en per team de spelers (incl. keepers) beheren.
 * De wedstrijdregels van het team pas je aan onder "Wedstrijdinstellingen".
 */
@Composable
fun TeamsScreen(state: UiState, vm: MatchViewModel) {
    val active = state.activeTeam
    var showAddTeam by remember { mutableStateOf(false) }
    var newTeamName by remember { mutableStateOf("") }
    var editingTeam by remember { mutableStateOf<Team?>(null) }
    var deleteTeam by remember { mutableStateOf<Team?>(null) }
    var newName by remember { mutableStateOf("") }
    var newNumber by remember { mutableStateOf("") }
    var showAddPlayer by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }

    val squad = active.players.size
    val substitutes = active.settings.fieldSize
        ?.let { squad - it }
        ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Teams",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            state.setupError?.let { err ->
                Spacer(Modifier.height(4.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        err,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // ------------------------------------------------------ teams overzicht
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Alle teams", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { showAddTeam = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Team toevoegen")
                        }
                    }
                    state.teams.forEach { team ->
                        TeamRow(
                            team = team,
                            isActive = team.id == state.activeTeamId,
                            onActivate = { vm.setActiveTeam(team.id) },
                            onRename = { editingTeam = team },
                            onDelete = { deleteTeam = team }
                        )
                    }
                }
            }
        }

        // ---------------------------------------------------- spelers van team
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Spelers — ${active.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            if (active.settings.fieldSize == null) "$squad in selectie"
                            else "$squad in selectie • ${substitutes.coerceAtLeast(0)} wissel(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // ------------------------------------------------ vaste keeper
                    Text("Vaste keeper", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Eén keeper voor de hele wedstrijd. Deze naam komt alvast in de " +
                                "voorbereiding te staan — daar kun je hem nog per helft bijstellen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PlayerDropdown(
                        label = "Keeper",
                        value = active.players.firstOrNull { it.keeperFirst }?.id,
                        players = active.players,
                        emptyLabel = "Geen vaste keeper"
                    ) { id -> vm.setTeamKeeper(id) }

                    if (active.players.isEmpty()) {
                        Text(
                            "Nog geen spelers. Voeg ze toe met de knop hieronder.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    active.players.forEach { player ->
                        PlayerRow(
                            player = player,
                            isKeeper = player.keeperFirst || player.keeperSecond,
                            onEdit = { editingPlayer = player },
                            onDelete = { vm.removePlayer(player.id) }
                        )
                    }

                    OutlinedButton(onClick = { showAddPlayer = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Speler toevoegen")
                    }
                }
            }
        }

        item {
            Text(
                "Wedstrijdregels (helftduur, veld, time-out, kaarten) pas je aan onder " +
                        "Wedstrijdinstellingen — die gelden per team.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }

    // ------------------------------------------------------------- dialogen
    if (showAddTeam) {
        AlertDialog(
            onDismissRequest = { showAddTeam = false; newTeamName = "" },
            title = { Text("Team toevoegen") },
            text = {
                OutlinedTextField(
                    value = newTeamName,
                    onValueChange = { newTeamName = it },
                    label = { Text("Naam van het team") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.addTeam(newTeamName)
                        newTeamName = ""
                        showAddTeam = false
                    },
                    enabled = newTeamName.isNotBlank()
                ) { Text("Toevoegen") }
            },
            dismissButton = {
                TextButton(onClick = { newTeamName = ""; showAddTeam = false }) { Text("Annuleren") }
            }
        )
    }

    editingTeam?.let { team ->
        var name by remember(team.id) { mutableStateOf(team.name) }
        AlertDialog(
            onDismissRequest = { editingTeam = null },
            title = { Text("Team hernoemen") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Naam van het team") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.renameTeam(team.id, name)
                        editingTeam = null
                    },
                    enabled = name.isNotBlank()
                ) { Text("Opslaan") }
            },
            dismissButton = {
                TextButton(onClick = { editingTeam = null }) { Text("Annuleren") }
            }
        )
    }

    deleteTeam?.let { team ->
        AlertDialog(
            onDismissRequest = { deleteTeam = null },
            title = { Text("Team verwijderen?") },
            text = {
                Text("Weet je zeker dat je \"${team.name}\" wilt verwijderen? " +
                        "De ${team.players.size} speler(s) en instellingen van dit team gaan verloren.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteTeam(team.id)
                        deleteTeam = null
                    }
                ) { Text("Verwijderen") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTeam = null }) { Text("Annuleren") }
            }
        )
    }

    if (showAddPlayer) {
        AddPlayerDialog(
            name = newName,
            number = newNumber,
            onName = { newName = it },
            onNumber = { newNumber = it },
            onConfirm = {
                vm.addPlayer(newName, newNumber)
                newName = ""
                newNumber = ""
                showAddPlayer = false
            },
            onDismiss = {
                newName = ""
                newNumber = ""
                showAddPlayer = false
            }
        )
    }

    editingPlayer?.let { player ->
        EditPlayerDialog(
            player = player,
            onSave = { name, number ->
                vm.updatePlayer(player.id) { it.copy(name = name.trim(), number = number.trim()) }
                editingPlayer = null
            },
            onDismiss = { editingPlayer = null }
        )
    }
}

@Composable
private fun TeamRow(
    team: Team,
    isActive: Boolean,
    onActivate: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            val veld = team.settings.fieldSize?.let { "$it in veld" } ?: "veld: vragen"
            Text(team.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "${team.players.size} spelers · $veld · ${team.settings.halfMinutes}' per helft",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isActive) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Actief",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(4.dp))
        } else {
            TextButton(onClick = onActivate) { Text("Kies") }
        }
        IconButton(onClick = onRename) {
            Icon(Icons.Default.Edit, contentDescription = "Hernoem ${team.name}")
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Verwijder ${team.name}",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
