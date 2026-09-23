# Wear OS design system

The local watch app has two equally prominent modes: Guided Workout and Pool Swim. Home contains only these choices. Setup, session, and summary share type, color, spacing, and controls from `wear/src/main/java/org/openswim/wear/WearDesign.kt`. The session-specific screens are in `SessionScreens.kt`.

| Role | Treatment |
| --- | --- |
| Canvas | Near black `#02080D` |
| Surface | Deep blue `#0D1B24` and `#122530` |
| Action and active state | Cyan `#67DEFA` |
| Rest, pause, end prompt | Amber `#FFC878` |
| Primary text | Cool white `#F5FBFF` |
| Supporting text | Blue gray `#A7BBC5` |
| Borders and tracks | `#29414C` |

The guided Current page gives the largest area to distance, then stroke/type, repetition, and the explicit SWIM state. Rest gives the largest area to the countdown and names the next repetition. Pool Swim opens on Metrics with time, manually counted distance, and lengths. Both session types place controls on the next horizontal page; Guided Workout has Current, Metrics, and Controls. Large buttons use cyan only for the immediate primary action. Pause, Lock, Drill/Kick, and End occupy a compact two-row grid so every control is visible on both tested round sizes.

Reusable elements include `ScrollPage`, `FixedPage`, `Eyebrow`, `Status`, `Heading`, `WideButton`, `FilterChip`, `PoolOption`, `WorkoutCard`, `SectionCard`, `MetricTile`, `RestDial`, and `ThinProgress`. Session pages crossfade when swiped or when their arrow is tapped. Discovery pages scroll vertically. The Current, Rest, Metrics, Controls, Lock, and Pause screens fit without vertical scrolling at 227 dp and 195 dp.

Metrics are based only on local manual actions. Guided repetition completion counts its planned distance; Pool Swim advances distance only when a length or Drill/Kick distance is entered. There are no sensor-derived values in this milestone.
