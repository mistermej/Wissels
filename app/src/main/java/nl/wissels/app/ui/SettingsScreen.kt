package nl.wissels.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
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
import nl.wissels.app.model.ActionChannels
import nl.wissels.app.model.NotifSettings
import nl.wissels.app.model.NotifyAction

/**
 * App-instellingen: alleen meldingen/trilling/geluid.
 * De matrix: per actie (wisselmoment, rust, einde, time-out, doelpunt, kaart)
 * apart instellen of er een melding, trilling en/of geluid komt,
 * plus drie globale meesterschakelaars erboven.
 *
 * Spelregels horen hier NIET meer — die staan per team onder Wedstrijdinstellingen.
 */
@Composable
fun SettingsScreen(state: UiState, vm: MatchViewModel) {
    val notif = state.notif

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "App-instellingen",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Bepaal wat de app doet bij elk wisselmoment, rust, einde, " +
                        "time-out, doelpunt en kaart.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ------------------------------------------------- globale schakelaars
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Algemeen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    MasterRow("Meldingen", "Toon een melding op het scherm", notif.masterNotify) { v ->
                        vm.updateNotif { it.copy(masterNotify = v) }
                    }
                    MasterRow("Trilling", "Telefoon laat een trilsignaal voelen", notif.masterVibrate) { v ->
                        vm.updateNotif { it.copy(masterVibrate = v) }
                    }
                    MasterRow("Geluid", "Speel een meldingsgeluid af", notif.masterSound) { v ->
                        vm.updateNotif { it.copy(masterSound = v) }
                    }
                    Text(
                        "Een schakelaar hierboven uitzetten schakelt die optie voor alle acties uit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // --------------------------------------------------------- per actie
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Per actie", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider(Modifier.padding(vertical = 12.dp))

                    Text(
                        "Melding · Trilling · Geluid",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    NotifyAction.entries.forEachIndexed { index, action ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        ActionMatrixRow(
                            action = action,
                            channels = notif.channelsFor(action),
                            enabledNotify = notif.masterNotify,
                            enabledVibrate = notif.masterVibrate,
                            enabledSound = notif.masterSound,
                            onNotify = { v -> vm.updateNotif { setChannels(it, action) { c -> c.copy(notify = v) } } },
                            onVibrate = { v -> vm.updateNotif { setChannels(it, action) { c -> c.copy(vibrate = v) } } },
                            onSound = { v -> vm.updateNotif { setChannels(it, action) { c -> c.copy(sound = v) } } }
                        )
                    }
                }
            }
        }
    }
}

private fun setChannels(
    n: NotifSettings,
    action: NotifyAction,
    transform: (ActionChannels) -> ActionChannels
): NotifSettings = n.copy(actions = n.actions + (action to transform(n.channelsFor(action))))

@Composable
private fun MasterRow(label: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionMatrixRow(
    action: NotifyAction,
    channels: ActionChannels,
    enabledNotify: Boolean,
    enabledVibrate: Boolean,
    enabledSound: Boolean,
    onNotify: (Boolean) -> Unit,
    onVibrate: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit
) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(action.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            MiniToggle("Melding", channels.notify, enabledNotify, Modifier.weight(1f), onNotify)
            MiniToggle("Trilling", channels.vibrate, enabledVibrate, Modifier.weight(1f), onVibrate)
            MiniToggle("Geluid", channels.sound, enabledSound, Modifier.weight(1f), onSound)
        }
    }
}

@Composable
private fun MiniToggle(
    label: String,
    checked: Boolean,
    masterOn: Boolean,
    modifier: Modifier,
    onChange: (Boolean) -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Switch(
            checked = checked && masterOn,
            onCheckedChange = onChange,
            enabled = masterOn
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (masterOn) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}
