# Compact round-screen visual audit

The first 390 × 390 pixel pass revealed that several screens used space that is safe at 454 × 454 but not at 390 × 390. These before images were captured on the emulator before the compact layout fixes; the after images were captured after rebuilding and navigating the same flow.

| Screen | Before | After | Fix |
| --- | --- | --- | --- |
| Home | [before](before-home-390.png) | [after](../screenshots/compact-home.png) | Shortened the primary action and supporting line. |
| Library | [before](before-library-390.png) | [after](../screenshots/compact-library.png) | Shortened the heading to leave a full workout card visible. |
| Pool | [before](before-pool-390.png) | [after](../screenshots/compact-pool.png) | Reduced option height and gaps so Continue stays inside the round display. |
| Ready | [before](before-ready-390.png) | [after](../screenshots/compact-ready.png) | Reduced vertical gaps so Start Workout remains reachable. |
| Active | [before](before-active-390.png) | [after](../screenshots/compact-active.png) | Scaled the type and controls to keep Rep Done and pause visible. |
| Complete | [before](before-complete-390.png) | [after](../screenshots/compact-complete.png) | Compressed the summary so Back to Home stays visible. |

Rest and pause were also inspected at 390 × 390. The compact rest dial and action row remain on screen; see [compact rest](../screenshots/compact-rest.png). The 454 × 454 set covers the end confirmation and all major screens.
