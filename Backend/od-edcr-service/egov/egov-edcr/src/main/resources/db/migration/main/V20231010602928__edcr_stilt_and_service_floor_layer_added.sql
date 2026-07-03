insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_STILT_FLOOR','STILT_FLOOR',1,now(),1,now(),0 
where not exists(select key from state.egdcr_layername where key='LAYER_NAME_STILT_FLOOR');

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_SERVICE_FLOOR','SERVICE_FLOOR',1,now(),1,now(),0 
where not exists(select key from state.egdcr_layername where key='LAYER_NAME_SERVICE_FLOOR');
