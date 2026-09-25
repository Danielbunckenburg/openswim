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

## Concept board redesign

Concepts A, B, and C were used as the visual targets for the current Wear OS pass. The screens below were captured from the rebuilt app on a 390 × 390 round emulator:

| Flow | Reviewed screenshot |
| --- | --- |
| Home | [Start a Swim](after-home-390.png) |
| Browse | [Workout library](after-library-390.png) |
| Prepare | [Workout detail](after-detail-390.png) |
| Swim | [Active workout](after-active-390.png) |
| Recover | [Rest countdown](after-rest-390.png) |
| Control | [Workout controls](after-controls-390.png) |
| Finish | [Completion summary](after-complete-390.png) |

The main visual changes are a single cyan action on home, lighter workout rows, a pool selector before the scrollable breakdown, an anchored workout start action, a progress arc around the active metric, a larger rest dial, and quieter destructive controls. The active, rest, controls, and completion screens were also reviewed at 454 × 454.

## Concept C implementation

The follow-up pass uses Concept C specifically. It replaces the home button with a circular swimmer control, adds the board's three featured workouts, filters, and pill rows, condenses workout detail above the pool selector, uses a horizontal active progress bar, switches rest to cyan, and gives controls the board's four actions. Manual Rep Done is on the separate metrics page because this version advances workouts by touch.

| Screen | 390 × 390 emulator capture |
| --- | --- |
| Home | [Concept C home](concept-c-home-390.png) |
| Library | [Concept C library](concept-c-library-390.png) |
| Detail | [Concept C detail](concept-c-detail-390.png) |
| Active | [Concept C active](concept-c-active-390.png) |
| Rest | [Concept C rest](concept-c-rest-390.png) |
| Controls | [Concept C controls](concept-c-controls-390.png) |
