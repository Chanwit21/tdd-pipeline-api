# DB scripts — run manually

No migration framework. The app connects to an already-correct schema
(`spring.jpa.hibernate.ddl-auto: none`) and never touches DDL itself. Schema
changes are plain SQL files here, applied by hand, in filename order
(`V1`, `V2`, …). Each script `SET search_path TO tddpipeline` itself, so it's
safe to paste into any client without extra setup.

## Where to run them

- **Supabase (staging/prod):** dashboard → **SQL Editor** → paste the file →
  Run. Or `psql "$SPRING_DATASOURCE_URL" -f db/scripts/V6__whatever.sql`
  (session pooler URL from [`DEPLOY.md`](../../DEPLOY.md), port 5432).
- **Local:** `docker compose up` mounts this folder into the `postgres`
  container's `docker-entrypoint-initdb.d`, so Postgres itself runs every
  script here (in filename order) the first time the `pgdata` volume is
  created — nothing to do by hand for a fresh `docker compose up --build`.
  To re-seed, `docker compose down -v` and bring it back up.

## Current scripts

| File | What |
|---|---|
| `V1__init.sql` | schema + all tables |
| `V2__seed_master_config.sql` | departments, deal types/statuses/stages, probability rules |
| `V3__seed_sample_deals.sql` | sample deals so the UI isn't empty |
| `V4__user_last_login.sql` | `users.last_login_at` column |
| `V5__enable_rls.sql` | enable RLS (no policies) — defense-in-depth, see file header |

## Adding a new script

Next file is `V6__<short_description>.sql`. Write it idempotent where
practical (`IF NOT EXISTS`, guarded `DO` blocks) — nothing tracks what has
already run against a given database, so re-running a script (or running it
against an out-of-sync environment) must be safe. Apply it to Supabase
**before** deploying the app code that depends on it (the app never migrates
on boot). Update the table above and `../../DEPLOY.md` if the deploy steps
change.

There is no `flyway_schema_history` bookkeeping for anything from `V6`
onward — the old one (`V1`–`V5`, tracked while this project used Flyway)
still exists on Supabase as a historical record and is otherwise unused.
