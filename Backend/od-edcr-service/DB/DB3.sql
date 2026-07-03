/* INSERT QUERY NO: 164 */
INSERT INTO generic.egbpa_sub_occupancy(id, code, name, ordernumber, isactive, createdby, createddate, lastmodifieddate, lastmodifiedby, version, maxcoverage, minfar, maxfar, occupancy, description,colorcode)
VALUES 
(164, 'AF-AWWS', 'Accommodation of watch and ward/maintenance staff', 3, 't', 1, now(), NULL, 1, 0, 225, 163, 164, (select id from generic.egbpa_occupancy where code='AF'), 'Accommodation of watch and ward/maintenance staff', 104);

--Missed layer name 

insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_ACCBLK_UNIT','ACCBLK_%s_UNIT_%s',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_ACCBLK_UNIT');



insert into state.egdcr_layername(id,key,value,createdby,createddate,lastmodifiedby,lastmodifieddate,version) 
select nextval('state.seq_egdcr_layername'),'LAYER_NAME_ACCBLK_DIST','ACCBLK_%s_DIST',1,now(),1,now(),0 where not exists(select key from state.egdcr_layername where key='LAYER_NAME_ACCBLK_DIST');