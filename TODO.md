# TODO / Backlog

Structured task list. Priority: **P1** = must have, **P2** = should have, **P3** = nice to have.

---

## P1 — Core Functionality

- [x] **Splash screen**
  - Show VV Wijster logo on launch (loaded from URL via Coil)
  - Bottom text: "Gemaakt door Jaimie"
  - Auto-navigate to SetupScreen after 2.5s

- [x] **Main menu / navigation drawer**
  - Accessible from all screens (Setup, Match, Summary, Settings, Teams, About)
  - Items: Instellingen, Wedstrijd, Overzicht, Teams, App-instellingen, Over

- [ ] **Match history / persistence**
  - Save finished matches (config + state) to local storage
  - List previous matches in SetupScreen
  - Load old match → view summary or resume (new match with same team)

- [ ] **Export match report**
  - Generate PDF or shareable text/image of SummaryScreen
  - Include: lineups, play times, cards, goals, timeline

- [ ] **Undo last action**
  - Undo button for: manual sub, card, goal, timeout
  - 5-second toast with undo, or dedicated button in MatchScreen

---

## P1 — Wensen ronde 2 (openstaand)

- [ ] **Teams beheren: alleen namen, geen deelname-vinkjes**
  - De lijst met spelers toont alleen de namen van de spelers die in het elftal zitten
  - Geen checkbox per speler in dit scherm: wel/niet meedoen wordt in de
    **voorbereiding** bepaald (vinklijst daar)
  - Keeper-vinkjes weg → **één dropdown per wedstrijd** met daarin de naam van de
    keeper selecteren (niet meer per helft)
  - Rugnummer duidelijker tonen: nu leest `2 XXXX` als "2 = naam"; oplossing
    verzinnen (bv. nummer in een badge/gedaan-kolom, of `#2 · XXXX`)

- [ ] **Wedstrijd starten: actieve team niet meer tonen**
  - Het basisscherm toont nu het team dat in Teams als *actief* staat — weg ermee
  - "Actief" in Teams is alleen bedoeld om te kiezen welk team je aanpast; het team
    voor de wedstrijd kies je in de voorbereiding

- [ ] **Voorbereiding: keepers via twee dropdowns**
  - In plaats van een checkbox-rij per speler: dropdown **1e helft** + dropdown
    **2e helft** waarin je de keeper kiest (alleen deelnemers als opties)

- [ ] **Wedstrijd: scorebord uitlijnen**
  - Het scorebord is verticaal niet netjes gecentreerd

- [ ] **Wedstrijd: doelpunten toevoegen én wegwerken**
  - Nu is er alleen `+` voor beide teams; een doelpunt kan niet ongedaan worden gemaakt
  - Mooi alternatief bedenken (bv. `−`-knop, lang drukken, of +/- per team)

- [ ] **Wedstrijd: time-out-knop tekst**
  - "T/o gebruikt" → **"Time-out gebruikt"**

- [ ] **Wedstrijd: zichtbaar wie klaarstaat voor de wissel**
  - Tijdens het spelen moet in het overzicht al duidelijk zijn wie erin en wie eruit
    gaat bij het volgende wisselmoment (bv. vooruitblik-kaart met namen)

- [ ] **Aantal wissels per moment instellen**
  - Instelbaar in de **voorbereiding** én tijdens het **spelen**
  - Bepaalt hoeveel spelers er tegelijk worden gewisseld

---

## P2 — UX Improvements

- [x] **Settings screen (via Menu)**
  - Notifications: enable/disable (placeholder — needs persistence)
  - Vibration: enable/disable (placeholder — needs persistence)
  - Gameplay rules:
    - Match duration (half time in minutes) ✓
    - Time-out toggle + duration ✓
    - Automatic substitute toggle ✓
    - Placeholders for: max substitutions per half, stoppage time

- [x] **Check for updates (via Menu → Over)**
  - Button to check GitHub releases for newer version
  - Show current version vs latest
  - If update available: "Installeren" → opens GitHub releases page in browser
  - If up to date: show "App is up-to-date (v0.1)"

- [x] **Teams management (via Menu → Teams)** — *UI skeleton, persistence not yet implemented*
  - List all saved teams (placeholder)
  - Add new team (placeholder)
  - Modify team: rename, edit players (redirects to SetupScreen)
  - Delete team (placeholder)
  - Switch active team (placeholder)

- [ ] **Player statistics across matches**
  - Total minutes, matches played, cards, goals per player
  - View in SetupScreen (long-press player → stats)

- [ ] **Formation / positions**
  - Optional: assign positions (GK, DEF, MID, FWD)
  - Rotation respects position groups (don't sub defender for striker)

- [ ] **Sound/vibration customization**
  - Settings: enable/disable vibration, choose notification sound

- [ ] **Dark mode polish**
  - Verify all screens in dark theme (keeper badge, cards, progress bars)

- [ ] **Accessibility**
  - Content descriptions for all icons
  - TalkBack support for live clock, substitution announcements

- [ ] **Landscape support**
  - MatchScreen: side-by-side on-pitch/bench on tablets/landscape

---

## P3 — Nice to Have

- [ ] **Import/export team (JSON)**
  - Share team config via file or QR code
  - Import team from JSON (via Menu → Teams → Import)

- [ ] **Half-time auto-pause**
  - Auto-pause at half-time, require explicit "Start 2nd Half"

- [ ] **Injury stoppage time**
  - Add extra time per half, adjust substitution windows proportionally

- [ ] **Widget / live activity**
  - Home screen widget with clock + next substitution

- [ ] **Wear OS support**
  - Companion app: vibration + next sub on watch

- [ ] **Localization**
  - Dutch (current), English, German, French strings

---

## Technical Debt / Refactors

- [ ] **Extract rotation logic to pure Kotlin module**
  - Move RotationEngine to `core` module (no Android deps)
  - Enables JVM tests, desktop preview, future multiplatform

- [ ] **StateFlow → Compose State migration**
  - MatchViewModel uses MutableStateFlow; could use `mutableStateOf` + `snapshotFlow` for simpler Compose integration

- [ ] **Add integration tests**
  - UI tests with Compose Test Rules for critical flows

- [x] **Dependency: Coil image loading** (for splash screen logo from URL)

- [ ] **Dependency updates**
  - Track Compose BOM, Kotlin, AGP versions quarterly

- [ ] **ProGuard/R8 rules**
  - Add rules for release build (currently `minifyEnabled = false`)

---

## Done / Archived

- [x] Initial project setup (Compose, Gradle, signing)
- [x] RotationEngine with fair distribution algorithm
- [x] SetupScreen with full team config
- [x] MatchScreen with live clock, subs, cards, timeout
- [x] SummaryScreen with play time bars + timeline
- [x] Notifications + vibration
- [x] SharedPreferences persistence
- [x] Unit test (RotationFairnessTest)
- [x] GitHub Actions CI/CD for signed releases
- [x] v0.1 APK released
- [x] **v0.2** — Splash screen, navigation drawer, settings, update check, teams UI, Coil

---

## How to Use

1. **Pick a task** → move to "In Progress" (add `🚧` emoji)
2. **Work on it** → commit with conventional message: `feat: add match history`
3. **Done** → check the box, move to "Done" section
4. **Weekly review** → reprioritize, add new items

> Tip: Keep this file updated. It's the single source of truth for what's next.