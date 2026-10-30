# Changelog

Alle wijzigingen aan dit project worden hier gedocumenteerd.

Het formaat is gebaseerd op [Keep a Changelog](https://keepachangelog.com/nl/1.0.0/),
en dit project volgt [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4] - 2026-10-30

### Toegevoegd
- **Automatische update**: wanneer er een nieuwe versie is, downloadt de app de
  APK zelf en opent daarna de Android-installer — je hoeft alleen nog te
  bevestigen. De voortgang is zichtbaar in het Over-scherm en de app vraagt
  indien nodig om toestemming voor het installeren van onbekende apps
- **Changelog in de app**: bij een beschikbare update zie je in het Over-scherm
  de knoppen **Installeren** en **Changelog**; de changelog-popup toont de
  release-opmerkingen van GitHub met daarin ook **Installeren** en **Annuleren**
- **Responsive layout**: alle afmetingen (knoppen, logo's, scorekolommen,
  rasterkolommen, paddings) schalen mee met de schermresolutie, met veilige
  onder- en bovengrenzen voor kleine telefoons en tablets
- **Tests voor meerdere resoluties**: 13 unit tests controleren de responsive
  afmetingen op resoluties van 320×568 tot 800×1280

### Gewijzigd
- **Menu**: het VV Wijster-logo is groter (26% van de schermhoogte, 120–220dp)
  zonder het menu breder te maken; de lege ruimte boven het logo is weggehaald
  zodat de menu-inhoud bovenaan begint
- **Over-scherm**: bij een beschikbare update vervangt de **Changelog**-knop de
  knop "Controleren op updates"; de **Installeren**-knop staat erboven
- **Wedstrijdscherm**: de actieknoppen onderaan zijn iets kleiner dan de overige
  knoppen
- **Voorgestelde wissel**: de keuzelijsten zijn nu even groot als de andere
  keuzelijsten
- **Update-check**: haalt naast de versie en de APK-link nu ook de changelog-tekst
  van de GitHub-release op

### Opgelost
- **Update-download**: redirects, lege downloads en het openen van de installer
  worden correct afgehandeld; download- en installatiefouten zijn zichtbaar in
  de app

## [0.3] - 2026-10-28

### Toegevoegd
- **Wissels per moment**: instellen hoeveel spelers er op een wisselmoment tegelijk
  wisselen (0 = *Alle tegelijk*, of 1 tot 8 spelers) — in de voorbereiding én
  tijdens de wedstrijd bij te stellen; kan nooit méér zijn dan er tegelijk KAN
  wisselen (bank én veldspelers zonder keeper) en toont *Geen wissels* als er
  niets te wisselen valt
- **"Klaarzetten voor de wissel"** in het wedstrijdscherm: bij het volgende
  wisselmoment zie je al in **twee keuzelijsten naast elkaar** welke spelers
  erin (links) en welke eruit (rechts) gaan
- **Doelpunten wegwerken**: per team een **−** naast de **+** bij de stand; het
  laatste doelpunt (en de regel in het verloop) wordt ongedaan gemaakt
- **Rust: opstelling 2e helft verifiëren en aanpassen** — de keeper staat als
  keuzelijst en door op spelers te tikken wissel je er één van het veld en één
  van de bank, vóór "2e helft starten"
- **Logo VV Wijster in het menu**: "Wissels" gecentreerd met het clublogo eronder
  (de versietekst is uit het menu verdwenen)
- **Statusbalk-melding bij een lopende wedstrijd**: blijvende (stille) melding
  met helft, klok en stand; tikken opent het wedstrijdscherm. Verdwijnt zodra de
  wedstrijd voorbij is of de app gestopt wordt
- **Keeper onderweg wijzigen**: dropdown "Keeper" boven het spelersraster in het
  wedstrijdscherm (alleen spelers die in het veld staan)
- **"Klaarzetten voor de wissel" aanpasbaar**: de keuzelijsten (erin/uit) kun je
  nu zelf bijstellen; de keuze blijft staan tot het wisselmoment
- **Keuze bij "Einde" in de 1e helft**: *Eerste helft beëindigen → rust*,
  *Hele wedstrijd beëindigen* of *Annuleren*

### Gewijzigd
- **Kleuren en thema**: het paars/grijze (Material-standaard) vervangen door een groen/wit thema (VV Wijster):
  - Banners (vooraanbalken) zijn nu groen (`primary`), kaarten zijn wit (geen paarsgrijze achtergrond meer)
  - Geselecteerde menu-items hebben een groene rand, niet paars vlak
  - De grijze vlakken (`surfaceVariant`) zijn nu wit, met groene tekst
- **Over-scherm**: het logo is groter (104dp), de vakken (Links / Updates) zijn op inhouds hoogte, tekst gecentreerd, knop "Controleren op updates" onder de statusgegevens
- **Menu / navigatie**: sluit automatisch zodra een menuitem gekozen is
- **Wedstrijdscherm**: de keeper-dropdown is verplaatst van het hoofdscherm naar de wissel-dialog (Handmatige Wissel); de knop is hernoemd naar "Handmatige Wissel" en bevat een keeper-selectie in de dialoog
- **Wissels per moment / veldspelers**: beide dropdowns staan nu boven de wissel-kaart-einde-rij; de veldformaat kan tijdens de wedstrijd aangepast worden
- **Update-check**: wordt bij het openen van de app uitgevoerd, status getoond in About, en een item in het menu toeft een update als die beschikbaar is; de knop in About forceert een nieuwe controle
- **Eerste helft beëindigen**: tekst "→ Rust" verwijderd, nu alleen nog "Eerste helft beëindigen"
- **Teams beheren**: alleen nog de namen van de spelers (geen vinkjes) — wél/niet
  meedoen bepaal je in de voorbereiding; spelers gesorteerd op rugnummer
  (1 bovenaan), of op naam als er geen nummers zijn; alle teams in één keuzelijst
  met potlood en prullenbak ernaast
- **Vaste keeper**: van keeper-vinkjes per speler naar **één keuzelijst** met een
  naam voor de hele wedstrijd
- **Keepers in de voorbereiding**: **één keuzelijst** als het team een vaste keeper
  heeft (vooraf ingevuld, nog wijzigbaar), anders **twee** (1e helft / 2e helft) —
  en **zonder keeper kan de wedstrijd niet starten**
- **Wissels met keuzelijst-paren**: de handmatige wissel én het bevestigen van een
  automatisch wisselvoorstel tonen paren *links erin – rechts eruit*; meerdere
  wissels tegelijk kunnen en ze zijn nog aanpasbaar vóór bevestiging
- **Spelers in een raster**: in het veld en op de wisselbank staan de spelers als
  tegels in een grid van maximaal 2 kolommen (2×2/2×3/2×4), met speeltijd,
  keeper-label en kaarten
- **Rugnummer** duidelijker: als badge (`#2`) bij Teams en als `#2 · Naam` op de
  andere schermen
- **Scorebord**: per team naam boven en stand daaronder, netjes gecentreerd
- **Startscherm**: geen "actief team" meer (het team kies je in de voorbereiding)
  en een grote, gecentreerde knop "Start Voorbereiding"
- **Wedstrijdinstellingen**: "Wissels per moment" staat hier niet meer — dat
  hangt af van de opkomst en bepaal je in de voorbereiding
- **Lege schermen** (Geen wedstrijd actief / Overzicht): tekst breekt netjes na
  de eerste zin en tekst + knop zijn groter
- Time-out-knop toont "Time-out gebruikt" in plaats van "T/o gebruikt"
- **Thema** staat vast op de dagstand (themakeuze volgt later)
- **Menu (zijbalk) is smaller**: de breedte volgt nu de langste menu-optie
  (minimaal 40%, maximaal 85% van de schermbreedte) — geen afgebroken regels
- **Over-scherm**: Links komt vóór Updates en het update-vak is kleiner dan de
  andere vakken
- **Teams**: potlood en prullenbak als **iconen** naast de teamkeuzelijst én bij
  elke speler — geen tekstknoppen ("Bewerk"/"Verwijder") meer
- **Over-scherm**: de vakken (app-versie, update-check, links) verdelen de
  beschikbare ruimte dynamisch; "Gemaakt door Jaimie" staat gecentreerd onderaan
- **Keuzelijsten passen nu naast elkaar**: `Modifier.weight(1f)` werd op het
  tekstveld in plaats van op de keuzelijst zelf gezet, waardoor het Teams-veld en
  de wissel-dialog de hele breedte innamen; de modifier zit nu op de
  `ExposedDropdownMenuBox`
- **Trilling**: van vier naar twee tikjes (0/250 ms) bij elk trillend moment
- README is alleen nog gebruikersdocumentatie; de ontwikkelaars-/keystore-informatie
  staat in `RELEASE_PROCESS.md`

### Verwijderd
- **Doelpunten en kaarten uit de notificatie-matrix**: er komt geen melding,
  trilling of geluid meer bij een doelpunt of kaart — dat zijn handmatige acties
  van de coach
- **"Open source"** op het Over-scherm
- **Kaarten in de notificatie-matrix**: kaarten verschillen niet meer in de meldingsinstellingen

## [0.2] - 2026-10-26

### Toegevoegd
- **Startscherm (splash)** — toont het VV Wijster-logo (ligt als afbeelding IN de APK:
  werkt ook zonder internet) en "Gemaakt door Jaimie" onderaan; navigeert na 2,5 seconden
  naar het startscherm
- **Navigatiemenu (zijbalk)** — bereikbaar vanaf alle schermen (Start wedstrijd, Wedstrijd,
  Overzicht, Wedstrijdinstellingen, Teams, App-instellingen, Over)
- **Voorbereidingsscherm** (`Start Voorbereiding`): team kiezen, tegenstander
  invullen, deelnemende spelers aanvinken (vinklijst), aantal spelers in het veld
  en de keepers bepalen — pas daar start de wedstrijd
- **Wedstrijdinstellingen per team**: helftduur, aantal spelers in het veld, time-out,
  kaarten en automatische wissels — met keuzelijsten (dropdowns)
- **Optie "Vraag in Voorbereiding"** bij *Aantal spelers in het veld*; het aantal
  wordt dan in het voorbereidingsscherm opgegeven
- **Kaarten aan/uit** in de wedstrijdinstellingen (de Kaart-knop volgt daarna)
- **Vaste keeper** per team: aan = de keeper-vinkjes staan bij het team en gaan mee
  naar de voorbereiding; uit = geen vinkjes in het Teams-scherm, de keepers bepaal
  je in de voorbereiding
- **App-instellingen vs. Wedstrijdinstellingen** uit elkaar getrokken:
  - *App-instellingen* = melding/trilling/geluid, met een matrix per actie
    (wisselmoment, rust, einde, time-out, doelpunt, kaart) én drie globale
    meesterschakelaars; wat per actie gebeurt is dus per kanaal in te stellen
  - *Wedstrijdinstellingen* = de spelregels, **per team**
- **Teams-scherm**: teams toevoegen/hernoemen/verwijderen/actief kiezen en per
  team de spelers (incl. keepers per helft) beheren
- **Navigation Compose** — de navigatie loopt via `NavHost` + `ModalNavigationDrawer`
  in plaats van een `when` op het huidige scherm
- **Update-controle** — knop in "Over" om GitHub releases te checken; toont of de app
  up-to-date is of dat er een update beschikbaar is, met een "Installeren"-knop en
  uitleg als de repo privé is
- **Over-scherm** — app-info, links naar VV Wijster/GitHub, credits, update-check
- Lege-staten met uitleg en actieknop: *Geen wedstrijd actief* (Wedstrijd),
  *Nog niets te overzien* (Overzicht) en *Geen voorbereiding actief*

### Gewijzigd
- De versie wordt overal uit het Android-manifest gelezen (`AppVersion`) in plaats van
  hardcoded; de update-check vergelijkt met de échte eigen versie
- Het clublogo wordt niet meer via internet geladen: Coil is verwijderd als afhankelijkheid
- **Basisscherm**: alleen nog "Start Voorbereiding" — tegenstander, teamkeuze,
  spelers en regels zijn verhuisd naar het voorbereidingsscherm
- **Wedstrijdinstellingen**: alleen nog een team *kiezen* (geen aanmaken of
  hernoemen, geen tegenstandersveld); de tegenstander vul je tijdens de
  voorbereiding in
- **Time-out**: geen duur meer (die is niet gedefinieerd) — alleen een toggle in
  de instellingen en tijdens de wedstrijd een knop **Start/Einde time-out**
- **Aantal spelers in het veld**: niet meer beperkt tot het aantal deelnemers
- **Over**: het bal-icoon is vervangen door het logo van VV Wijster
- APK's heten nu `Wissels-vX.X.apk` (release) en `Wissels-vX.X-debug.apk`
  (debug) in plaats van `app-release.apk`/`app-debug.apk`; de rename draait
  automatisch na `assembleRelease`/`assembleDebug`
- **Over**: alleen nog "Versie 0.2" — build-nummer en de oude omschrijving
  ("voetbal wissel-manager") zijn weg
- **Update-controle**: 404 van GitHub wordt herkend als *prive repository* en
  krijgt een uitleg + knop om de releasespagina handmatig te openen (andere
  fouten tonen "geen internet?")
- **Startscherm**: "Made by Jaimie" → "Gemaakt door Jaimie"
- Oude losse instellingen (één team) worden bij het laden gemigreerd naar het
  team-model, zodat bestaande spelers/instellingen behouden blijven

### Verwijderd
- Tegenstandersveld op het basisscherm en in de wedstrijdinstellingen
- Duur van de time-out (instelling én de terugteller tijdens de wedstrijd)
- Beperking van het veldformaat tot het aantal deelnemers, plus de automatische
  correctie van het veldformaat bij het verwijderen van een speler

### Opgelost
- **Crash bij het openen van de app** — `AndroidManifest.xml` miste de INTERNET-toestemming;
  de logo-lader crashte met `SecurityException`. Toestemming toegevoegd (en inmiddels
  overbodig: het logo zit in de APK)
- Na "Wedstrijd starten" gebeurde niets: de navigatie naar het wedstrijdscherm
  ontbrak (alleen de drawer navigeerde)
- Compileerfouten: `OutlinedButton` ontbrak in `SetupScreen` en
  `NotificationCompat.Builder.setVibrationPattern` bestaat niet (trilling gaat
  via het notificatiekanaal + de eigen tril-aanroep)

---

## [0.1.0] - 2024-XX-XX

### Toegevoegd
- Eerste werkende APK (Wissels-v0.1.apk)
- Kern wissel-logica: keeper vast, bank roteert op minst speeltijd
- Automatisch wisselen of bevestiging vragen
- Time-out ondersteuning (één per helft)
- Gele/rode kaarten (rode kaart haalt speler uit rotatie)
- GitHub Actions CI/CD voor getekende releases

---

## Sjabloon voor toekomstige releases

## [X.Y.Z] - JJJJ-MM-DD

### Toegevoegd
- Feature beschrijving

### Gewijzigd
- Verbetering beschrijving

### Opgelost
- Bug fix beschrijving

### Verwijderd
- Verwijderde feature beschrijving