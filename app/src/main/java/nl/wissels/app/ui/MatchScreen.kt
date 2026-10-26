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
import androidx.compose.material.icons.filled.Remove
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
import androidx.navigation.NavController
import nl.wissels.app.MatchViewModel
import nl.wissels.app.model.CardType
import nl.wissels.app.model.Half
import nl.wissels.app.model.MatchState
import nl.wissels.app.model.Player
import nl.wissels.app.model.Screen
import nl.wissels.app.model.SubPlan
import nl.wissels.app.engine.RotationEngine

@Composable
fun MatchScreen(state: nl.wissels.app.UiState, vm: MatchViewModel, navController: NavController) {
    val m = state.match
    if (m == null) {
        EmptyState(
            title = "Geen wedstrijd actief",
            message = "Er wordt op dit moment geen wedstrijd gespeeld. " +
                    "Start de voorbereiding om een nieuwe wedstrijd te beginnen.",
            actionLabel = "Start Voorbereiding",
            onAction = {
                vm.startPreparation()
                navController.navigate(Screen.PREPARE.route)
            }
        )
        return
    }
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
                        if (m.timeoutActive) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "TIME-OUT",
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
                    // stand + doelpunten: symmetrisch en verticaal gecentreerd
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ScoreSide(
                            name = m.config.homeTeam,
                            score = m.homeScore,
                            enabled = !m.finished,
                            onPlus = { vm.goal(true) },
                            onMinus = { vm.removeGoal(true) },
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "–",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        ScoreSide(
                            name = m.config.awayTeam,
                            score = m.awayScore,
                            enabled = !m.finished,
                            onPlus = { vm.goal(false) },
                            onMinus = { vm.removeGoal(false) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ------------------------------------------------- volgende wissel
            if (!m.finished && !m.atHalfTime) {
                Spacer(Modifier.height(8.dp))
                val windows = RotationEngine.windowSeconds(m.config, m.half)
                val nextWindow = windows.firstOrNull { it > m.elapsedInHalf }
                val preview = if (nextWindow != null && m.pending == null) RotationEngine.plan(m) else null
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    m.timeoutActive -> "Time-out loopt — klok stil"
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

                        // wie moet zich al klaarzetten?
                        if (preview != null && (preview.onIds.isNotEmpty() || preview.offIds.isNotEmpty())) {
                            Text(
                                "Klaarzetten voor de wissel",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            PreviewLine(
                                label = "erin",
                                ids = preview.onIds,
                                match = m,
                                color = MaterialTheme.colorScheme.primary
                            )
                            PreviewLine(
                                label = "uit",
                                ids = preview.offIds,
                                match = m,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Dit kan nog veranderen naarmate de wedstrijd vordert.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // tijdens het spelen nog bijstellen
                        IntDropdown(
                            label = "Wissels per moment",
                            value = m.config.subsPerWindow,
                            options = listOf(0) + (1..8).toList(),
                            suffix = { if (it == 0) "Alle tegelijk" else if (it == 1) "1 speler" else "$it spelers" }
                        ) { v -> vm.setMatchSubsPerWindow(v) }
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
                    onClick = { navController.navigate(Screen.SUMMARY.route) },
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
                        enabled = !m.timeoutActive,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (m.running) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            when {
                                m.timeoutActive -> "Time-out loopt"
                                m.running -> "Pauze"
                                else -> "Start"
                            }
                        )
                    }
                    if (m.config.timeoutEnabled) {
                        FilledTonalButton(
                            onClick = { vm.toggleTimeout() },
                            enabled = m.timeoutActive || !m.timeoutUsedThisHalf,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                when {
                                    m.timeoutActive -> "Einde time-out"
                                    m.timeoutUsedThisHalf -> "Time-out gebruikt"
                                    else -> "Start time-out"
                                }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                    OutlinedButton(
                        onClick = { showManualSub = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("Wissel") }
                    if (m.config.cardsEnabled) {
                        OutlinedButton(
                            onClick = { showCardDialog = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("Kaart") }
                    }
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

/**
 * Eén zijde van het scorebord: naam bovenaan, daaronder [−] stand [+].
 * Alles netjes gecentreerd en een doelpunt is ook weer weg te halen.
 */
@Composable
private fun ScoreSide(
    name: String,
    score: Int,
    enabled: Boolean,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMinus, enabled = enabled && score > 0) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Doelpunt van $name weghalen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "$score",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(48.dp)
            )
            IconButton(onClick = onPlus, enabled = enabled) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Doelpunt voor $name",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Regel in het "klaarzetten voor de wissel"-blok: wie erin/uit gaat. */
@Composable
private fun PreviewLine(
    label: String,
    ids: List<Long>,
    match: MatchState,
    color: Color
) {
    Row {
        Text(
            if (label == "erin") "▲ erin" else "▼ uit",
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(56.dp)
        )
        Text(
            ids.joinToString(", ") { id ->
                match.config.players.firstOrNull { it.id == id }?.label ?: "?"
            },
            style = MaterialTheme.typography.bodyMedium
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
