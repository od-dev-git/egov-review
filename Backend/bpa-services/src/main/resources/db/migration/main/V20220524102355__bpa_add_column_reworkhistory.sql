ALTER TABLE eg_bpa_buildingplan
ADD COLUMN IF NOT EXISTS reworkhistory jsonb;

ALTER TABLE eg_bpa_auditdetails
ADD COLUMN IF NOT EXISTS reworkhistory jsonb;