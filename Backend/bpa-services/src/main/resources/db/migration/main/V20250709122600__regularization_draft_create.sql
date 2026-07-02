CREATE TABLE IF NOT EXISTS  eg_regularization_draft(
    id character varying(256) NOT NULL,
    draftno character varying(64),
    tenantid character varying(64),
    regularizationapplicationno character varying(64) NULL,
    additionaldetails jsonb,
    status character varying(64),
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT pk_eg_regularization_draft PRIMARY KEY (id),
    CONSTRAINT unique_draftno UNIQUE (draftno)
);

CREATE index if not exists regularization_draft_index  ON eg_regularization_draft 
(
    draftno,
    tenantid
);
