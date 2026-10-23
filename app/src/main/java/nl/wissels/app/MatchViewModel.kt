package nl.wissels.app

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import nl.wissels.app.data.Store
import nl.wissels.app.engine.RotationEngine
import nl.wissels.app.model.CardType
import nl.wissels.app.model.EventKind
import nl.wissels.app.model.Half
import nl.wissels.app.model.MatchConfig
import nl.wissels.app.model.MatchEvent
import nl.wissels.app.model.MatchState
import nl.wissels.app.model.Player
import nl.wissels.app.model.PlayerCard
import nl.wissels.app.model.Screen
import nl.wissels.app.model.SubPlan
import nl.wissels.app.notify.Notifier

data class UiState(
    val screen: Screen = Screen.SETUP,
    val config: MatchConfig = MatchConfig(),
    val match: MatchState? = null,
    val setupError: String? = null,
    val toast: String? = null
)

class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var timerJob: Job? = null
    private var lastTickMs = 0L
    private var carryMs = 0L

    init {
        _ui.update { it.copy(config = Store.loadConfig(application)) }
    }

    // ---------------------------------------------------------------- setup

    fun updateConfig(transform: (MatchConfig) -> MatchConfig) {
        _ui.update { state ->
            val newCfg = transform(state.config)
            Store.saveConfig(getApplication(), newCfg)
            state.copy(config = newCfg, setupError = null)
        }
    }

    fun addPlayer(name: String, number: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _ui.update { it.copy(setupError = "Vul een naam in") }
            return
        }
        updateConfig { cfg ->
            val id = (cfg.players.maxOfOrNull { it.id } ?: 0L) + 1L
            cfg.copy(players = cfg.players + Player(id = id, name = trimmed, number = number.trim()))
        }
    }

    fun removePlayer(id: Long) = updateConfig { cfg ->
        cfg.copy(
            players = cfg.players.filterNot { it.id == id },
            fieldSize = cfg.fieldSize.coerceAtMost(
                (cfg.players.count { it.id != id && it.participate }).coerceAtLeast(2)
            )
        )
    }

    fun updatePlayer(id: Long, transform: (Player) -> Player) = updateConfig { cfg ->
        cfg.copy(players = cfg.players.map { if (it.id == id) transform(it) else it })
    }

    /** Alleen één keeper per helft: bij selecteren wissen we de keeper-status van de rest. */
    fun setKeeper(id: Long, half: Half, value: Boolean) = updateConfig { cfg ->
        val players = cfg.players.map { p ->
            var np = p
            if (value && p.id != id) {
                np = if (half == Half.FIRST) np.copy(keeperFirst = false)
                else np.copy(keeperSecond = false)
            }
            if (np.id == id) {
                np = if (half == Half.FIRST) np.copy(keeperFirst = value)
                else np.copy(keeperSecond = value)
            }
            np
        }
        cfg.copy(players = players)
    }

    fun toggleParticipant(id: Long) = updateConfig { cfg ->
        cfg.copy(players = cfg.players.map { if (it.id == id) it.copy(participate = !it.participate) else it })
    }

    fun clearError() = _ui.update { it.copy(setupError = null) }

    fun clearToast() = _ui.update { it.copy(toast = null) }

    // --------------------------------------------------------------- wedstrijd

    fun startMatch() {
        val cfg = _ui.value.config
        val participants = cfg.participants
        if (participants.size < cfg.fieldSize) {
            _ui.update {
                it.copy(setupError = "Te weinig spelers: ${cfg.fieldSize} in het veld nodig, " +
                        "maar er doen er ${participants.size} mee")
            }
            return
        }
        if (participants.isEmpty()) {
            _ui.update { it.copy(setupError = "Selecteer eerst spelers die meedoen") }
            return
        }

        val keeper = cfg.keeperOf(Half.FIRST)
        val starters = cfg.outfieldPool(Half.FIRST)
            .take(cfg.outfieldSpots)
            .map { it.id }
            .toMutableSet()
        if (keeper != null) starters.add(keeper.id)

        val state = MatchState(
            config = cfg,
            half = Half.FIRST,
            elapsedInHalf = 0,
            running = false,
            onPitch = starters.toSet(),
            playedSeconds = participants.associate { it.id to 0 },
            consecutiveSeconds = participants.associate { it.id to 0 }
        )
        _ui.update { it.copy(screen = Screen.MATCH, match = state, setupError = null, toast = null) }
        Notifier.ensureChannel(getApplication())
        startTimer()
    }

    fun toggleRun() {
        mutateMatch { m ->
            if (m.finished || m.atHalfTime) m
            else {
                if (!m.running && m.pending == null && m.timeoutLeft == null) {
                    notifyUpcomingWindow(m)
                }
                m.copy(running = !m.running, timeoutLeft = null)
            }
        }
    }

    fun toggleTimeout() {
        mutateMatch { m ->
            if (!m.config.timeoutEnabled || m.timeoutUsedThisHalf || m.atHalfTime || m.finished) m
            else {
                Notifier.wisselMoment(
                    getApplication(),
                    "Time-out",
                    "${m.config.timeoutMinutes} min time-out (${if (m.half == Half.FIRST) "1e" else "2e"} helft)"
                )
                m.copy(
                    timeoutLeft = m.config.timeoutMinutes * 60,
                    timeoutUsedThisHalf = true,
                    running = true
                )
            }
        }
    }

    fun goal(home: Boolean) = mutateMatch { m ->
        if (m.finished) m else {
            val ev = MatchEvent(
                half = m.half, secondInHalf = m.elapsedInHalf,
                kind = if (home) EventKind.GOAL_THUIS else EventKind.GOAL_UIT,
                text = if (home) "Doelpunt ${m.config.homeTeam}" else "Doelpunt ${m.config.awayTeam}"
            )
            if (home) m.copy(homeScore = m.homeScore + 1, events = m.events + ev)
            else m.copy(awayScore = m.awayScore + 1, events = m.events + ev)
        }
    }

    fun giveCard(playerId: Long, type: CardType) = mutateMatch { m ->
        val player = m.config.players.firstOrNull { it.id == playerId } ?: return@mutateMatch m
        val card = PlayerCard(playerId, type, m.half, m.elapsedInHalf)
        val kind = if (type == CardType.GEEL) EventKind.GEEL else EventKind.ROOD
        val ev = MatchEvent(
            half = m.half, secondInHalf = m.elapsedInHalf, kind = kind,
            text = "${if (type == CardType.GEEL) "Gele" else "Rode"} kaart: ${player.label}"
        )
        var new = m.copy(cards = m.cards + card, events = m.events + ev)
        if (type == CardType.ROOD) {
            // Rode kaart: speler eraf en de rest van de wedstrijd uit de rotatie.
            new = new.copy(
                redCards = m.redCards + playerId,
                onPitch = m.onPitch - playerId,
                consecutiveSeconds = new.consecutiveSeconds + (playerId to 0),
                pending = if (m.pending?.offIds?.contains(playerId) == true) null else m.pending
            )
        }
        new
    }

    /** Handmatige wissel door de coach (naast de automatische wisselmomenten). */
    fun manualSub(offIds: List<Long>, onIds: List<Long>) = mutateMatch { m ->
        if (offIds.isEmpty() || offIds.size != onIds.size) return@mutateMatch m
        applyPlan(
            m, SubPlan(offIds, onIds),
            buildString {
                append("Handmatige wissel — ")
                append(onIds.joinToString(", ") { nameOf(m, it) })
                append(" komt erin")
            }
        )
    }

    fun confirmPending() = mutateMatch { m ->
        val plan = m.pending ?: return@mutateMatch m
        val text = buildString {
            append("Wissel: ")
            append(plan.onIds.joinToString(", ") { nameOf(m, it) })
            append(" erin, ")
            append(plan.offIds.joinToString(", ") { nameOf(m, it) })
            append(" eruit")
        }
        applyPlan(m, plan, text)
    }

    fun dismissPending() = mutateMatch { m -> m.copy(pending = null) }

    fun startSecondHalf() = mutateMatch { m ->
        if (m.half != Half.FIRST) return@mutateMatch m
        val withKeeper = m.copy(
            half = Half.SECOND,
            elapsedInHalf = 0,
            atHalfTime = false,
            running = false,
            timeoutUsedThisHalf = false,
            pending = null,
            consecutiveSeconds = m.consecutiveSeconds.mapValues { 0 }
        )
        val normalized = withKeeper.copy(onPitch = RotationEngine.normalizeOnPitch(withKeeper))
        notifyUpcomingWindow(normalized)
        normalized
    }

    fun finishMatch() = mutateMatch { m ->
        if (m.finished) m else {
            Notifier.wisselMoment(
                getApplication(), "Wedstrijd voorbij",
                "Eindstand ${m.config.homeTeam} ${m.homeScore} – ${m.awayScore} ${m.config.awayTeam}"
            )
            m.copy(finished = true, running = false, timeoutLeft = null, atHalfTime = false, pending = null)
        }
    }

    fun goToSummary() = _ui.update { it.copy(screen = Screen.SUMMARY) }

    fun newMatch() {
        stopTimer()
        _ui.update { it.copy(screen = Screen.SETUP, match = null, toast = null, setupError = null) }
    }

    fun backToSetup() {
        stopTimer()
        _ui.update { it.copy(screen = Screen.SETUP, match = null, toast = null, setupError = null) }
    }

    // ------------------------------------------------------------ helpers

    private fun nameOf(m: MatchState, id: Long): String =
        m.config.players.firstOrNull { it.id == id }?.label ?: "?"

    private fun mutateMatch(transform: (MatchState) -> MatchState) {
        _ui.update { state ->
            val m = state.match ?: return@update state
            state.copy(match = transform(m))
        }
    }

    private fun applyPlan(m: MatchState, plan: SubPlan, eventText: String): MatchState {
        val onPitch = (m.onPitch - plan.offIds.toSet()) + plan.onIds.toSet()
        val newConsec = m.consecutiveSeconds +
                plan.offIds.associateWith { 0 } +
                plan.onIds.associateWith { 0 }
        val ev = MatchEvent(m.half, m.elapsedInHalf, EventKind.WISSEL, eventText)
        return m.copy(
            onPitch = onPitch,
            consecutiveSeconds = newConsec,
            pending = null,
            subsDone = m.subsDone + plan.onIds.size,
            events = m.events + ev
        )
    }

    private fun notifyUpcomingWindow(m: MatchState) {
        val windows = RotationEngine.windowSeconds(m.config, m.half)
        val next = windows.firstOrNull { it > m.elapsedInHalf } ?: return
        val preview = next - m.elapsedInHalf
        Notifier.wisselMoment(
            getApplication(),
            "Wedstrijd hervat",
            "Volgende wissel over ${formatTime(preview)}"
        )
    }

    // ----------------------------------------------------------------- timer

    private fun startTimer() {
        stopTimer()
        lastTickMs = SystemClock.elapsedRealtime()
        carryMs = 0L
        timerJob = viewModelScope.launch {
            while (true) {
                delay(200)
                val now = SystemClock.elapsedRealtime()
                val delta = now - lastTickMs
                lastTickMs = now
                val m = _ui.value.match
                if (m == null || !m.running || m.finished || m.atHalfTime) continue

                if (m.timeoutLeft != null) {
                    carryMs += delta
                    while (carryMs >= 1000L) {
                        carryMs -= 1000L
                        stepTimeout()
                    }
                    continue
                }

                carryMs += delta
                while (carryMs >= 1000L) {
                    carryMs -= 1000L
                    stepSecond()
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
        carryMs = 0L
    }

    /** Eén seconde wedstrijdklok: speeltijd bijwerken, wisselmomenten en helft-afloop. */
    private fun stepSecond() {
        mutateMatch { m ->
            var state = m.copy(
                elapsedInHalf = m.elapsedInHalf + 1,
                // NB: mappen aangevullen (niet vervangen), anders verdwijnt de tijd
                // van spelers op de wisselbank.
                playedSeconds = m.playedSeconds +
                        m.onPitch.associateWith { (m.playedSeconds[it] ?: 0) + 1 },
                consecutiveSeconds = m.consecutiveSeconds +
                        m.onPitch.associateWith { (m.consecutiveSeconds[it] ?: 0) + 1 }
            )

            // wisselmoment bereikt?
            val windows = RotationEngine.windowSeconds(state.config, state.half)
            if (windows.contains(state.elapsedInHalf)) {
                state = onWindowReached(state)
            }

            // helft afgelopen?
            if (state.elapsedInHalf >= state.config.halfSeconds) {
                state = if (state.half == Half.FIRST) {
                    Notifier.wisselMoment(
                        getApplication(), "Rust",
                        "Einde eerste helft — rust"
                    )
                    state.copy(running = false, atHalfTime = true, timeoutLeft = null, pending = null)
                } else {
                    Notifier.wisselMoment(
                        getApplication(), "Wedstrijd voorbij",
                        "Eindstand ${state.config.homeTeam} ${state.homeScore} – " +
                                "${state.awayScore} ${state.config.awayTeam}"
                    )
                    state.copy(finished = true, running = false, timeoutLeft = null, pending = null)
                }
            }
            state
        }
    }

    private fun onWindowReached(m: MatchState): MatchState {
        val plan = RotationEngine.plan(m)
        if (plan.offIds.isEmpty() && plan.onIds.isEmpty()) return m

        val inNames = plan.onIds.joinToString(", ") { nameOf(m, it) }
        val offNames = plan.offIds.joinToString(", ") { nameOf(m, it) }

        return if (m.config.autoApply) {
            applyPlan(m, plan, "Wissel: $inNames erin, $offNames eruit").also {
                Notifier.wisselMoment(
                    getApplication(), "Wisselmoment!",
                    "IN: $inNames\nUIT: $offNames"
                )
            }
        } else {
            Notifier.wisselMoment(
                getApplication(), "Wisselmoment!",
                "IN: $inNames\nUIT: $offNames"
            )
            m.copy(pending = plan)
        }
    }

    private fun stepTimeout() {
        mutateMatch { m ->
            val left = (m.timeoutLeft ?: return@mutateMatch m) - 1
            if (left <= 0) m.copy(timeoutLeft = null, running = true)
            else m.copy(timeoutLeft = left)
        }
    }

    override fun onCleared() {
        stopTimer()
        super.onCleared()
    }

    companion object {
        fun formatTime(totalSeconds: Int): String {
            val s = totalSeconds.coerceAtLeast(0)
            return "%d:%02d".format(s / 60, s % 60)
        }
    }
}
