package nl.wissels.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
// RoundedCornerShape removed — use MaterialTheme.shapes everywhere for consistency
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import nl.wissels.app.model.Player

/**
 * Groene rand voor kaarten en vlakken: de app is wit/groen (VV Wijster) in
 * plaats van het Material-grijs. Standaard 1dp in de lichte randkleur;
 * geselecteerde vlakken krijgen een dikkere, donkergroene variant.
 */
@Composable
fun greenBorder(
    width: androidx.compose.ui.unit.Dp = 1.dp,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.outline
): BorderStroke = BorderStroke(width, color)

/**
 * Keuzelijst (dropdown) voor getallen zoals helftduur en aantal spelers.
 * Let op: [modifier] (bv. `Modifier.weight(1f)` in een Row) wordt op de
 * ExposedDropdownMenuBox gezet — díe is het kind van de Row. Op het
 * tekstveld heeft een weight geen effect (parent data geldt maar één laag).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntDropdown(
    label: String,
    value: Int,
    options: List<Int>,
    suffix: (Int) -> String = { "$it" },
    modifier: Modifier = Modifier,
    onChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = suffix(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(suffix(opt)) },
                    onClick = {
                        onChange(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Leeg scherm met uitleg en (optioneel) één actieknop.
 * Gebruikt voor "geen wedstrijd actief", "geen voorbereiding" enzovoort.
 * [message] mag een `\n` bevatten om regels te breken.
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAction,
                modifier = Modifier.height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(actionLabel, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/**
 * Keuzelijst (dropdown) om een SPELER te kiezen, bv. de keeper.
 * `value` = speler-id of `null` (dan staat [emptyLabel] er).
 * [modifier] gaat op de ExposedDropdownMenuBox (het kind van de Row),
 * zodat `Modifier.weight(1f)` naast een andere keuzelijst werkt.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDropdown(
    label: String,
    value: Long?,
    players: List<Player>,
    emptyLabel: String,
    modifier: Modifier = Modifier,
    labelColor: Color = Color.Unspecified,
    labelTrailingIcon: ImageVector? = null,
    labelTrailingIconTint: Color = Color.Unspecified,
    onChange: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = players.firstOrNull { it.id == value }?.label ?: emptyLabel,
            onValueChange = {},
            readOnly = true,
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, color = labelColor)
                    if (labelTrailingIcon != null) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            labelTrailingIcon,
                            contentDescription = null,
                            tint = if (labelTrailingIconTint == Color.Unspecified) labelColor else labelTrailingIconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(emptyLabel) },
                onClick = {
                    onChange(null)
                    expanded = false
                }
            )
            players.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.label) },
                    onClick = {
                        onChange(p.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Rugnummer als badge, zodat het duidelijk is dat "2" een rugnummer is.
 * Reserveert altijd dezelfde ruimte, ook zonder nummer.
 */
@Composable
fun NumberBadge(number: String) {
    Box(
        modifier = Modifier
            .width(36.dp)
            .height(32.dp)
            .background(
                color = if (number.isBlank()) androidx.compose.ui.graphics.Color.Transparent
                else MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        if (number.isNotBlank()) {
            Text(
                "#$number",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

/**
 * Eén rij in het Teams-scherm: alleen de naam (met rugnummer-badge) — geen
 * vinkjes. Wél/niet meedoen en de keepers bepaal je in de voorbereiding.
 */
@Composable
fun PlayerRow(
    player: Player,
    /** Toont een "KEEPER"-label bij de vaste keeper van het team. */
    isKeeper: Boolean = false,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        NumberBadge(player.number)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(player.name, fontWeight = FontWeight.SemiBold)
                if (isKeeper) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "KEEPER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        IconButton(onClick = onEdit) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Bewerk ${player.name}",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Verwijder ${player.name}",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun AddPlayerDialog(
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
fun EditPlayerDialog(
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

/**
 * Een grid van spelers om uit te kiezen (multi-select). Gebruikt in de
 * voorbereiding (deelnemende spelers / startopstelling) en in het
 * wedstrijdscherm (opstelling). Spelers staan in 2/3/4 kolommen afhankelijk
 * van de schermresolutie, zodat ze compact passen in plaats van in een lange
 * verticale lijst (waarvan de helft buiten beeld bleef).
 *
 * [locked] bevat de id's die altijd geselecteerd zijn en niet aangevinkt/
 * uitgevinkt kunnen worden (bv. de keeper in de startopstelling).
 */
@Composable
fun PlayerSelectionGrid(
    players: List<Player>,
    selected: Set<Long>,
    onToggle: (Long) -> Unit,
    locked: Set<Long> = emptySet(),
    keeperId: Long? = null,
    enabled: Boolean = true,
    showCheckbox: Boolean = true,
    columns: Int = ResponsiveSizes.selectionGridColumns(LocalConfiguration.current.screenWidthDp)
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        players.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { p ->
                    val isLocked = p.id in locked
                    val isChecked = p.id in selected || isLocked
                    Box(modifier = Modifier.weight(1f)) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = if (enabled && !isLocked) ({ onToggle(p.id) }) else ({}),
                            enabled = enabled && !isLocked,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isChecked) 2.dp else 1.dp,
                                color = if (isChecked) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                if (showCheckbox) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = if (enabled && !isLocked) ({ onToggle(p.id) }) else null,
                                        enabled = enabled && !isLocked
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        p.label,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isChecked) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center
                                    )
                                    // Altijd een tweede regel renderen zodat de tegels
                                    // van keeper en veldspelers dezelfde hoogte hebben.
                                    Text(
                                        if (p.id == keeperId || p.id in locked) "Keeper" else " ",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (p.id == keeperId || p.id in locked)
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
