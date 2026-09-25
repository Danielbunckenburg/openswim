# Android navigation

## Graph

```text
Home ────────────────→ Workout Detail
  ├─ Browse workouts → Workouts
  ├─ My plan ────────→ Plans
  ├─ Watch ──────────→ Account / Connected Devices
  └─ Weekly summary ─→ Progress
Workouts ────────────→ Workout Detail
Progress ────────────→ Workout Result
Plans ───────────────→ Plan Detail ─→ Workout Detail
Account ─────────────→ Connected Devices
```

The bottom bar switches between the five roots. Secondary routes are pushed within the active tab. A Home shortcut to a secondary route sets the matching owning tab first. A workout opened from Plan Detail remains under Plans until Back returns to Plan Detail. There is one Workout Detail implementation.

## Allowed transitions

| From | To | Trigger |
| --- | --- | --- |
| Any screen | Any primary root | Bottom bar; clear current secondary stack |
| Home | Workout Detail | Today's workout |
| Home | Workouts, Plans, Progress | Quick action or weekly preview |
| Home | Connected Devices | Watch shortcut, owned by Account |
| Workouts | Workout Detail | Workout row |
| Progress | Workout Result | Completed workout row |
| Plans | Plan Detail | Current plan |
| Plans, Plan Detail | Workout Detail | Scheduled workout row |
| Account | Connected Devices | Devices row |

No secondary screen links back to itself or to another unrelated secondary screen. Disabled future actions do not navigate.

## Back behavior

System Back and the top-bar Back button pop one secondary screen. At a primary root, system Back follows Android's normal activity behavior. Switching tabs discards secondary history, so Back never jumps through old tabs. Route IDs are resolved from shared core data. An invalid or removed ID returns to its owning root instead of displaying another record.

## Future deep links

A future shared-workout link should carry a versioned workout ID or share token, resolve it to a core `Workout`, then open `Workouts / Workout Detail` with Workouts selected. Unavailable, private, or unsupported workouts need an explicit error state at the library root. A plan invitation should similarly land under Plans. Do not expose a deep link intent filter or accept external IDs in this prototype; authorization and link resolution will be designed with sharing.
