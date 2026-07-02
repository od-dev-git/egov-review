CREATE TABLE IF NOT EXISTS eg_bpa_regularization_buildinginfo(
    id character varying(256) NOT NULL,
    tenantid character varying(256),
    regularizationid character varying(256),
    basefar character varying(256),
    maxpermissiblefar character varying(256),
    approvedfar character varying(256),
    asbuiltfar character varying(256),
    farstatus character varying(256),
    totalprovidedbua character varying(256),
    totalapprovedbua character varying(256),
    totalunauthorizedbua character varying(256),
    totalunauthareaonsbwithinnorms character varying(256),
    totalunauthareaonsbbeyondnormsbutunder5 character varying(256),
    totalunauthareaonsbbeyondnormsbutunder10 character varying(256),
    buildingblocks jsonb,
    additionaldetails jsonb,
    createdby character varying(64),
    lastmodifiedby character varying(64),
    createdtime bigint,
    lastmodifiedtime bigint,
    constraint uk_eg_bpa_regularization_buildinginfo primary key (id),
    constraint fk_eg_bpa_regularization_buildinginfo foreign key (regularizationid)
        references eg_bpa_regularization_application (id)
);

CREATE INDEX if not exists building_regularization_index ON eg_bpa_regularization_buildinginfo
(
    tenantid,
    id
);