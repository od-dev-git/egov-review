ALTER TABLE IF EXISTS public.eg_bpa_buildingplan
ADD IF NOT EXISTS isRevalidationApplication BOOLEAN,
ADD IF NOT EXISTS permitExpiryDate BIGINT;
