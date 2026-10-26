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
import nl.wissels.app.model.NotifSettings
import nl.wissels.app.model.NotifyAction
import nl.wissels.app.model.Player
import nl.wissels.app.model.PlayerCard
import nl.wissels.app.model.PrepState
import nl.wissels.app.model.Screen
import nl.wissels.app.model.SubPlan
import nl.wissels.app.model.Team
import nl.wissels.app.model.TeamGameSettings
import nl.wissels.app.notify.Notifier

data class UiState(
    val screen: Screen = Screen.SETUP,
    val teams: List<Team> = emptyList(),
    val activeTeamId: Long = 1L,
    val awayTeam: String = "Tegenstander",
    val notif: NotifSettings = NotifSettings(),
    val match: MatchState? = null,
    /** Concept-wedstrijd tijdens de voorbereiding; `null` = geen voorbereiding actief. */
    val prep: PrepState? = null,
    val setupError: String? = null,
    val toast: String? = null
) {
    val activeTeam: Team
        get() = teams.firstOrNull { it.id == activeTeamId } ?: teams.firstOrNull() ?: Team(id = activeTeamId)

    /** Het team waar de voorbereiding op betrekking heeft (anders het actieve team). */
    val prepTeam: Team
        get() = prep?.let { p -> teams.firstOrNull { it.id == p.teamId } } ?: activeTeam

    /**
     * De wedstrijdconfig van het actieve team (spelers + spelregels van dat team).
     * Alleen voor weergave in de instellingen; de échte wedstrijdconfig ontstaat
     * in de voorbereiding ([MatchViewModel.startMatch]).
     */
    val config: MatchConfig
        get() = activeTeam.let { t ->
            MatchConfig(
                homeTeam = t.name,
                awayTeam = awayTeam,
                halfMinutes = t.settings.halfMinutes,
                fieldSize = t.settings.fieldSize ?: t.participants.size.coerceAtLeast(2),
                timeoutEnabled = t.settings.timeoutEnabled,
                cardsEnabled = t.settings.cardsEnabled,
                autoApply = t.settings.autoApply,
                players = t.players
            )
        }
}

