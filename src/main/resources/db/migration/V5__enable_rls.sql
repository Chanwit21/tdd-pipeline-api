-- Defense-in-depth: enable Row Level Security on every table.
-- Deployment is Option A (Supabase = managed Postgres only). The app connects
-- as role `postgres` (rolbypassrls = true), so RLS does not affect the backend.
-- We intentionally add NO policies: this blocks the Supabase `anon` /
-- `authenticated` roles (PostgREST) from reading or writing any row if the
-- `tddpipeline` schema is ever exposed via the auto-generated REST API.

ALTER TABLE deal_history             ENABLE ROW LEVEL SECURITY;
ALTER TABLE deal_notes               ENABLE ROW LEVEL SECURITY;
ALTER TABLE deal_stages              ENABLE ROW LEVEL SECURITY;
ALTER TABLE deal_statuses            ENABLE ROW LEVEL SECURITY;
ALTER TABLE deal_types               ENABLE ROW LEVEL SECURITY;
ALTER TABLE deals                    ENABLE ROW LEVEL SECURITY;
ALTER TABLE departments              ENABLE ROW LEVEL SECURITY;
ALTER TABLE flyway_schema_history    ENABLE ROW LEVEL SECURITY;
ALTER TABLE probability_situation_map ENABLE ROW LEVEL SECURITY;
ALTER TABLE rule_config              ENABLE ROW LEVEL SECURITY;
ALTER TABLE users                    ENABLE ROW LEVEL SECURITY;
