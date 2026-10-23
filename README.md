# Wissels ⚽

Een Android-app die **automatisch berekent wanneer en wie er gewisseld moet worden** tijdens een
voetbalwedstrijd, zodat iedereen ongeveer evenveel speeltijd krijgt.

## De APK installeren op je telefoon

1. Kopieer **`Wissels-v0.1.apk`** naar je Android-telefoon (via USB, WhatsApp, e-mail, Google Drive, ...).
2. Open het bestand op de telefoon.
3. Android vraagt: *"Je telefoon installatie van onbekende bronnen niet toestaan"* → tik op
   **Instellingen** en zet **Toestaan van deze bron** aan.
4. Terug en tik op **Installeren**. Klaar.

> De app werkt vanaf Android 8.0 (API 26) en ouder niet — nieuwere telefoons zijn allemaal oké.
> Bij de eerste start geef je meldingen toestaan, anders mis je de seintjes bij een wisselmoment.

## Zo gebruik je de app

### 1. Instellingen (startscherm)
- **Tijd per helft**: bijv. 35 minuten.
- **Aantal spelers in het veld**: bijv. 11 (of 7/8 bij zaalvoetbal). De keeper telt hierin mee.
- **Time-out**: aan/uit + duur in minuten.
- **Wissels automatisch toepassen**: uit = je krijgt eerst een melding en bevestigt zelf,
  aan = de app wisselt direct.
- **Spelers**: voeg iedereen van je selectie toe. Vink aan wie meedoet, en kies per helft de
  **keeper** (de keeper draait niet mee in de rotatie — hij speelt de hele helft).
- Onderaan zie je hoeveel wissels het oplevert: *deelnemers − spelers in het veld = wissels*.

### 2. Wedstrijd
- Grote klok, stand, en **"Volgende wissel over 3:24"**.
- Op het wisselmoment trilt de telefoon + melding met **wie erin en wie eruit** gaat.
- **Pauze / Start** knop, **Time-out** (een per helft), **Wissel** (handmatig), **Kaart**
  (geel/rood — rode kaart haalt de speler uit de rotatie), **+ doelpunt** voor beide teams.
- Na de 1e helft druk je op **2e helft starten** (keeper van de 2e helft wordt automatisch geplaatst).

### 3. Wedstrijdoverzicht
- Speeltijd per speler met balkje, alle kaarten en het volledige wedstrijdverloop.

## Hoe de berekening werkt

- De keeper van de betreffende helft staat altijd in het veld en wordt nooit gewisseld.
- De wisselmomenten staan vooraf vast: **even verdeeld over de helft**, één moment per
  wisselspeler (max 8 per helft).
- Op elk moment gaat de **bank er volledig in** en gaan de spelers met de **meeste speeltijd**
  eruit (bij gelijkheid: wie het langst onafgebroken speelde, anders de volgorde in de selectie).
- Zo krijgt iedere veldspeler over de hele wedstrijd nagenoeg evenveel minuten.

Voorbeeld (14 spelers, 11 in het veld, 2×35 min, simulatietest):
keeper 100%, alle veldspelers 75–87,5% (ideaal = 77%).

## Ontwikkelaars

- Kotlin + Jetpack Compose, package `nl.wissels.app`, minSdk 26 / targetSdk 34.
- Geen server/geen database: de selectie en instellingen staan in SharedPreferences op de telefoon.
- Builden:
  ```
  gradle assembleRelease     # levert app-release.apk op
  gradle test                # draait de rotatie-simulatie
  ```
- Signing: `wissels.keystore` (alias `wissels`). Dit bestand staat **niet** in de repo
  (staat in `.gitignore`) — maak er een offline backup van. Zonder dit keystore kan een
  volgende versie niet over de huidige heen worden geïnstalleerd.
