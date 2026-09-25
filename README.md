# OpenSwim

OpenSwim is a free, open-source swim workout app. Its standalone Wear OS app supports local guided workouts and manual pool swims with clear information at a glance. Physical pool testing and accessibility review remain priorities.

[Project website](https://danielbunckenburg.github.io/openswim/) · [Web app](https://danielbunckenburg.github.io/openswim/app.html) · [Issues](https://github.com/Danielbunckenburg/openswim/issues) · [Roadmap](ROADMAP.md)

## Available now

- Twelve original offline workouts across Easy, Technique, Aerobic, Endurance, Threshold, and Sprint, with verified distances and repetitions.
- Two standalone watch modes: Guided Workout with a 12-plan local library, and Pool Swim with explicit manual length logging.
- Shared pool setup, ready screen, session metrics, controls, deliberate hold-to-unlock, Drill/Kick distance entry, pause/resume, end confirmation, and completion summary.
- Local recovery of the current workout and its progress after the app restarts.
- Framework-independent, serializable Kotlin workout model and tested session state machine.
- Project website and cloud-connected [web app](docs/web/WEB_APP.md) in `website/`.

Guided Workout follows **Library → Detail → Pool size → Ready → Current / Metrics / Controls**. Tap **Rep done** after the displayed distance; the app advances to the next repetition and runs planned rest. Pool Swim follows **Pool size → Ready → Metrics / Controls**. Tap **Log length** for each completed length. Both modes allow manual Drill/Kick entries in pool-length steps. The watch does not measure laps: distance and time shown are based on local manual session state. A pool is selectable for a guided plan only when every step fits a full length. Current plans use meters; a yard pool is not offered. Completed workout history is not persisted, but an in-progress session is restored locally after restart.

The [verified watch screenshots](docs/screenshots/README.md) cover every major screen on a round emulator, with additional captures at a smaller display size.

## Planned

Health Services exercise tracking and real sensor metrics, watch-to-phone sync, richer web planning, and Apple platforms are future milestones. The Android companion has cloud account sign-in and cloud-backed reads; see [Android app structure](docs/android/ANDROID_APP_STRUCTURE.md). See [ROADMAP.md](ROADMAP.md).

## Develop on Windows

Install Android Studio with Android SDK 36, JDK 17, and an AVD named `OpenSwim_WearOS`. The included Gradle wrapper uses Gradle 9.6. Set `ANDROID_HOME` to the SDK directory and `JAVA_HOME` to the JDK directory, or let `scripts/run-wear.ps1` discover Android Studio's bundled JDK and the standard SDK location.

```powershell
.\gradlew.bat build
.\gradlew.bat test
.\gradlew.bat lint
.\scripts\run-wear.ps1
```

Individual emulator commands:

```powershell
& "$env:ANDROID_HOME\emulator\emulator.exe" -avd OpenSwim_WearOS
& "$env:ANDROID_HOME\platform-tools\adb.exe" wait-for-device
.\gradlew.bat :wear:assembleDebug
& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r wear\build\outputs\apk\debug\wear-debug.apk
& "$env:ANDROID_HOME\platform-tools\adb.exe" shell am start -n org.openswim.wear/.MainActivity
```

The watch app id is `org.openswim.wear`. Source modules: `core/` (shared workout and planning models), `wear/` (watch app), `android/` (phone companion), and `website/` (project site and web app). The repository is MIT licensed.

The phone app is in `android/` (`org.openswim.android`). Add `supabase.url` and `supabase.publishableKey` to ignored `local.properties`, build with `.\gradlew.bat :android:assembleDebug`, and launch it on an Android phone emulator or device. It reads cloud workouts, plans, and results, and supports email/password Auth. It does not connect to the Wear app yet.

The backend foundation is versioned in `supabase/` with a privacy-first schema, RLS policies, fictional local seed data, and database security tests. The schema is deployed to a [Supabase cloud project](docs/backend/CLOUD_DEPLOYMENT.md) in Frankfurt. Start with the [backend architecture](docs/backend/ARCHITECTURE.md) and [security notes](docs/backend/SECURITY.md). The cloud database has no seeded workouts or users; public workouts must be published before the phone library has content.

## Contribute

Read [CONTRIBUTING.md](CONTRIBUTING.md), choose an issue, and send a focused pull request. We welcome accessibility feedback and testing on actual pool hardware.
