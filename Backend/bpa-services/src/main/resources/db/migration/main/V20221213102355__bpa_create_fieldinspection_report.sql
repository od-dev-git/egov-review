CREATE TABLE IF NOT EXISTS  eg_bpa_fieldinspection_details(
    id character varying(256) NOT NULL,
    applicationno character varying(64),
    tenantid character varying(256),
	approachRoad jsonb,
	siteSituation jsonb,
	buildingSituation jsonb,
	report_details jsonb,
	additionaldetails jsonb,
	createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
	CONSTRAINT pk_eg_bpa_fieldinspection_details PRIMARY KEY (id)
	 );
	 
CREATE INDEX bpa_fieldinspection_index  ON eg_bpa_fieldinspection_details 
(
    applicationno,
    id
    
);

