package nl.wissels.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import nl.wissels.app.R

/**
 * Startscherm: het clublogo zit IN de APK (geen internet nodig),
 * "Gemaakt door Jaimie" staat onderaan. Na 2,5 seconden naar de instellingen.
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2500)
        onFinished()
    }

    val logoW = ResponsiveSizes.logoSize(LocalConfiguration.current.screenWidthDp)
    val splashPadBottom = ResponsiveSizes.creditsBottomPadding(LocalConfiguration.current.screenHeightDp)

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.vv_wijster_logo),
                contentDescription = "VV Wijster logo",
                modifier = Modifier.size(logoW).heightIn(max = 240.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                "VV Wijster",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            "Gemaakt door Jaimie",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = splashPadBottom),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
