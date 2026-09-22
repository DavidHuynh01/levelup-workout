# Level Up: Workout

An Android app for logging workouts and competing on leaderboards ranked by total volume
lifted, personal records, and training consistency.

Log your lifts, and the app works out the rest: volume per session, three kinds of personal
record per exercise, and a training streak.

## Status

All six phases are built and running on a device.

| Phase | Feature | State |
|---|---|---|
| 1 | Accounts, sessions, exercise catalogue | Done |
| 2 | Workout logging, history, edit and delete | Done |
| 3 | Personal records, streaks, consistency | Done |
| 4 | Global and friends leaderboards | Done |
| 5 | Friend requests with an in-app badge | Done |
| 6 | Profile editing, record history, empty and loading states | Done |

## Features

- **Accounts** — sign up and sign in, with the session surviving a force-stop and a reboot.
- **Workout logging** — pick from a 53-exercise catalogue or add your own, then enter sets,
  reps and weight with a running volume total as you type. Warmup sets are marked and
  excluded from totals and records.
- **History** — every session grouped by month, with per-month volume, tap through for the
  full set breakdown, and edit, delete or repeat any past workout. Repeating opens the log
  screen already filled in with the same exercises and weights, dated today.
- **Personal records** — three per exercise, detected automatically: heaviest weight, best
  Epley-estimated 1RM, and best single-session volume. Breaking one raises a dialog naming
  what you beat and by how much, and each exercise keeps a full record timeline including
  the records it has since beaten.
- **Rest timer** — presets of 1, 1:30, 2 and 3 minutes on the log screen, with +30s and
  skip. The remaining time is derived from when the rest started, so it stays right after
  the app has been backgrounded. Your last-used length is remembered.
- **Progress chart** — estimated 1RM per session for an exercise, plotted from the sessions
  themselves rather than from the record chain, so plateaus and bad weeks are visible
  instead of being smoothed into a staircase.
- **Profile** — lifetime stats, an editable display name, and an emoji avatar that shows up
  on the leaderboard.
- **Streaks and consistency** — a running day streak with a one-day grace period, a longest
  streak, and sessions logged this ISO week.
- **Leaderboards** — a global board and a friends-only board, each rankable by total volume,
  current streak or number of records. Your own row is highlighted wherever it lands.
- **Friends** — search lifters, send requests, accept or decline, with a count badge on the
  Profile tab. If two people add each other at once it resolves to a friendship rather than
  a pair of stuck requests.
- **Units** — weights are stored in kilograms and displayed in pounds or kilograms; the
  toggle in Profile never rewrites stored data.

## Tech

| | |
|---|---|
| Language | Kotlin 2.2.21 |
| UI | Jetpack Compose, Material 3 |
| Database | Room 2.8.5 (SQLite), schema v1 exported to `app/schemas` |
| Preferences | DataStore |
| Build | AGP 8.11.1, Gradle 8.14.1, Java 17 |
| Min / target SDK | 29 (Android 10) / 36 |
| Dependency injection | A hand-written `AppContainer`; no Hilt |

Data lives on the device. Every repository is an interface in `domain/repository` with a
Room implementation behind it, so a Firebase implementation can be added later by changing
one line per repository in `AppContainer` — no UI changes.

## Getting started

Requires Android Studio, JDK 17 or newer, and a device or emulator on Android 10+.

```bash
git clone https://github.com/<your-username>/<repo-name>.git
```

Open the folder in Android Studio, let Gradle sync, and press Run.

From the command line on Windows, where `java` is not on PATH:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:installDebug
```

Debug builds seed eight demo lifters with about six months of training history, so the
leaderboard has something in it from the first launch. Release builds start empty.

## Project structure

```
app/src/main/java/com/davidhuynh/levelup/
├── domain/               # no android imports anywhere in here
│   ├── model/            # Workout, ExerciseSet, PersonalRecord, UserStats, …
│   ├── logic/            # VolumeCalculator, OneRepMax, PrDetector, StreakCalculator,
│   │                     # ConsistencyCalculator, PasswordPolicy, WeightConverter
│   ├── security/         # PBKDF2 password hashing, token generation
│   ├── repository/       # interfaces only — the seam a Firebase backend would slot into
│   └── usecase/          # SaveWorkout, DeleteWorkout, sign up / in / out
├── data/
│   ├── local/            # Room entities, DAOs, relations, seed data
│   ├── prefs/            # DataStore session and settings
│   ├── repository/       # Room implementations + DerivedDataRecomputer
│   └── mapper/           # entity <-> domain
├── di/                   # AppContainer and the ViewModel factory
└── ui/                   # theme, navigation, and one package per screen
```

Keeping `domain/` free of `android.*` imports is what lets the calculation logic be covered
by fast JVM tests with no Robolectric.

### How records and stats are kept correct

`personal_records` and `user_stats` are caches with no authority. After any change to a
workout they are rebuilt from the sets that remain, never patched in place. That is the only
approach that handles the three cases that actually come up:

- Deleting the workout that held a record has to *lower* that record to the previous best.
- Editing a workout to remove an exercise has to withdraw that exercise's records.
- Logging a back-dated workout can change which achievement came first, reordering the chain.

The streak and this-week count are derived at read time instead of being cached, because
they change with the calendar rather than with anything the user does.

## Tests

```powershell
.\gradlew.bat :app:testDebugUnitTest          # 99 JVM tests, no emulator needed
.\gradlew.bat :app:connectedDebugAndroidTest  # 18 Room tests, needs a device
```

The JVM tests cover volume, 1RM estimation, record detection (including chain rebuilds after
a delete and after a back-dated entry), streaks across month ends and daylight saving,
password policy and hashing, leaderboard ranking and ties, progress series, rest timing, and unit conversion. The instrumented tests cover the same
recompute paths against a real database, foreign-key cascade behaviour, and the friend
request flow.

Debug builds also give a new account two pending friend requests from demo lifters, so the
accept and decline paths have something to act on.

## Notes

- Passwords are hashed with PBKDF2-HMAC-SHA256, 120,000 iterations and a per-account salt.
  The plaintext is never stored.
- With no server, the session token is a local marker of when a sign-in expires rather than
  a credential that can be presented anywhere.
- Signing out clears the session only. Workout history stays on the device, so signing back
  in restores it.
