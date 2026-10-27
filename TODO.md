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

## P1 — Wensen ronde 2 (afgerond in v0.3)

- [x] **Teams beheren: alleen namen, geen deelname-vinkjes**
  - De lijst met spelers toont alleen de namen van de spelers die in het elftal zitten
  - Geen checkbox per speler in dit scherm: wel/niet meedoen wordt in de
    **voorbereiding** bepaald (vinklijst daar)
  - Keeper-vinkjes weg → **één dropdown** met daarin de naam van de vaste keeper
    (voor de hele wedstrijd, niet per helft)
  - Rugnummer duidelijker: als **badge** (`#2`) in het Teams-scherm en als
    `#2 · Naam` op de andere schermen

- [x] **Wedstrijd starten: actieve team niet meer tonen**
  - Het basisscherm toont alleen nog "Start Voorbereiding" — het team kies je daar
  - "Actief" in Teams is alleen bedoeld om te kiezen welk team je aanpast

- [x] **Voorbereiding: keepers via twee dropdowns**
  - Dropdown **1e helft** + dropdown **2e helft** (alleen deelnemers als opties);
    de vaste keeper van het team staat hier alvast geselecteerd

- [x] **Wedstrijd: scorebord uitlijnen**
  - Per team een kolom (naam boven, stand eronder), symmetrisch en verticaal
    gecentreerd rond de klok

- [x] **Wedstrijd: doelpunten toevoegen én wegwerken**
  - Per team **−** en **+** naast de stand; − haalt het laatste doelpunt (én de
    regel in het wedstrijdverloop) ongedaan

- [x] **Wedstrijd: time-out-knop tekst**
  - "T/o gebruikt" → "Time-out gebruikt"

- [x] **Wedstrijd: zichtbaar wie klaarstaat voor de wissel**
  - Kaart "Volgende wissel" toont nu ook **"Klaarzetten voor de wissel"**: ▲ erin /
    ▼ uit met de namen (kan nog veranderen naarmate de wedstrijd vordert)

- [x] **Aantal wissels per moment instellen**
  - "Wissels per moment": 0 = alle tegelijk, of 1–8 spelers tegelijk
  - Instelbaar bij de **wedstrijdinstellingen**, in de **voorbereiding** én
    **tijdens het spelen** (geldt dan voor de rest van de helft)

- [x] **README: ontwikkelaars-/keystore-sectie eruit**
  - Verplaatst naar `RELEASE_PROCESS.md` (sectie 8: Technische details); de README
    is alleen nog gebruikersdocumentatie

---

## P1 — Wensen ronde 3 (afgerond in v0.3)

- [x] **Menu: logo VV Wijster**
  - "Wissels" gecentreerd met het clublogo eronder; de versietekst is weg

- [x] **Startscherm: grotere, gecentreerde startknop**
  - "Start Voorbereiding" is horizontaal én verticaal gecentreerd en groter

- [x] **Lege schermen (Wedstrijd/Overzicht): enter + grotere tekst/knop**
  - Bericht breekt na de eerste zin; tekst en actieknop zijn groter

- [x] **Wedstrijdinstellingen: "Wissels per moment" eruit**
  - Alleen nog in de voorbereiding (afhankelijk van de opkomst) en tijdens
    het spelen bij te stellen; niet meer vooraf vastleggen per team

- [x] **Teams: sorteren op rugnummer**
  - Rugnummer 1 bovenaan t/m 99 onderaan; zijn er geen nummers, dan op naam

- [x] **Teams: alle teams als keuzelijst**
  - Eén dropdown om het team te kiezen, met potlood (hernoemen) en
    prullenbak (verwijderen) ernaast

- [x] **Teams: "x in selectie • x wissel(s)" weg**
  - Aantallen hangen af van de opkomst; die horen niet in Teams

- [x] **Teams: "Wedstrijdregels…"-toelichting onderin weg**

- [x] **Voorbereiding: wissels per moment afgekapt op de bank**
  - Opties nooit meer dan bank (deelnemers − veldformaat)

- [x] **Keepers verplicht**
  - Zonder keeper kan de wedstrijd niet starten; heeft het team een vaste
    keeper, dan volstaat **één dropdown** (vooraf ingevuld, wijzigbaar)

- [x] **Wedstrijd: spelers als grid (max 2×4)**
  - In het veld en de wisselbank als dynamisch raster van tegels
    (2×2/2×3/2×4), met speeltijd, keeper-label en kaarten

- [x] **Rust: opstelling 2e helft verifiëren en aanpassen**
  - Keeper-keuzelijst + tikken om spelers te wisselen vóór "2e helft starten"

- [x] **Wissels met dropdown-paren (links erin – rechts eruit)**
  - Zowel de handmatige wissel als het bevestigen van een automatisch
    wisselvoorstel: per paar keuzelijstjes, meerdere wissels mogelijk,
    nog aanpasbaar vóór bevestiging

- [x] **Thema op dagstand**
  - App staat vast op de licht/dagstand; thema's zijn hieronder open

- [ ] **Thema's**
  - Keuze uit thema's (donker/licht/systeem, kleurvarianten); nu vast op dagstand

