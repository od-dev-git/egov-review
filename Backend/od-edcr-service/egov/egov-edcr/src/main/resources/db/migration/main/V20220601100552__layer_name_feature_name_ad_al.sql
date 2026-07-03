--LAYER_NAME_BLK_FLR_APPROVED_CONSTRUCTION
INSERT INTO state.egdcr_layername(id,"key",value,createdby,createddate,lastmodifieddate,lastmodifiedby,"version")
SELECT  nextval('state.seq_egdcr_layername'),'LAYER_NAME_BLK_FLR_APPROVED_CONSTRUCTION','BLK_%s_FLR_%s_APPROVED_CONSTRUCTION',1,now(),now(),1,0
WHERE NOT EXISTS ( SELECT "key" FROM state.egdcr_layername WHERE "key" = 'LAYER_NAME_BLK_FLR_APPROVED_CONSTRUCTION');

--LAYER_NAME_BLK_FLR_UNAUTHORIZED_CONSTRUCTION
INSERT INTO state.egdcr_layername(id,"key",value,createdby,createddate,lastmodifieddate,lastmodifiedby,"version")
SELECT  nextval('state.seq_egdcr_layername'),'LAYER_NAME_BLK_FLR_UNAUTHORIZED_CONSTRUCTION','BLK_%s_FLR_%s_UNAUTHORIZED_CONSTRUCTION',1,now(),now(),1,0
WHERE NOT EXISTS ( SELECT "key" FROM state.egdcr_layername WHERE "key" = 'LAYER_NAME_BLK_FLR_UNAUTHORIZED_CONSTRUCTION');


--LAYER_NAME_BLK_FLR_DEMOLITION_AREA
INSERT INTO state.egdcr_layername(id,"key",value,createdby,createddate,lastmodifieddate,lastmodifiedby,"version")
SELECT  nextval('state.seq_egdcr_layername'),'LAYER_NAME_BLK_FLR_DEMOLITION_AREA','BLK_%s_FLR_%s_DEMOLITION_AREA',1,now(),now(),1,0
WHERE NOT EXISTS ( SELECT "key" FROM state.egdcr_layername WHERE "key" = 'LAYER_NAME_BLK_FLR_DEMOLITION_AREA');

--LAYER_NAME_AFFECTED_LAND_AREA
INSERT INTO state.egdcr_layername(id,"key",value,createdby,createddate,lastmodifieddate,lastmodifiedby,"version")
SELECT  nextval('state.seq_egdcr_layername'),'LAYER_NAME_AFFECTED_LAND_AREA','AFFECTED_LAND_AREA',1,now(),now(),1,0
WHERE NOT EXISTS ( SELECT "key" FROM state.egdcr_layername WHERE "key" = 'LAYER_NAME_AFFECTED_LAND_AREA');


-- Feature AffectedLandArea

INSERT INTO generic.egdcr_sub_feature_colorcode(id, feature, subfeature, colorcode,ordernumber) 
SELECT nextval('generic.seq_egdcr_sub_feature_colorcode'), 'AffectedLandArea', 'CDP road widening', 1, 1
WHERE NOT EXISTS ( SELECT subfeature FROM generic.egdcr_sub_feature_colorcode WHERE feature = 'AffectedLandArea' AND subfeature = 'CDP road widening');

INSERT INTO generic.egdcr_sub_feature_colorcode(id, feature, subfeature, colorcode,ordernumber) 
SELECT nextval('generic.seq_egdcr_sub_feature_colorcode'), 'AffectedLandArea', 'CDP drain widening', 2, 1
WHERE NOT EXISTS ( SELECT subfeature FROM generic.egdcr_sub_feature_colorcode WHERE feature = 'AffectedLandArea' AND subfeature = 'CDP drain widening');

INSERT INTO generic.egdcr_sub_feature_colorcode(id, feature, subfeature, colorcode,ordernumber) 
SELECT nextval('generic.seq_egdcr_sub_feature_colorcode'), 'AffectedLandArea', 'Proposed road', 3, 1
WHERE NOT EXISTS ( SELECT subfeature FROM generic.egdcr_sub_feature_colorcode WHERE feature = 'AffectedLandArea' AND subfeature = 'Proposed road');

INSERT INTO generic.egdcr_sub_feature_colorcode(id, feature, subfeature, colorcode,ordernumber) 
SELECT nextval('generic.seq_egdcr_sub_feature_colorcode'), 'AffectedLandArea', 'CDP proposed road', 4, 1
WHERE NOT EXISTS ( SELECT subfeature FROM generic.egdcr_sub_feature_colorcode WHERE feature = 'AffectedLandArea' AND subfeature = 'CDP proposed road');

INSERT INTO generic.egdcr_sub_feature_colorcode(id, feature, subfeature, colorcode,ordernumber) 
SELECT nextval('generic.seq_egdcr_sub_feature_colorcode'), 'AffectedLandArea', 'Road widening', 5, 1
WHERE NOT EXISTS ( SELECT subfeature FROM generic.egdcr_sub_feature_colorcode WHERE feature = 'AffectedLandArea' AND subfeature = 'Road widening');

INSERT INTO generic.egdcr_sub_feature_colorcode(id, feature, subfeature, colorcode,ordernumber) 
SELECT nextval('generic.seq_egdcr_sub_feature_colorcode'), 'AffectedLandArea', 'Restricted area', 6, 1
WHERE NOT EXISTS ( SELECT subfeature FROM generic.egdcr_sub_feature_colorcode WHERE feature = 'AffectedLandArea' AND subfeature = 'Restricted area');


