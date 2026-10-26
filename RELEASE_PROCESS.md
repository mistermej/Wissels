# Release Proces (voor toekomstige versies)

Dit document beschrijft hoe je een nieuwe release maakt, de changelog bijwerkt, een PR opent en de release tagged.

---

## 1. Versie bepalen

Volg **Semantic Versioning** (MAJOR.MINOR.PATCH), maar voor deze app gebruiken we **MAJOR.MINOR** (bijv. 0.1, 0.2, 0.3):
- **MAJOR** — breaking changes (bijv. 0.x → 1.0)
- **MINOR** — nieuwe features, backwards compatible (bijv. 0.1 → 0.2)
- **PATCH** — bugfixes (alleen versionCode verhogen, versionName blijft gelijk, bijv. 0.2 met versionCode 3)

Voor deze app:
- `versionCode` = integer (moet steeds oplopen, bv. 1, 2, 3...)
- `versionName` = string (bijv. "0.2")

---

## 2. Release stappen

### Stap 1: Werk de CHANGELOG.md bij

1. Open `CHANGELOG.md`
2. Verplaats items uit `## [Ongepubliceerd]` naar een nieuwe sectie `## [X.Y.Z] - JJJJ-MM-DD`
3. Schrijf in het **Nederlands**, gericht op **gebruikers** (niet technisch jargon)
4. Gebruik categorieën: `### Toegevoegd`, `### Gewijzigd`, `### Opgelost`, `### Verwijderd`

**Voorbeeld:**
```markdown
## [0.2] - 2024-01-15

### Toegevoegd
- Startscherm met VV Wijster logo
- Navigatiemenu met toegang tot alle schermen
- Instellingen voor notificaties en spelregels
- Update-controle via GitHub
- Teams beheer (UI)

### Gewijzigd
- Navigatie herschreven naar Navigation Compose
```

### Stap 2: Update versionCode / versionName

In `app/build.gradle.kts`:
```kotlin
defaultConfig {
    versionCode = 2        // moet hoger zijn dan vorige release
    versionName = "0.2"    // MAJOR.MINOR formaat
}
```

### Stap 3: Commit en push naar dev branch

```bash
git checkout -b dev/v0.2
git add .
git commit -m "chore: release v0.2

- Update CHANGELOG.md
- Bump versionCode to 2, versionName to 0.2"
git push origin dev/v0.2
```

### Stap 4: Open Pull Request op GitHub

1. Ga naar https://github.com/mistermej/Wissels/pulls
2. Klik **New pull request**
3. Base: `main` ← Compare: `dev/v0.2`
4. Titel: `Release v0.2`
5. Beschrijving: **kopieer de CHANGELOG.md sectie voor deze versie** (Nederlands!)

**PR beschrijving sjabloon:**
```markdown
## Release v0.2

### Toegevoegd
- Startscherm met VV Wijster logo
- Navigatiemenu (zijbalk) bereikbaar overal
- Instellingenscherm: notificaties, spelregels
- Update-controle in "Over" scherm
- Teams beheer (UI)

### Gewijzigd
- Navigatie gemigreerd naar Navigation Compose

### Testen
- [ ] App start → splash → instellingen
- [ ] Menu openen → alle items navigeren correct
- [ ] Instellingen wijzigen → worden opgeslagen
- [ ] "Over" → update check werkt
- [ ] Wedstrijd starten/stoppen werkt nog
```

### Stap 5: Review & Merge

- Je doet de merge zelf op GitHub (Squash and merge aanbevolen)
- Na merge: verwijder de dev branch (GitHub biedt dit aan)

### Stap 6: Tag en Release maken (trigger CI)

```bash
git checkout main
git pull origin main
git tag v0.2
git push origin v0.2
```

**Dit triggert GitHub Actions:**
- Bouwt debug + release APK
- Tekent release APK met keystore (uit GitHub Secrets)
- Maakt GitHub Release aan met APK als asset
- Gebruikt de tag naam als release titel

### Stap 7: GitHub Release notities

Op de releases pagina (https://github.com/mistermej/Wissels/releases):
1. Klik op de zojuist gemaakte release `v0.2`
2. Klik **Edit**
3. Plak de **CHANGELOG.md sectie** als release notities (Nederlands)
4. Publiceer

---

## 3. GitHub Secrets (éénmalig instellen)

Voor signed releases moeten deze secrets in GitHub repo settings → Secrets → Actions:

| Secret | Waarde |
|--------|--------|
| `KEYSTORE_BASE64` | `base64 -w0 wissels.keystore \| tee` output |
| `KEYSTORE_PASSWORD` | keystore wachtwoord |
| `KEY_ALIAS` | `wissels` |
| `KEY_PASSWORD` | key wachtwoord |

> Zie `README.md` voor decrypt instructies.

---

## 4. Checklist voor elke release

- [ ] CHANGELOG.md bijgewerkt (Nederlands, gebruiksvriendelijk)
- [ ] `versionCode` +1, `versionName` geüpdatet in `build.gradle.kts`
- [ ] Dev branch aangemaakt en gepushed
- [ ] PR geopend met CHANGELOG als beschrijving
- [ ] PR gemerged (squash)
- [ ] Tag aangemaakt (`vX.Y.Z`) en gepushed
- [ ] GitHub Release notities ingevuld (kopieer uit CHANGELOG)
- [ ] APK getest op telefoon (debug + release)

---

## 5. Handige commando's samengevat

```bash
# Nieuwe feature branch
git checkout -b feat/nieuwe-feature

# Na development: release prep
git checkout main
git pull
git checkout -b dev/v0.3

# CHANGELOG.md en versionCode/Name aanpassen
git add CHANGELOG.md app/build.gradle.kts
git commit -m "chore: release v0.3"
git push origin dev/v0.3

# PR openen via GitHub web UI, mergen

# Na merge:
git checkout main
git pull
git tag v0.3
git push origin v0.3
```

---

## 6. Veelgemaakte fouten

| Fout | Oplossing |
|------|-----------|
| `versionCode` niet verhoogd | Play Store/APK installatie faalt: "versie niet nieuwer" |
| Tag push vergeten | CI draait niet, geen release APK |
| CHANGELOG niet bijgewerkt | Gebruikers zien niet wat er nieuw is |
| Release notities leeg | Gebruikers zien geen details in GitHub Releases |
| Keystore secrets verkeerd | Release build faalt met signing error |

---

## 7. Voorbeeld: v0.3 planning

Als volgende features klaar zijn:
- Match history opslaan/laden
- Export wedstrijdverslag (PDF)
- Undo knop
- Teams persistentie

Dan:
1. `versionCode = 3`, `versionName = "0.3"`
2. CHANGELOG: nieuwe sectie `[0.3]`
3. Tag `v0.3` pushen
4. GitHub Actions bouwt `Wissels-v0.3.apk`