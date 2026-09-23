# Wear OS visual verification

Captured from the Android 16 / API 36 `OpenSwim_WearOS` round emulator (`sdk_gwear_x86_64`) after installing the tested debug build.

| Screen | 454 × 454 pixels | 390 × 390 pixels |
| --- | --- | --- |
| Home | [home.png](home.png) | [compact-home.png](compact-home.png) |
| Workout library | [workouts.png](workouts.png) | [compact-library.png](compact-library.png) |
| Workout detail | [detail.png](detail.png) | [compact-detail.png](compact-detail.png) |
| Pool-length selection | [pool.png](pool.png) | [compact-pool.png](compact-pool.png) |
| Ready | [ready.png](ready.png) | [compact-ready.png](compact-ready.png) |
| Active workout | [active.png](active.png) | [compact-active.png](compact-active.png) |
| Rest | [rest.png](rest.png) | [compact-rest.png](compact-rest.png) |
| Paused | [paused.png](paused.png) | [compact-paused.png](compact-paused.png) |
| End confirmation | [end-confirm.png](end-confirm.png) | [compact-end-confirm.png](compact-end-confirm.png) |
| Workout complete | [complete.png](complete.png) | [compact-complete.png](compact-complete.png) |

The emulator reports a physical **454 × 454 pixel** display at **320 dpi** (227 × 227 dp). The compact captures use `wm size 390x390` at the same 320 dpi (195 × 195 dp); the original emulator remained round. These are emulator checks, not physical watch or in-pool usability tests.

The 454-pixel completion capture follows manual progression through all 10 repetitions of Easy Reset. It shows 1000 m, 10/10 repetitions, and 4/4 sets. The paused and end-confirmation captures demonstrate the guarded early-end path.
