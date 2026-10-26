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
                fieldSize = t.settings.fieldSize ?: t.players.size.coerceAtLeast(2),
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

    /**
     * Vaste keeper van het team: één naam voor de hele wedstrijd (beide helften).
     * `null` = geen vaste keeper; de keepers bepaal je dan in de voorbereiding.
     */
    fun setTeamKeeper(id: Long?) = updateActiveTeam { t ->
        t.copy(players = t.players.map { p ->
            p.copy(keeperFirst = p.id == id, keeperSecond = p.id == id)
        })
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
            ).clamped()
        )
    }

    /** `null` = "nog niet bepaald" (team staat op "Vraag in Voorbereiding"). */
    fun setPrepFieldSize(value: Int?) = _ui.update { state ->
        val prep = state.prep ?: return@update state
        state.copy(prep = prep.copy(fieldSize = value).clamped(), setupError = null)
    }

    /**
     * Hoeveel spelers er per wisselmoment tegelijk wisselen (0 = alle tegelijk).
     * Kan nooit méér zijn dan er op de bank zit (deelnemers − veldformaat).
     */
    fun setPrepSubsPerWindow(value: Int) = _ui.update { state ->
        val prep = state.prep ?: return@update state
        state.copy(prep = prep.copy(subsPerWindow = value).clamped(), setupError = null)
    }

    /** Keeper van een helft kiezen (`null` = geen keeper). Alleen voor deze wedstrijd. */
    fun setPrepKeeper(id: Long?, half: Half) = _ui.update { state ->
        val prep = state.prep ?: return@update state
        val updated = if (half == Half.FIRST) prep.copy(keeperFirstId = id)
        else prep.copy(keeperSecondId = id)
        state.copy(prep = updated, setupError = null)
    }

    /**
     * Vaste keeper: één keuzelijst die voor beide helften geldt (de coach kan
     * de naam nog wel bijstellen).
     */
    fun setPrepFixedKeeper(id: Long?) = _ui.update { state ->
        val prep = state.prep ?: return@update state
        state.copy(prep = prep.copy(keeperFirstId = id, keeperSecondId = id), setupError = null)
    }

    /** Houdt "wissels per moment" binnen wat er op de bank zit. */
    private fun PrepState.clamped(): PrepState {
        val bank = (selectedIds.size - (fieldSize ?: 0)).coerceAtLeast(0)
        return if (subsPerWindow > bank) copy(subsPerWindow = bank) else this
    }

    /**
     * Het concept van de voorbereiding op basis van het team: de hele selectie
     * doet mee (aan/uit vink je hier), keepers en veldformaat staan al klaar.
     */
    private fun prepFor(team: Team, awayTeam: String): PrepState = PrepState(
        teamId = team.id,
        awayTeam = awayTeam,
        selectedIds = team.players.map { it.id }.toSet(),
        fieldSize = team.settings.fieldSize,
        // 0 = "Alle tegelijk"; meer kan niet dan de bank (zie [clamped]).
        subsPerWindow = 0,
        keeperFirstId = team.players.firstOrNull { it.keeperFirst }?.id,
        keeperSecondId = team.players.firstOrNull { it.keeperSecond }?.id
            ?: team.players.firstOrNull { it.keeperFirst }?.id
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
        // Zonder keeper in de 1e helft kan er niet gespeeld worden. De keeper van
        // de 2e helft mag "Wordt later bepaald" zijn.
        if (prep.keeperFirstId == null) {
            _ui.update { it.copy(setupError = "Kies de keeper van de 1e helft") }
            return
        }

        val keepersApplied = team.players.map { p ->
            p.copy(
                keeperFirst = p.id == prep.keeperFirstId,
                keeperSecond = p.id == prep.keeperSecondId
            )
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
            subsPerWindow = prep.subsPerWindow,
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
                else "${m.config.awayTeam} scoort — stand ${m.homeScore} – ${m.awayScore + 1}",
                vibrate = false
            )
            if (home) m.copy(homeScore = m.homeScore + 1, events = m.events + ev)
            else m.copy(awayScore = m.awayScore + 1, events = m.events + ev)
        }
    }

    /** Maakt het laatste doelpunt van een team ongedaan (per ongeluk op + gedrukt). */
    fun removeGoal(home: Boolean) = mutateMatch { m ->
        val kind = if (home) EventKind.GOAL_THUIS else EventKind.GOAL_UIT
        val idx = m.events.indexOfLast { it.kind == kind }
        if (idx == -1) return@mutateMatch m
        val events = m.events.toMutableList().apply { removeAt(idx) }
        if (home) m.copy(homeScore = (m.homeScore - 1).coerceAtLeast(0), events = events)
        else m.copy(awayScore = (m.awayScore - 1).coerceAtLeast(0), events = events)
    }

    /**
     * Aantal wissels per moment aanpassen tijdens de wedstrijd: geldt voor de
     * rest van de huidige helft en kan nooit méér zijn dan er op de bank zit.
     */
    fun setMatchSubsPerWindow(value: Int) = mutateMatch { m ->
        val keeperId = m.config.keeperOf(m.half)?.id
        val bench = m.config.participants.count { it.id !in m.onPitch && it.id != keeperId }
        m.copy(config = m.config.copy(subsPerWindow = value.coerceIn(0, bench)))
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
            player.label,
            vibrate = false
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

    fun confirmPending(plan: SubPlan) = mutateMatch { m ->
        if (plan.offIds.isEmpty() || plan.offIds.size != plan.onIds.size) return@mutateMatch m
        val keeperId = m.config.keeperOf(m.half)?.id
        val poolIds = m.config.outfieldPool(m.half).map { it.id }.toSet()
        val valid = plan.offIds.all { it in m.onPitch && it != keeperId } &&
                plan.onIds.all { it !in m.onPitch && it in poolIds } &&
                plan.offIds.toSet().size == plan.offIds.size &&
                plan.onIds.toSet().size == plan.onIds.size &&
                plan.onIds.none { it in plan.offIds }
        if (!valid) return@mutateMatch m
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

    /** Keeper van de 2e helft wijzigen (of bevestigen) tijdens de rust. */
    fun setHalfTimeKeeper(id: Long?) = mutateMatch { m ->
        val on = m.halfTimeOnPitch ?: return@mutateMatch m
        if (id == null || id == m.halfTimeKeeperId) return@mutateMatch m
        m.copy(
            halfTimeKeeperId = id,
            halfTimeOnPitch = on.filter { it != m.halfTimeKeeperId }.toSet() + id
        )
    }

    /** Opstelling 2e helft aanpassen: één speler eruit, één erin. */
    fun swapHalfTime(offId: Long, onId: Long) = mutateMatch { m ->
        val on = m.halfTimeOnPitch ?: return@mutateMatch m
        if (offId == m.halfTimeKeeperId || offId !in on || onId in on) return@mutateMatch m
        m.copy(halfTimeOnPitch = (on - offId) + onId)
    }

    fun startSecondHalf() = mutateMatch { m ->
        if (m.half != Half.FIRST) return@mutateMatch m
        // Zonder keeper van de 2e helft mag de 2e helft niet starten.
        val keeperId = m.halfTimeKeeperId ?: return@mutateMatch m
        // Keeper-keuze vastleggen, zodat de rest van de wedstrijd (en het
        // overzicht) met de juiste doelvelder rekent.
        val withKeeper = m.copy(
            config = m.config.copy(
                players = m.config.players.map { p -> p.copy(keeperSecond = p.id == keeperId) }
            ),
            half = Half.SECOND,
            elapsedInHalf = 0,
            atHalfTime = false,
            running = false,
            timeoutActive = false,
            timeoutWasRunning = false,
            timeoutUsedThisHalf = false,
            pending = null,
            halfTimeKeeperId = null,
            halfTimeOnPitch = null,
            onPitch = m.halfTimeOnPitch ?: emptySet(),
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
                timeoutActive = false, timeoutWasRunning = false,
                halfTimeKeeperId = null, halfTimeOnPitch = null
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
                    val halfTime = state.copy(
                        running = false, atHalfTime = true, pending = null,
                        timeoutActive = false, timeoutWasRunning = false
                    )
                    // Concept-opstelling 2e helft: keeper verifiëren en aanpassen
                    // kan nog vóór "2e helft starten". Als de keeper van de 2e
                    // helft "Wordt later bepaald" was, staat de keuze hier leeg.
                    val secondKeeperChosen = halfTime.config.players.any { it.keeperSecond }
                    val norm = RotationEngine.normalizeOnPitch(halfTime.copy(half = Half.SECOND))
                    halfTime.copy(
                        halfTimeOnPitch = norm,
                        halfTimeKeeperId = if (secondKeeperChosen) halfTime.config.keeperOf(Half.SECOND)?.id else null
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
