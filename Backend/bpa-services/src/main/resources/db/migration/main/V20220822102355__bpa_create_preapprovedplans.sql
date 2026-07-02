CREATE TABLE IF NOT EXISTS  eg_bpa_preapprovedplan(
    id character varying(256) NOT NULL,
    drawing_number character varying(64),
    tenantid character varying(256),
    plot_length numeric,
    plot_width numeric,
    road_width numeric,
    drawing_detail jsonb,
    active boolean,
    additional_details jsonb,
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT pk_eg_bpa_preapprovedplan PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS bpa_preapprovedplan_index ON eg_bpa_preapprovedplan 
(
    drawing_number,
    active,
    tenantid,
    id
);

CREATE SEQUENCE IF NOT EXISTS seq_eg_bpa_preapprovedplan;

CREATE TABLE IF NOT EXISTS  eg_bpa_preapprovedplan_documents(
    id character varying(64)  NOT NULL,
    documenttype character varying(64),
    filestoreid character varying(64),
    documentuid character varying(64),
    preapprovedplanid character varying(64),
    additionaldetails jsonb,
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT pk_eg_bpa_preapprovedplan_documents PRIMARY KEY (id),
    CONSTRAINT fk_eg_bpa_preapprovedplan_documents FOREIGN KEY (preapprovedplanid)
        REFERENCES eg_bpa_preapprovedplan (id) MATCH SIMPLE
        ON UPDATE NO ACTION
        ON DELETE NO ACTION
);


ALTER TABLE eg_bpa_preapprovedplan
ADD COLUMN IF NOT EXISTS plot_length_in_feet numeric;

ALTER TABLE eg_bpa_preapprovedplan
ADD COLUMN IF NOT EXISTS plot_width_in_feet numeric;


ALTER TABLE eg_bpa_preapprovedplan
ADD COLUMN IF NOT EXISTS preapproved_code character varying(64);


