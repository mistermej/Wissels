# Changelog

Alle wijzigingen aan dit project worden hier gedocumenteerd.

Het formaat is gebaseerd op [Keep a Changelog](https://keepachangelog.com/nl/1.0.0/),
en dit project volgt [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Ongepubliceerd]

### Toegevoegd
- Projectstructuur met Kotlin + Jetpack Compose
- RotationEngine: eerlijk wissel-algoritme met gelijke speeltijdfverdeling
- SetupScreen: teamconfiguratie, spelers, keepers per helft
- MatchScreen: live klok, stand, veld/bank lijsten, handmatige wissel/kaart/time-out
- SummaryScreen: speeltijdbalkjes, kaarten, volledige wedstrijdverloop
- Notifier: notificaties met hoge prioriteit + trilling
- Store: SharedPreferences opslag
- Unittest: RotationFairnessTest simuleert volledige wedstrijd
- **Voorbereidingsscherm** (`Start Voorbereiding`): team kiezen, tegenstander
  invullen, deelnemende spelers aanvinken (vinklijst), aantal spelers in het veld
  en de keepers bepalen — pas daar start de wedstrijd
- **Optie "Vraag in Voorbereiding"** bij *Aantal spelers in het veld*; het aantal
  wordt dan in het voorbereidingsscherm opgegeven
- **Kaarten aan/uit** in de wedstrijdinstellingen (de Kaart-knop volgt daarna)
- **Vaste keeper** per team: aan = de keeper-vinkjes staan bij het team en gaan mee
  naar de voorbereiding; uit = geen vinkjes in het Teams-scherm, de keepers bepaal
  je in de voorbereiding
- Lege-staten met uitleg en actieknop: *Geen wedstrijd actief* (Wedstrijd),
  *Nog niets te overzien* (Overzicht) en *Geen voorbereiding actief*
- **App-instellingen vs. Wedstrijdinstellingen** uit elkaar getrokken:
  - *App-instellingen* = melding/trilling/geluid, met een matrix per actie
    (wisselmoment, rust, einde, time-out, doelpunt, kaart) én drie globale
    meesterschakelaars; wat per actie gebeurt is dus per kanaal in te stellen
  - *Wedstrijdinstellingen* = de spelregels, **per team** (helftduur, aantal
    spelers in het veld, time-out, automatische wissels)
- **Teams-scherm**: teams toevoegen/hernoemen/verwijderen/actief kiezen en per
  team de spelers (incl. keepers per helft) beheren
- Keuzelijsten (dropdowns) voor *Tijd per helft*, *Aantal spelers in het veld*
  en *Duur time-out*

### Gewijzigd
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
- Na "Wedstrijd starten" gebeurde niets: de navigatie naar het wedstrijdscherm
  ontbrak (alleen de drawer navigeerde)
- Compileerfouten: `OutlinedButton` ontbrak in `SetupScreen` en
  `NotificationCompat.Builder.setVibrationPattern` bestaat niet (trilling gaat
  via het notificatiekanaal + de eigen tril-aanroep)

---

## [0.2] - 2024-XX-XX

### Toegevoegd
- **Startscherm (splash)** — toont het VV Wijster-logo (ligt nu als afbeelding IN de APK: werkt ook zonder internet), "Made by Jaimie" onderaan het scherm; navigeert na 2,5s naar instellingen
- **Navigatiemenu (zijbalk)** — bereikbaar vanaf alle schermen (Instellingen, Wedstrijd, Overzicht, Teams, App-instellingen, Over)
- **Instellingenscherm** — notificaties/trilling aan/uit (placeholders), spelregels: helftduur, time-out aan/uit + duur, automatische wissels aan/uit
- **Update-controle** — knop in "Over" om GitHub releases te checken (mistermej/Wissels); toont of app up-to-date is of update beschikbaar met "Installeren" knop
- **Teams beheer** — UI-skelet voor meerdere teams toevoegen/wijzigen/verwijderen/schakelen (persistentie volgt in volgende versie)
- **Over-scherm** — app-info, links naar VV Wijster/GitHub, credits, update-check

### Gewijzigd
- Navigatie van `when(state.screen)` naar `Navigation Compose` met `NavHost` + `ModalNavigationDrawer`
- `Screen` enum uitgebreid met `SPLASH`, `SETTINGS`, `TEAMS`, `ABOUT` en `route` property
- Versienummer wordt overal uit de Androidmanifest gelezen (`AppVersion`) — niet meer hardcoden (stond nog op "0.1" op enkele schermen)
- Update-controle vergelijkt de GitHub-tag met de échte eigen versie in plaats van met de vaste waarde "0.1"
- Logo niet meer via internet laden: Coil is verwijderd als afhankelijkheid

### Opgelost
- **Crash bij het openen van de app** — `AndroidManifest.xml` miste de INTERNET-toestemming; Coil/OkHttp crashte met `SecurityException: Permission denied (missing INTERNET permission?)`. Toestemming toegevoegd

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