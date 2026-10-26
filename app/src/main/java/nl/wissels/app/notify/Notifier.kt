package nl.wissels.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
                        vibrationPattern = if (vibrate) longArrayOf(0, 300, 150, 300) else null
                    }
                mgr.createNotificationChannel(channel)
            }
        }
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
                VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1)
            )
        } catch (_: Exception) {
        }
    }
}
