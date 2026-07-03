-- generic
INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (57,'HeightOfRoom','Stilt Floor',38,7);
	
INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (58,'HeightOfRoom','Service Floor',39,7);
	
INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (59,'HeightOfRoom','MEP Room',30,7);

INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (60,'HeightOfRoom','Laundry Room',31,7);

INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (61,'HeightOfRoom','Generator Room',36,7);

INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (62,'HeightOfRoom','Lift Lobby',32,7);

INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (63,'HeightOfRoom','CCTV Room',28,7);
	 
INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (64,'HeightOfRoom','Service Room',29,7);
	

-- Passage layername changed
update state.egdcr_layername  set value = 'PASSAGE_DOUBLELOADED' where "key" ='LAYER_NAME_PASSAGE_STAIR';


--SITE_COMPONENTS
update state.egdcr_layername  set value = 'SITE_COMPONENTS' where "key" ='LAYER_NAME_SUPPLY_LINE' and value ='UTILITY_SUPPLY_LINE';

update state.egdcr_layername  set value = 'WATER_TREATMENT_PLANT' where "key" ='LAYER_NAME_INSITU_WASTE_TREATMENT_PLANT' and value ='INSITU_WASTE_TREATMENT_PLANT';


INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (65,'HeightOfRoom','Habitable Room (Naturally Ventilated)',1,7);	
	
INSERT INTO generic.egdcr_sub_feature_colorcode (id,feature,subfeature,colorcode,ordernumber) VALUES
	 (66,'HeightOfRoom','Habitable Room (Mechanically Ventilated)',2,7);

--LAYER_NAME_VEHICLE_RAMP
update state.egdcr_layername  set value = 'BLK_%s_FLR_%s_VEHICLERAMP' where "key" ='LAYER_NAME_VEHICLE_RAMP';

insert
	into
	generic.egdcr_sub_feature_colorcode (id, feature, subfeature, colorcode, ordernumber)
values (68, 'HeightOfRoom', 'Study Room', 3, 7),
(69, 'HeightOfRoom', 'Library Room', 4, 7),
(70, 'HeightOfRoom', 'Game Room', 5, 7),
(71, 'HeightOfRoom', 'Store Room', 6, 7),
(72, 'HeightOfRoom', 'Guard Room', 33, 7),
(73, 'HeightOfRoom', 'Electric Cabin', 34, 7),
(74, 'HeightOfRoom', 'Sub-Station', 35, 7),
(75, 'HeightOfRoom', 'Gym Room', 7, 7);
