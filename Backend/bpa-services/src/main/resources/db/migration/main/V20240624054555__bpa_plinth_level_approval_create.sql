CREATE TABLE IF NOT EXISTS  public.eg_bpa_plinth_level_approval (
    id character varying(256) NOT NULL,
    applicationno character varying(64),
    tenantid character varying(256),
    bpaapplicationno character varying(64),
	declaration_details jsonb,
	accredited_person_details jsonb,
	pmo_details jsonb,
	additionaldetails jsonb,
    status character varying(64),
    approvalno character varying(64),
    bpaapprover character varying(64),
	createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
	CONSTRAINT pk_eg_bpa_plinth_level_approval PRIMARY KEY (id)
);
	 
CREATE INDEX IF NOT EXISTS  bpa_plinth_level_approval_index  ON eg_bpa_plinth_level_approval (
    applicationno,
    id
);

CREATE TABLE IF NOT EXISTS  public.eg_bpa_plinth_level_approval_document(
    id character varying(64)  NOT NULL,
    documenttype character varying(64),
    filestoreid character varying(64),
    documentuid character varying(64),
    plinthapprovalid character varying(64),
    additionaldetails jsonb,
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT uk_eg_bpa_plinth_level_approval_document PRIMARY KEY (id),
    CONSTRAINT fk_eg_bpa_plinth_level_approval_document FOREIGN KEY (plinthapprovalid)
        REFERENCES public.eg_bpa_plinth_level_approval (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);