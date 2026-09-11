-- Manual DB script — run after V3, in filename order. See db/scripts/README.md.
SET search_path TO tddpipeline;

ALTER TABLE users ADD COLUMN last_login_at TIMESTAMPTZ;
