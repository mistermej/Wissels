package nl.wissels.app.ui

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
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import nl.wissels.app.MatchViewModel
import nl.wissels.app.model.CardType
import nl.wissels.app.model.EventKind
import nl.wissels.app.model.Half
import nl.wissels.app.model.MatchState
import nl.wissels.app.model.Screen

@Composable
fun SummaryScreen(state: nl.wissels.app.UiState, vm: MatchViewModel, navController: NavController) {
    val m = state.match
    if (m == null) {
        EmptyState(
            title = "Nog niets te overzien",
            message = "Zodra er een wedstrijd gespeeld is, zie je hier de stand, " +
                    "de speeltijden, de kaarten en het volledige wedstrijdverloop.",
            actionLabel = "Start Voorbereiding",
            onAction = {
                vm.startPreparation()
                navController.navigate(Screen.PREPARE.route)
            }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Wedstrijdoverzicht",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // stand
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${m.config.homeTeam}  ${m.homeScore} – ${m.awayScore}  ${m.config.awayTeam}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Aantal wissels: ${m.subsDone}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // speeltijd
            item {
                Text("Speeltijd", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            val byMinutes = m.config.participants
                .sortedByDescending { m.playedSeconds[it.id] ?: 0 }
            items(byMinutes) { p ->
                val secs = m.playedSeconds[p.id] ?: 0
                val total = m.config.halfSeconds
                val fraction = if (total > 0) secs.toFloat() / total else 0f
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(p.label, fontWeight = FontWeight.SemiBold)
                            // balkje
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .padding(top = 4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        RoundedCornerShape(4.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                        .height(8.dp)
                                        .background(
                                            if (m.config.keeperOf(Half.FIRST)?.id == p.id ||
                                                m.config.keeperOf(Half.SECOND)?.id == p.id
                                            ) MaterialTheme.colorScheme.tertiary
                                            else MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${secs / 60} min",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // kaarten
            if (m.cards.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Kaarten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(m.cards) { c ->
                    val p = m.config.players.firstOrNull { it.id == c.playerId }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(18.dp)
                                .background(
                                    color = if (c.type == CardType.GEEL) Color(0xFFFFC107) else Color(0xFFD32F2F),
                                    shape = RoundedCornerShape(3.dp)
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${p?.label ?: "?"} — ${if (c.type == CardType.GEEL) "gele" else "rode"} kaart " +
                                    "(${if (c.half == Half.FIRST) "1e" else "2e"} helft)"
                        )
                    }
                }
            }

            // verloop
            if (m.events.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Wedstrijdverloop", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                }
                items(m.events) { ev ->
                    val tijd = "${if (ev.half == Half.FIRST) "1e" else "2e"} helft ${MatchViewModel.formatTime(ev.secondInHalf)}"
                    val prefix = when (ev.kind) {
                        EventKind.WISSEL -> "🔁"
                        EventKind.GEEL -> "🟨"
                        EventKind.ROOD -> "🟥"
                        EventKind.GOAL_THUIS, EventKind.GOAL_UIT -> "⚽"
                        EventKind.TIMEOUT -> "⏸"
                        EventKind.RUST -> "☕"
                        EventKind.EINDE -> "🏁"
                    }
                    Row {
                        Text("$prefix ", style = MaterialTheme.typography.bodyMedium)
                        Column {
                            Text(ev.text, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                tijd,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 16.dp)) {
            OutlinedButton(
                onClick = {
                    vm.backToSetup()
                    navController.navigate(Screen.SETUP.route) {
                        popUpTo(Screen.MATCH.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Terug naar start") }
            Button(
                onClick = {
                    vm.newMatch()
                    navController.navigate(Screen.SETUP.route) {
                        popUpTo(Screen.MATCH.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Nieuwe wedstrijd") }
        }
    }
}
