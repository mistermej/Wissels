package nl.wissels.app.data

import android.content.Context
import nl.wissels.app.model.ActionChannels
import nl.wissels.app.model.NotifSettings
import nl.wissels.app.model.NotifyAction
import nl.wissels.app.model.Player
import nl.wissels.app.model.Team
import nl.wissels.app.model.TeamGameSettings
import org.json.JSONArray
import org.json.JSONObject

/**
 * Bewaart alles lokaal (SharedPreferences, geen server):
 * - teams met spelers én eigen wedstrijdinstellingen
 * - welk team actief is
 * - de tegenstander van de laatste wedstrijd
 * - de notificatie-matrix (melding/trilling/geluid per actie)
 *
 * Oude opslag (één team onder losse sleutels) wordt bij het laden gemigreerd.
 */
object Store {

    private const val PREFS = "wissels_prefs"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ------------------------------------------------------------------ teams

    fun saveTeams(context: Context, teams: List<Team>, activeTeamId: Long) {
        val arr = JSONArray()
        teams.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("players", playersJson(t.players))
                    put("halfMinutes", t.settings.halfMinutes)
                    // -1 = "Vraag in Voorbereiding"
                    put("fieldSize", t.settings.fieldSize ?: -1)
                    put("timeoutEnabled", t.settings.timeoutEnabled)
                    put("cardsEnabled", t.settings.cardsEnabled)
                    put("subsPerWindow", t.settings.subsPerWindow)
                    put("autoApply", t.settings.autoApply)
                }
            )
        }
        prefs(context).edit()
            .putString("teams", arr.toString())
            .putLong("activeTeamId", activeTeamId)
            .apply()
    }

    fun loadTeams(context: Context): List<Team> {
        val p = prefs(context)
        val raw = p.getString("teams", null)
        if (raw == null) return migrateLegacy(context)

        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Team(
                    id = o.getLong("id"),
                    name = o.optString("name", "Ons team"),
                    players = playersFromJson(o.optJSONArray("players")),
                    settings = TeamGameSettings(
                        halfMinutes = o.optInt("halfMinutes", 35).coerceIn(1, 90),
                        // -1 (of oude opslag zonder sleutel → 7) = "Vraag in Voorbereiding"
                        fieldSize = o.optInt("fieldSize", 7).takeIf { it > 0 }?.coerceIn(2, 20),
                        timeoutEnabled = o.optBoolean("timeoutEnabled", false),
                        cardsEnabled = o.optBoolean("cardsEnabled", true),
                        // 0 = "Alle tegelijk"
                        subsPerWindow = o.optInt("subsPerWindow", 0).coerceIn(0, 8),
                        autoApply = o.optBoolean("autoApply", false)
                    )
                )
            }
        } catch (e: Exception) {
            migrateLegacy(context)
        }
    }

    fun loadActiveTeamId(context: Context): Long {
        val p = prefs(context)
        return p.getLong("activeTeamId", -1L).takeIf { id -> id >= 0 }
            ?: loadTeams(context).firstOrNull()?.id ?: 1L
    }

    /**
     * Oude opslag (v0.1/early v0.2: losse sleutels "players", "halfMinutes", …)
     * omzetten naar één team. Lege lijst als er echt niets is.
     */
    private fun migrateLegacy(context: Context): List<Team> {
        val p = prefs(context)
        if (!p.contains("players")) return listOf(Team(id = 1L))

        val players = playersFromJson(try { JSONArray(p.getString("players", "[]") ?: "[]") } catch (e: Exception) { null })
        val team = Team(
            id = 1L,
            name = p.getString("homeTeam", "Ons team") ?: "Ons team",
            players = players,
            settings = TeamGameSettings(
                halfMinutes = p.getInt("halfMinutes", 35).coerceIn(1, 90),
                fieldSize = p.getInt("fieldSize", 7).takeIf { it > 0 }?.coerceIn(2, 20),
                timeoutEnabled = p.getBoolean("timeoutEnabled", false),
                cardsEnabled = p.getBoolean("cardsEnabled", true),
                subsPerWindow = p.getInt("subsPerWindow", 0).coerceIn(0, 8),
                autoApply = p.getBoolean("autoApply", false)
            )
        )
        // Direct wegschrijven zodat de migratie maar één keer loopt.
        saveTeams(context, listOf(team), team.id)
        return listOf(team)
    }

    private fun playersJson(players: List<Player>): JSONArray = JSONArray().also { arr ->
        players.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("number", p.number)
                    put("participate", p.participate)
                    put("k1", p.keeperFirst)
                    put("k2", p.keeperSecond)
                }
            )
        }
    }

    private fun playersFromJson(arr: JSONArray?): List<Player> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).mapNotNull { i ->
            try {
                val o = arr.getJSONObject(i)
                Player(
                    id = o.getLong("id"),
                    name = o.getString("name"),
                    number = o.optString("number", ""),
                    participate = o.optBoolean("participate", true),
                    keeperFirst = o.optBoolean("k1", false),
                    keeperSecond = o.optBoolean("k2", false)
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    // ------------------------------------------------------------ tegenstander

    fun saveAwayTeam(context: Context, name: String) {
        prefs(context).edit().putString("awayTeam", name).apply()
    }

    fun loadAwayTeam(context: Context): String =
        prefs(context).getString("awayTeam", "Tegenstander") ?: "Tegenstander"

    // ---------------------------------------------------------- notificaties

    fun saveNotif(context: Context, n: NotifSettings) {
        val edit = prefs(context).edit()
            .putBoolean("n_masterNotify", n.masterNotify)
            .putBoolean("n_masterVibrate", n.masterVibrate)
            .putBoolean("n_masterSound", n.masterSound)
        NotifyAction.entries.forEach { action ->
            val c = n.channelsFor(action)
            edit.putBoolean("${action.name}_notify", c.notify)
                .putBoolean("${action.name}_vibrate", c.vibrate)
                .putBoolean("${action.name}_sound", c.sound)
        }
        edit.apply()
    }

    fun loadNotif(context: Context): NotifSettings {
        val p = prefs(context)
        val actions = NotifyAction.entries.associateWith { action ->
            ActionChannels(
                notify = p.getBoolean("${action.name}_notify", true),
                vibrate = p.getBoolean("${action.name}_vibrate", true),
                sound = p.getBoolean("${action.name}_sound", true)
            )
        }
        return NotifSettings(
            masterNotify = p.getBoolean("n_masterNotify", true),
            masterVibrate = p.getBoolean("n_masterVibrate", true),
            masterSound = p.getBoolean("n_masterSound", true),
            actions = actions
        )
    }
}
