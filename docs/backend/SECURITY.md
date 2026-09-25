# Security model and verification

## Authentication and authorization

Supabase Auth owns password handling and `auth.users.id`. Email/password can be enabled first; Google and Apple sign-in may be added through Auth later. Public tables reference Auth UUIDs, not emails. An Auth trigger creates a minimal profile. Do not add a custom password store.

Every client-facing table has RLS enabled. Explicit grants restrict `anon` to `SELECT` on workout templates and their sections/steps. `authenticated` gets only the operations needed for its own rows. The public workout policy reads `visibility = 'PUBLIC'`; unlisted remains owner-only. Child rows follow the parent workout or completed result. A `WITH CHECK` rule prevents changing ownership during an update. The migration revokes broad default grants before granting operations; grants and RLS both matter. [Supabase RLS guidance](https://supabase.com/docs/guides/database/postgres/row-level-security).

The server-only service-role credential bypasses RLS and must never be in Android, Wear, Web, Git, screenshots or logs. Clients may eventually receive only a publishable configuration value plus the user's Auth session. Admin operations remain server-side. Storage buckets, if introduced, must be private with owner authorization; none are created now.

## Local verification

`supabase/tests/rls.sql` uses the fictional IDs in `supabase/seed.sql`. Run it against a disposable local Supabase database as a database admin using `psql -v ON_ERROR_STOP=1 -f supabase/tests/rls.sql`. It starts a transaction and rolls it back. The test checks RLS flags, anonymous grants, own records, another user's hidden records, public template visibility, private and unlisted template denial, non-owner update/delete and cross-owner insert denial, nested sections and intervals, and preservation of a user's schedule when someone else's public template is deleted.

With the Supabase CLI local stack running, obtain the local database connection from `supabase status` and pass it to `psql`. Never point this seed or test workflow at production. The tests use `SET ROLE authenticated` and `request.jwt.claim.sub` to exercise database policy behavior.

The `database` CI job runs the migration and SQL assertions against disposable PostgreSQL 17 with the test-only `scripts/test-support/auth_shim.sql`. The shim must never be applied to Supabase, which already manages Auth.

On 24 September 2026, a clean PostgreSQL 17 replay created **9 app tables and 35 RLS policies**. The fictional seed applied, and all assertions in `supabase/tests/rls.sql` passed. `:android:assembleDebug` and `:core:test` also passed after adding the repository interface.

The [live Auth/API script](../../scripts/test-support/live_auth_api.mjs) also passed twice against disposable PostgreSQL 17.11, official Supabase Auth v2.197.0, and PostgREST 16.4. It created two real email/password test sessions, used their JWTs for REST requests, checked public and private template visibility, and verified that another user could not insert or update an owner's workout or section. It uses random `example.test` addresses and passwords.

After disk space was restored, the local Supabase CLI 2.117.0 stack started successfully with database, Auth, REST, Kong, and Mailpit. It applied `20260924000000_backend_foundation.sql` and `seed.sql` without errors. The live script then passed through the local API gateway with email confirmation enabled; it read the test emails from Mailpit, confirmed both users, logged them in, and ran the JWT-backed REST checks. `supabase/tests/rls.sql` also passed against that same database. Its anonymous visibility assertions now select only the seed fixture IDs, so unrelated public workouts created by the HTTP test do not change the expected fixture counts.

To repeat the HTTP checks after `supabase start` and `supabase db reset --local`, use the local API URL and local anon key from `supabase status`:

```powershell
$env:AUTH_URL = 'http://127.0.0.1:54321/auth/v1'
$env:REST_URL = 'http://127.0.0.1:54321/rest/v1'
$env:MAILPIT_URL = 'http://127.0.0.1:54324'
$env:SUPABASE_ANON_KEY = '<local anon key>'
node scripts/test-support/live_auth_api.mjs
```

`MAILPIT_URL` lets the script confirm signup mail when local email confirmation is enabled. Without it, Auth must issue a session at signup. Use only a disposable local database. The HTTP script and SQL suite cover complementary authorization paths; neither is a substitute for a staging review before launch.

One remaining privacy review item is that direct reads of a `PUBLIC` workout return its pseudonymous `creator_id` UUID along with the template. Profiles remain protected, but the UUID can correlate a creator's public templates. Decide whether a limited public projection should hide that identifier before enabling sharing for real users.

## Deployment boundary

The schema is deployed to a Frankfurt cloud project without local seed data. The read-only cloud schema check and anonymous HTTPS API check passed; see [cloud deployment state](CLOUD_DEPLOYMENT.md). Before real user signup: configure Auth email delivery and redirects, inspect the Dashboard Security Advisor, define backup, incident and deletion processes, and run end-to-end tests with controlled test accounts. Do not seed fictional users into production. [Supabase production checklist](https://supabase.com/docs/guides/deployment/going-into-prod).
