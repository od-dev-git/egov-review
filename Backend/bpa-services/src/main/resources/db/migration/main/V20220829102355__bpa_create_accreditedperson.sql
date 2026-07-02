CREATE TABLE IF NOT EXISTS  eg_bpa_accredited_person(
    id character varying(256) NOT NULL,
	user_uuid character varying(256) NOT NULL,
	user_id bigint NOT NULL,
    person_name character varying(256),
	firm_name character varying(256),
    accreditation_no character varying(256),
	certificate_issue_date bigint NOT NULL,
	valid_till bigint NOT NULL,
	createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT pk_eg_bpa_accredited_person PRIMARY KEY (id)

);

CREATE INDEX IF NOT EXISTS eg_bpa_accredited_person_index ON eg_bpa_accredited_person 
(
    person_name,
    firm_name,
    accreditation_no
);

CREATE SEQUENCE IF NOT EXISTS seq_eg_bpa_accredited_person
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;