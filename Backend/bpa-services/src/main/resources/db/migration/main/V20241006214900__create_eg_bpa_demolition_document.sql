CREATE TABLE if not exists eg_bpa_demolition_document (
	id varchar(64) NULL,
	documenttype varchar(64) NULL,
	filestoreid varchar(64) NULL,
	documentuid varchar(64) NULL,
	demolitionid varchar(64) NULL,
	additionaldetails jsonb NULL,
	createdby varchar(64) NULL,
	lastmodifiedby varchar(64) NULL,
	createdtime int8 NULL,
	lastmodifiedtime int8 NULL,

	constraint pk_eg_bpa_demolition_document primary key (id),
	constraint fk_eg_bpa_demolition_documnet FOREIGN KEY (demolitionid) REFERENCES eg_bpa_demolition_application (id)
);


CREATE INDEX IF NOT EXISTS index_eg_bpa_demolition_document_id ON eg_bpa_demolition_document (id);
