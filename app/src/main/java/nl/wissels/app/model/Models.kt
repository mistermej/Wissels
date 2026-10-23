package nl.wissels.app.model

enum class Half { FIRST, SECOND }

enum class Screen { SETUP, MATCH, SUMMARY }

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
    val timeoutMinutes: Int = 2,
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
    val timeoutLeft: Int? = null,
    val timeoutUsedThisHalf: Boolean = false,
    val atHalfTime: Boolean = false,
    val finished: Boolean = false
) {
    val secondsLeft: Int get() = (config.halfSeconds - elapsedInHalf).coerceAtLeast(0)
}
