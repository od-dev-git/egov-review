ALTER TABLE eg_bpa_regularization_buildinginfo
ADD COLUMN IF NOT EXISTS numberoftemporarystructures varchar(256),
ADD COLUMN IF NOT EXISTS hasprojectprovidedmin10percentbuaforewswithin5kmfromprojectsite boolean,
ADD COLUMN IF NOT EXISTS projectvalueifeidpfeeapplicableforproject varchar(256),
ADD COLUMN IF NOT EXISTS totalnoofdwellingunits varchar(256),
ADD COLUMN IF NOT EXISTS isshelterfeeapplicable boolean,
ADD COLUMN IF NOT EXISTS effectiveewsarea varchar(256),
ADD COLUMN IF NOT EXISTS issecuritydepositrequired boolean,
ADD COLUMN IF NOT EXISTS tdrfarrelaxation varchar(256);

