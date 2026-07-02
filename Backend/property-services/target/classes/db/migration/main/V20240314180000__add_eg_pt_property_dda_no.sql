ALTER TABLE eg_pt_property ADD COLUMN IF NOT EXISTS ddaNo CHARACTER VARYING (256);
ALTER TABLE eg_pt_property ADD COLUMN IF NOT EXISTS legacyHoldingNo CHARACTER VARYING (256);

CREATE INDEX IF NOT EXISTS index_eg_pt_property_ddaNo_legacyHoldingNo ON eg_pt_property (ddaNo,legacyHoldingNo);