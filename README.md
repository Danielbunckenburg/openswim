# OpenSwim

OpenSwim is a free, open-source swim workout app. Its standalone Wear OS app supports local guided workouts and manual pool swims with clear information at a glance. Physical pool testing and accessibility review remain priorities.

[Project website](https://danielbunckenburg.github.io/openswim/) · [Issues](https://github.com/Danielbunckenburg/openswim/issues) · [Roadmap](ROADMAP.md)

## Available now

- Twelve original offline workouts across Easy, Technique, Aerobic, Endurance, Threshold, and Sprint, with verified distances and repetitions.
- Two standalone watch modes: Guided Workout with a 12-plan local library, and Pool Swim with explicit manual length logging.
- Shared pool setup, ready screen, session metrics, controls, deliberate hold-to-unlock, Drill/Kick distance entry, pause/resume, end confirmation, and completion summary.
- Local recovery of the current workout and its progress after the app restarts.
- Framework-independent, serializable Kotlin workout model and tested session state machine.
- Static project website in `website/`.

Guided Workout follows **Library → Detail → Pool size → Ready → Current / Metrics / Controls**. Tap **Rep done** after the displayed distance; the app advances to the next repetition and runs planned rest. Pool Swim follows **Pool size → Ready → Metrics / Controls**. Tap **Log length** for each completed length. Both modes allow manual Drill/Kick entries in pool-length steps. The watch does not measure laps: distance and time shown are based on local manual session state. A pool is selectable for a guided plan only when every step fits a full length. Current plans use meters; a yard pool is not offered. Completed workout history is not persisted, but an in-progress session is restored locally after restart.

The [verified watch screenshots](docs/screenshots/README.md) cover every major screen on a round emulator, with additional captures at a smaller display size.

## Planned

Health Services exercise tracking and real sensor metrics, local history, Android companion app, sync, accounts, web planning, and Apple platforms are future milestones. See [ROADMAP.md](ROADMAP.md).

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

The app id is `org.openswim.wear`. Source modules: `core/` (workout model and progression), `wear/` (watch app), and `website/` (static site). The repository is MIT licensed.

## Contribute

Read [CONTRIBUTING.md](CONTRIBUTING.md), choose an issue, and send a focused pull request. We welcome accessibility feedback and testing on actual pool hardware.
