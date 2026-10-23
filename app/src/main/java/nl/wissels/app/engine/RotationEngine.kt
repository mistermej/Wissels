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
 * - Wisselmomenten staan vast: de bank komt per moment gezamenlijk naar binnen,
 *   de spelers met de meeste speeltijd gaan eruit.
 */
object RotationEngine {

    /** Aantal wisselmomenten per helft = aantal wisselspelers (max 8). */
    fun windowsPerHalf(config: MatchConfig, half: Half): Int =
        config.substitutes(half).coerceIn(0, 8)

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

        val swapCount = minOf(bench.size, onOutfield.size)

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
