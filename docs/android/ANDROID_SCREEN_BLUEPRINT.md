# Android screen blueprint

This is an information blueprint. Labels and placement are provisional. "Future" actions are visible as context only and do not modify data.

| Screen | User question | Required information | Primary action → destination | Secondary actions → destination | Data required | Future features omitted |
| --- | --- | --- | --- | --- | --- | --- |
| Home | What matters now? | Today's title, distance, estimate, scheduled state; watch status and last sync; weekly preview | View workout → Workout Detail | Browse → Workouts; My plan → Plans; Watch → Connected Devices; weekly preview → Progress | ScheduledWorkout, Workout, status placeholder, CompletedWorkout summary | Send/start on watch, real sync, personalized recommendations |
| Workout Library | What should I swim? | Title, distance, estimate, category and useful intensity; category filters | Open workout → Workout Detail | Filter category → same screen | Shared Workout catalog | User-created and shared catalogs, full search |
| Workout Detail | What exactly will I swim? | Title, description, total, estimate, category/intensity, pool compatibility; warm up, main set, cool down; repetitions, stroke, distance, rest, targets, notes and equipment where present | Review structured workout → remains here | Back → origin; reserved Send to watch, Save, Schedule, Share → no destination yet | Workout, WorkoutSection, RepetitionSet, WorkoutStep | Device transfer, save, scheduling, sharing, advanced step editing |
| Progress Overview | How is my swimming developing? | Distance, count, training time, pace only if recorded; week/month/year; trend placeholders; recent completed swims | Open result → Workout Result | Change time range → same screen | CompletedWorkout records | Charts, comparisons, sensor analytics |
| Workout Result | What happened in this swim? | Name, date/time, recorded distance and duration; optional pace, lengths, heart rate, SWOLF, strokes, splits only when present | Review recorded summary → remains here | Back → Progress | CompletedWorkout linked to Workout | Analysis, editing, export, fabricated sensor values |
| Plans | What is coming up? | Current title, duration, week, progress; scheduled workout days and titles | Open current plan → Plan Detail | Upcoming/Calendar → same screen; workout → Workout Detail | TrainingPlan, ScheduledWorkout, Workout | Plan editing, scheduling, recurring rules, calendar integration |
| Plan Detail | How is this plan structured? | Objective, duration, sessions per week, difficulty, progress; weeks and workout references | Open workout → Workout Detail | Back → Plans | TrainingPlan, ScheduledWorkout, shared Workout | Plan creation, reassignment, adaptive planning |
| Account | Where do I manage OpenSwim and devices? | Local profile state; devices; sync/data; units/pool/notifications; about/help/privacy | Connected Devices → Connected Devices | GitHub/help/privacy concepts → no in-app destination yet | Local settings placeholders, device status placeholder | Authentication, cloud sync, export, preferences persistence |
| Connected Devices | Is my watch connected? | Device name, connection state, last sync | Review status → remains here | Back → Account; reserved connect/sync controls → no destination yet | Device status placeholder | Pairing, Wear OS transport, manual sync |

## Content rules

- Core `Workout` is used everywhere. Plans and completed records contain its ID, never a second workout representation.
- Sample completed data is identified as a demo. Show only fields populated in that record; sensor cards remain absent.
- A scheduled workout is a plan placement, distinct from a completed swim. Home can show a scheduled workout without implying it was sent to a watch.
- Empty states should state why information is absent. Placeholders must not look like live measurements.
