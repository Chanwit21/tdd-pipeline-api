-- Manual DB script — run after V4, in filename order. See db/scripts/README.md.
--
-- Defense-in-depth: enable Row Level Security on every table in this schema.
--
-- Deployment is Option A (Supabase = managed Postgres only). The backend connects
-- as role `postgres` (rolbypassrls = true), so RLS never affects it. We add NO
-- policies on purpose: with RLS on and no policy, the Supabase `anon` /
-- `authenticated` (PostgREST) roles can read/write nothing, even if the
-- `tddpipeline` schema were ever exposed through the auto-generated REST API.
--
-- Idempotent + lock-safe:
--   * Only touches tables that do NOT already have RLS, so this is a clean no-op
--     on an environment where RLS was enabled out of band (e.g. via the Supabase
--     dashboard) — no ACCESS EXCLUSIVE lock is taken in that case.
--   * `lock_timeout` keeps a fresh-deploy ALTER from blocking forever behind a
--     previous instance's ACCESS SHARE locks during a zero-downtime rollout.

SET search_path TO tddpipeline;

DO $$
DECLARE
    tbl text;
BEGIN
    SET LOCAL lock_timeout = '5s';
    FOR tbl IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = current_schema()
          AND NOT rowsecurity
        ORDER BY tablename
    LOOP
        EXECUTE format('ALTER TABLE %I ENABLE ROW LEVEL SECURITY', tbl);
        RAISE NOTICE 'RLS enabled on %', tbl;
    END LOOP;
END $$;
