CREATE TABLE IF NOT EXISTS public.eg_bpa_edcrdata(
    id character varying(64) NOT NULL,
    applicationId character varying(64) NOT NULL,
    applicationNo character varying(64),
    mauza character varying(64),
    riskType character varying(64),
    serviceType character varying(64),
    plotNumber character varying(64),
    isBUAAbove500 boolean,
    workflowName character varying(64),
    CONSTRAINT pk_eg_bpa_edcrdata PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS eg_bpa_edcrdata_index ON public.eg_bpa_edcrdata 
(
    applicationNo,
    mauza,
    riskType,
    serviceType,
    workflowName
);

