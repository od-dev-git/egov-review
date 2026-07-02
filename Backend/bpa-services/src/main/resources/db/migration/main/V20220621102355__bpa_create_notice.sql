CREATE TABLE IF NOT EXISTS  eg_bpa_notice(
    id character varying(256) NOT NULL,
    businessid character varying(256),
	tenantid character varying(256),
    letter_number character varying(256),
    filestoreid character varying(256),
    letter_type  character varying(256),
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT pk_eg_bpa_notice PRIMARY KEY (id)
);



CREATE INDEX IF NOT EXISTS bpa_notice_index ON eg_bpa_notice 
(
    letter_number,
    businessid,
    filestoreid,
	tenantid,
    id
);

CREATE SEQUENCE IF NOT EXISTS seq_eg_bpa_notice;