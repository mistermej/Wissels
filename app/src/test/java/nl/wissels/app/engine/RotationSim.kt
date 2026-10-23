package nl.wissels.app.engine

import nl.wissels.app.model.Half
import nl.wissels.app.model.MatchConfig
import nl.wissels.app.model.MatchState
import nl.wissels.app.model.Player

/**
 * Simuleert een hele wedstrijd en logt de speeltijd per speler.
 * Draaibaar als gewone JVM via de testbronnen, of handmatig ter controle.
 */
fun main() {
    // 14 spelers, 1 keeper, 10 in het veld => 3 wisselspelers, helften van 35 min
    val players = (1L..14L).map { id ->
        Player(
            id = id,
            name = "Speler$id",
            number = id.toString(),
            participate = true,
            keeperFirst = id == 1L,
            keeperSecond = id == 1L
        )
    }
    val cfg = MatchConfig(
        homeTeam = "Thuis", awayTeam = "Uit",
        halfMinutes = 35, fieldSize = 11,
        players = players
    )

    println("Wissels per helft: ${cfg.substitutes(Half.FIRST)}")
    println("Wisselmomenten (seconden in helft): ${RotationEngine.windowSeconds(cfg, Half.FIRST)}")

    var state = MatchState(
        config = cfg,
        half = Half.FIRST,
        onPitch = buildSet {
            add(1L) // keeper
            addAll((2L..11L)) // eerste 10 veldspelers
        },
        playedSeconds = players.associate { it.id to 0 },
        consecutiveSeconds = players.associate { it.id to 0 }
    )

    var windowIdx = 0
    val windows1 = RotationEngine.windowSeconds(cfg, Half.FIRST)

    // eerste helft simuleren
    for (sec in 1..cfg.halfSeconds) {
        state = state.copy(
            elapsedInHalf = sec,
            playedSeconds = state.playedSeconds +
                    state.onPitch.associateWith { (state.playedSeconds[it] ?: 0) + 1 },
            consecutiveSeconds = state.consecutiveSeconds +
                    state.onPitch.associateWith { (state.consecutiveSeconds[it] ?: 0) + 1 }
        )
        if (windowIdx < windows1.size && sec == windows1[windowIdx]) {
            windowIdx++
            val plan = RotationEngine.plan(state)
            println(
                "1e helft @${sec}s  IN: ${plan.onIds}  UIT: ${plan.offIds}"
            )
            state = state.copy(
                onPitch = (state.onPitch - plan.offIds.toSet()) + plan.onIds.toSet(),
                consecutiveSeconds = state.consecutiveSeconds +
                        plan.offIds.associateWith { 0 } + plan.onIds.associateWith { 0 }
            )
        }
    }

    // rust -> 2e helft
    state = state.copy(
        half = Half.SECOND, elapsedInHalf = 0,
        consecutiveSeconds = state.consecutiveSeconds.mapValues { 0 }
    )
    state = state.copy(onPitch = RotationEngine.normalizeOnPitch(state))
    println("2e helft start: IN HET VELD=${state.onPitch}")

    val windows2 = RotationEngine.windowSeconds(cfg, Half.SECOND)
    windowIdx = 0
    for (sec in 1..cfg.halfSeconds) {
        state = state.copy(
            elapsedInHalf = sec,
            playedSeconds = state.playedSeconds +
                    state.onPitch.associateWith { (state.playedSeconds[it] ?: 0) + 1 },
            consecutiveSeconds = state.consecutiveSeconds +
                    state.onPitch.associateWith { (state.consecutiveSeconds[it] ?: 0) + 1 }
        )
        if (windowIdx < windows2.size && sec == windows2[windowIdx]) {
            windowIdx++
            val plan = RotationEngine.plan(state)
            println(
                "2e helft @${sec}s  IN: ${plan.onIds}  UIT: ${plan.offIds}"
            )
            state = state.copy(
                onPitch = (state.onPitch - plan.offIds.toSet()) + plan.onIds.toSet(),
                consecutiveSeconds = state.consecutiveSeconds +
                        plan.offIds.associateWith { 0 } + plan.onIds.associateWith { 0 }
            )
        }
    }

    println("\n=== SPEELTIJD (wedstrijd = ${cfg.halfSeconds * 2}s) ===")
    val times = players.map { it to (state.playedSeconds[it.id] ?: 0) }
        .sortedByDescending { it.second }
    times.forEach { (p, s) ->
        val pct = 100.0 * s / (cfg.halfSeconds * 2)
        println("%-12s %4d s (%.1f%%)".format(p.label, s, pct))
    }
    val veldTimes = times.filter { it.first.id != 1L }.map { it.second }
    println(
        "\nVeldspelers: min=${veldTimes.min()}s max=${veldTimes.max()}s " +
                "verschil=${veldTimes.max() - veldTimes.min()}s"
    )
    println("Keeper: ${state.playedSeconds[1L]}s (hoort ${(cfg.halfSeconds * 2)}s te zijn)")
}
