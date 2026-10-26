package nl.wissels.app.model

enum class Half { FIRST, SECOND }

/**
 * Waarde die in een keuzelijst staat voor "Vraag in Voorbereiding":
 * het aantal spelers in het veld wordt dan in het voorbereidingsscherm bepaald.
 * In de modellen zelf wordt hiervoor `null` gebruikt.
 */
const val ASK_FIELD_SIZE: Int = 0

enum class Screen {
    SPLASH,
    SETUP,
    PREPARE,
    MATCH,
    SUMMARY,
    SETTINGS,
    GAMESETTINGS,
    TEAMS,
    ABOUT;

    val route: String = name.lowercase()
}

enum class CardType { GEEL, ROOD }

enum class EventKind { WISSEL, GEEL, ROOD, GOAL_THUIS, GOAL_UIT, TIMEOUT, RUST, EINDE }

data class Player(
    val id: Long,
    val name: String,
    val number: String = "",
    val participate: Boolean = true,
    val keeperFirst: Boolean = false,
    val keeperSecond: Boolean = false
) {
    val label: String get() = if (number.isBlank()) name else "$number $name"
}

data class PlayerCard(
    val playerId: Long,
    val type: CardType,
    val half: Half,
    val secondInHalf: Int
)

/**
 * De wedstrijdregels van ÉÉN team. Niet alle teams spelen hetzelfde
 * (zaalvoetbal vs. veld, 2×35 vs. 2×45) — daarom hangt dit aan het team
 * en niet aan de app.
 */
data class TeamGameSettings(
    /** Duur van een helft in minuten. */
    val halfMinutes: Int = 35,
    /**
     * Aantal spelers IN HET VELD (inclusief doelvelder).
     * `null` = "Vraag in Voorbereiding": het aantal wordt in het
     * voorbereidingsscherm opgegeven.
     */
    val fieldSize: Int? = 7,
    /**
     * Time-out toestaan. Er is bewust GEEN duur: hoe lang een time-out duurt is
     * niet gedefinieerd, de coach start/eindigt de time-out tijdens de wedstrijd.
     */
    val timeoutEnabled: Boolean = false,
    /** Gele/rode kaarten toestaan tijdens de wedstrijd. */
    val cardsEnabled: Boolean = true,
    /**
     * Vaste keeper aan: de keepers per helft staan op het team en worden
     * meegenomen naar de voorbereiding (daar staan de vinkjes al ingevuld).
     * Uit: de keepers worden in de voorbereiding bepaald en het Teams-scherm
     * toont geen keeper-vinkjes.
     */
    val fixedKeeper: Boolean = false,
    /** Wisselmomenten automatisch toepassen (anders krijgt de coach een voorstel). */
    val autoApply: Boolean = false
)

/** Een team met zijn eigen spelers én eigen wedstrijdinstellingen. */
data class Team(
    val id: Long,
    val name: String = "Ons team",
    val players: List<Player> = emptyList(),
    val settings: TeamGameSettings = TeamGameSettings()
) {
    val participants: List<Player> get() = players.filter { it.participate }
}

/** De acties waarop de app kan reageren met melding/trilling/geluid. */
enum class NotifyAction(val label: String) {
    WISSELMOMENT("Wisselmoment"),
    RUST("Rust"),
    EINDE("Einde wedstrijd"),
    TIMEOUT("Time-out"),
    DOELPUNT("Doelpunt"),
    KAART("Kaart")
}

/** Wat er per actie gebeurt: melding tonen, trillen en/of geluid afspelen. */
data class ActionChannels(
    val notify: Boolean = true,
    val vibrate: Boolean = true,
    val sound: Boolean = true
)

/**
 * App-brede notificatie-instellingen: drie globale meesterschakelaars
 * (melding/trilling/geluid) maal per actie een eigen combinatie.
 */
data class NotifSettings(
    val masterNotify: Boolean = true,
    val masterVibrate: Boolean = true,
    val masterSound: Boolean = true,
    val actions: Map<NotifyAction, ActionChannels> = emptyMap()
) {
    fun channelsFor(action: NotifyAction): ActionChannels =
        actions[action] ?: ActionChannels()

    fun notifyOn(action: NotifyAction): Boolean = masterNotify && channelsFor(action).notify
    fun vibrateOn(action: NotifyAction): Boolean = masterVibrate && channelsFor(action).vibrate
    fun soundOn(action: NotifyAction): Boolean = masterSound && channelsFor(action).sound
}

