package nl.wissels.app

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Uitkomst van de controle of er een nieuwe versie klaarstaat. */
enum class UpdateStatus { Unknown, Checking, UpToDate, UpdateAvailable, Private, Error }

data class UpdateInfo(
    val status: UpdateStatus = UpdateStatus.Unknown,
    /** Versie van de nieuwste release zonder "v", bv. "0.4". */
    val latestVersion: String = "",
    /** Link naar de nieuwste release (voor "Installeren"). */
    val releaseUrl: String = "",
    /** Directe download-URL van de APK-asset (voor automatische installatie). */
    val apkDownloadUrl: String = "",
    /** Versie die deze app nu draagt, bv. "0.3". */
    val currentVersion: String = ""
)

/**
 * Controleert bij het openen van de app of er een nieuwe versie op GitHub
 * staat. Eén gedeelde bron, zodat het startscherm, "Over" en het menu
 * dezelfde status tonen — de knop in "Over" forceert een nieuwe controle.
 */
object UpdateChecker {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var running: Job? = null
    private var lastCheckMs = 0L

    /** Niet vaker dan één controle per vijf minuten bij gewone openen. */
    private const val MIN_INTERVAL_MS = 5 * 60 * 1000L

    private val _state = MutableStateFlow(UpdateInfo())
    val state: StateFlow<UpdateInfo> = _state

    /**
     * Start een controle. Terwijl er al gecontroleerd wordt (of er net een
     * was) gebeurt er niets, zodat app-start en "Over" geen dubbel werk doen.
     *
     * @param force negeer de pauze — voor de knop "Controleren op updates".
     */
    fun check(context: Context, force: Boolean = false) {
        if (running?.isActive == true) return
        val now = System.currentTimeMillis()
        if (!force && now - lastCheckMs < MIN_INTERVAL_MS) return
        lastCheckMs = now

        val current = AppVersion.name(context)
        _state.value = UpdateInfo(status = UpdateStatus.Checking, currentVersion = current)
        running = scope.launch {
            _state.value = fetch(current)
        }
    }

    /**
     * Vraagt de nieuwste release op. 200 = release gevonden, 404 = repo
     * bestaat niet (publiek) — dat is meestal een PRIVÉ repository: zonder
     * GitHub-token krijgen we die niet te zien. Alles wat overblijft is een
     * netwerkfout.
     */
    private fun fetch(currentVersion: String): UpdateInfo {
        val result: Pair<UpdateStatus, Triple<String, String, String>?> = try {
            val conn = URL("https://api.github.com/repos/mistermej/Wissels/releases/latest")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            when (conn.responseCode) {
                200 -> {
                    val json = conn.inputStream.reader().use { it.readText() }
                    conn.disconnect()
                    val obj = JSONObject(json)
                    val tag = obj.getString("tag_name").removePrefix("v").removePrefix("V")
                    // Zoek de APK-asset voor automatische download/installatie.
                    val apkUrl = try {
                        val assets = obj.optJSONArray("assets")
                        var found = ""
                        if (assets != null) {
                            for (i in 0 until assets.length()) {
                                val asset = assets.getJSONObject(i)
                                val name = asset.optString("name", "")
                                if (name.endsWith(".apk", ignoreCase = true)) {
                                    found = asset.optString("browser_download_url", "")
                                    break
                                }
                            }
                        }
                        found
                    } catch (_: Exception) { "" }
                    Log.d("UpdateChecker", "Latest: $tag, html_url: ${obj.getString("html_url")}, apk_url: $apkUrl")
                    UpdateStatus.Checking to Triple(tag, obj.getString("html_url"), apkUrl)
                }
                404 -> {
                    conn.disconnect()
                    UpdateStatus.Private to null
                }
                else -> {
                    conn.disconnect()
                    UpdateStatus.Error to null
                }
            }
        } catch (_: Exception) {
            UpdateStatus.Error to null
        }

        val (status, payload) = result
        if (payload == null) {
            return UpdateInfo(status = status, currentVersion = currentVersion)
        }
        val (version, url, apkUrl) = payload
        return UpdateInfo(
            // Pas een melding als de release écht nieuwer is dan deze app.
            status = if (AppVersion.asNumber(version) > AppVersion.asNumber(currentVersion)) {
                UpdateStatus.UpdateAvailable
            } else {
                UpdateStatus.UpToDate
            },
            latestVersion = version,
            releaseUrl = url,
            apkDownloadUrl = apkUrl,
            currentVersion = currentVersion
        )
    }
}
