CREATE TABLE IF NOT EXISTS eg_bpa_oc_outsidesujog_scrutiny(
    id character varying(256) NOT NULL,
	bpaid character varying(64),
    tenantid character varying(256),
	infotype character varying(256),
	outsidepermitnumber character varying(256),
	plotarea character varying(256),
	giftedlandarea  character varying(256),
	buildingblocks jsonb,
	basefar character varying(256),
	maxpermissiblefar character varying(256),
	approvedfar character varying(256),
	providedfar character varying(256),
	tdrfarrelaxation character varying(256),
	totalbua  character varying(256),
	totalfloorarea character varying(256),
	totalcarpetarea character varying(256),
	nooftemporarystructures character varying(256),
	projectvalueforeidp character varying(256),
	isShelterFeeApplicable bool,
	isSecurityDepositRequired bool,
	bmvperacre character varying(256),
	isretentionfeeapplicable bool,
	totalnoofdwellingunits character varying(256),
	occupancyTypeHelperCode character varying(256),
	occupancySubTypeHelperCode character varying(256),
	permitfee jsonb,
	additionaldetails jsonb,
    constraint uk_eg_bpa_oc_outsidesujog_scrutiny primary key (id),
	constraint fk_eg_bpa_oc_outsidesujog_scrutiny foreign key (bpaid)
        references eg_bpa_buildingplan (id)
);

create index eg_bpa_oc_outsidesujog_scrutiny_index on eg_bpa_oc_outsidesujog_scrutiny(
	tenantid,
	id
)
