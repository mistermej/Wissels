package nl.wissels.app.engine

import nl.wissels.app.model.Player
import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit-tests voor het bepalen en opschonen van de startopstelling. */
class LineupTest {

    private val players = (1L..12L).map { Player(id = it, name = "Speler $it") }
    private val allSelected = players.map { it.id }.toSet()

    @Test
    fun fieldSizeIsStartersPlusKeeper() {
        assertEquals(7, Lineup.fieldSize(6))
        assertEquals(11, Lineup.fieldSize(10))
        assertEquals(2, Lineup.fieldSize(1))
    }

    @Test
    fun noStartersMeansOnlyTheKeeper() {
        assertEquals(1, Lineup.fieldSize(0))
    }

    @Test
    fun sanitizeKeepsTheChosenStarters() {
        val lineup = Lineup.sanitize(
            players = players,
            selectedIds = allSelected,
            keeperId = 1L,
            starterIds = setOf(9L, 12L)
        )
        assertEquals(setOf(9L, 12L), lineup)
    }

    @Test
    fun sanitizeRemovesTheKeeper() {
        val lineup = Lineup.sanitize(
            players = players,
            selectedIds = allSelected,
            keeperId = 4L,
            starterIds = setOf(2L, 3L, 4L, 5L)
        )
        assertEquals(setOf(2L, 3L, 5L), lineup)
    }

    @Test
    fun sanitizeRemovesNonParticipants() {
        val selected = setOf(2L, 3L, 4L, 5L, 6L, 7L, 8L)
        val lineup = Lineup.sanitize(
            players = players,
            selectedIds = selected,
            keeperId = 2L,
            starterIds = setOf(2L, 9L, 3L, 10L)
        )
        assertEquals(setOf(3L), lineup)
    }

    @Test
    fun anyNumberOfStartersIsAllowed() {
        // Geen vast veldformaat meer: één, drie of iedereen mag beginnen.
        assertEquals(setOf(2L), Lineup.sanitize(players, allSelected, 1L, setOf(2L)))
        assertEquals(
            setOf(2L, 5L, 8L),
            Lineup.sanitize(players, allSelected, 1L, setOf(2L, 5L, 8L))
        )
        assertEquals(
            (2L..12L).toSet(),
            Lineup.sanitize(players, allSelected, 1L, allSelected - 1L)
        )
    }

    @Test
    fun emptyStarterSelectionStaysEmpty() {
        assertEquals(
            emptySet<Long>(),
            Lineup.sanitize(players, allSelected, 1L, emptySet())
        )
    }

    @Test
    fun withoutKeeperEveryParticipantCanStart() {
        val lineup = Lineup.sanitize(
            players = players,
            selectedIds = allSelected,
            keeperId = null,
            starterIds = setOf(1L, 2L)
        )
        assertEquals(setOf(1L, 2L), lineup)
    }
}
