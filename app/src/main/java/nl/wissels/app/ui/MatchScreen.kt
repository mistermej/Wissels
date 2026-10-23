package nl.wissels.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import nl.wissels.app.MatchViewModel
import nl.wissels.app.model.CardType
import nl.wissels.app.model.Half
import nl.wissels.app.model.MatchState
import nl.wissels.app.model.Player
import nl.wissels.app.model.SubPlan
import nl.wissels.app.engine.RotationEngine

@Composable
fun MatchScreen(state: nl.wissels.app.UiState, vm: MatchViewModel) {
    val m = state.match ?: return
    var showCardDialog by remember { mutableStateOf(false) }
    var showManualSub by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ------------------------------------------------------- klok + stand
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val halfLabel = if (m.finished) "Wedstrijd voorbij"
                        else if (m.atHalfTime) "Rust"
                        else if (m.half == Half.FIRST) "1e helft"
                        else "2e helft"
                        Text(halfLabel, fontWeight = FontWeight.Bold)
                        if (m.timeoutLeft != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "TIME-OUT ${MatchViewModel.formatTime(m.timeoutLeft!!)}",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        MatchViewModel.formatTime(m.secondsLeft),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    // stand
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            m.config.homeTeam,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f, fill = false),
                            textAlign = TextAlign.End
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${m.homeScore} – ${m.awayScore}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            m.config.awayTeam,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { vm.goal(true) }, enabled = !m.finished) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.width(16.dp))
                            Text("+ ${m.config.homeTeam}")
                        }
                        FilledTonalButton(onClick = { vm.goal(false) }, enabled = !m.finished) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.width(16.dp))
                            Text("+ ${m.config.awayTeam}")
                        }
                    }
                }
            }

            // ------------------------------------------------- volgende wissel
            if (!m.finished && !m.atHalfTime) {
                Spacer(Modifier.height(8.dp))
                val windows = RotationEngine.windowSeconds(m.config, m.half)
                val nextWindow = windows.firstOrNull { it > m.elapsedInHalf }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                m.timeoutLeft != null -> "Time-out loopt — klok stil"
                                !m.running -> "Wedstrijd gepauzeerd"
                                nextWindow == null -> "Geen wisselmomenten meer deze helft"
                                else -> "Volgende wissel over " +
                                        MatchViewModel.formatTime(nextWindow - m.elapsedInHalf) +
                                        (if (windows.size > 1)
                                            "  (wissel ${windows.indexOf(nextWindow) + 1} van ${windows.size})"
                                        else "")
                            },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // --------------------------------------------------- spelerslijsten
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    Text(
                        "In het veld (${m.onPitch.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(m.config.players.filter { it.id in m.onPitch }) { p ->
                    PlayerLiveRow(
                        player = p,
                        minutes = (m.playedSeconds[p.id] ?: 0) / 60,
                        isKeeper = m.config.keeperOf(m.half)?.id == p.id,
                        cards = m.cards.filter { it.playerId == p.id },
                        enabled = !m.finished
                    )
                }
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Wisselbank",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                val bench = m.config.participants.filter { it.id !in m.onPitch }
                if (bench.isEmpty()) {
                    item {
                        Text(
                            if (m.config.substitutes(m.half) == 0) "Geen wisselspelers"
                            else "Iedereen staat in het veld",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(bench) { p ->
                    PlayerLiveRow(
                        player = p,
                        minutes = (m.playedSeconds[p.id] ?: 0) / 60,
                        isKeeper = false,
                        cards = m.cards.filter { it.playerId == p.id },
                        enabled = !m.finished,
                        benched = true
                    )
                }
            }

            // ------------------------------------------------------- acties
            Spacer(Modifier.height(8.dp))
            if (m.finished) {
                Button(
                    onClick = { vm.goToSummary() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("Wedstrijdoverzicht bekijken")
                }
            } else if (m.atHalfTime) {
                Button(
                    onClick = { vm.startSecondHalf() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text("2e helft starten")
                }
                OutlinedButton(
                    onClick = { showFinishDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text("Wedstrijd vroegtijdig beëindigen")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.toggleRun() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (m.running) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (m.running) "Pauze" else "Start")
                    }
                    if (m.config.timeoutEnabled) {
                        FilledTonalButton(
                            onClick = { vm.toggleTimeout() },
                            enabled = m.timeoutLeft == null && !m.timeoutUsedThisHalf,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (m.timeoutUsedThisHalf) "T/o gebruikt" else "Time-out")
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(
                        onClick = { showManualSub = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Wissel") }
                    OutlinedButton(
                        onClick = { showCardDialog = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Kaart") }
                    OutlinedButton(
                        onClick = { showFinishDialog = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Einde") }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ------------------------------------------------ wisselvoorstel popup
        m.pending?.let { plan ->
            PendingSubCard(
                plan = plan,
                match = m,
                onConfirm = { vm.confirmPending() },
                onDismiss = { vm.dismissPending() }
            )
        }
    }

    if (showCardDialog) {
        CardDialog(
            match = m,
            onGive = { id, type ->
                vm.giveCard(id, type)
                showCardDialog = false
            },
            onDismiss = { showCardDialog = false }
        )
    }

    if (showManualSub) {
        ManualSubDialog(
            match = m,
            onConfirm = { off, on ->
                vm.manualSub(off, on)
                showManualSub = false
            },
            onDismiss = { showManualSub = false }
        )
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Wedstrijd beëindigen?") },
            text = { Text("De klok stopt en je gaat naar het overzicht. Dit kan niet ongedaan worden gemaakt.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.finishMatch()
                    showFinishDialog = false
                }) { Text("Beëindigen") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("Doorgaan") }
            }
        )
    }
}

@Composable
private fun PlayerLiveRow(
    player: Player,
    minutes: Int,
    isKeeper: Boolean,
    cards: List<nl.wissels.app.model.PlayerCard>,
    enabled: Boolean,
    benched: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (benched) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(player.label, fontWeight = FontWeight.SemiBold)
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
                Text(
                    "$minutes min gespeeld",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            cards.forEach { c ->
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(18.dp)
                        .background(
                            color = if (c.type == CardType.GEEL) Color(0xFFFFC107) else Color(0xFFD32F2F),
                            shape = RoundedCornerShape(3.dp)
                        )
                )
            }
        }
    }
}

@Composable
private fun PendingSubCard(
    plan: SubPlan,
    match: MatchState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
        title = { Text("Wisselmoment!", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlayerPillList(
                    title = "Erin komen",
                    ids = plan.onIds,
                    match = match,
                    color = MaterialTheme.colorScheme.primary
                )
                PlayerPillList(
                    title = "Eruit gaan",
                    ids = plan.offIds,
                    match = match,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Wissel uitgevoerd")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Overslaan") }
        }
    )
}

@Composable
private fun PlayerPillList(
    title: String,
    ids: List<Long>,
    match: MatchState,
    color: Color
) {
    if (ids.isEmpty()) return
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            fontWeight = FontWeight.Bold
        )
        ids.forEach { id ->
            val p = match.config.players.firstOrNull { it.id == id }
            Text("• ${p?.label ?: "?"}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CardDialog(
    match: MatchState,
    onGive: (Long, CardType) -> Unit,
    onDismiss: () -> Unit
) {
    var selected by remember { mutableStateOf<Long?>(null) }
    val players = match.config.participants
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kaart geven") },
        text = {
            Column {
                Text("Kies een speler:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(players) { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { selected = p.id }, modifier = Modifier.weight(1f)) {
                                Text(
                                    p.label,
                                    textAlign = TextAlign.Start,
                                    fontWeight = if (selected == p.id) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            if (selected == p.id) {
                                Icon(Icons.Default.Check, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = { selected?.let { onGive(it, CardType.GEEL) } },
                    enabled = selected != null,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF9A6700))
                ) { Text("Geel") }
                TextButton(
                    onClick = { selected?.let { onGive(it, CardType.ROOD) } },
                    enabled = selected != null,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Rood") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}

@Composable
private fun ManualSubDialog(
    match: MatchState,
    onConfirm: (List<Long>, List<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    val keeperId = match.config.keeperOf(match.half)?.id
    val onField = match.config.participants.filter { it.id in match.onPitch && it.id != keeperId }
    val bench = match.config.participants.filter { it.id !in match.onPitch }
    var offSelected by remember { mutableStateOf(setOf<Long>()) }
    var onSelected by remember { mutableStateOf(setOf<Long>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Handmatige wissel") },
        text = {
            Column {
                Text(
                    "Kies wie eraf gaat en wie erin komt (gelijke aantallen).",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    item {
                        Text("Eruit", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    items(onField) { p ->
                        SelectableRow(
                            label = p.label,
                            checked = p.id in offSelected,
                            onToggle = {
                                offSelected = if (p.id in offSelected) offSelected - p.id else offSelected + p.id
                            }
                        )
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Erin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    items(bench) { p ->
                        SelectableRow(
                            label = p.label,
                            checked = p.id in onSelected,
                            onToggle = {
                                onSelected = if (p.id in onSelected) onSelected - p.id else onSelected + p.id
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(offSelected.toList(), onSelected.toList()) },
                enabled = offSelected.isNotEmpty() && offSelected.size == onSelected.size
            ) { Text("Wissel doorvoeren") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuleren") }
        }
    )
}

@Composable
private fun SelectableRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(label, modifier = Modifier.weight(1f))
    }
}
