-- Manual DB script — run after V1, in filename order. See db/scripts/README.md.
SET search_path TO tddpipeline;

-- Departments (from Master_Config)
INSERT INTO departments (code, name, default_owner, sort_order) VALUES
  ('AMC', 'Account Management C', 'Somchai',  1),
  ('AMO', 'Account Management O', 'Suwanna',  2),
  ('AMT', 'Account Management T', 'Prasert',  3),
  ('IRM', 'Infrastructure & Risk Mgmt', 'Suda', 4),
  ('SPM', 'Special Projects Mgmt', 'Kanchai', 5),
  ('TIN', 'Technology & Innovation', 'Nattapong', 6),
  ('UXT', 'UX Team', 'Ploy', 7),
  ('SP1', 'Support 1', 'Wichai', 8);

INSERT INTO deal_types (name, sort_order) VALUES ('New', 1), ('Renew', 2);

INSERT INTO deal_statuses (name, sort_order) VALUES
  ('Follow Up', 1), ('PR', 2), ('Inactive', 3);

INSERT INTO deal_stages (name, allowed_for, sort_order) VALUES
  ('Contact Created',                ARRAY['Follow Up']::text[], 1),
  ('Prospecting',                    ARRAY['Follow Up']::text[], 2),
  ('Appointment Scheduled',          ARRAY['Follow Up']::text[], 3),
  ('Operational People Bought-In',   ARRAY['Follow Up']::text[], 4),
  ('Decision Maker Bought-In',       ARRAY['Follow Up']::text[], 5),
  ('Final Proposal Submit',          ARRAY['Follow Up']::text[], 6),
  ('Won',                            ARRAY['Follow Up']::text[], 7),
  ('PO',                             ARRAY['PR']::text[],        8),
  ('Lost',                           ARRAY['Inactive']::text[],  9),
  ('Cancelled',                      ARRAY['Inactive']::text[],  10),
  ('On Hold',                        ARRAY['Inactive']::text[],  11);

INSERT INTO probability_situation_map (probability, situation, sort_order) VALUES
  ('< 50%',      'Worst Case', 1),
  ('50% - 74%',  'Base Case',  2),
  ('75% - 98%',  'Best Case',  3),
  ('99% - 100%', 'Best Case',  4);

INSERT INTO rule_config (config_key, config_value, description) VALUES
  ('XREF_WON_PROBABILITY', '75% - 98%',  'Probability ที่ต้องมีเมื่อ Deal Stage = Won'),
  ('XREF_PO_PROBABILITY',  '99% - 100%', 'Probability ที่ต้องมีเมื่อ Deal Stage = PO');
