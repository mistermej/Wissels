package nl.wissels.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import nl.wissels.app.MatchViewModel
import nl.wissels.app.UiState
import nl.wissels.app.model.ASK_FIELD_SIZE

/**
 * Wedstrijdinstellingen: alleen een team KIEZEN (niet aanmaken/hernoemen —
 * dat gebeurt onder Teams) en de spelregels van dat team aanpassen:
 * helftduur, aantal spelers in het veld, time-out, kaarten en automatische
 * wissels. De tegenstander vul je in bij de voorbereiding.
 */
@Composable
fun GameSettingsScreen(state: UiState, vm: MatchViewModel) {
    val team = state.activeTeam
    val settings = team.settings
    val participants = team.participants.size

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Wedstrijdinstellingen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Kies hier een team — de regels gelden alleen voor dat team.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ------------------------------------------------------- team kiezen
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Team", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IntDropdown(
                        label = "Welk team",
                        value = team.id.toInt(),
                        options = state.teams.map { it.id.toInt() },
                        suffix = { id -> state.teams.firstOrNull { it.id.toInt() == id }?.name ?: "?" }
                    ) { id ->
                        vm.setActiveTeam(state.teams.firstOrNull { it.id.toInt() == id }?.id ?: team.id)
                    }
                    Text(
                        "Teams toevoegen of hernoemen doe je onder Menu → Teams.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // -------------------------------------------------------- spelregels
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Spelregels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    IntDropdown(
                        label = "Tijd per helft (minuten)",
                        value = settings.halfMinutes,
                        options = (1..90).toList(),
                        suffix = { "$it min" }
                    ) { v -> vm.updateActiveTeamSettings { it.copy(halfMinutes = v) } }

                    IntDropdown(
                        label = "Aantal spelers in het veld (incl. keeper)",
                        value = settings.fieldSize ?: ASK_FIELD_SIZE,
                        options = listOf(ASK_FIELD_SIZE) + (2..20).toList(),
                        suffix = { if (it == ASK_FIELD_SIZE) "Vraag in Voorbereiding" else "$it spelers" }
                    ) { v ->
                        vm.updateActiveTeamSettings {
                            it.copy(fieldSize = if (v == ASK_FIELD_SIZE) null else v)
                        }
                    }
                    Text(
                        if (settings.fieldSize == null)
                            "Het aantal wordt in het voorbereidingsscherm opgegeven."
                        else
                            "$participants spelers in de selectie — " +
                                    "${(participants - settings.fieldSize).coerceAtLeast(0)} wissel(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    // ------------------------------------------------------------ time-out
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Time-out tijdens de wedstrijd")
                            Text(
                                "Eén time-out per helft. Hoe lang die duurt is niet vast-" +
                                        "gelegd: tijdens de wedstrijd druk je op Start/Einde time-out.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.timeoutEnabled,
                            onCheckedChange = { v -> vm.updateActiveTeamSettings { it.copy(timeoutEnabled = v) } }
                        )
                    }

                    // ------------------------------------------------------------- kaarten
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Kaarten (geel/rood)")
                            Text(
                                "Aan: de Kaart-knop staat tijdens de wedstrijd klaar. " +
                                        "Uit: er kunnen geen kaarten worden gegeven.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.cardsEnabled,
                            onCheckedChange = { v -> vm.updateActiveTeamSettings { it.copy(cardsEnabled = v) } }
                        )
                    }

                    HorizontalDivider()

                    // --------------------------------------------------- automatische wissels
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Wissels automatisch toepassen")
                            Text(
                                "Aan: de app wisselt direct. Uit: eerst een melding, jij bevestigt.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.autoApply,
                            onCheckedChange = { v -> vm.updateActiveTeamSettings { it.copy(autoApply = v) } }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.padding(bottom = 16.dp)) }
    }
}
