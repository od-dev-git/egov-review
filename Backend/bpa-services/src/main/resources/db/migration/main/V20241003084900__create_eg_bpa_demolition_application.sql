create table if not exists eg_bpa_demolition_application(
	id character varying(256) not null,
	tenantid character varying(256),
    applicationno character varying(64),
    status character varying(64),
    applicationtype character varying(64),
    servicetype character varying(64),
    plotdetails jsonb,
    additionaldetails jsonb,
	applicationdate bigint,
    approvalno character varying(64) default null,
    approvaldate bigint,
    businessservice character varying(64) default null::character varying,
    accountid character varying(256) default null,
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,

    constraint pk_eg_bpa_demolition_application primary key (id)
);

create table if not exists eg_bpa_demolition_landinfo(
	id character varying(256) not null,
	tenantid character varying(256),
    demolitionid character varying(64),
    totallandarea character varying(64),
    plotnumber character varying(64),
    mauza character varying(64),
    landownername character varying(64),
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,

    constraint pk_eg_bpa_demolition_landinfo primary key (id),
    constraint fk_eg_bpa_demolition_landinfo FOREIGN KEY (demolitionid) REFERENCES eg_bpa_demolition_application (id)
);

create table if not exists eg_bpa_demolition_blockinfo(
	id character varying(256) not null,
	tenantid character varying(256),
    demolitionlandinfoid character varying(64),
    anyapprovedarea character varying(64),
    occupancy character varying(64),
    totalbua character varying(64),
    nooffloors character varying(64),
    setbackdetails jsonb,
    buildingdistance character varying(64),
    buildingheight character varying(64),
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,

    constraint pk_eg_bpa_demolition_blockinfo primary key (id),
    constraint fk_eg_bpa_demolition_blockinfo FOREIGN KEY (demolitionlandinfoid) REFERENCES eg_bpa_demolition_landinfo (id)
);

CREATE TABLE if not exists eg_bpa_demolition_address (
	id varchar(64) NULL,
	tenantid varchar(256) NULL,
	doorno varchar(64) NULL,
	plotno varchar(64) NULL,
	landmark varchar(64) NULL,
	city varchar(64) NULL,
	district varchar(64) NULL,
	region varchar(64) NULL,
	state varchar(64) NULL,
	country varchar(64) NULL,
	locality varchar(64) NULL,
	pincode varchar(64) NULL,
	additiondetails varchar(64) NULL,
	buildingname varchar(64) NULL,
	street varchar(64) NULL,
	landinfoid varchar(64) NULL,
	createdby varchar(64) NULL,
	lastmodifiedby varchar(64) NULL,
	createdtime int8 NULL,
	lastmodifiedtime int8 NULL,

    constraint pk_eg_bpa_demolition_address primary key (id),
    constraint fk_eg_bpa_demolition_address FOREIGN KEY (landinfoid) REFERENCES eg_bpa_demolition_landinfo (id)
);

CREATE TABLE if not exists eg_bpa_demolition_geolocation (
	id varchar(64) NULL,
	latitude float8 NULL,
	longitude float8 NULL,
	addressid varchar(64) NULL,
	additionaldetails jsonb NULL,
	createdby varchar(64) NULL,
	lastmodifiedby varchar(64) NULL,
	createdtime int8 NULL,
	lastmodifiedtime int8 NULL,

    constraint pk_eg_bpa_demolition_geolocation primary key (id),
    constraint fk_eg_bpa_demolition_geolocation FOREIGN KEY (addressid) REFERENCES eg_bpa_demolition_address (id)
);

CREATE TABLE if not exists eg_bpa_demolition_owners (
	id varchar(256) NULL,
	uuid varchar(256) NULL,
	isprimaryowner bool NULL,
	ownershippercentage float8 NULL,
	institutionid varchar(256) NULL,
	additionaldetails jsonb NULL,
	demolitionid varchar(256) NULL,
	relationship varchar(64) NULL,
	createdby varchar(64) NULL,
	lastmodifiedby varchar(64) NULL,
	createdtime int8 NULL,
	lastmodifiedtime int8 NULL,

    constraint pk_eg_bpa_demolition_owners primary key (id)
);

CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_application_tenantid ON eg_bpa_demolition_application (tenantid);
CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_application_applicationno ON eg_bpa_demolition_application (applicationno);
CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_application_status ON eg_bpa_demolition_application (status);
CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_application_approvalno ON eg_bpa_demolition_application (approvalno);
CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_application_accountid ON eg_bpa_demolition_application (accountid);

CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_landinfo_tenantid ON eg_bpa_demolition_landinfo (tenantid);
CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_landinfo_mauza ON eg_bpa_demolition_landinfo (mauza);
CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_landinfo_landownername ON eg_bpa_demolition_landinfo (landownername);

CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_blockinfo_tenantid ON eg_bpa_demolition_blockinfo (tenantid);

CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_address_tenantid ON eg_bpa_demolition_address (tenantid);
CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_address_city ON eg_bpa_demolition_address (city);
CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_address_locality ON eg_bpa_demolition_address (locality);

create sequence if not exists seq_demolition_app_no;

create sequence if not exists seq_demolition_permit_no;