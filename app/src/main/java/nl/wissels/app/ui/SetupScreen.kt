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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import nl.wissels.app.MatchViewModel
import nl.wissels.app.UiState
import nl.wissels.app.model.Half
import nl.wissels.app.model.Player

@Composable
fun SetupScreen(state: UiState, vm: MatchViewModel) {
    val cfg = state.config
    var newName by remember { mutableStateOf("") }
    var newNumber by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlayer by remember { mutableStateOf<Player?>(null) }

    val participants = cfg.participants.size
    val substitutes = participants - cfg.fieldSize
    val enoughPlayers = participants >= cfg.fieldSize

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Wissels",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    "Stel je team en de wedstrijd in",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ------------------------------------------------ instellingen
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Wedstrijd", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Tijd per helft (minuten)", Modifier.weight(1f))
                            NumberStepper(
                                value = cfg.halfMinutes,
                                min = 1, max = 90,
                                onChange = { v -> vm.updateConfig { it.copy(halfMinutes = v) } }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Aantal spelers in het veld", Modifier.weight(1f))
                            NumberStepper(
                                value = cfg.fieldSize,
                                min = 2, max = (participants).coerceAtLeast(2),
                                onChange = { v -> vm.updateConfig { it.copy(fieldSize = v) } }
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Time-out tijdens de wedstrijd")
                                if (cfg.timeoutEnabled) {
                                    Text(
                                        "${cfg.timeoutMinutes} min per helft",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = cfg.timeoutEnabled,
                                onCheckedChange = { v -> vm.updateConfig { it.copy(timeoutEnabled = v) } }
                            )
                        }
                        if (cfg.timeoutEnabled) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Duur time-out (minuten)", Modifier.weight(1f))
                                NumberStepper(
                                    value = cfg.timeoutMinutes,
                                    min = 1, max = 10,
                                    onChange = { v -> vm.updateConfig { it.copy(timeoutMinutes = v) } }
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Wissels automatisch toepassen")
                                Text(
                                    "Uit: de app wisselt direct. Uit: eerst een melding, jij bevestigt.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = cfg.autoApply,
                                onCheckedChange = { v -> vm.updateConfig { it.copy(autoApply = v) } }
                            )
                        }
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = cfg.homeTeam,
                                onValueChange = { v -> vm.updateConfig { it.copy(homeTeam = v) } },
                                label = { Text("Eigen team") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(Modifier.width(8.dp))
                            OutlinedTextField(
                                value = cfg.awayTeam,
                                onValueChange = { v -> vm.updateConfig { it.copy(awayTeam = v) } },
                                label = { Text("Tegenstander") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // --------------------------------------------------- spelers
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Spelers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(
                                "$participants in de selectie • $substitutes wissel(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (enoughPlayers) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.error
                            )
                        }

                        if (cfg.players.isEmpty()) {
                            Text(
                                "Nog geen spelers. Voeg ze toe met de knop hieronder.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        cfg.players.forEach { player ->
                            PlayerRow(
                                player = player,
                                onToggle = { vm.toggleParticipant(player.id) },
                                onKeeper1 = { v -> vm.setKeeper(player.id, Half.FIRST, v) },
                                onKeeper2 = { v -> vm.setKeeper(player.id, Half.SECOND, v) },
                                onEdit = { editingPlayer = player },
                                onDelete = { vm.removePlayer(player.id) }
                            )
                        }

                        OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Speler toevoegen")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }

            // --------------------------------------------------- fouten
            item {
                state.setupError?.let { err ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Text(
                            err,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // --------------------------------------------------------- startknop
        Button(
            onClick = { vm.startMatch() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            enabled = enoughPlayers && participants > 0
        ) {
            Text(
                if (participants == 0) "Voeg eerst spelers toe"
                else if (!enoughPlayers) "Te weinig spelers (nodig: ${cfg.fieldSize})"
                else "Wedstrijd starten — $substitutes wissel(s)"
            )
        }
    }

    if (showAddDialog) {
        AddPlayerDialog(
            name = newName,
            number = newNumber,
            onName = { newName = it },
            onNumber = { newNumber = it },
            onConfirm = {
                vm.addPlayer(newName, newNumber)
                newName = ""
                newNumber = ""
                showAddDialog = false
            },
            onDismiss = {
                newName = ""
                newNumber = ""
                showAddDialog = false
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
private fun PlayerRow(
    player: Player,
    onToggle: () -> Unit,
    onKeeper1: (Boolean) -> Unit,
    onKeeper2: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = player.participate, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    player.label,
                    fontWeight = if (player.participate) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (player.participate) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onEdit) { Text("Bewerk") }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Verwijder ${player.name}")
            }
        }
        if (player.participate) {
            Row(
                modifier = Modifier.padding(start = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KeeperToggle(
                    label = "Keeper 1e helft",
                    checked = player.keeperFirst,
                    onChange = onKeeper1
                )
                Spacer(Modifier.width(8.dp))
                KeeperToggle(
                    label = "Keeper 2e helft",
                    checked = player.keeperSecond,
                    onChange = onKeeper2
                )
            }
        }
    }
}

@Composable
private fun KeeperToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun NumberStepper(
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (value > min) onChange(value - 1) }, enabled = value > min) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Minder")
        }
        Text("$value", fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
        IconButton(onClick = { if (value < max) onChange(value + 1) }, enabled = value < max) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Meer")
        }
    }
}

@Composable
private fun AddPlayerDialog(
    name: String,
    number: String,
    onName: (String) -> Unit,
    onNumber: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Speler toevoegen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onName,
                    label = { Text("Naam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { v -> onNumber(v.filter { it.isDigit() }) },
                    label = { Text("Rugnummer (optioneel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = name.isNotBlank()) { Text("Toevoegen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}

@Composable
private fun EditPlayerDialog(
    player: Player,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(player.name) }
    var number by remember { mutableStateOf(player.number) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Speler bewerken") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Naam") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { v -> number = v.filter { it.isDigit() } },
                    label = { Text("Rugnummer") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, number) }, enabled = name.isNotBlank()) { Text("Opslaan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}