class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var timerJob: Job? = null
    private var lastTickMs = 0L
    private var carryMs = 0L

    init {
        val teams = Store.loadTeams(application)
        _ui.update {
            it.copy(
                teams = teams,
                activeTeamId = Store.loadActiveTeamId(application),
                awayTeam = Store.loadAwayTeam(application),
                notif = Store.loadNotif(application)
            )
        }
    }

    // ---------------------------------------------------------------- teams

    fun addTeam(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _ui.update { it.copy(setupError = "Vul een naam in") }
            return
        }
        _ui.update { state ->
            val id = (state.teams.maxOfOrNull { it.id } ?: 0L) + 1L
            val teams = state.teams + Team(id = id, name = trimmed)
            persistTeams(state.copy(teams = teams, activeTeamId = id, setupError = null))
        }
    }

    fun renameTeam(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        _ui.update { state ->
            persistTeams(state.copy(teams = state.teams.map { if (it.id == id) it.copy(name = trimmed) else it }))
        }
    }

    /** Verwijdert een team (spelers + instellingen). Het actieve team schuift door. */
    fun deleteTeam(id: Long) {
        _ui.update { state ->
            if (state.teams.size <= 1) return@update state.copy(setupError = "Er moet minstens één team overblijven")
            val teams = state.teams.filterNot { it.id == id }
            val active = if (state.activeTeamId == id) teams.first().id else state.activeTeamId
            persistTeams(state.copy(teams = teams, activeTeamId = active, setupError = null))
        }
    }

    fun setActiveTeam(id: Long) {
        _ui.update { state ->
            if (state.teams.none { it.id == id }) state
            else persistTeams(state.copy(activeTeamId = id, setupError = null))
        }
    }

    // ------------------------------------------------- team-instellingen

    fun updateActiveTeamSettings(transform: (TeamGameSettings) -> TeamGameSettings) {
        _ui.update { state ->
            val teams = state.teams.map { t ->
                if (t.id == state.activeTeamId) t.copy(settings = transform(t.settings)) else t
            }
            persistTeams(state.copy(teams = teams))
        }
    }

    fun updateAwayTeam(name: String) {
        _ui.update { state ->
            Store.saveAwayTeam(getApplication(), name)
            state.copy(awayTeam = name)
        }
    }

    // ------------------------------------------------------------ spelers
    // Alles werkt op het actieve team.

    fun addPlayer(name: String, number: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _ui.update { it.copy(setupError = "Vul een naam in") }
            return
        }
        updateActiveTeam { t ->
            val id = (t.players.maxOfOrNull { it.id } ?: 0L) + 1L
            t.copy(players = t.players + Player(id = id, name = trimmed, number = number.trim()))
        }
    }

    fun removePlayer(id: Long) = updateActiveTeam { t ->
        // Het veldformaat wordt NIET meer automatisch aangepast: dat kies je
        // in de voorbereiding (of onder Wedstrijdinstellingen).
        t.copy(players = t.players.filterNot { it.id == id })
    }

    fun updatePlayer(id: Long, transform: (Player) -> Player) = updateActiveTeam { t ->
        t.copy(players = t.players.map { if (it.id == id) transform(it) else it })
    }

    /** Alleen één keeper per helft: bij selecteren wissen we de keeper-status van de rest. */
    fun setKeeper(id: Long, half: Half, value: Boolean) = updateActiveTeam { t ->
        t.copy(players = withKeeper(t.players, id, half, value))
    }

    fun toggleParticipant(id: Long) = updateActiveTeam { t ->
        t.copy(players = t.players.map { if (it.id == id) it.copy(participate = !it.participate) else it })
    }

    private fun updateActiveTeam(transform: (Team) -> Team) {
        _ui.update { state ->
            val teams = state.teams.map { if (it.id == state.activeTeamId) transform(it) else it }
            persistTeams(state.copy(teams = teams, setupError = null))
        }
    }

    private fun persistTeams(state: UiState): UiState {
        Store.saveTeams(getApplication(), state.teams, state.activeTeamId)
        return state
    }

    // -------------------------------------------------------- notificaties

    fun updateNotif(transform: (NotifSettings) -> NotifSettings) {
        _ui.update { state ->
            val n = transform(state.notif)
            Store.saveNotif(getApplication(), n)
            state.copy(notif = n)
        }
    }

    fun clearError() = _ui.update { it.copy(setupError = null) }

    fun clearToast() = _ui.update { it.copy(toast = null) }

    // -------------------------------------------------------- voorbereiding

    /**
     * Opent het voorbereidingsscherm met een concept-wedstrijd op basis van
     * het actieve team: deelnemers, keepers en veldformaat staan al klaar.
     */
    fun startPreparation() {
        _ui.update { state ->
            val t = state.activeTeam
            state.copy(prep = prepFor(t, state.awayTeam), setupError = null)
        }
    }

    /** Sluit de voorbereiding zonder een wedstrijd te starten. */
    fun cancelPreparation() = _ui.update { it.copy(prep = null, setupError = null) }

    /** Een ander team kiezen binnen de voorbereiding: het concept gaat mee over. */
    fun setPrepTeam(id: Long) {
        _ui.update { state ->
            val t = state.teams.firstOrNull { it.id == id } ?: return@update state
            val prep = state.prep ?: return@update state
            persistTeams(
                state.copy(
                    activeTeamId = id,
                    prep = prepFor(t, prep.awayTeam),
                    setupError = null
                )
            )
        }
    }

    fun setPrepAwayTeam(name: String) = _ui.update { state ->
        val prep = state.prep ?: return@update state
        Store.saveAwayTeam(getApplication(), name)
        state.copy(awayTeam = name, prep = prep.copy(awayTeam = name))
    }

    fun togglePrepParticipant(id: Long) = _ui.update { state ->
        val prep = state.prep ?: return@update state
        val selected = if (id in prep.selectedIds) prep.selectedIds - id else prep.selectedIds + id
        state.copy(
            setupError = null,
            prep = prep.copy(
                selectedIds = selected,
                // Keepers moeten zelf meedoen.
                keeperFirstId = prep.keeperFirstId?.takeIf { it in selected },
                keeperSecondId = prep.keeperSecondId?.takeIf { it in selected }
            )
        )
    }

    /** `null` = "nog niet bepaald" (team staat op "Vraag in Voorbereiding"). */
    fun setPrepFieldSize(value: Int?) = _ui.update { state ->
        val prep = state.prep ?: return@update state
        state.copy(prep = prep.copy(fieldSize = value), setupError = null)
    }

    fun setPrepKeeper(id: Long, half: Half, value: Boolean) = _ui.update { state ->
        val prep = state.prep ?: return@update state
        val newId = if (value) id else null
        val updated = if (half == Half.FIRST) prep.copy(keeperFirstId = newId)
        else prep.copy(keeperSecondId = newId)
        val newState = state.copy(prep = updated, setupError = null)

        // Vaste keeper: meteen op het team bewaren. Anders geldt de keuze
        // alleen voor deze wedstrijd.
        val team = newState.teams.firstOrNull { it.id == prep.teamId }
        if (team?.settings?.fixedKeeper != true) return@update newState

        persistTeams(
            newState.copy(
                teams = newState.teams.map { t ->
                    if (t.id != prep.teamId) t
                    else t.copy(players = withKeeper(t.players, id, half, value))
                }
            )
        )
    }

    private fun prepFor(team: Team, awayTeam: String): PrepState = PrepState(
        teamId = team.id,
        awayTeam = awayTeam,
        selectedIds = team.participants.map { it.id }.toSet(),
        fieldSize = team.settings.fieldSize,
        keeperFirstId = if (team.settings.fixedKeeper) team.players.firstOrNull { it.keeperFirst }?.id else null,
        keeperSecondId = if (team.settings.fixedKeeper) team.players.firstOrNull { it.keeperSecond }?.id else null
    )

    // --------------------------------------------------------------- wedstrijd

    /** Zet de voorbereiding om in een echte wedstrijd en opent het wedstrijdscherm. */
    fun startMatch() {
        val state = _ui.value
        val prep = state.prep
        if (prep == null) {
            _ui.update { it.copy(setupError = "Start eerst de voorbereiding") }
            return
        }
        val team = state.teams.firstOrNull { it.id == prep.teamId } ?: state.activeTeam

        if (prep.selectedIds.isEmpty()) {
            _ui.update { it.copy(setupError = "Vink minstens één speler aan die meedoet") }
            return
        }
        val fieldSize = prep.fieldSize
        if (fieldSize == null || fieldSize <= 0) {
            _ui.update { it.copy(setupError = "Kies hoeveel spelers er in het veld staan") }
            return
        }

        val keepersApplied = team.players.map { p ->
            p.copy(
                keeperFirst = p.id == prep.keeperFirstId,
                keeperSecond = p.id == prep.keeperSecondId
            )
        }
        // Vaste keeper: de keuze uit de voorbereiding weer terug op het team.
        if (team.settings.fixedKeeper && keepersApplied != team.players) {
            _ui.update { s ->
                persistTeams(
                    s.copy(teams = s.teams.map { t ->
                        if (t.id == team.id) t.copy(players = keepersApplied) else t
                    })
                )
            }
        }

        val awayTeam = prep.awayTeam.trim().ifBlank { "Tegenstander" }
        Store.saveAwayTeam(getApplication(), awayTeam)

        val cfg = MatchConfig(
            homeTeam = team.name,
            awayTeam = awayTeam,
            halfMinutes = team.settings.halfMinutes,
            fieldSize = fieldSize,
            timeoutEnabled = team.settings.timeoutEnabled,
            cardsEnabled = team.settings.cardsEnabled,
            autoApply = team.settings.autoApply,
            players = keepersApplied.map { it.copy(participate = it.id in prep.selectedIds) }
        )

        val participants = cfg.participants
        val keeper = cfg.keeperOf(Half.FIRST)
        val starters = cfg.outfieldPool(Half.FIRST)
            .take(cfg.outfieldSpots)
            .map { it.id }
            .toMutableSet()
        if (keeper != null) starters.add(keeper.id)

        val matchState = MatchState(
            config = cfg,
            half = Half.FIRST,
            elapsedInHalf = 0,
            running = false,
            onPitch = starters.toSet(),
            playedSeconds = participants.associate { it.id to 0 },
            consecutiveSeconds = participants.associate { it.id to 0 }
        )
        _ui.update {
            it.copy(
                screen = Screen.MATCH,
                match = matchState,
                prep = null,
                awayTeam = awayTeam,
                setupError = null,
                toast = null
            )
        }
        Notifier.ensureChannel(getApplication())
        startTimer()
    }

    fun toggleRun() {
        mutateMatch { m ->
            if (m.finished || m.atHalfTime || m.timeoutActive) m
            else {
                if (!m.running && m.pending == null) {
                    notifyUpcomingWindow(m)
                }
                m.copy(running = !m.running)
            }
        }
    }

    /** Start of beëindigt de time-out (geen vaste duur — de coach bepaalt wanneer). */
    fun toggleTimeout() {
        mutateMatch { m ->
            when {
                !m.config.timeoutEnabled || m.atHalfTime || m.finished -> m
                m.timeoutActive -> {
                    Notifier.action(
                        getApplication(), NotifyAction.TIMEOUT,
                        "Time-out voorbij",
                        "Wedstrijd hervat (${if (m.half == Half.FIRST) "1e" else "2e"} helft)"
                    )
                    m.copy(
                        timeoutActive = false,
                        timeoutWasRunning = false,
                        running = m.timeoutWasRunning,
                        events = m.events + MatchEvent(
                            half = m.half, secondInHalf = m.elapsedInHalf,
                            kind = EventKind.TIMEOUT, text = "Time-out beëindigd"
                        )
                    )
                }
                m.timeoutUsedThisHalf -> m
                else -> {
                    Notifier.action(
                        getApplication(), NotifyAction.TIMEOUT,
                        "Time-out",
                        "Time-out gestart (${if (m.half == Half.FIRST) "1e" else "2e"} helft)"
                    )
                    m.copy(
                        timeoutActive = true,
                        timeoutWasRunning = m.running,
                        running = false,
                        timeoutUsedThisHalf = true,
                        events = m.events + MatchEvent(
                            half = m.half, secondInHalf = m.elapsedInHalf,
                            kind = EventKind.TIMEOUT, text = "Time-out gestart"
                        )
                    )
                }
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
            Notifier.action(
                getApplication(), NotifyAction.DOELPUNT,
                "Doelpunt!",
                if (home) "${m.config.homeTeam} scoort — stand ${m.homeScore + 1} – ${m.awayScore}"
                else "${m.config.awayTeam} scoort — stand ${m.homeScore} – ${m.awayScore + 1}"
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
        Notifier.action(
            getApplication(), NotifyAction.KAART,
            "${if (type == CardType.GEEL) "Gele" else "Rode"} kaart",
            player.label
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
            timeoutActive = false,
            timeoutWasRunning = false,
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
            Notifier.action(
                getApplication(), NotifyAction.EINDE,
                "Wedstrijd voorbij",
                "Eindstand ${m.config.homeTeam} ${m.homeScore} – ${m.awayScore} ${m.config.awayTeam}"
            )
            m.copy(
                finished = true, running = false, atHalfTime = false, pending = null,
                timeoutActive = false, timeoutWasRunning = false
            )
        }
    }

    fun goToSummary() = _ui.update { it.copy(screen = Screen.SUMMARY) }

    fun newMatch() {
        stopTimer()
        _ui.update {
            it.copy(screen = Screen.SETUP, match = null, prep = null, toast = null, setupError = null)
        }
    }

    /** Terug naar het startscherm; het lopende/afgeronde overzicht blijft bewaard. */
    fun backToSetup() {
        _ui.update { it.copy(screen = Screen.SETUP, prep = null, toast = null, setupError = null) }
    }

    // ------------------------------------------------------------ helpers

    /** Zet de keeper-vlaggen: maximaal één keeper per helft. */
    private fun withKeeper(players: List<Player>, id: Long, half: Half, value: Boolean): List<Player> =
        players.map { p ->
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
        Notifier.action(
            getApplication(), NotifyAction.WISSELMOMENT,
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
                if (m == null || !m.running || m.finished || m.atHalfTime || m.timeoutActive) continue

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
                // NB: mappen aangevuld (niet vervangen), anders verdwijnt de tijd
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
                    Notifier.action(
                        getApplication(), NotifyAction.RUST,
                        "Rust",
                        "Einde eerste helft — rust"
                    )
                    state.copy(
                        running = false, atHalfTime = true, pending = null,
                        timeoutActive = false, timeoutWasRunning = false
                    )
                } else {
                    Notifier.action(
                        getApplication(), NotifyAction.EINDE,
                        "Wedstrijd voorbij",
                        "Eindstand ${state.config.homeTeam} ${state.homeScore} – " +
                                "${state.awayScore} ${state.config.awayTeam}"
                    )
                    state.copy(
                        finished = true, running = false, pending = null,
                        timeoutActive = false, timeoutWasRunning = false
                    )
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
                Notifier.action(
                    getApplication(), NotifyAction.WISSELMOMENT,
                    "Wisselmoment!",
                    "IN: $inNames\nUIT: $offNames"
                )
            }
        } else {
            Notifier.action(
                getApplication(), NotifyAction.WISSELMOMENT,
                "Wisselmoment!",
                "IN: $inNames\nUIT: $offNames"
            )
            m.copy(pending = plan)
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
