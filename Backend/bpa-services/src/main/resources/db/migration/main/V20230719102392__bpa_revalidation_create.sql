CREATE TABLE IF NOT EXISTS public.eg_bpa_buildingplan_revalidation (
	id varchar(256) NOT NULL,
	bpaapplicationno varchar(256) NULL,
	bpaapplicationid varchar(256) NULL,
	permitno varchar(256) NOT NULL,
	permitdate int8 NOT NULL,
	permitexpirydate int8 NOT NULL,
	issujogexistingapplication bool NOT NULL,
	tenantid varchar(256) NULL,
	refbpaapplicationno varchar(256) NULL,
	refapplicationdetails jsonb NULL,
	isConstructionPresent bool NULL,
	isConstructionAsPerApprovedPlan bool NULL,
	createdby varchar(64) NULL,
	lastmodifiedby varchar(64) NULL,
	createdtime int8 NULL,
	lastmodifiedtime int8 NULL,
	CONSTRAINT pk_eg_bpa_buildingplan_revalidation PRIMARY KEY (id)
);
CREATE INDEX eg_bpa_buildingplan_revalidation_index
ON public.eg_bpa_buildingplan_revalidation 
USING btree (tenantid, bpaapplicationno, bpaapplicationid, issujogexistingapplication, refbpaapplicationno, permitno);


CREATE TABLE IF NOT EXISTS public.eg_bpa_revalidation_documents (
	id varchar(64) NOT NULL,
	documenttype varchar(64) NULL,
	filestoreid varchar(64) NULL,
	documentuid varchar(64) NULL,
	revalidationid varchar(256) NULL,
	additionaldetails jsonb NULL,
	createdby varchar(64) NULL,
	lastmodifiedby varchar(64) NULL,
	createdtime int8 NULL,
	lastmodifiedtime int8 NULL,
	CONSTRAINT pk_eg_bpa_revalidation_documents PRIMARY KEY (id),
	CONSTRAINT fk_eg_bpa_revalidation_documents FOREIGN KEY (revalidationid) REFERENCES public.eg_bpa_buildingplan_revalidation(id)
);