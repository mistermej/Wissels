package nl.wissels.app.data

import android.content.Context
import nl.wissels.app.model.MatchConfig
import nl.wissels.app.model.Player
import org.json.JSONArray

/** Bewaart de opstelling en instellingen lokaal (SharedPreferences, geen server). */
object Store {

    private const val PREFS = "wissels_prefs"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveConfig(context: Context, cfg: MatchConfig) {
        val arr = JSONArray()
        cfg.players.forEach { p ->
            arr.put(
                org.json.JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("number", p.number)
                    put("participate", p.participate)
                    put("k1", p.keeperFirst)
                    put("k2", p.keeperSecond)
                }
            )
        }
        prefs(context).edit()
            .putString("players", arr.toString())
            .putString("homeTeam", cfg.homeTeam)
            .putString("awayTeam", cfg.awayTeam)
            .putInt("halfMinutes", cfg.halfMinutes)
            .putInt("fieldSize", cfg.fieldSize)
            .putBoolean("timeoutEnabled", cfg.timeoutEnabled)
            .putInt("timeoutMinutes", cfg.timeoutMinutes)
            .putBoolean("autoApply", cfg.autoApply)
            .apply()
    }

    fun loadConfig(context: Context): MatchConfig {
        val p = prefs(context)
        val players = try {
            val arr = JSONArray(p.getString("players", "[]") ?: "[]")
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Player(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    number = o.optString("number", ""),
                    participate = o.optBoolean("participate", true),
                    keeperFirst = o.optBoolean("k1", false),
                    keeperSecond = o.optBoolean("k2", false)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
        return MatchConfig(
            homeTeam = p.getString("homeTeam", "Ons team") ?: "Ons team",
            awayTeam = p.getString("awayTeam", "Tegenstander") ?: "Tegenstander",
            halfMinutes = p.getInt("halfMinutes", 35).coerceIn(1, 90),
            fieldSize = p.getInt("fieldSize", 7).coerceIn(2, 20),
            timeoutEnabled = p.getBoolean("timeoutEnabled", false),
            timeoutMinutes = p.getInt("timeoutMinutes", 2).coerceIn(1, 10),
            autoApply = p.getBoolean("autoApply", false),
            players = players
        )
    }
}
