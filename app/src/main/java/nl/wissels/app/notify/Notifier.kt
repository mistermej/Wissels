package nl.wissels.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import nl.wissels.app.MainActivity
import nl.wissels.app.data.Store
import nl.wissels.app.model.NotifyAction

/**
 * Notificaties, trilling en geluid — per actie apart instelbaar.
 *
 * Let op: vanaf Android 8 (O) gelden geluid en trilling per notificatie-kanaal,
 * niet per melding. Daarom maken we per combinatie geluid × trilling een eigen
 * kanaal en kiezen we per actie het juiste kanaal.
 */
object Notifier {

    private const val NOTIF_ID = 1001

    /** Blijvende melding in de statusbalk zolang er een wedstrijd loopt. */
    private const val MATCH_NOTIF_ID = 1002
    private const val MATCH_CHANNEL = "wissels_wedstrijd"

    /** Extra op de intent van de statusbalk-melding: direct naar de wedstrijd. */
    const val EXTRA_OPEN_MATCH = "open_match"

    private fun channelId(sound: Boolean, vibrate: Boolean): String =
        "wissels_" + (if (sound) "geluid" else "stil") + "_" + (if (vibrate) "tril" else "geenvib")

    private fun channelName(sound: Boolean, vibrate: Boolean): String =
        "Wissels " + when {
            sound && vibrate -> "met geluid en trilling"
            sound -> "met geluid"
            vibrate -> "met trilling"
            else -> "stil"
        }

    /** Maakt alle vier de kanalen aan (eenmalig); veilig om elke keer aan te roepen. */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        for (sound in booleanArrayOf(true, false)) {
            for (vibrate in booleanArrayOf(true, false)) {
                val id = channelId(sound, vibrate)
                if (mgr.getNotificationChannel(id) != null) continue
                val channel = NotificationChannel(id, channelName(sound, vibrate), NotificationManager.IMPORTANCE_HIGH)
                    .apply {
                        description = "Meldingen tijdens de wedstrijd"
                        if (sound) {
                            setSound(
                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                    .build()
                            )
                        } else {
                            setSound(null, null)
                        }
                        enableVibration(vibrate)
                        vibrationPattern = if (vibrate) longArrayOf(0, 250) else null
                    }
                mgr.createNotificationChannel(channel)
            }
        }
        ensureMatchChannel(context)
    }

    /** Kanaal voor de blijvende statusbalk-melding (stil, geen trilling). */
    private fun ensureMatchChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(MATCH_CHANNEL) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(
                MATCH_CHANNEL,
                "Wedstrijd op de achtergrond",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Toont stand en klok zolang er een wedstrijd loopt"
                setSound(null, null)
                enableVibration(false)
            }
        )
    }

    /**
     * Blijvende melding in de statusbalk zolang er een wedstrijd loopt: stand,
     * helft en resterende tijd. Tikken opent het wedstrijdscherm.
     */
    fun matchStatus(context: Context, title: String, text: String) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        if (!granted) return
        ensureMatchChannel(context)

        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_MATCH, true)
        }
        val contentIntent = PendingIntent.getActivity(
            context, MATCH_NOTIF_ID, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, MATCH_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setAutoCancel(false)
        context.getSystemService(NotificationManager::class.java)
            ?.notify(MATCH_NOTIF_ID, builder.build())
    }

    /** Verwijdert de statusbalk-melding (wedstrijd afgelopen of app gestopt). */
    fun cancelMatchStatus(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(MATCH_NOTIF_ID)
    }

    /**
     * Melding + trilling + geluid voor een actie, volgens de app-instellingen.
     * Doet niets als alles voor deze actie uit staat.
     */
    fun action(context: Context, act: NotifyAction, title: String, text: String) {
        val settings = Store.loadNotif(context)
        val doNotify = settings.notifyOn(act)
        val doVibrate = settings.vibrateOn(act)
        val doSound = settings.soundOn(act)
        if (!doNotify && !doVibrate && !doSound) return

        if (doNotify) {
            ensureChannel(context)
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU

            if (granted) {
                val builder = NotificationCompat.Builder(context, channelId(doSound, doVibrate))
                    .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                // minSdk 26 = Android 8: geluid en trilling komen altijd uit het
                // kanaal hierboven. Trilling doen we daarnaast zelf (hieronder),
                // zodat het ook werkt als de melding zelf niet getoond mag worden.
                val mgr = context.getSystemService(NotificationManager::class.java)
                mgr?.notify(NOTIF_ID, builder.build())
            }
        }
        if (doVibrate) vibrate(context)
    }

    fun vibrate(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(VibratorManager::class.java)
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 250), -1)
            )
        } catch (_: Exception) {
        }
    }
}
