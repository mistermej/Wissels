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
import nl.wissels.app.model.sortedByNumberThenName

/**
 * Teams beheren: het team kiezen via een keuzelijst (wijzigen/verwijderen
 * met de icoontjes ernaast) en per team de spelers beheren.
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

    // Gesorteerd op rugnummer (1 bovenaan); zonder nummers op naam.
    val players = active.players.sortedByNumberThenName()

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IntDropdown(
                            label = "Welk team pas je aan",
                            value = active.id.toInt(),
                            options = state.teams.map { it.id.toInt() },
                            suffix = { id -> state.teams.firstOrNull { it.id.toInt() == id }?.name ?: "?" },
                            modifier = Modifier.weight(1f)
                        ) { id ->
                            vm.setActiveTeam(state.teams.firstOrNull { it.id.toInt() == id }?.id ?: active.id)
                        }
                        IconButton(onClick = { editingTeam = active }) {
                            Icon(Icons.Default.Edit, contentDescription = "Hernoem ${active.name}")
                        }
                        IconButton(onClick = { deleteTeam = active }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Verwijder ${active.name}",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        // ---------------------------------------------------- spelers van team
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Spelers — ${active.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    // ------------------------------------------------ vaste keeper
                    Text("Vaste keeper", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Eén keeper voor de hele wedstrijd. Deze naam komt alvast in de " +
                                "voorbereiding te staan — daar kun je hem nog bijstellen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PlayerDropdown(
                        label = "Keeper",
                        value = active.players.firstOrNull { it.keeperFirst }?.id,
                        players = players,
                        emptyLabel = "Geen vaste keeper"
                    ) { id -> vm.setTeamKeeper(id) }

                    if (players.isEmpty()) {
                        Text(
                            "Nog geen spelers. Voeg ze toe met de knop hieronder.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    players.forEach { player ->
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

        item { Spacer(Modifier.padding(bottom = 16.dp)) }
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
