package nl.wissels.app.engine

import nl.wissels.app.model.Half
import nl.wissels.app.model.MatchConfig
import nl.wissels.app.model.MatchState
import nl.wissels.app.model.SubPlan

/**
 * De kern van de app: berekent WANNEER en WIE er gewisseld moeten worden
 * zodat iedere veldspeler ongeveer evenveel speeltijd krijgt.
 *
 * - De doelvelder van de helft draait niet mee in de rotatie.
 * - Rode kaart = speler is de rest van de wedstrijd uit (team speelt met minder).
 * - Wisselmomenten staan vast en liggen even verdeeld over de helft. Hoeveel
 *   spelers er per moment TEGELIJK wisselen is instelbaar (`subsPerWindow`):
 *   0 = de hele bank gaat in op een moment, anders telkens dat aantal.
 */
object RotationEngine {

    /**
     * Aantal wisselmomenten per helft (max 8).
     * Met `subsPerWindow` = 0 wisselt de hele bank per moment (een moment per
     * wisselspeler); anders zijn er zoveel momenten als nodig om iedereen met
     * dat aantal tegelijk te laten wisselen.
     */
    fun windowsPerHalf(config: MatchConfig, half: Half): Int {
        val subs = config.substitutes(half)
        if (subs == 0) return 0
        val per = config.subsPerWindow
        val windows = if (per <= 0) subs else (subs + per - 1) / per
        return windows.coerceIn(1, 8)
    }

    /** Tijdstippen (seconden in de helft) waarop gewisseld kan worden. */
    fun windowSeconds(config: MatchConfig, half: Half): List<Int> {
        val w = windowsPerHalf(config, half)
        if (w == 0) return emptyList()
        val halfSec = config.halfSeconds
        return (1..w).map { it * halfSec / (w + 1) }
    }

    /** Berekent de wissel voor een wisselmoment op basis van de huidige speeltijden. */
    fun plan(state: MatchState): SubPlan {
        val cfg = state.config
        val keeperId = cfg.keeperOf(state.half)?.id
        val pool = cfg.outfieldPool(state.half).filter { it.id !in state.redCards }
        val onOutfield = state.onPitch.filter { it != keeperId }
        val bench = pool.filter { it.id !in state.onPitch }.map { it.id }

        if (bench.isEmpty() || onOutfield.isEmpty()) return SubPlan(emptyList(), emptyList())

        // Hoeveel er per moment wisselen: 0 = de hele bank, anders dat aantal.
        // Nooit meer dan er (na rode kaarten) in het veld horen te staan.
        val targetOutfield = (cfg.outfieldSpots - state.redCards.size).coerceAtLeast(0)
        val wanted = if (cfg.subsPerWindow <= 0) Int.MAX_VALUE else cfg.subsPerWindow
        val swapCount = minOf(bench.size, onOutfield.size, wanted, targetOutfield)

        // Eruit: meeste speeltijd, dan langste aaneengesloten op het veld, dan roster-volgorde.
        fun rosterIndex(id: Long): Int = pool.indexOfFirst { it.id == id }
            .let { if (it == -1) Int.MAX_VALUE else it }

        val offIds = onOutfield
            .sortedWith(
                compareByDescending<Long> { state.playedSeconds[it] ?: 0 }
                    .thenByDescending { state.consecutiveSeconds[it] ?: 0 }
                    .thenBy { rosterIndex(it) }
            )
            .take(swapCount)

        // Erin: minste speeltijd, dan roster-volgorde.
        val onIds = bench
            .sortedWith(
                compareBy<Long> { state.playedSeconds[it] ?: 0 }
                    .thenBy { rosterIndex(it) }
            )
            .take(swapCount)

        return SubPlan(offIds, onIds)
    }

    /**
     * Of een wisselplan nu uitvoerbaar is: er moet echt iemand het veld in en
     * iemand eruit, elke speler mag maar één keer voorkomen en de keeper
     * wisselt nooit mee.
     */
    fun isValidPlan(state: MatchState, plan: SubPlan): Boolean {
        if (plan.offIds.isEmpty() || plan.offIds.size != plan.onIds.size) return false
        val keeperId = state.config.keeperOf(state.half)?.id
        val poolIds = state.config.outfieldPool(state.half).map { it.id }.toSet()
        return plan.offIds.all { it in state.onPitch && it != keeperId } &&
                plan.onIds.all { it !in state.onPitch && it in poolIds } &&
                plan.offIds.toSet().size == plan.offIds.size &&
                plan.onIds.toSet().size == plan.onIds.size &&
                plan.onIds.none { it in plan.offIds }
    }

    /**
     * Normaliseert het aantal spelers in het veld (gebruikt bij de start van de
     * tweede helft, bv. na een keeperwissel of een rode kaart in de eerste helft).
     */
    fun normalizeOnPitch(state: MatchState): Set<Long> {
        val cfg = state.config
        val keeperId = cfg.keeperOf(state.half)?.id
        val pool = cfg.outfieldPool(state.half)
            .filter { it.id !in state.redCards && it.id != keeperId }
        val targetOutfield = (cfg.outfieldSpots - state.redCards.size).coerceAtLeast(0)
        val current = state.onPitch.filter { it != keeperId }

        var on = current.toMutableList()
        // Bijvullen met minste speelde spelers
        while (on.size < targetOutfield) {
            val candidate = pool.filter { it.id !in on }
                .minByOrNull { state.playedSeconds[it.id] ?: 0 } ?: break
            on.add(candidate.id)
        }
        // Ontsmetten (te veel op het veld) met meeste speeltijd eraf
        while (on.size > targetOutfield) {
            val worst = on.maxByOrNull { state.playedSeconds[it] ?: 0 } ?: break
            on.remove(worst)
        }
        return on.toSet() + setOfNotNull(keeperId)
    }
}
