ALTER TABLE eg_bpa_draft
ADD COLUMN IF NOT EXISTS bpaapplicationno character varying(64) NULL;
