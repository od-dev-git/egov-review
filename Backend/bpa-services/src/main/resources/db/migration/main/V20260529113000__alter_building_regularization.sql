ALTER TABLE eg_bpa_regularization_buildinginfo
ADD COLUMN IF NOT EXISTS iseidpfeeapplicable boolean DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS iscwwcfeeapplicable boolean DEFAULT FALSE;
