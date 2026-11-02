package nl.wissels.app.engine

import nl.wissels.app.model.Player

/**
 * De startopstelling van de wedstrijd: wie beginnen er in het veld?
 *
 * De keeper van de 1e helft staat altijd in het veld en telt niet mee in de
 * keuze. De coach kiest zelf hoeveel veldspelers er beginnen; dat aantal
 * bepaalt de veldgrootte. Alle andere deelnemers beginnen op de bank en
 * wisselen daarna automatisch volgens de normale rotatie.
 */
object Lineup {

    /**
     * Veldgrootte (inclusief keeper) op basis van het aantal gekozen
     * veldspelers. De keeper staat er altijd bij.
     */
    fun fieldSize(outfieldStarters: Int): Int = outfieldStarters.coerceAtLeast(0) + 1

    /**
     * Houdt de opstelling kloppend: alleen deelnemers, nooit de keeper en
     * alleen spelers die de coach daadwerkelijk heeft aangevinkt. Er is geen
     * vast aantal plekken — de coach bepaalt de veldgrootte met zijn keuze.
     */
    fun sanitize(
        players: List<Player>,
        selectedIds: Set<Long>,
        keeperId: Long?,
        starterIds: Set<Long>
    ): Set<Long> = players
        .asSequence()
        .filter { it.id in selectedIds && it.id != keeperId && it.id in starterIds }
        .map { it.id }
        .toSet()
}
