insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_DIST_FROM_FRNT_EXT_WALL','BLK_%s_FLR_0_COMM_DIST_FROM_FRNT_EXT_WALL',1,now(),1,now(),0 
where not exists(select key from state.egdcr_layername where key='LAYER_NAME_DIST_FROM_FRNT_EXT_WALL');