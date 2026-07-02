ALTER TABLE IF EXISTS eg_bpa_auditdetails
ADD IF NOT EXISTS isrevalidationapplication BOOLEAN NULL,
ADD IF NOT EXISTS permitexpirydate BIGINT NULL;
