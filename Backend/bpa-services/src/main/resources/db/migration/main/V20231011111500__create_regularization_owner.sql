CREATE TABLE IF NOT EXISTS eg_bpa_regularization_owners(
	id character varying(256),
	uuid character varying(256),
	isprimaryowner boolean,
	ownershippercentage double precision,
	institutionId character varying(256),
	additionalDetails JSONB,
	regularizationid character varying(256),
	relationship character varying(64),
	createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,

	CONSTRAINT pk_eg_bpa_regularization_owners PRIMARY KEY (id),
	CONSTRAINT fk_eg_bpa_regularization_owners FOREIGN KEY (regularizationid) REFERENCES eg_bpa_regularization_application (id)
);

CREATE TABLE IF NOT EXISTS eg_bpa_regularization_institution(
	id character varying(256),
	tenantId character varying(256),
	type character varying(256),
	designation character varying(256),
	nameOfAuthorizedPerson character varying(256),
	additionalDetails JSONB,
	regularizationid character varying(64),
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    
	CONSTRAINT pk_eg_bpa_regularization_institution PRIMARY KEY (id),
	CONSTRAINT fk_eg_bpa_regularization_institution FOREIGN KEY (regularizationid) REFERENCES eg_bpa_regularization_application (id)
);


alter table eg_bpa_regularization_plotinfo 
add column saleDeedDate bigint;

