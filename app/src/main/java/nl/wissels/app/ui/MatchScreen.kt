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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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

import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalConfiguration
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

    // Responsive afmetingen: knop-hoogte, score-breedte en padding schalen
    // met de schermresolutie in plaats van vaste dp-waarden te zijn.
    val screenH = LocalConfiguration.current.screenHeightDp
    val btnH = ResponsiveSizes.actionButtonHeight(screenH)
    val pad = ResponsiveSizes.sectionPadding(screenH)

    if (m == null) {
        EmptyState(
            title = "Geen wedstrijd actief",
            message = "Er wordt op dit moment geen wedstrijd gespeeld.\n" +
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
    var manualSubInitialMode by remember { mutableStateOf(ManualSubMode.WISSELS) }
    var showParticipationDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(pad))

            // ------------------------------------------------------- klok + stand
            // Compact scorebord: klok en helft op één regel, daaronder de stand.
            // Zo blijft er meer ruimte over voor de veld- en wisselspelers.
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = pad, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val halfLabel = if (m.finished) "Wedstrijd voorbij"
                        else if (m.atHalfTime) "Rust"
                        else if (m.half == Half.FIRST) "1e helft"
                        else "2e helft"
                        Text(halfLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (m.timeoutActive) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "TIME-OUT",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            MatchViewModel.formatTime(m.secondsLeft),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    // stand + doelpunten: symmetrisch en verticaal gecentreerd
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
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
                            style = MaterialTheme.typography.titleMedium,
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

            // ------------------------------------------------- speelstatus gecentreerd
            if (!m.finished && !m.atHalfTime) {
                Spacer(Modifier.height(8.dp))
                val statusText = when {
                    m.timeoutActive -> "Time-out loopt"
                    m.running -> "Wedstrijd loopt"
                    else -> "Wedstrijd gepauzeerd"
                }
                Text(
                    statusText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (m.running) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // ------------------------------------------------- dropdowns boven de wissel
            if (!m.finished && !m.atHalfTime) {
                Spacer(Modifier.height(8.dp))
                val benchNow = m.config.participants.count {
                    it.id !in m.onPitch && it.id != m.config.keeperOf(m.half)?.id
                }
                val maxPerWindow = minOf(
                    benchNow,
                    (m.onPitch.size - 1).coerceAtLeast(0)
                ).coerceAtMost(8)
                // De veldgrootte volgt de opstelling (Startopstelling en
                // "Handmatige wissels + Opstelling"); hier stel je alleen in
                // hoeveel spelers er per wisselmoment tegelijk wisselen.
                IntDropdown(
                    label = "Wissels per moment",
                    value = m.config.subsPerWindow,
                    options = listOf(0) + (1..maxPerWindow).toList(),
                    suffix = {
                        when {
                            maxPerWindow == 0 -> "Geen wissels"
                            it == 0 -> "Alle tegelijk"
                            it == 1 -> "1 speler"
                            else -> "$it spelers"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { v -> vm.setMatchSubsPerWindow(v) }
            }

            // ------------------------------------------------- volgende wissel
            if (!m.finished && !m.atHalfTime) {
                Spacer(Modifier.height(8.dp))
                val windows = RotationEngine.windowSeconds(m.config, m.half)
                val nextWindow = windows.firstOrNull { it > m.elapsedInHalf }
                val preview = if (nextWindow != null && m.pending == null) RotationEngine.plan(m) else null
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    m.timeoutActive -> "Time-out loopt — klok staat stil"
                                    nextWindow == null -> "Geen wisselmomenten meer deze helft"
                                    else -> "Volgende wissel over " +
                                            MatchViewModel.formatTime(nextWindow - m.elapsedInHalf) +
                                            (if (windows.size > 1)
                                                "  (wissel ${windows.indexOf(nextWindow) + 1} van ${windows.size})"
                                            else "")
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }

                        if (preview != null && (preview.onIds.isNotEmpty() || preview.offIds.isNotEmpty())) {
                            val shown = m.previewPlan
                                ?.takeIf { RotationEngine.isValidPlan(m, it) } ?: preview
                            PreviewPairs(plan = shown, match = m) { adjusted ->
                                vm.setPreviewPlan(adjusted)
                            }
                        }
                    }
                }
            }

            // --------------------------------------------------- spelerslijsten
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (m.atHalfTime) {
                    // Rust: opstelling 2e helft verifiëren (keeper) en aanpassen.
                    item { HalfTimeSection(match = m, vm = vm) }
                } else {
                    val keeperId = m.config.keeperOf(m.half)?.id
                    val onPitch = m.config.players.filter { it.id in m.onPitch }
                        .sortedWith(compareBy<Player> { it.id != keeperId }.thenBy { it.label.lowercase() })
                    val bench = m.config.participants.filter { it.id !in m.onPitch }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "In het veld (${onPitch.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = {
                                manualSubInitialMode = ManualSubMode.OPSTELLING
                                showManualSub = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Opstelling aanpassen")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        PlayerGrid(
                            players = onPitch,
                            minutes = m.playedSeconds,
                            keeperId = keeperId,
                            cards = m.cards,
                            enabled = !m.finished
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Wisselbank (${bench.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = { showParticipationDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Deelnemende spelers aanpassen")
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        if (bench.isEmpty()) {
                            Text(
                                if (m.config.substitutes(m.half) == 0) "Geen wisselspelers"
                                else "Iedereen staat in het veld",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            PlayerGrid(
                                players = bench,
                                minutes = m.playedSeconds,
                                keeperId = null,
                                cards = m.cards,
                                enabled = !m.finished,
                                benched = true
                            )
                        }
                    }
                }
            }

            // ------------------------------------------------------- acties
            Spacer(Modifier.height(8.dp))
            if (m.finished) {
                Button(
                    onClick = { navController.navigate(Screen.SUMMARY.route) },
                    modifier = Modifier.fillMaxWidth().height(btnH).padding(vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Wedstrijdoverzicht bekijken")
                }
            } else if (m.atHalfTime) {
                Button(
                    onClick = { vm.startSecondHalf() },
                    enabled = m.halfTimeKeeperId != null,
                    modifier = Modifier.fillMaxWidth().height(btnH).padding(vertical = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("2e helft starten")
                }
                OutlinedButton(
                    onClick = { showFinishDialog = true },
                    modifier = Modifier.fillMaxWidth().height(btnH).padding(bottom = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Wedstrijd vroegtijdig beëindigen")
                }
            } else {
                // Start/Pauze + optioneel Time-out
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { vm.toggleRun() },
                        enabled = !m.timeoutActive,
                        modifier = Modifier.weight(1f).height(btnH),
                        shape = MaterialTheme.shapes.medium
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
                            modifier = Modifier.weight(1f).height(btnH),
                            shape = MaterialTheme.shapes.medium
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
                // Handmatige wissels + opstelling aanpassen + Kaart + Einde
                OutlinedButton(
                    onClick = { showManualSub = true },
                    modifier = Modifier.fillMaxWidth().height(btnH).padding(top = 8.dp),
                    shape = MaterialTheme.shapes.medium
                ) { Text("Handmatige wissels + Opstelling") }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    if (m.config.cardsEnabled) {
                        OutlinedButton(
                            onClick = { showCardDialog = true },
                            modifier = Modifier.weight(1f).height(btnH),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("Kaart") }
                    }
                    OutlinedButton(
                        onClick = { showFinishDialog = true },
                        modifier = Modifier.weight(1f).height(btnH),
                        shape = MaterialTheme.shapes.medium
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
                onConfirm = { adjusted -> vm.confirmPending(adjusted) },
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
            initialMode = manualSubInitialMode,
            onKeeperChange = { id -> vm.setMatchKeeper(id) },
            onConfirm = { off, on ->
                vm.manualSub(off, on)
                showManualSub = false
            },
            onLineupChange = { ids -> vm.setMatchLineup(ids) },
            onDismiss = { showManualSub = false }
        )
    }

    if (showParticipationDialog) {
        ParticipationDialog(
            match = m,
            onKeeperChange = { id -> vm.setMatchKeeper(id) },
            onToggle = { id -> vm.toggleMatchParticipation(id) },
            onDismiss = { showParticipationDialog = false }
        )
    }

    if (showFinishDialog) {
        // In de 1e helft kun je kiezen: alleen de helft afronden (naar de
        // rust) of meteen de hele wedstrijd stoppen.
        val firstHalf = m.half == Half.FIRST && !m.atHalfTime
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = {
                Text(if (firstHalf) "Eerste helft beëindigen?" else "Wedstrijd beëindigen?")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (firstHalf) {
                        Text(
                            "Ga naar de rust of stop meteen de hele wedstrijd.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                vm.endFirstHalf()
                                showFinishDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("Eerste helft beëindigen") }
                        OutlinedButton(
                            onClick = {
                                vm.finishMatch()
                                showFinishDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Hele wedstrijd beëindigen") }
                    } else {
                        Text("De klok stopt en je gaat naar het overzicht. Dit kan niet ongedaan worden gemaakt.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!firstHalf) vm.finishMatch()
                    showFinishDialog = false
                }) { Text(if (firstHalf) "Annuleren" else "Beëindigen") }
            },
            dismissButton = {
                if (!firstHalf) {
                    TextButton(onClick = { showFinishDialog = false }) { Text("Doorgaan") }
                }
            }
        )
    }
}

/**
 * Eén zijde van het compacte scorebord: [−] score/naam [+]. Alles netjes
 * gecentreerd en een doelpunt is ook weer weg te halen.
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
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onMinus,
            enabled = enabled && score > 0,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.Remove,
                contentDescription = "Doelpunt van $name weghalen",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "$score",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        IconButton(
            onClick = onPlus,
            enabled = enabled,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Doelpunt voor $name",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * "Klaarzetten voor de wissel": per wissel één rij met twee keuzelijsten
 * NAAST elkaar — links wie erin komt, rechts wie eraf gaat. De coach mag ze
 * hier alvast aanpassen; [onChange] levert het aangepaste plan op.
 */
@Composable
private fun PreviewPairs(
    plan: SubPlan,
    match: MatchState,
    onChange: (SubPlan) -> Unit
) {
    val keeperId = match.config.keeperOf(match.half)?.id
    val onField = match.config.participants.filter { it.id in match.onPitch && it.id != keeperId }
    val bench = match.config.participants.filter { it.id !in match.onPitch }
    val pairs = plan.onIds.zip(plan.offIds)
    if (pairs.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        pairs.forEachIndexed { index, (onId, offId) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                PlayerDropdown(
                    label = "In ↑",
                    labelColor = Color(0xFF2E7D32),
                    value = onId,
                    players = bench.filter { p -> p.id == onId || pairs.none { it.first == p.id } },
                    emptyLabel = "Kies…",
                    modifier = Modifier.weight(1f)
                ) { id ->
                    if (id != null) {
                        val changed = pairs.toMutableList().also { it[index] = id to offId }
                        onChange(SubPlan(changed.map { it.second!! }, changed.map { it.first!! }))
                    }
                }
                Spacer(Modifier.width(8.dp))
                PlayerDropdown(
                    label = "Uit ↓",
                    labelColor = Color(0xFFC62828),
                    value = offId,
                    players = onField.filter { p -> p.id == offId || pairs.none { it.second == p.id } },
                    emptyLabel = "Kies…",
                    modifier = Modifier.weight(1f)
                ) { id ->
                    if (id != null) {
                        val changed = pairs.toMutableList().also { it[index] = onId to id }
                        onChange(SubPlan(changed.map { it.second!! }, changed.map { it.first!! }))
                    }
                }
            }
        }
    }
}

/**
 * Spelers als tegels in een grid dat zich aanpast aan het schermformaat:
 * 3 kolommen op ruime schermen, 2 kolommen op kleinere (bv. bij schermen
 * kleiner dan ~360dp breed). Kolommen blijven in gelijke breedte.
 */
@Composable
private fun PlayerGrid(
    players: List<Player>,
    minutes: Map<Long, Int>,
    keeperId: Long?,
    cards: List<nl.wissels.app.model.PlayerCard>,
    enabled: Boolean,
    benched: Boolean = false,
    selected: Set<Long> = emptySet(),
    onToggle: ((Long) -> Unit)? = null
) {
    val columns = ResponsiveSizes.gridColumns(LocalConfiguration.current.screenWidthDp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        players.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { p ->
                    Box(modifier = Modifier.weight(1f)) {
                        PlayerTile(
                            player = p,
                            minutes = (minutes[p.id] ?: 0) / 60,
                            isKeeper = p.id == keeperId,
                            cards = cards.filter { it.playerId == p.id },
                            benched = benched,
                            selected = p.id in selected,
                            onClick = if (enabled && onToggle != null) ({ onToggle(p.id) }) else null
                        )
                    }
                }
                // Vul ontbrekende plekken in de laatste rij op zodat kolommen gelijk zijn.
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Eén tegel in het spelersgrid: naam, speeltijd, keeper-label en kaarten. */
@Composable
private fun PlayerTile(
    player: Player,
    minutes: Int,
    isKeeper: Boolean,
    cards: List<nl.wissels.app.model.PlayerCard>,
    benched: Boolean,
    selected: Boolean,
    onClick: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick ?: {},
        enabled = onClick != null,
        colors = CardDefaults.cardColors(
            // In het veld = licht groen, op de bank = wit (was grijs).
            containerColor = if (benched) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.secondaryContainer
        ),
        // Selectie valt op door een dikkere, donkergroene rand.
        border = greenBorder(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    player.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    modifier = Modifier.weight(1f)
                )
                cards.forEach { c ->
                    Spacer(Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(16.dp)
                            .background(
                                color = if (c.type == CardType.GEEL) Color(0xFFFFC107) else Color(0xFFD32F2F),
                                shape = MaterialTheme.shapes.extraSmall
                            )
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$minutes min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (isKeeper) {
                    Text(
                        "KEEPER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Rust: de opstelling van de 2e helft. De keeper staat hier als keuzelijst
 * (verifiëren of wijzigen) en door op spelers te tikken haal je er één van
 * het veld en zet je er één van de bank in — vóór "2e helft starten".
 */
@Composable
private fun HalfTimeSection(match: MatchState, vm: MatchViewModel) {
    var offSel by remember { mutableStateOf<Long?>(null) }
    var onSel by remember { mutableStateOf<Long?>(null) }
    val onIds = match.halfTimeOnPitch ?: emptySet()
    val keeperId = match.halfTimeKeeperId
    val onPitch = match.config.players.filter { it.id in onIds }
    val bench = match.config.participants.filter { it.id !in onIds }
    val nameOf: (Long?) -> String =
        { id -> match.config.players.firstOrNull { it.id == id }?.label ?: "?" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Opstelling 2e helft",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Controleer de keeper en pas de opstelling aan waar nodig.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PlayerDropdown(
                label = "Keeper 2e helft",
                value = keeperId,
                players = match.config.participants,
                emptyLabel = "Kies de keeper"
            ) { id ->
                vm.setHalfTimeKeeper(id)
                offSel = null
                onSel = null
            }

            Text(
                "In het veld (${onPitch.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            PlayerGrid(
                players = onPitch,
                minutes = match.playedSeconds,
                keeperId = keeperId,
                cards = match.cards,
                enabled = true,
                selected = setOfNotNull(offSel),
                onToggle = { id ->
                    if (id != keeperId) {
                        offSel = if (offSel == id) null else id
                        onSel = null
                    }
                }
            )

            Text(
                "Wisselbank (${bench.size})",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            if (bench.isEmpty()) {
                Text(
                    "Geen wisselspelers op de bank.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                PlayerGrid(
                    players = bench,
                    minutes = match.playedSeconds,
                    keeperId = null,
                    cards = match.cards,
                    enabled = true,
                    benched = true,
                    selected = setOfNotNull(onSel),
                    onToggle = { id ->
                        onSel = if (onSel == id) null else id
                    }
                )
            }

            if (offSel != null && onSel != null) {
                Button(
                    onClick = {
                        vm.swapHalfTime(offSel!!, onSel!!)
                        offSel = null
                        onSel = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("${nameOf(onSel)} erin — ${nameOf(offSel)} eruit")
                }
            } else {
                Text(
                    "Tik op een veldspeler om hem eruit te halen en op een " +
                            "bankspeler om hem erin te zetten.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Wisselmoment (automatisch voorstel): de paren staan als keuzelijstjes —
 * links de speler die erin komt, rechts de speler die eruit gaat. De coach
 * kan ze nog aanpassen (of een wissel toevoegen) vóór "Wissel(s) uitgevoerd".
 */
@Composable
private fun PendingSubCard(
    plan: SubPlan,
    match: MatchState,
    onConfirm: (SubPlan) -> Unit,
    onDismiss: () -> Unit
) {
    var pairs by remember(plan) {
        mutableStateOf<List<Pair<Long?, Long?>>>(
            plan.onIds.zip(plan.offIds) { on, off -> on to off }
        )
    }
    val keeperId = match.config.keeperOf(match.half)?.id
    val onField = match.config.participants.filter { it.id in match.onPitch && it.id != keeperId }
    val bench = match.config.participants.filter { it.id !in match.onPitch }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
        title = { Text("Wisselmoment!", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.5f).dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Controleer de wissel(s): links erin, rechts eruit. " +
                            "Je kunt ze hier nog aanpassen.",
                    style = MaterialTheme.typography.bodySmall
                )
                SubPairList(
                    pairs = pairs,
                    inOptions = bench,
                    outOptions = onField,
                    onChange = { pairs = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(SubPlan(pairs.map { it.second!! }, pairs.map { it.first!! }))
                },
                enabled = pairs.isNotEmpty() && pairs.all { it.first != null && it.second != null },
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Wissel(s) uitgevoerd")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Overslaan") }
        }
    )
}

/**
 * Lijst van wissel-paren met keuzelijstjes: links de speler die erin komt,
 * rechts de speler die eruit gaat. Meerdere wissels tegelijk kunnen; een
 * speler kan maar in één paar voorkomen.
 */
@Composable
private fun SubPairList(
    pairs: List<Pair<Long?, Long?>>,
    inOptions: List<Player>,
    outOptions: List<Player>,
    onChange: (List<Pair<Long?, Long?>>) -> Unit
) {
    pairs.forEachIndexed { index, (onId, offId) ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            PlayerDropdown(
                label = "In ↑",
                labelColor = Color(0xFF2E7D32),
                value = onId,
                players = inOptions.filter { p -> p.id == onId || pairs.none { it.first == p.id } },
                emptyLabel = "Kies…",
                modifier = Modifier.weight(1f)
            ) { id ->
                onChange(pairs.toMutableList().also { it[index] = id to offId })
            }
            Spacer(Modifier.width(6.dp))
            PlayerDropdown(
                label = "Uit ↓",
                labelColor = Color(0xFFC62828),
                value = offId,
                players = outOptions.filter { p -> p.id == offId || pairs.none { it.second == p.id } },
                emptyLabel = "Kies…",
                modifier = Modifier.weight(1f)
            ) { id ->
                onChange(pairs.toMutableList().also { it[index] = onId to id })
            }
            if (pairs.size > 1) {
                IconButton(onClick = { onChange(pairs.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(Icons.Default.Close, contentDescription = "Deze wissel weghalen")
                }
            }
        }
    }
    val freeIn = inOptions.count { p -> pairs.none { it.first == p.id } }
    val freeOut = outOptions.count { p -> pairs.none { it.second == p.id } }
    TextButton(
        onClick = { onChange(pairs + (null to null)) },
        enabled = freeIn > 0 && freeOut > 0
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(4.dp))
        Text("Nog een wissel")
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
                LazyColumn(modifier = Modifier.height((LocalConfiguration.current.screenHeightDp * 0.4f).dp)) {
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

/** Twee modi in de popup: wisselen of de veldbezetting aanpassen. */
private enum class ManualSubMode { WISSELS, OPSTELLING }

/**
 * Handmatige wissels + opstelling: paren van keuzelijstjes (links erin, rechts
 * eruit) én een stand waarin de coach de veldspelers/opstelling direct kan
 * aanpassen. De keeper van deze helft zit óók hier (onder de knop
 * "Handmatige wissels + Opstelling"), niet in het hoofdscherm.
 */
@Composable
private fun ManualSubDialog(
    match: MatchState,
    initialMode: ManualSubMode = ManualSubMode.WISSELS,
    onKeeperChange: (Long?) -> Unit,
    onConfirm: (List<Long>, List<Long>) -> Unit,
    onLineupChange: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    // Sla de originele keeper op bij het openen van het dialoog zodat we
    // de wijziging kunnen terugdraaien als de coach op "Annuleren" drukt.
    val originalKeeperId = remember { match.config.keeperOf(match.half)?.id }
    val currentKeeperId = match.config.keeperOf(match.half)?.id

    val onField = match.config.participants.filter { it.id in match.onPitch }
    val bench = match.config.participants.filter { it.id !in match.onPitch }
    var mode by remember { mutableStateOf(initialMode) }
    var pairs by remember { mutableStateOf(listOf<Pair<Long?, Long?>>(null to null)) }
    // Huidige veldspelers (zonder keeper) als beginpunt voor de opstelling.
    var lineup by remember {
        mutableStateOf(
            match.config.participants
                .filter { it.id in match.onPitch && it.id != currentKeeperId }
                .map { it.id }
                .toSet()
        )
    }

    val keeperChanged = currentKeeperId != originalKeeperId
    val pairsComplete = pairs.isNotEmpty() && pairs.all { it.first != null && it.second != null }
    // Als de keeper in de "uit"-lijst staat, moet er een nieuwe keeper gekozen
    // worden voordat de wissel doorgevoerd kan worden.
    val keeperBeingSubbed = pairsComplete && pairs.any { it.second == currentKeeperId }
    val keeperNeedsReplacement = keeperBeingSubbed && !keeperChanged
    val availableForField = match.config.participants
        .filter { it.id !in match.redCards && it.id != currentKeeperId }
    val chosenOutfield = (lineup - setOfNotNull(currentKeeperId))
        .filter { id -> availableForField.any { it.id == id } }
        .toSet()

    // Label en enabled-toestand van de bevestigingsknop:
    // - alleen keeper gewijzigd  → "Keeper wisselen" (altijd enabled)
    // - spelers + eventueel keeper → "Wissel doorvoeren"
    val confirmLabel = when {
        keeperNeedsReplacement -> "Kies eerst de nieuwe keeper"
        keeperChanged && !pairsComplete -> "Keeper wisselen"
        else -> "Wissel doorvoeren"
    }
    val confirmEnabled = (keeperChanged || pairsComplete) && !keeperNeedsReplacement

    AlertDialog(
        onDismissRequest = {
            // Sluit dialoog zonder wijzigingen: keeper terugzetten als die
            // al aangepast was voordat de coach op "Annuleren" drukte.
            if (keeperChanged) onKeeperChange(originalKeeperId)
            onDismiss()
        },
        title = { Text("Handmatige wissels + Opstelling") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.5f).dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (mode == ManualSubMode.WISSELS) {
                        Button(
                            onClick = { mode = ManualSubMode.WISSELS },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("Wissels") }
                    } else {
                        OutlinedButton(
                            onClick = { mode = ManualSubMode.WISSELS },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("Wissels") }
                    }
                    if (mode == ManualSubMode.OPSTELLING) {
                        Button(
                            onClick = { mode = ManualSubMode.OPSTELLING },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("Opstelling") }
                    } else {
                        OutlinedButton(
                            onClick = { mode = ManualSubMode.OPSTELLING },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("Opstelling") }
                    }
                }

                if (mode == ManualSubMode.WISSELS) {
                    Text(
                        "Links de speler die erin komt, rechts de speler die eraf " +
                                "gaat. Meerdere wissels tegelijk kan.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    // Keeper van deze helft onderweg veranderen: alleen spelers
                    // die in het veld staan kunnen keeper worden.
                    if (!match.finished && currentKeeperId != null) {
                        PlayerDropdown(
                            label = "Keeper",
                            value = currentKeeperId,
                            players = onField,
                            emptyLabel = "Kies de keeper",
                            modifier = Modifier.fillMaxWidth()
                        ) { id -> onKeeperChange(id) }
                    }
                    if (keeperBeingSubbed) {
                        Text(
                            "De keeper wordt gewisseld — kies hierboven de nieuwe keeper.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    SubPairList(
                        pairs = pairs,
                        inOptions = bench,
                        outOptions = onField,
                        onChange = { pairs = it }
                    )
                } else {
                    Text(
                        "Vink de spelers aan die in het veld staan. De keeper staat " +
                                "er altijd bij; de rest komt op de bank. Het aantal " +
                                "aangevinkte spelers bepaalt de veldgrootte.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    // De keeper van deze helft staat vast in het veld; de overige
                    // spelers kies je in een grid (compact, zodat iedereen in
                    // beeld past).
                    var showKeeperReplace by remember { mutableStateOf(false) }
                    PlayerSelectionGrid(
                        players = listOfNotNull(onField.firstOrNull { it.id == currentKeeperId }) + availableForField,
                        selected = chosenOutfield + setOfNotNull(currentKeeperId),
                        onToggle = { id ->
                            if (id == currentKeeperId) {
                                showKeeperReplace = true
                            } else {
                                lineup = if (id in chosenOutfield) lineup - id else lineup + id
                            }
                        },
                        locked = emptySet(),
                        keeperId = currentKeeperId,
                        enabled = true
                    )
                    if (showKeeperReplace) {
                        val candidates = chosenOutfield.mapNotNull { id ->
                            match.config.players.firstOrNull { it.id == id }
                        }
                        AlertDialog(
                            onDismissRequest = { showKeeperReplace = false },
                            title = { Text("Nieuwe keeper kiezen") },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        "Kies wie de nieuwe keeper wordt. De huidige keeper " +
                                                "gaat naar de bank.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    PlayerSelectionGrid(
                                        players = candidates,
                                        selected = emptySet(),
                                        onToggle = { newId ->
                                            onKeeperChange(newId)
                                            lineup = lineup - newId
                                            showKeeperReplace = false
                                        },
                                        showCheckbox = false
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
                    Text(
                        "${chosenOutfield.size} veldspeler(s) + keeper = " +
                                "${chosenOutfield.size + 1} in het veld",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (chosenOutfield.isEmpty()) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                    if (chosenOutfield.isEmpty()) {
                        Text(
                            "Kies minstens één veldspeler.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when (mode) {
                        ManualSubMode.WISSELS -> {
                            // Alleen keeper gewisseld: geen spelers doorvoeren.
                            if (pairsComplete) {
                                onConfirm(pairs.map { it.second!! }, pairs.map { it.first!! })
                            } else {
                                onDismiss()
                            }
                        }
                        ManualSubMode.OPSTELLING -> {
                            onLineupChange(chosenOutfield)
                            onDismiss()
                        }
                    }
                },
                enabled = when (mode) {
                    ManualSubMode.WISSELS -> confirmEnabled
                    ManualSubMode.OPSTELLING -> chosenOutfield.isNotEmpty()
                }
            ) {
                Text(
                    when (mode) {
                        ManualSubMode.WISSELS -> confirmLabel
                        ManualSubMode.OPSTELLING -> "Opstelling opslaan"
                    }
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                // Annuleren: keeper terugzetten als die al gewijzigd was.
                if (keeperChanged) onKeeperChange(originalKeeperId)
                onDismiss()
            }) { Text("Annuleren") }
        }
    )
}

/**
 * Popup om de deelnemende spelers tijdens de wedstrijd aan te passen (bv. bij
 * een blessure). Spelers die je uitvinkt verdwijnen uit de rotatie; de keeper
 * blijft altijd deelnemen.
 */
@Composable
private fun ParticipationDialog(
    match: MatchState,
    onKeeperChange: (Long?) -> Unit,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val keeperId = match.config.keeperOf(match.half)?.id
    val participating = match.config.participants.map { it.id }.toSet()
    var showKeeperReplace by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deelnemende spelers") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.6f).dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Vink uit wie niet meer meedoet (blessure e.d.). De keeper " +
                            "kun je ook wisselen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PlayerSelectionGrid(
                    players = match.config.players.sortedBy { it.id },
                    selected = participating,
                    onToggle = { id ->
                        if (id == keeperId) {
                            showKeeperReplace = true
                        } else {
                            onToggle(id)
                        }
                    },
                    locked = emptySet(),
                    keeperId = keeperId
                )
                if (showKeeperReplace) {
                    val candidates = participating.mapNotNull { id ->
                        match.config.players.firstOrNull { it.id == id && id != keeperId }
                    }
                    AlertDialog(
                        onDismissRequest = { showKeeperReplace = false },
                        title = { Text("Nieuwe keeper kiezen") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Kies wie de nieuwe keeper wordt. De huidige keeper " +
                                            "stopt met deelnemen.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                PlayerSelectionGrid(
                                    players = candidates,
                                    selected = emptySet(),
                                    onToggle = { newId ->
                                        val oldKeeper = keeperId
                                        onKeeperChange(newId)
                                        if (oldKeeper != null) onToggle(oldKeeper)
                                        showKeeperReplace = false
                                    },
                                    showCheckbox = false
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Klaar") }
        }
    )
}