data class MatchEvent(
    val half: Half,
    val secondInHalf: Int,
    val kind: EventKind,
    val text: String
)

/** Een geplande wissel: spelers die eraf gaan en spelers die erin komen. */
data class SubPlan(
    val offIds: List<Long>,
    val onIds: List<Long>
)

data class MatchConfig(
    val homeTeam: String = "Ons team",
    val awayTeam: String = "Tegenstander",
    val halfMinutes: Int = 35,
    /** Aantal spelers IN HET VELD (inclusief de doelvelder). */
    val fieldSize: Int = 7,
    val timeoutEnabled: Boolean = false,
    /** Kaarten (geel/rood) toestaan tijdens deze wedstrijd. */
    val cardsEnabled: Boolean = true,
    /** Wisselmomenten automatisch toepassen (anders krijgt de coach een voorstel). */
    val autoApply: Boolean = false,
    val players: List<Player> = emptyList()
) {
    val participants: List<Player> get() = players.filter { it.participate }

    fun keeperOf(half: Half): Player? {
        val p = participants
        return if (half == Half.FIRST) {
            p.firstOrNull { it.keeperFirst }
        } else {
            p.firstOrNull { it.keeperSecond } ?: p.firstOrNull { it.keeperFirst }
        }
    }

    /** Veldspelers (zonder doelvelder) die in de betreffende helft roteren. */
    fun outfieldPool(half: Half): List<Player> {
        val k = keeperOf(half)
        return participants.filter { it.id != k?.id }
    }

    /** Aantal veldspelers in het veld (het veld formaat minus de doelvelder). */
    val outfieldSpots: Int get() = (fieldSize - 1).coerceAtLeast(0)

    fun substitutes(half: Half): Int = (outfieldPool(half).size - outfieldSpots).coerceAtLeast(0)

    val halfSeconds: Int get() = halfMinutes * 60
}

data class MatchState(
    val config: MatchConfig,
    val half: Half = Half.FIRST,
    val elapsedInHalf: Int = 0,
    val running: Boolean = false,
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val onPitch: Set<Long> = emptySet(),
    val playedSeconds: Map<Long, Int> = emptyMap(),
    val consecutiveSeconds: Map<Long, Int> = emptyMap(),
    val redCards: Set<Long> = emptySet(),
    val cards: List<PlayerCard> = emptyList(),
    val events: List<MatchEvent> = emptyList(),
    val subsDone: Int = 0,
    /** Open voorstel voor een wisselmoment (coach moet bevestigen). */
    val pending: SubPlan? = null,
    val pendingWindow: Int = 0,
    val totalWindows: Int = 0,
    /** True zolang de time-out loopt (geen vaste duur: de coach beëindigt hem). */
    val timeoutActive: Boolean = false,
    /** Of de klok liep op het moment dat de time-out begon (voor het hervatten). */
    val timeoutWasRunning: Boolean = false,
    val timeoutUsedThisHalf: Boolean = false,
    val atHalfTime: Boolean = false,
    val finished: Boolean = false
) {
    val secondsLeft: Int get() = (config.halfSeconds - elapsedInHalf).coerceAtLeast(0)
}

/**
 * De concept-wedstrijd die in het voorbereidingsscherm wordt ingevuld:
 * welk team, tegenstander, wie meedoen, hoeveel er in het veld staan en
 * wie de keepers zijn. Pas bij "Wedstrijd starten" wordt hier een
 * [MatchConfig] van gemaakt.
 */
data class PrepState(
    val teamId: Long,
    val awayTeam: String = "Tegenstander",
    /** Deelnemende spelers (vinklijst in de voorbereiding). */
    val selectedIds: Set<Long> = emptySet(),
    /** `null` = nog niet bepaald (team staat op "Vraag in Voorbereiding"). */
    val fieldSize: Int? = null,
    val keeperFirstId: Long? = null,
    val keeperSecondId: Long? = null
)
