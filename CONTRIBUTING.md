# Contributing

OpenSwim is an early Wear OS swim workout prototype. It has local sample workouts and manual workout progression; it does not measure a swim or store completed sessions. Start with [README.md](README.md) and [ROADMAP.md](ROADMAP.md).

## Find work

Choose an open issue, especially one labeled `good first issue` or `help wanted`. Comment on the issue before a larger change so scope can be agreed. For a new idea or bug, open an issue first. Current priority is visual quality, accessibility, and testing on watch sizes.

## Build and check

The repository has `core/` (Kotlin workout model and state machine), `wear/` (Wear OS Compose app), `website/` (static site), `docs/`, and `.github/workflows/`.

Install Android Studio, Android SDK 36, and JDK 17. On Windows, set `ANDROID_HOME` and `JAVA_HOME`, then run:

```powershell
.\gradlew.bat build test lint
.\scripts\run-wear.ps1
```

The script starts the `OpenSwim_WearOS` AVD, installs the debug app, and launches it. If you use another AVD, build with `./gradlew :wear:assembleDebug` and install with `adb install -r wear/build/outputs/apk/debug/wear-debug.apk`. Check the changed flow on a round emulator or watch and attach a screenshot for visual changes. Do not claim sensor measurements from the manual prototype.

## Send a pull request

1. Pick an issue and create a descriptive branch from `main`, for example `git switch -c fix/rest-timer`.
2. Make a focused change, with tests for workout logic and documentation when behavior changes.
3. Run `build test lint` and check affected watch screens.
4. Open a pull request to `main`, link the issue, and fill in the PR template.
5. Respond to review and wait for CI before merge.

The flow is **issue → branch → implementation → checks → pull request → review → main**. Read the [Code of Conduct](CODE_OF_CONDUCT.md); report security issues through [SECURITY.md](SECURITY.md).