---

## P1 — Wensen ronde 4 (laatste gebruikersronde, afgerond in v0.3)

- [x] **Notificaties: doelpunten en kaarten er helemaal uit**
  - Niet meer in de notificatie-matrix (App-instellingen), en géén melding,
    trilling of geluid meer bij een doelpunt of kaart — het zijn handmatige
    acties van de coach

- [x] **Menu smaller**
  - Zijbalk is nu ~40% van de schermbreedte (was 70%)

- [x] **Teams: "Alle teams"-veld weer klein + iconen**
  - Potlood (bewerken) en prullenbak (verwijderen) als iconen naast de
    teamkeuzelijst; ook bij de spelersrijen — geen tekstknoppen meer

- [x] **Over: vakken dynamisch verdelen**
  - App-versie, update-check en links delen de ruimte (elk `weight(1f)`,
    scrollbaar bij kleine schermen); "Gemaakt door Jaimie" gecentreerd onderaan;
    "Open source" weg

- [x] **Wissels per moment: "Geen wissels" bij nul mogelijkheden**
  - In plaats van "Alle tegelijk" als er niets te wisselen valt

- [x] **Wissels per moment: cap op veldspelers − 1**
  - De keeper wordt niet automatisch gewisseld, dus het maximum is
    `min(bank, veldformaat − 1)` — bv. 2 in het veld + 3 op de bank → maximaal 1
  - Dezelfde cap in de voorbereiding én tijdens het spelen

- [x] **Klaarzetten voor de wissel: twee keuzelijsten naast elkaar**
  - Links "Erin", rechts "Uit" (alleen-lezen weergave) in plaats van twee
    tekstregels onder elkaar

- [x] **Handmatige wissel past op het scherm**
  - Oorzaak: `Modifier.weight(1f)` zat op het tekstveld in de keuzelijst in
    plaats van op de `ExposedDropdownMenuBox` — daardoor werd de weight
    genegeerd (geldt ook voor het Teams-veld)

---

## P1 — Wensen ronde 5

- [x] **Menu: geen afgebroken labels**
  - De breedte volgt nu de LANGSTE menu-optie (gemeten), met 40% van het
    scherm als minimum en 85% als maximum — dus niets op twee regels

- [x] **Over: Links vóór Updates, Updates-vak kleiner**
  - Volgorde: app-info → Links → Updates; het update-vak krijgt minder ruimte
    (`weight(0.6f)` t.o.v. `1f`)

- [x] **"Klaarzetten voor de wissel" is nu écht een keuzelijst**
  - De coach mag de paren (erin/uit) aanpassen; de keuze wordt bewaard
    (`MatchState.previewPlan`) en gaat mee naar het wisselmoment — vervalt hij
    daar (bv. na een kaart), dan telt de automatische berekening weer

- [x] **Keeper onderweg wijzigen**
  - Dropdown "Keeper" boven het spelersraster in het wedstrijdscherm; alleen
    spelers die in het veld staan kunnen keeper worden (`setMatchKeeper`)

- [x] **"Einde" in de 1e helft geeft een keuze**
  - Dialoog met *Eerste helft beëindigen → rust*, *Hele wedstrijd
    beëindigen* en *Annuleren*; in de 2e helft blijft het zoals het was

- [x] **Statusbalk-melding bij een lopende wedstrijd**
  - Blijvende (stille) melding met helft, klok en stand; tikken opent het
    wedstrijdscherm. Verdwijnt bij het einde van de wedstrijd en bij het
    stoppen van de app (`Notifier.matchStatus` / `cancelMatchStatus`)

---

## P1 — Wensen ronde 6 (afgerond in v0.4)

- [x] **Keeper-dropdown in plaats in wedstrijd**
  - Verplaats van het hoofdscherm naar de wissel-dialog (ManualSubDialog);
    knop hernoemd naar "Handmatige Wissel"
- [x] **"Wissels per moment" + "Spelers in het veld" knoppen boven de wissel**
  - Raden boven de wissel-kaart-einde-rij; het veldformaat instelbaar
  - Menu sluiten zodra een menuitem gekozen is
- [x] **Update-check bij het openen**
  - Start: gezamenlijke controle (in About, in menu, in matchstart)
  - Over: tonen of up-to-date of update beschikbaar (logo groter)
  - Links-vak minder hoog, passend op het aantal links; Updates-vak idem;
    tekst meer centreren en de knop "Controleren op updates" ónder de tekst plaatsen
- [x] **Totaal groen/wit thema (VV Wijster)**
  - Geen paars/grijs; banners groen, Selectie met groene rand, ruwe kaarten wit met groene randen
- [x] **Notificatie-matrix + statusbalk – toegevoegd in UpdateChecker**
  - Vrijgevestigde staat in Matchescherm, Over-scherm en menu (rollen naar About bij update)
- [x] **"Eerste helft beëindigen" (zonder → rust)**
  - Tekst "Eerste helft beëindigen → rust" → "Eerste helft beëindigen" in de einddialog

---

## P2 — UX Improvements

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