CREATE TABLE IF NOT EXISTS  eg_bpa_installment(
    id character varying(256) NOT NULL,
    tenantid character varying(250) NOT NULL,
    installmentno integer NOT NULL,
    status character varying(64),
	consumercode character varying(250) NOT NULL,
    taxheadcode character varying(250) NOT NULL,
    taxamount numeric(12,2) NOT NULL,
	demandid character varying(64),
	ispaymentcompletedindemand boolean DEFAULT false,
    additional_details jsonb,
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT pk_eg_bpa_installment PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS eg_bpa_installment_index ON eg_bpa_installment 
(
    consumercode,
    taxheadcode,
    tenantid,
    demandid,
    id
);
