ALTER TABLE IF EXISTS eg_bpa_buildingplan
ADD COLUMN IF NOT EXISTS isOCOutsideSujogApplication BOOLEAN;

ALTER TABLE eg_bpa_auditdetails
ADD COLUMN IF NOT EXISTS isOCOutsideSujogApplication BOOLEAN;
