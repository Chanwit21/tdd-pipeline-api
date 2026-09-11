-- Manual DB script — run after V2, in filename order. See db/scripts/README.md.
SET search_path TO tddpipeline;

-- Sample deals so the UI isn't empty on first boot. created_by NULL = imported/seed.
INSERT INTO deals (record_id, department_id, deal_owner, customer, deal_name, deal_type,
                   deal_status, deal_stage, probability, situation, closed_date, amount,
                   project_code, cost_sheet_no, created_date, is_legacy_migrated, migration_remark)
VALUES
  ('TDD-' || lpad(nextval('deal_record_seq')::text, 6, '0'),
   (SELECT id FROM departments WHERE code='IRM'), 'Suda', 'ETDA',
   'OS DEV (.NET + SA 2024)', 'New', 'Follow Up', 'Won', '75% - 98%', 'Best Case',
   date_trunc('month', now() + interval '3 month')::date, 427091, 'PJ-2024-011', 'CS-0091',
   (now() - interval '200 day')::date, FALSE, NULL),

  ('TDD-' || lpad(nextval('deal_record_seq')::text, 6, '0'),
   (SELECT id FROM departments WHERE code='SPM'), 'Kanchai', 'UOB',
   'LDS upgrade OS/DB และ Enhance Report Engine', 'Renew', 'PR', 'PO', '99% - 100%', 'Best Case',
   date_trunc('month', now() - interval '2 month')::date, 2150000, NULL, NULL,
   (now() - interval '210 day')::date, FALSE, NULL),

  ('TDD-' || lpad(nextval('deal_record_seq')::text, 6, '0'),
   (SELECT id FROM departments WHERE code='AMT'), 'Prasert', 'MIMO',
   'VB Spending — VB as a SOF (Ariba)', 'New', 'PR', 'PO', '99% - 100%', 'Best Case',
   date_trunc('month', now() - interval '5 month')::date, 1691760, NULL, NULL,
   (now() - interval '260 day')::date, FALSE, NULL),

  ('TDD-' || lpad(nextval('deal_record_seq')::text, 6, '0'),
   (SELECT id FROM departments WHERE code='AMT'), 'Prasert', 'TIPCO',
   'LINE Application for Internal Users', 'New', 'Follow Up', 'Prospecting', '50% - 74%', 'Base Case',
   date_trunc('month', now() - interval '1 month')::date, 1007000, NULL, NULL,
   (now() - interval '90 day')::date, FALSE, NULL),

  -- overdue: Follow Up + closed_date in the past
  ('TDD-' || lpad(nextval('deal_record_seq')::text, 6, '0'),
   (SELECT id FROM departments WHERE code='IRM'), 'Suda', 'GBank',
   'Core Banking Assessment', 'New', 'Follow Up', 'Final Proposal Submit', '50% - 74%', 'Base Case',
   date_trunc('month', now() - interval '2 month')::date, 880000, NULL, NULL,
   (now() - interval '120 day')::date, FALSE, NULL),

  -- legacy migrated & rule-breaking: stage Won but probability 50-74%
  ('TDD-' || lpad(nextval('deal_record_seq')::text, 6, '0'),
   (SELECT id FROM departments WHERE code='AMO'), 'Suwanna', 'Legacy Corp',
   'ข้อมูลนำเข้าจาก Excel เดิม', 'Renew', 'Follow Up', 'Won', '50% - 74%', 'Base Case',
   date_trunc('month', now() + interval '1 month')::date, 500000, NULL, NULL,
   (now() - interval '400 day')::date, TRUE,
   'Deal Stage = Won แต่ Probability = 50% - 74% (ไม่ตรงกฎ XREF-WON-PROB)');
