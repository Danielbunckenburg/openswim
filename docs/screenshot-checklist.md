# Repeatable Wear OS screenshot review

Use an Android Wear OS round AVD such as `OpenSwim_WearOS`. The verified AVD is Android 16 / API 36, `sdk_gwear_x86_64`, 454 × 454 physical pixels, 320 dpi. On Windows, set `ANDROID_HOME` and use `$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"`.

1. Build with `.\gradlew.bat build lint`, install `wear/build/outputs/apk/debug/wear-debug.apk`, and launch `org.openswim.wear/.MainActivity`.
2. Record `adb shell wm size` and `adb shell wm density`. Capture at the AVD's default size, then run `adb shell wm size 390x390` for the compact pass. Keep density at 320. Restore with `adb shell wm size reset`.
3. From Home, open the library, choose a workout, inspect its detail, choose the pool length, and start the session. Capture Home, library, detail, pool, ready, active, rest, paused, end confirmation, and complete. For rest, finish one repetition that has a planned rest. For complete, advance every repetition and skip rests as needed.
4. Capture with `adb shell screencap -p /sdcard/openswim.png` followed by `adb pull /sdcard/openswim.png <local-file.png>`. Wait for page transitions to settle before capturing. Inspect each image at its original pixel size.
5. Check that distance, stroke/type, repetition, swim/rest state, and next action are readable at a glance. Confirm primary and pause controls stay within the round safe area, text does not wrap unexpectedly, and cards make scrolling clear.
6. Compare before and after images for any change. The [compact visual audit](visual-audit/README.md) records the first 390 × 390 pass and fixes. The [verified gallery](screenshots/README.md) lists the current screenshots.

Use the emulator only for layout and manual-flow checks. Physical watch testing is still needed for wet touch, glare, and in-pool glanceability.
