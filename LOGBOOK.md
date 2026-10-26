# Development Logbook

Daily/weekly notes on development progress, decisions, and context.

---

## 2024-XX-XX — Project Initialization

- Created Android project with Compose, minSdk 26, targetSdk 34
- Package: `nl.wissels.app`
- Set up Gradle with Compose BOM 2024.09.02, Kotlin 1.9+, Java 17

## 2024-XX-XX — Core Engine (RotationEngine)

- Implemented substitution window calculation: evenly spaced across half
- Algorithm: bench players with least play time → on pitch; most play time → off
- Tie-breakers: consecutive seconds, then roster order
- Keepers excluded from rotation (fixed per half)
- Max 8 substitution windows per half

## 2024-XX-XX — Data Model & Persistence

- Models: Player, MatchConfig, MatchState, SubPlan, MatchEvent, PlayerCard
- Store.kt: SharedPreferences with JSON for player list
- No database, no server — fully local

## 2024-XX-XX — UI Screens

### SetupScreen
- Team names, half minutes, field size (2-20)
- Time-out toggle + duration
- Auto-apply substitutions toggle
- Player management: add/edit/delete, participate toggle, keeper per half
- Live substitute count display

### MatchScreen
- Large clock with half indicator
- Score buttons for both teams
- On-pitch list (green) + bench list (gray)
- Actions: Start/Pause, Time-out, Manual Sub, Card, Finish
- Pending substitution dialog (when autoApply = false)

### SummaryScreen
- Final score + substitution count
- Play time per player with progress bars (keepers = tertiary color)
- Cards list with color coding
- Full event timeline with icons

## 2024-XX-XX — Notifications & Timer

- Notifier.kt: NotificationChannel (IMPORTANCE_HIGH), vibration pattern
- Android 13+ POST_NOTIFICATIONS permission handling
- ViewModel timer: 200ms tick, accumulates ms → steps seconds
- Handles timeout countdown separately from match clock

## 2024-XX-XX — Testing

- RotationSim.kt: simulates 14 players, 11v11, 2×35 min
- RotationFairnessTest runs simulation as unit test
- Output shows fair distribution: keeper 100%, outfield 75-87.5%

## 2024-XX-XX — Release Pipeline

- GitHub Actions: build, sign, attach APK to release on tag push
- Keystore encrypted in repo (`wissels.keystore.enc`), decrypted via `openssl` + passphrase
- Local signed build requires `keystore.properties` (gitignored)
- Version: 0.1 (versionCode 1)

---

## Ongoing Decisions / Open Questions

| Date | Topic | Decision / Notes |
|------|-------|------------------|
|      | Max field size | Currently 20 — enough for 11v11 + subs |
|      | Max half minutes | 90 — covers full 90+extra time |
|      | Red card handling | Removes from rotation, team plays short |
|      | Keeper change at half | Auto-places 2nd half keeper via normalizeOnPitch |
|      | No undo for manual sub | Intentional — keep it simple |

---

## Commands Reference

```bash
# Run tests (includes rotation simulation)
./gradlew test

# Debug APK
./gradlew assembleDebug

# Release APK (needs keystore)
./gradlew assembleRelease

# Decrypt keystore for local signing
openssl enc -aes-256-cbc -pbkdf2 -iter 600000 -d -in wissels.keystore.enc -out wissels.keystore
```