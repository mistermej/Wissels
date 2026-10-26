package nl.wissels.app

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat

/**
 * Leest de versie uit de AndroidManifest, zodat schermen en de update-controle
 * nooit zelf een versienummer hoeven te hardcoden (dat liep achter op 0.1/0.2).
 */
object AppVersion {

    /** Versienaam zoals in versionName staat, bv. "0.2". */
    fun name(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "?"

    /** Interne build-nummer (versionCode), bv. 2. */
    fun code(context: Context): Long = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        PackageInfoCompat.getLongVersionCode(info)
    }.getOrNull() ?: 0L

    /** "0.2" -> 0.2, zodat vergeleken kan worden met een GitHub-tag ("v0.2"). */
    fun asNumber(version: String): Double = version.trim()
        .removePrefix("v")
        .removePrefix("V")
        .toDoubleOrNull() ?: 0.0
}
