alter table eg_bpa_edcrdata 
add column occupancytype varchar(128),
add column basementPresent boolean,
add column affordableUnitsPresent varchar(64),
add column noOfDwellingUnits varchar(64),
add column publicWashRoomProvided boolean,
add column noOfFloors varchar(64),
add column approachRoadWidth varchar(64),
add column proposedFar varchar(64);