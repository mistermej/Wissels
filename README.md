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

### 1. Startscherm
- Eén knop: **Start Voorbereiding**. Verder staat er niets — tegenstander, spelers en
  regels kies je in de volgende stap.

### 2. Voorbereiding
- **Team** kiezen (dropdown) en de **tegenstander** invullen.
- **Deelnemende spelers**: een vinklijst met iedereen uit de selectie.
- **Aantal spelers in het veld**: gekozen waarde uit de wedstrijdinstellingen, of — als daar
  *Vraag in Voorbereiding* staat — hier opgeven.
- **Keepers** per helft aanvinken. Staat **Vaste keeper** aan bij het team, dan komen de
  vinkjes al ingevuld mee en worden ze terug op het team opgeslagen.
- **Wedstrijdinstellingen aanpassen** opent de regels (helftduur, time-out, kaarten, wissels).
- Onderaan: **Wedstrijd starten**.

### 3. Wedstrijdinstellingen (menu)
- Alleen een **team kiezen** — aanmaken of hernoemen doe je onder **Teams**.
- **Tijd per helft**, **Aantal spelers in het veld** (optie *Vraag in Voorbereiding*, niet
  meer beperkt tot het aantal deelnemers), **Time-out** aan/uit (zonder duur), **Kaarten**
  aan/uit en **Wissels automatisch toepassen**.

### 4. Wedstrijd
- Geen actieve wedstrijd? Dan zie je **Geen wedstrijd actief** met een knop om te starten.
- Grote klok, stand, en **"Volgende wissel over 3:24"**.
- Op het wisselmoment trilt de telefoon + melding met **wie erin en wie eruit** gaat.
- **Start / Pauze** knop, **Start time-out / Einde time-out** (één per helft; hoe lang een
  time-out duurt is niet vastgelegd), **Wissel** (handmatig), **Kaart** (geel/rood — rode
  kaart haalt de speler uit de rotatie, alleen als kaarten aanstaan), **+ doelpunt**.
- Na de 1e helft druk je op **2e helft starten** (keeper van de 2e helft wordt automatisch geplaatst).

### 5. Wedstrijdoverzicht
- Zonder wedstrijd krijg je uitleg en een knop om te starten. Daarna: speeltijd per speler
  met balkje, alle kaarten en het volledige wedstrijdverloop.

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

### Nieuwe release maken (geen Android-sdk nodig)

GitHub Actions bouwt, tekent en hangt de APK automatisch aan de release zodra je een tag pusht:

```
git tag v0.2
git push origin v0.2
```

### Lokaal bouwen

```
./gradlew test              # draait de rotatie-simulatie
./gradlew assembleDebug     # gewone debug-APK
```

Voor een **getekende** release-build lokaal heb je `wissels.keystore` nodig. Die staat
versleuteld in de repo; herstellen met (Git for Windows bevat `openssl`):

```
openssl enc -aes-256-cbc -pbkdf2 -iter 600000 -d -in wissels.keystore.enc -out wissels.keystore
```

Het commando **vraagt om de bewaarpassphrase** — die staat in je wachtwoordbeheerder
(niet in de repo!). Vul daarna `keystore.properties` in (zie `app/build.gradle.kts`
voor de sleutelnamen; het bestand staat in `.gitignore`). Zonder keystore wordt de
release ongetekende gebouwd.

### Signing / keystore

- `wissels.keystore` (alias `wissels`) tekent de app. **Zonder dit bestand kan een nieuwe
  versie niet als update over een geïnstalleerde versie heen** — gebruikers moeten dan eerst
  verwijderen en verliezen hun teamopstelling.
- `wissels.keystore.enc` = AES-256-versleutelde backup in de repo; veilig zélfs als de repo
  publiek wordt. De bewaarpassphrase is nooit in git en leeft alleen in je wachtwoordbeheerder.
- CI tekent vanuit de GitHub-secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_PASSWORD`) — de platte keystore staat dus nergens in een repo.
- Verlies je de passphrase én je lokale bestand, dan is de sleutel voorgoed kwijt: geen
  digitaal kopietje bestaat dan nog.
