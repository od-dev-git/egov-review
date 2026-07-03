insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_GLASS_FACADE_OPENING','BLK_%s_FLR_%s_GLASS_FACADE',1,now(),1,now(),0 
where not exists(select key from state.egdcr_layername where key='LAYER_NAME_GLASS_FACADE_OPENING');


insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_MEZZANINE_AT_ROOM','BLK_%s_FLR_%s_ROOM_%s_MEZ_AREA_%s',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_MEZZANINE_AT_ROOM');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_MEZZANINE_AT_ACROOM','BLK_%s_FLR_%s_ACROOM_%s_MEZ_AREA_%s',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_MEZZANINE_AT_ACROOM');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_ICT_LP','ICT_LANDING_POINT_%s',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_ICT_LP');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_ICT_LP_LIGHT_VENTILATION','ICT_LANDING_POINT_%s_LIGHT_VENTILATION_%s',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_ICT_LP_LIGHT_VENTILATION');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_ICT_LP_DOOR','ICT_LANDING_POINT_%s_DOOR_%s',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_ICT_LP_DOOR');


insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_BLK_PORTICO','BLK_%s_PORTICO',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_BLK_PORTICO');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_SUPPLY_LINE','UTILITY_SUPPLY_LINE',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_SUPPLY_LINE');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_DRAIN_DISTANCE','DISTANCE_FROM_DRAIN',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_DRAIN_DISTANCE');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_FOOTPATH','FOOTPATH',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_FOOTPATH');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_ROAD_RESERVE_FRONT','ROAD_RESERVE_FRONT',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_ROAD_RESERVE_FRONT');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_ROAD_RESERVE_REAR','ROAD_RESERVE_REAR',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_ROAD_RESERVE_REAR');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_ROAD_RESERVE_SIDE1','ROAD_RESERVE_SIDE1',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_ROAD_RESERVE_SIDE1');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_ROAD_RESERVE_SIDE2','ROAD_RESERVE_SIDE2',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_ROAD_RESERVE_SIDE2');

--public washroom supporting feature
insert
	into
	generic.egdcr_sub_feature_colorcode (id, feature, subfeature, colorcode, ordernumber)
values (68, 'HeightOfRoom', 'Public Washroom', 101, 7);