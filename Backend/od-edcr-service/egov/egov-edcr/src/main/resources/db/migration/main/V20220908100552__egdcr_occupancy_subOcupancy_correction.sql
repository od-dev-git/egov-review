SELECT setval('generic.seq_egbpa_occupancy', (SELECT max(id) FROM generic.egbpa_occupancy));

SELECT setval('generic.seq_egbpa_sub_occupancy', (SELECT max(id) FROM generic.egbpa_sub_occupancy));

delete from generic.egbpa_sub_occupancy where name = 'Mixed Use';

delete from generic.egbpa_occupancy where name = 'Mixed Use';

delete from generic.egbpa_sub_occupancy where occupancy  in (select id from generic.egbpa_occupancy eo where name ='Additional Feature');

delete from generic.egbpa_occupancy where name ='Additional Feature';

INSERT INTO generic.egbpa_occupancy (id,code,"name",isactive,"version",createdby,createddate,lastmodifiedby,lastmodifieddate,maxcoverage,minfar,maxfar,ordernumber,description,colorcode) VALUES
	 (nextval('generic.seq_egbpa_occupancy'),'M','Mixed Use',true,0,1,'2022-08-16 07:45:07.890',1,NULL,65,2,3,1,'Mixed Use',NULL);
	 

INSERT INTO generic.egbpa_sub_occupancy (id,code,"name",ordernumber,isactive,createdby,createddate,lastmodifieddate,lastmodifiedby,"version",description,maxcoverage,minfar,maxfar,occupancy,colorcode) VALUES
	 (nextval('generic.seq_egbpa_sub_occupancy'),'M-M','Mixed Use',1,true,1,'2022-08-16 07:46:28.076',NULL,1,0,'Mixed Use',65,3,4,(select id from generic.egbpa_occupancy eo where code ='M'),-1);
	 

INSERT INTO generic.egbpa_occupancy (id,code,"name",isactive,"version",createdby,createddate,lastmodifiedby,lastmodifieddate,maxcoverage,minfar,maxfar,ordernumber,description,colorcode) VALUES
	 (nextval('generic.seq_egbpa_occupancy'),'AF','Additional Feature',true,0,1,'2021-02-22 06:25:12.652',1,NULL,72,9,10,8,'Additional Feature',NULL);
	 
	
INSERT INTO generic.egbpa_sub_occupancy (id,code,"name",ordernumber,isactive,createdby,createddate,lastmodifieddate,lastmodifiedby,"version",description,maxcoverage,minfar,maxfar,occupancy,colorcode) VALUES
	 (nextval('generic.seq_egbpa_sub_occupancy'),'AF-OH','Outhouse',1,true,1,'2021-02-22 16:03:00.448',NULL,1,0,'Outhouse',222,160,161,(select id from generic.egbpa_occupancy eo where code ='AF'),100),
	 (nextval('generic.seq_egbpa_sub_occupancy'),'AF-PW','Public Washrooms',2,true,1,'2021-02-22 16:03:00.448',NULL,1,0,'Public Washrooms',222,160,161,(select id from generic.egbpa_occupancy eo where code ='AF'),101),
	 (nextval('generic.seq_egbpa_sub_occupancy'),'AF-AWWS','Accommodation of watch and ward/maintenance staff',3,true,1,'2021-05-04 08:30:48.406',NULL,1,0,'Accommodation of watch and ward/maintenance staff',225,163,164,(select id from generic.egbpa_occupancy eo where code ='AF'),104);