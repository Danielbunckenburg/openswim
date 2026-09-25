# Backend foundation

OpenSwim uses Supabase Auth for identity and PostgreSQL for structured workout templates, plans, schedules, and swim summaries. User data is private by default. The schema is deployed to a [Supabase cloud project](CLOUD_DEPLOYMENT.md), and the web and Android clients connect to it.

```text
Web app ───────────┐
Android companion ─┼─ HTTPS/TLS ─ Supabase Auth + API ─ PostgreSQL (RLS)
                  │
Wear OS local cache ┘  (later through Android; no constant network requirement)
```

The web app in `website/app.html` reads and writes through the Supabase JavaScript client. The Android `CompanionRepository` boundary is backed by `CloudRepository`, which authenticates through Supabase Auth and fetches workouts, plans and results with a client-safe publishable key and user session. Both clients depend on RLS for private records; neither contains service-role credentials.

The watch remains usable offline. A later sync path is Cloud → Android → Wear local cache for planned workouts and Wear local result → Android → Cloud after a swim. Sync needs explicit conflict rules and durable queues before implementation. Do not make the watch depend on a live Supabase connection.

`core.Workout` remains the shared Android/Wear model. `CloudRepository` maps database UUIDs to its string IDs, `title` to `name`, `category` to `type`, ordered sections to `WorkoutSection`, and `workout_steps` rows to repetitions and `WorkoutStep`. The workout's one `distance_unit` applies to all steps, matching the core invariant. Incomplete sections and workouts are excluded from the Android model.

PostgreSQL holds summary and interval records. Optional future raw sensor files belong in private object storage, with authenticated ownership policies and a separate retention design. No raw sensor file storage or bucket exists now.

## Local development

The versioned source of truth is `supabase/migrations/`; `supabase/seed.sql` contains only fictional, passwordless users. With a Docker-compatible runtime and Supabase CLI installed:

```text
supabase start
supabase db reset --local
```

The reset command is destructive for the **local** database. Never use `--linked` or `--include-seed` on production. A standalone PostgreSQL test is also documented in [SECURITY.md](SECURITY.md). Cloud deployment state is documented in [CLOUD_DEPLOYMENT.md](CLOUD_DEPLOYMENT.md).

For production, choose a **specific EU region** when creating the Supabase project; do not assume the broad “Europe” grouping guarantees EU residency. Keep region selection in deployment configuration, not app logic. Use TLS for all client connections. Region choice is one data-location control, not a complete privacy-compliance assessment. [Supabase region guidance](https://supabase.com/docs/guides/platform/regions).
