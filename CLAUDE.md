# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK
./gradlew build                # Full build
./gradlew clean                # Clean build artifacts
./gradlew test                 # Run unit tests
./gradlew lint                 # Run lint checks
```

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Architecture:** MVVM
- **Backend:** Firebase Auth + Firestore
- **Min SDK:** 26 (Android 8), Target SDK: 35 (Android 14/15)

## Architecture

```
com.pushapp/
├── model/              # Data classes (User, WorkoutEntry)
├── repository/         # Firebase data access (AuthRepository, WorkoutRepository)
├── viewmodel/          # State management (AuthViewModel, WorkoutViewModel)
├── ui/
│   ├── navigation/     # AppNavigation — bottom nav + route definitions
│   ├── screens/        # One file per screen
│   └── theme/          # Material3 theme, colors, typography
└── notification/       # AlarmManager-based daily reminders at 20:00 MSK
```

**Data flow:** Composable screens → ViewModels (StateFlow) → Repositories → Firebase

## Key Design Patterns

- ViewModels expose state via `StateFlow<T>`; screens collect with `collectAsState()`
- Repositories wrap Firebase calls in Kotlin coroutines and return `Result<T>`
- Auth uses synthetic emails: `"$username@pushapp.app"` to support username-only login
- Firestore collection `workouts` uses composite index on `(userId, date)` — see `firestore.indexes.json`
- `WorkoutRepository.getUserWorkoutsForPeriod()` fetches all user docs then filters on client (no Firestore date range filter)
- Notifications: `NotificationHelper` schedules next-day alarm recursively inside `ReminderReceiver.onReceive()`; `BootReceiver` restores the alarm after reboot

## Firebase Setup

`app/google-services.json` must be present (from Firebase Console). The project uses:
- Firebase Auth (email/password)
- Firestore with a `workouts` collection and a `users` collection
