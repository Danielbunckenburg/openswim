# Repeatable Wear OS screenshot review

Use an Android Wear OS round AVD such as `OpenSwim_WearOS`. The verified AVD is Android 16 / API 36, `sdk_gwear_x86_64`, 454 × 454 physical pixels, 320 dpi. On Windows, set `ANDROID_HOME` and use `$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"`.

1. Build with `.\gradlew.bat build lint`, install `wear/build/outputs/apk/debug/wear-debug.apk`, and launch `org.openswim.wear/.MainActivity`.
2. Record `adb shell wm size` and `adb shell wm density`. Capture at the AVD's default size, then run `adb shell wm size 390x390` for the compact pass. Keep density at 320. Restore with `adb shell wm size reset`.
3. From Home, test Guided Workout through library, detail, pool size, ready, Current, Metrics, Controls, rest, pause, Lock, Drill/Kick, end confirmation, and summary. Finish a repetition with planned rest. Then test Pool Swim through pool size, ready, Metrics, Controls, a manually logged length, Drill/Kick, pause, end confirmation, and summary.
4. Capture with `adb shell screencap -p /sdcard/openswim.png` followed by `adb pull /sdcard/openswim.png <local-file.png>`. Wait for page transitions to settle before capturing. Inspect each image at its original pixel size.
5. Check that distance, stroke/type, repetition, swim/rest state, and next action are readable at a glance. Confirm all controls stay within the round safe area, text does not wrap unexpectedly, manual values are labeled, and cards make scrolling clear. Verify a two-second hold is needed to unlock and that Pool Swim distance changes only after explicit taps.
6. Compare before and after images for any change. The [compact visual audit](visual-audit/README.md) records the first 390 × 390 pass and fixes. The [verified gallery](screenshots/README.md) lists the current screenshots.

Use the emulator only for layout and manual-flow checks. Physical watch testing is still needed for wet touch, glare, and in-pool glanceability.
