# OpenSwim web app

The web app is [website/app.html](../../website/app.html). It shares Supabase Cloud project `utbmyhytdnmorktxeaei` with the Android companion. The public project page in `website/index.html` links to it. GitHub Pages publishes the `website/` directory when a change to that directory reaches `main`.

## What works

- Guests can browse public workouts. Signed-in users can also see their private workouts, plans, schedule, completed swims, and profile.
- Signed-in users can create a workout with one set, create a plan with its first workout, schedule a swim, log a swim manually, and edit swimming preferences. These records use the same tables and RLS policies as Android.
- Email/password login, signup with email confirmation, persistent browser session, and sign-out use Supabase Auth. The signup redirect returns to `app.html`.
- A new cloud account starts empty. No fictional seed data or public workout library was deployed.

The creation forms intentionally start with one set and one plan entry. Full workout and plan editing, watch pairing, automatic sync, data export, and account deletion remain separate work.

## Local preview

Run from the repository root:

```powershell
python -m http.server 8000 --bind 127.0.0.1 --directory website
```

Open `http://localhost:8000/app.html`. Local email-confirmation redirects are allowlisted for `http://localhost:8000/app.html`; use that hostname for signup testing. The published URL is `https://danielbunckenburg.github.io/openswim/app.html`.

`website/config.js` contains only the project URL and a **publishable key**. Supabase treats these as public client configuration; RLS protects private rows. Never add a secret/service-role key, database password, or personal access token to web files. The Supabase browser SDK is pinned to a specific version with a subresource integrity hash in `app.html`.

Cloud signup email currently reaches only authorized project team addresses through Supabase's default mail service. Configure custom SMTP and test a real confirmation flow before inviting general users. The public website can be reviewed without an account.
