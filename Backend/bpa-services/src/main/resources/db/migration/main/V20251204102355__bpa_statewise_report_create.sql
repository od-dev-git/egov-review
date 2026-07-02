CREATE table IF NOT EXISTS eg_bpa_stagewisereport (
    id                  VARCHAR(64) PRIMARY KEY,
    applicationno       VARCHAR(64),
    leveltype           VARCHAR(64),
    blockno             VARCHAR(64),
    floorno             VARCHAR(64),
    documentdetails     JSONB,
    additionaldetails   JSONB,
    status              VARCHAR(64),
    approvalno          VARCHAR(64),
    createdby           VARCHAR(64),
    lastmodifiedby      VARCHAR(64),
    createdtime         BIGINT,
    lastmodifiedtime    BIGINT
);

CREATE INDEX IF NOT EXISTS idx_stage_wise_report_id 
    ON eg_bpa_stagewisereport (id);

CREATE INDEX IF NOT EXISTS idx_stage_wise_report_applicationno 
    ON eg_bpa_stagewisereport (applicationno);

CREATE INDEX IF NOT EXISTS idx_stage_wise_report_leveltype 
    ON eg_bpa_stagewisereport (leveltype);

CREATE INDEX IF NOT EXISTS idx_stage_wise_report_blockno 
    ON eg_bpa_stagewisereport (blockno);

CREATE INDEX IF NOT EXISTS idx_stage_wise_report_floorno 
    ON eg_bpa_stagewisereport (floorno);

CREATE INDEX IF NOT EXISTS idx_stage_wise_report_createdby 
    ON eg_bpa_stagewisereport (createdby);

