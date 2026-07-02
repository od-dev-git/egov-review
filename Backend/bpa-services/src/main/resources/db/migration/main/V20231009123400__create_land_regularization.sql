CREATE TABLE IF NOT EXISTS eg_bpa_regularization_application(
    id character varying(256) NOT NULL,
    tenantid character varying(256),
    applicationno character varying(64),
    apptype character varying(64),
    applicationdate bigint,
    approvaldate bigint,
    status character varying(256),
    applicationtype character varying(256),
    servicetype character varying(256),
    servicesubtype character varying(256),
    businessService character varying(64) DEFAULT NULL::character varying,
    accountid character varying(256) DEFAULT NULL,
    approvalno character varying(64) DEFAULT NULL,
    permitexpirydate bigint,
    additionaldetails jsonb,
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT uk_eg_bpa_regularization_application PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS eg_bpa_regularization_landinfo(
    id character varying(256) NOT NULL,
    tenantid character varying(256),
    regularizationid character varying(256),
    regularizationtype character varying(256),
    totalplotarea character varying(256),
    accessroadwidth character varying(256),
    additionaldetails jsonb,
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    constraint uk_eg_bpa_regularization_landinfo primary key (id),
    constraint fk_eg_bpa_regularization_landinfo foreign key (regularizationid)
        references eg_bpa_regularization_application (id)
);

CREATE TABLE IF NOT EXISTS eg_bpa_regularization_plotinfo(
    id character varying(256) NOT NULL,
    tenantid character varying(256),
    landinfoid character varying(256),
    district character varying(256),
    tehsil character varying(256),
    village character varying(256),
    plotno character varying(1024),
    subplotno character varying(1024),
    subsubplotno character varying(1024),
    plotarea character varying(256),
    khata character varying(256),
    kisam character varying(256),
    landownername character varying(256),
    gpaholdername character varying(256),
    saledeedno character varying(256),
    bmvvalue character varying(256),
    plottobegifted bool,
    areatobegifted character varying(256),
    reasonforgift character varying(256),
    additionaldetails jsonb,
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    constraint uk_eg_bpa_regularization_plotinfo primary key (id),
    constraint fk_eg_bpa_regularization_plotinfo foreign key (landinfoid)
        references eg_bpa_regularization_landinfo (id)
);

create table if not exists eg_bpa_regularization_document(
    id character varying(64) not null,
    documenttype character varying(64),
    filestoreid character varying(64),
    documentuid character varying(64),
    regularizationid character varying(64),
    additionaldetails jsonb,
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    constraint uk_eg_bpa_regularization_document primary key (id),
    constraint fk_eg_bpa_regularization_document foreign key (regularizationid)
        references eg_bpa_regularization_application (id)
);

create table if not exists eg_bpa_regularization_dscdetails(
    id character varying(64),
    tenantid character varying(64),
    documenttype character varying(64),
    documentid character varying(64),
    regularizationid character varying(64),
    applicationno character varying(64),
    approvedby character varying(64),
	additionaldetails JSONB,
    createdBy character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,

    constraint uk_eg_bpa_regularization_dscdetails primary key (id),
    constraint fk_eg_bpa_regularization_dscdetails foreign key (regularizationid) 
    	references eg_bpa_regularization_application (id)
);

CREATE TABLE IF NOT EXISTS eg_bpa_regularization_notice(
    id character varying(256) NOT NULL,
    tenantid character varying(256),
    businessid character varying(256),
    letter_number character varying(256),
    filestoreid character varying(256),
    letter_type character varying(256),
    isClosed bool,
    notice_reminder_count bigint,
    additionaldetails jsonb,
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    CONSTRAINT uk_eg_bpa_regularization_notice PRIMARY KEY (id)
);

CREATE INDEX if not exists regularization_index ON eg_bpa_regularization_application 
(
    applicationno,
    approvalno,
    tenantid,
    id,
    status
);

CREATE INDEX if not exists land_regularization_index ON eg_bpa_regularization_landinfo
(
    regularizationtype,
    tenantid,
    id
);

CREATE INDEX if not exists land_regularization_plotinfo_index ON eg_bpa_regularization_plotinfo
(
    landinfoid,
    tenantid,
    id,
    village
);

CREATE INDEX if not exists regularization_dscdetails_index ON eg_bpa_regularization_dscdetails
(
    tenantid,
    approvedby,
    documentid,
    applicationno
);

CREATE SEQUENCE IF NOT EXISTS seq_regularization_app_no;

CREATE SEQUENCE IF NOT EXISTS seq_regularization_permit_no;