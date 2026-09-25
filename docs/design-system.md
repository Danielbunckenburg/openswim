# Wear OS design system

The watch UI follows Concept C. Home uses a centered circular swimmer action with a quiet pool-length line and bottom workout chevron. Library uses compact pill rows and All, Easy, and Technique filters. Detail groups distance, duration, and section summary above the pool selector and anchored Start Workout action.

| Role | Color |
| --- | --- |
| Canvas | Near black `#050D13` |
| Surface | Deep blue `#162B38` |
| Action, active state, and rest | Aquatic cyan `#38D6FF` |
| Destructive action | Restrained red `#F09A9A` |
| Primary text | White `#FFFFFF` |
| Supporting text | Blue gray `#A7B3BE` |
| Dividers and progress track | `#294C5D` |

Guided Active follows Concept C's current instruction, horizontal progress bar, total distance, elapsed time, and pager dots. Rest uses a cyan circular countdown and names the next step. Controls use four rounded rows with Pause first. Rep Done lives on the separate metrics page because the workout engine advances manually; Concept C's board does not show that action.

`ConceptCScreens.kt` contains Home, Library, and Detail. `SessionScreens.kt` contains Active, Rest, Controls, and completion. `WearDesign.kt` contains shared colors and controls. The six Concept C screens were visually reviewed on 390 × 390 and 454 × 454 round emulator sizes.

Metrics come from local manual actions. Guided repetition completion counts its planned distance; Pool Swim advances distance only when a length or Drill/Kick distance is entered. There are no sensor-derived values in this milestone.
