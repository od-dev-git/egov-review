CREATE TABLE IF NOT EXISTS  eg_bpa_draft(
    id character varying(256) NOT NULL,
    edcrno character varying(64),
    tenantid character varying(256),
    additionaldetails jsonb,
    status character varying(64),
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT pk_eg_bpa_draft PRIMARY KEY (id),
    CONSTRAINT unique_edcrno UNIQUE (edcrno)
);

CREATE index if not exists bpa_draft_index  ON eg_bpa_draft 
(
    edcrno,
    tenantid
);
