CREATE TABLE IF NOT EXISTS eg_bpa_completion_certificate (
    id                  VARCHAR(100),
    certificateNo       VARCHAR(100) PRIMARY KEY,
    tenantId            VARCHAR(100),
    applicantName       VARCHAR(200),
    applicantAddress    TEXT,
    bpaPermitNumber     VARCHAR(100),
    bpaPermitDate       BIGINT,
    plotNo              VARCHAR(200),
    khataNo             VARCHAR(200),
    mouza               VARCHAR(200),
    architectName       VARCHAR(200),
    pmoName             VARCHAR(200),
    architectAddress    TEXT,
    phaseWiseCompletion VARCHAR(100),
    completionfilestoreid varchar(100) NULL,
    completionDate      BIGINT,
    status              VARCHAR(50),
    additionalDetails   JSONB,
    createdby           VARCHAR(100),
    createdtime         BIGINT,
    lastmodifiedby      VARCHAR(100),
    lastmodifiedtime    BIGINT
    
);

CREATE INDEX IF NOT EXISTS idx_completion_certificate_no 
    ON eg_bpa_completion_certificate (certificateNo);


CREATE INDEX IF NOT EXISTS idx_completion_tenant_id 
    ON eg_bpa_completion_certificate (tenantId);
    
CREATE INDEX IF NOT EXISTS idx_completion_permit_no
    ON eg_bpa_completion_certificate (bpaPermitNumber);

