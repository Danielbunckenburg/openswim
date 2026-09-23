# OpenSwim

OpenSwim is a free, open-source swim workout app. Its Wear OS prototype gives clear instructions at a glance in the pool. Visual quality, accessibility, and testing across watch sizes are the current priorities.

[Project website](https://danielbunckenburg.github.io/openswim/) · [Issues](https://github.com/Danielbunckenburg/openswim/issues) · [Roadmap](ROADMAP.md)

## Available now

- Twelve original offline workouts across Easy, Technique, Aerobic, Endurance, Threshold, and Sprint, with verified distances and repetitions.
- Wear OS Home, category-filtered library, workout detail, compatible pool selection, ready screen, active instruction, automatic rest countdown, pause/resume, manual repetition progression, and completion summary.
- Local recovery of the current workout and its progress after the app restarts.
- Framework-independent, serializable Kotlin workout model and tested session state machine.
- Static project website in `website/`.

Tap **Rep done** after swimming the displayed distance; the app advances to the next repetition and runs any planned rest. The selected pool length shows the planned number of lengths. A pool is selectable only when every step fits a full length. Current plans use meters; a yard pool is not offered. The app does not measure laps or persist a history of completed workouts. It restores the current manual session locally after a restart.

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
