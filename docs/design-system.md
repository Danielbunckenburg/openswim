# Watch design system

OpenSwim uses one visual language across the local watch flow. The source tokens and reusable Compose elements live in `wear/src/main/java/org/openswim/wear/MainActivity.kt`.

## Visual roles

| Role | Treatment |
| --- | --- |
| Canvas | Near-black `#02080D` |
| Surface | Deep blue `#0D1B24`, with `#122530` for emphasized cards |
| Primary action and progress | Restrained cyan `#67DEFA` |
| Rest and paused state | Warm amber `#FFC878` |
| Primary text | Cool white `#F5FBFF` |
| Supporting text | Blue gray `#A7BBC5` |
| Borders and tracks | `#29414C` |

Large distance or rest time communicates the immediate task. Stroke or drill type follows it, then repetition and next action. Workout progress and elapsed time remain secondary. On 195 dp screens the active header becomes simply “SWIM” so it stays inside the round safe area.

The reusable Compose elements include `ScrollPage`, `FixedPage`, `Eyebrow`, `Heading`, `Status`, `WideButton`, `ActionRow`, `WorkoutCard`, `SectionCard`, `MetricTile`, and `ThinProgress`. Buttons and the rest dial adapt to screen height. Primary actions use cyan fills; rest and paused labels use amber. Page changes crossfade, progress and the rest arc animate, and the manual session state determines which screen appears.

Scrolling is reserved for discovery and detail content. Active, rest, and pause screens keep their action controls in the visible round area without scrolling.
