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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import nl.wissels.app.MatchViewModel
import nl.wissels.app.UiState
import nl.wissels.app.model.Screen

/**
 * Basisscherm: hier begint het — alleen een knop "Start Voorbereiding".
 * Team, tegenstander, deelnemende spelers en de wedstrijdregels kies je
 * in het voorbereidingsscherm (het "actief" team uit Teams zegt hier niets).
 */
@Composable
fun SetupScreen(state: UiState, vm: MatchViewModel, navController: NavController) {
    val match = state.match

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Wedstrijd",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )

        Text(
            "Welk team speelt en wie meedoet kies je in de voorbereiding.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ------------------------------------------------------------ fouten
        state.setupError?.let { err ->
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )) {
                Text(
                    err,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // ------------------------------------------------------------- starten
        Button(
            onClick = {
                vm.startPreparation()
                navController.navigate(Screen.PREPARE.route)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Start Voorbereiding")
        }

        if (match != null) {
            OutlinedButton(
                onClick = {
                    navController.navigate(
                        if (match.finished) Screen.SUMMARY.route else Screen.MATCH.route
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.SportsSoccer, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (match.finished) "Wedstrijdoverzicht bekijken" else "Naar de wedstrijd")
            }
        }

        Spacer(Modifier.padding(bottom = 16.dp))
    }
}
