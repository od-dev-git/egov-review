
CREATE TABLE IF NOT EXISTS  eg_bpa_planning_assistant_checklist (
    id character varying(256) NOT NULL,
    applicationno character varying(64),
    tenantid character varying(256),
	documents_submitted jsonb,
	plans_submitted jsonb,
	nocs_submitted jsonb,
	builtup_area jsonb,
	setback_details jsonb,
	additionaldetails jsonb,
	createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
	CONSTRAINT pk_eg_bpa_planning_assistant_checklist PRIMARY KEY (id)
);
	 
CREATE INDEX IF NOT EXISTS bpa_planning_assistant_checklist_index  ON eg_bpa_planning_assistant_checklist (
    applicationno,
    id
);