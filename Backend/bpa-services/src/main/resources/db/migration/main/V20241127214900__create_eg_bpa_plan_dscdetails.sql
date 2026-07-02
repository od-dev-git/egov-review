CREATE table IF NOT EXISTS eg_bpa_plan_dscdetails(
    id character varying(64),
    tenantid character varying(64),
    documenttype character varying(64),
    documentid character varying(64),
    buildingplanid character varying(64),
    applicationno character varying(64),
    approvedby character varying(64),
	additionaldetails JSONB,
    createdBy character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,

    CONSTRAINT uk_eg_bpa_plan_dscdetails PRIMARY KEY (id),
    CONSTRAINT fk_eg_bpa_plan_dscdetails FOREIGN KEY (buildingplanid) REFERENCES eg_bpa_buildingplan (id)
);

CREATE INDEX IF NOT EXISTS index_eg_bpa_plan_dscdetails_tenantid ON eg_bpa_plan_dscdetails (tenantid);
CREATE INDEX IF NOT EXISTS index_eg_bpa_plan_dscdetails_approvedby ON eg_bpa_plan_dscdetails (approvedby);
CREATE INDEX IF NOT EXISTS index_eg_bpa_plan_dscdetails_documentid ON eg_bpa_plan_dscdetails (documentid);
CREATE INDEX IF NOT EXISTS index_eg_bpa_plan_dscdetails_applicationno ON eg_bpa_plan_dscdetails (applicationno);
CREATE INDEX IF NOT EXISTS index_eg_bpa_plan_dscdetails_buildingplanid ON eg_bpa_plan_dscdetails (buildingplanid);
