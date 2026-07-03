insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_BUILT_UP_AREA_ADD','BLT_UP_AREA_ADD',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_BUILT_UP_AREA_ADD');


insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_EXISTING_BLT_UP_AREA_ADD','BLK_%s_FLR_%s_BLT_UP_AREA_ADD_EXISTING',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_EXISTING_BLT_UP_AREA_ADD');

update state.egdcr_layername set value = 'STACK_PARKING' where key = 'LAYER_NAME_MECH_PARKING';

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_EV_CHARGING_POINT','EV_CHARGING_POINT',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_EV_CHARGING_POINT');
