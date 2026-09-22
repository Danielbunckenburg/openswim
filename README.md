# OpenSwim

OpenSwim is a free, open-source swim workout app. The first milestone is a Wear OS prototype designed for clear instructions at a glance in the pool.

## Available now

- Four local structured workouts with verified totals.
- Wear OS workout library, detail, pool selection, active instruction, automatic rest countdown, pause/resume, manual repetition progression, and completion summary.
- Framework-independent, serializable Kotlin workout model and tested session state machine.
- Static project website in `website/`.

This prototype advances when you tap **Next length**. It does not measure a swim or save completed sessions after the app closes. Pool selection is shown for preparation; workouts currently use their authored units and are not converted to a different pool length.

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
