# Privacy defaults

**Open-source code does not mean open user data.** Migrations, schema, policies and synthetic examples may be public in Git. Real user records, production dumps, access tokens and credentials must not be committed.

Profiles, private and unlisted workout templates, plans, scheduled swims, completed swims and intervals are readable only by their owner. A user must deliberately set a workout template to `PUBLIC` before anyone else can read it. That change does not share personal pace, heart rate, SWOLF, stroke history, schedules or plan progress. No plan-sharing policy exists yet.

The profile contains only a display name, distance unit and default pool length. It has no address, date of birth, gender or phone number. No advertising IDs, behavioral analytics, marketing tracking or unnecessary telemetry are introduced. Raw sensor streams are not stored.

Future export and deletion should be built around the explicit `user_id` and `creator_id` relationships. Export must authenticate the requester and return only that user's private records plus templates they own. History deletion should remove only the user's completed records and intervals. Account deletion should use a server-side Auth admin operation, then verify cascades and any future object-storage cleanup. Retention and deletion timing require a project privacy policy before launch.

The production project should be created in a specific EU region chosen for the intended residency requirement, with TLS connections. Region placement alone does not settle all GDPR obligations; document the host's current DPA, backups, subprocessors and retention before collecting real users' data. [Supabase region guidance](https://supabase.com/docs/guides/platform/regions).
