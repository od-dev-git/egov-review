CREATE TABLE IF NOT EXISTS eg_bpa_regularization_document_remark (
    id character varying(256) NOT NULL,
    businessid character varying(256),
    documentcode character varying(256),
	additionaldetails jsonb,
	isupdatable bool,
	createdby character varying(64),
	createdtime bigint,
	lastmodifiedby character varying(64),
	lastmodifiedtime bigint,
    CONSTRAINT pk_eg_bpa_regularization_document_remark PRIMARY KEY (id)  
);

CREATE INDEX IF NOT EXISTS bpa_regularization_document_remark_index ON eg_bpa_regularization_document_remark (
    businessid,
    documentcode,
    id
);

CREATE SEQUENCE IF NOT EXISTS seq_eg_bpa_regularization_document_remark;