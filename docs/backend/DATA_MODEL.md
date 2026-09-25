# Data model

All app-owned primary keys are UUIDs. `auth.users.id` is the authoritative user identifier; email is never a relational key. `auth.users` itself is managed by Supabase Auth.

| Table | Purpose | Ownership / relationship |
| --- | --- | --- |
| `profiles` | Minimal display and pool preferences | `id → auth.users.id`; created by Auth trigger |
| `workouts` | Reusable workout template | `creator_id → auth.users.id`; visibility defaults to `PRIVATE` |
| `workout_sections` | Ordered warm up, main set, cool down or other sections | `workout_id → workouts.id` |
| `workout_steps` | Ordered repetitions, distance, stroke, intensity, equipment, rest and optional targets | `section_id → workout_sections.id` |
| `training_plans` | Private reusable plan template | `creator_id → auth.users.id` |
| `training_plan_workouts` | Week/day placement of an existing workout | `plan_id → training_plans.id`, `workout_id → workouts.id`; initially owner-authored workouts only |
| `scheduled_workouts` | A user's dated planned swim | `user_id → auth.users.id`, `workout_id → workouts.id`, optional `plan_id → training_plans.id` |
| `completed_workouts` | Private session summary | `user_id → auth.users.id`, optional `workout_id → workouts.id` |
| `completed_intervals` | Private interval summary | `completed_workout_id → completed_workouts.id` |

Templates and personal activity are separate: making a workout `PUBLIC` exposes its template and steps, never schedules, results, intervals, profile, or plan. `UNLISTED` is owner-only until explicit link sharing is designed.

Deleting an Auth user cascades through owned templates, plans and activity. Deleting a template removes its plan-template placements and unsets references in scheduled and completed history, leaving another user's personal records intact. A later account-deletion service must call the privileged Auth deletion path and verify the cascade. No client has direct authority to delete `auth.users`.

The database stores one workout distance unit on the template, following the core `Workout` rule. It does not yet enforce that every workout has at least one section and step; the domain adapter must reject incomplete templates. It stores session and interval summaries only; there is no one-row-per-second sensor table.

Useful indexes cover owner lookups, parent-child joins, a user's schedule by date, and completed workouts by start time. Unique `(parent, position)` constraints keep step and plan placement ordering unambiguous.
