-- ============ Departments & Users ============
CREATE TABLE departments (
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(20)  UNIQUE NOT NULL,
    name          VARCHAR(100) NOT NULL,
    default_owner VARCHAR(100),
    sort_order    INT NOT NULL DEFAULT 0
);

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,               -- MANAGER / ADMIN
    department_id BIGINT REFERENCES departments(id),   -- null when ADMIN
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_active_manager_per_dept
    ON users (department_id)
    WHERE role = 'MANAGER' AND is_active = TRUE;

-- ============ Master config ============
CREATE TABLE deal_types (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);
CREATE TABLE deal_statuses (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);
CREATE TABLE deal_stages (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(80) UNIQUE NOT NULL,
    allowed_for TEXT[] NOT NULL,          -- Deal Status names this stage is valid under, e.g. {'Follow Up'} / {'PR'} / {'Inactive'}
    sort_order  INT NOT NULL DEFAULT 0
);
CREATE TABLE probability_situation_map (
    probability VARCHAR(20) PRIMARY KEY,  -- '< 50%','50% - 74%','75% - 98%','99% - 100%'
    situation   VARCHAR(20) NOT NULL,     -- Worst Case / Base Case / Best Case
    sort_order  INT NOT NULL DEFAULT 0
);

-- rule thresholds kept as config (not hardcoded) — spec §11
CREATE TABLE rule_config (
    config_key   VARCHAR(60) PRIMARY KEY,
    config_value VARCHAR(255) NOT NULL,
    description  VARCHAR(255)
);

-- ============ Deals ============
CREATE SEQUENCE deal_record_seq START 1;

CREATE TABLE deals (
    id                 BIGSERIAL PRIMARY KEY,
    record_id          VARCHAR(20) UNIQUE NOT NULL,
    department_id      BIGINT NOT NULL REFERENCES departments(id),
    deal_owner         VARCHAR(100) NOT NULL,      -- snapshot of departments.default_owner at save time
    customer           VARCHAR(255) NOT NULL,
    deal_name          TEXT NOT NULL,
    deal_type          VARCHAR(50) NOT NULL,
    deal_status        VARCHAR(50) NOT NULL,
    deal_stage         VARCHAR(80) NOT NULL,
    probability        VARCHAR(20) NOT NULL,
    situation          VARCHAR(20) NOT NULL,       -- computed on save
    closed_date        DATE NOT NULL,              -- always day 1 of month
    amount             NUMERIC(18,2) NOT NULL,
    project_code       VARCHAR(50),
    cost_sheet_no      VARCHAR(50),
    created_date       DATE NOT NULL,
    is_legacy_migrated BOOLEAN NOT NULL DEFAULT FALSE,
    migration_remark   TEXT,
    created_by         BIGINT REFERENCES users(id),
    updated_by         BIGINT REFERENCES users(id),
    created_at         TIMESTAMP NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX ix_deals_department ON deals(department_id);
CREATE INDEX ix_deals_status ON deals(deal_status);
CREATE INDEX ix_deals_closed_date ON deals(closed_date);

CREATE TABLE deal_notes (
    id         BIGSERIAL PRIMARY KEY,
    deal_id    BIGINT NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    note_text  TEXT NOT NULL,
    created_by BIGINT NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX ix_deal_notes_deal ON deal_notes(deal_id);

CREATE TABLE deal_history (
    id            BIGSERIAL PRIMARY KEY,
    deal_id       BIGINT NOT NULL REFERENCES deals(id) ON DELETE CASCADE,
    changed_field VARCHAR(50) NOT NULL,
    old_value     TEXT,
    new_value     TEXT,
    changed_by    BIGINT NOT NULL REFERENCES users(id),
    changed_at    TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX ix_deal_history_deal ON deal_history(deal_id);
