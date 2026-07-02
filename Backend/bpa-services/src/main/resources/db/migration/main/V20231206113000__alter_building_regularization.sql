ALTER TABLE eg_bpa_regularization_buildinginfo
ADD COLUMN IF NOT EXISTS farfeerate varchar(256),
ADD COLUMN IF NOT EXISTS unauthorizedsbbnunder5rate varchar(256),
ADD COLUMN IF NOT EXISTS unauthorizedsbbnunder10rate varchar(256);

ALTER TABLE eg_bpa_regularization_plotinfo
ALTER COLUMN plottobegifted type varchar(128);
