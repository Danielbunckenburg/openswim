# Android app structure

## Role

The phone app supports decisions before a swim (find and inspect workouts, follow a plan) and review after a swim (history and progress). The Wear app remains the in-water interface. The phone now reads Supabase Cloud data and supports email/password accounts. Payment and watch connection are not implemented.

## Primary navigation

A persistent bottom bar has five destinations: **Home**, **Workouts**, **Progress**, **Plans**, and **Account**. Each tab owns its secondary screens. Switching tabs returns to that tab's root. The app keeps no separate global menu.

## Screen tree

```text
Home
  └─ Workout Detail (today's scheduled workout)
Workouts
  └─ Workout Detail
Progress
  └─ Workout Result
Plans
  └─ Plan Detail
       └─ Workout Detail (the same shared Workout object)
Account
  └─ Connected Devices
```

Home quick actions link to the existing Workouts, Plans, and Account tabs. Weekly summary links to Progress. Secondary screens keep their originating tab selected.

## Screen responsibilities

| Screen | Responsibility |
| --- | --- |
| Home | Show today's scheduled workout, a few route shortcuts, watch status, and a compact weekly preview. |
| Workout Library | Browse the shared workout catalog by category. |
| Workout Detail | Explain one workout and its warm up, main set, and cool down steps. |
| Progress Overview | Summarize a chosen time range, reserve trend areas, and list completed swims. |
| Workout Result | Show recorded facts for one completed swim. Hide unavailable sensor fields. |
| Plans | Show the current plan and upcoming schedule, with a simple calendar alternative. |
| Plan Detail | Explain the plan and group its scheduled workouts by week. |
| Account | Group identity, device, sync/data, settings, and support concepts. |
| Connected Devices | Show the watch's connection state and future device-management location. |

## Shared data boundary

`core` owns `Workout`, `WorkoutSection`, `WorkoutStep`, `TrainingPlan`, `ScheduledWorkout`, and `CompletedWorkout`. Plans and results refer to a workout by stable ID. `CloudRepository` maps PostgREST rows to these models and uses the user's Supabase Auth token for private data; the sample repository remains for historical prototype content and is no longer used by the phone UI. Auth tokens are stored with Android Keystore encryption. No sensor measurements are invented.

## Later workflow

Select a workout → schedule it in Plans → sync it through Account/device status → swim on Wear → sync a completed workout → review it in Progress. Browsing cloud workouts, reading owned plans and results, and account sign-in work now. Scheduling, device pairing, and watch sync remain future capabilities.

## Questions before visual design

- Five bottom destinations are easy to understand but leave little room for long labels on compact phones. Confirm with a phone-size usability pass before choosing icon treatment.
- Home's weekly card should remain a preview that leads to Progress; repeating charts there would create two analytics destinations.
- The Calendar switch and much of Account currently demonstrate future locations with little usable data. They can be reduced or hidden in the first release if user testing finds them distracting.
- A fixed sample schedule cannot honestly represent the current day, and demo results cannot represent the user's real progress. Real dates, persisted history, and explicit empty states should precede any personalized copy.
