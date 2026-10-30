package nl.wissels.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Status van het downloaden/installeren van een update.
 */
sealed class UpdateInstallState {
    /** Niets aan de hand. */
    object Idle : UpdateInstallState()
    /** Download loopt. [progress] is 0..1, of null als onbekend. */
    data class Downloading(val progress: Float?) : UpdateInstallState()
    /** Download voltooid, installer wordt geopend. */
    object Installing : UpdateInstallState()
    /** Fout opgetreden. */
    data class Error(val message: String) : UpdateInstallState()
}

/**
 * Downloadt een APK van [url] naar de cache-map van de app en start daarna
 * de Android-pakketinstaller. Geen browser, geen handmatige stappen.
 */
object AppUpdater {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<UpdateInstallState>(UpdateInstallState.Idle)
    val state: StateFlow<UpdateInstallState> = _state

    /**
     * Start het downloaden van [apkUrl]. Zodra de download klaar is wordt de
     * installer geopend via [context]. Vereist dat de app in de manifest
     * `REQUEST_INSTALL_PACKAGES` heeft en een FileProvider geconfigureerd is.
     */
    fun downloadAndInstall(context: Context, apkUrl: String) {
        if (_state.value is UpdateInstallState.Downloading) return
        _state.value = UpdateInstallState.Downloading(null)

        scope.launch {
            try {
                Log.d("AppUpdater", "Starting download from: $apkUrl")
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                // Oude APK's opruimen zodat de schijf niet volloopt.
                dir.listFiles()?.forEach { it.delete() }
                val apkFile = File(dir, "update.apk")

                val conn = java.net.URL(apkUrl).openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "GET"
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 15000
                conn.readTimeout = 60000
                conn.connect()

                Log.d("AppUpdater", "Connected, response code: ${conn.responseCode}, content length: ${conn.contentLengthLong}")

                val total = conn.contentLengthLong
                var downloaded = 0L
                conn.inputStream.use { input ->
                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read
                            val progress = if (total > 0) downloaded.toFloat() / total else null
                            _state.value = UpdateInstallState.Downloading(progress)
                        }
                    }
                }
                conn.disconnect()

                Log.d("AppUpdater", "Download complete: ${apkFile.length()} bytes")

                if (!apkFile.exists() || apkFile.length() == 0L) {
                    _state.value = UpdateInstallState.Error("Download mislukt: leeg bestand")
                    return@launch
                }

                _state.value = UpdateInstallState.Installing
                Log.d("AppUpdater", "Starting install of ${apkFile.absolutePath}")
                installApk(context, apkFile)
            } catch (e: Exception) {
                Log.e("AppUpdater", "Download/install failed", e)
                _state.value = UpdateInstallState.Error(e.message ?: "Onbekende fout")
            }
        }
    }

    /**
     * Opent de Android-pakketinstaller voor [apkFile].
     */
    private fun installApk(context: Context, apkFile: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            Log.d("AppUpdater", "FileProvider URI: $uri")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
            Log.d("AppUpdater", "Install intent started")
        } catch (e: Exception) {
            Log.e("AppUpdater", "Failed to start install", e)
            _state.value = UpdateInstallState.Error("Installer openen mislukt: ${e.message}")
        }
    }

    /**
     * Controleert of de app APK's mag installeren. Zo niet, opent de
     * instellingen-pagina waar de gebruiker dit kan toestaan.
     */
    fun canInstall(context: Context): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }

    fun openInstallPermissionSettings(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(intent)
        }
    }

    /** Reset de state naar Idle (bv. na installatie of annulering). */
    fun reset() {
        _state.value = UpdateInstallState.Idle
    }
}
