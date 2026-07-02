 CREATE TABLE public.eg_pt_bulk_assessment_audit (
	id varchar(128) NOT NULL,
	batchoffset int8 NOT NULL,
	recordcount int8 NOT NULL,
	createdtime int8 NOT NULL,
	audittime int8 NOT NULL,
	tenantid varchar(256) NOT NULL,
	message varchar(2048) NULL,
	businessservice varchar(256) NOT NULL,
	CONSTRAINT pk_pt_bulk_assessment_audit_id PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS index_pt_bulk_assessment_audit_tenantid   ON eg_pt_owner (tenantid);
CREATE INDEX IF NOT EXISTS index_pt_bulk_assessment_audit_createdtime   ON eg_pt_owner (createdtime);
