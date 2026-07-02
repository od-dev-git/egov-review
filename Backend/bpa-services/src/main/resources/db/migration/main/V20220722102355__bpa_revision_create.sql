CREATE TABLE IF NOT EXISTS  eg_bpa_buildingplan_revision(
    id character varying(256) NOT NULL,
    tenantid character varying(256),
    isSujogExistingApplication boolean NOT NULL,
    bpaApplicationNo character varying(256),
	bpaApplicationId character varying(256),
	refBpaApplicationNo character varying(256),
	refPermitNo character varying(256) NOT NULL,
	refPermitDate bigint NOT NULL,
	refPermitExpiryDate bigint NOT NULL,
	refApplicationDetails jsonb,
	createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT pk_eg_bpa_buildingplan_revision PRIMARY KEY (id),
	CONSTRAINT fk_eg_bpa_buildingplan_revision FOREIGN KEY (bpaApplicationId)
	REFERENCES eg_bpa_buildingplan (id) MATCH SIMPLE
	ON UPDATE NO ACTION
	ON DELETE NO ACTION

);


CREATE INDEX IF NOT EXISTS eg_bpa_buildingplan_revision_index ON eg_bpa_buildingplan_revision 
(
    tenantid,
    bpaApplicationNo,
    bpaApplicationId,
    isSujogExistingApplication,
    refBpaApplicationNo,
	refPermitNo
);

CREATE TABLE IF NOT EXISTS  eg_bpa_revision_documents(
    id character varying(64)  NOT NULL,
    documenttype character varying(64),
    filestoreid character varying(64),
    documentuid character varying(64),
    revisionId character varying(256),
    additionaldetails jsonb,
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT pk_eg_bpa_revision_documents PRIMARY KEY (id),
    CONSTRAINT fk_eg_bpa_revision_documents FOREIGN KEY (revisionId)
        REFERENCES eg_bpa_buildingplan_revision (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);

ALTER TABLE eg_bpa_buildingplan
ADD COLUMN IF NOT EXISTS isRevisionApplication boolean;

ALTER TABLE eg_bpa_auditdetails
ADD COLUMN IF NOT EXISTS isRevisionApplication boolean;