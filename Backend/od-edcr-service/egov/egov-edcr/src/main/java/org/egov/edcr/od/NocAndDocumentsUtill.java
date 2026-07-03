package org.egov.edcr.od;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.edcr.constants.DocumentConstants;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.NOCConstants;

public class NocAndDocumentsUtill {

	public static List<String> updateNoc(Plan pl) {

		if (pl == null || pl.getPlanInformation() == null) {
			return Collections.emptyList();
		}

		List<String> list = new ArrayList<>();

		if (pl.getPlanInformation().isLowRiskBuilding()) {
//			list.add(NOCConstants.OSHB_OR_DEVELOPMENT_AUTHORITY_NOC);
//			if (DxfFileConstants.ENVIRONMENTALLY_SENSITIVE_ZONE.equals(pl.getPlanInformation().getLandUseZone()))
//				list.add(NOCConstants.DEPUTY_FORST_OFFICEER_NOC);
			if (DxfFileConstants.YES.equals(pl.getPlanInformation().getNocFromAAI())
					 || "Mandatory".equals(pl.getPlanInformation().getGisAAIDecls()))
				list.add(NOCConstants.AAI_NOC);
			if (DxfFileConstants.YES.equals(pl.getPlanInformation().getIsProjectNearOfCentrallyProtectedMonument())
					|| "Mandatory".equals(pl.getPlanInformation().getGisNMADecls()))
				list.add(NOCConstants.NMA_NOC);
//			if (DxfFileConstants.YES.equals(pl.getPlanInformation().getIsProjectNearOfStateProtectedMonument()))
//				list.add(NOCConstants.STATE_ARCHAEOLOGY);
		} else {
			if (DxfFileConstants.YES.equals(pl.getPlanInformation().getNocFromAAI())
					|| "Mandatory".equals(pl.getPlanInformation().getGisAAIDecls()))
				list.add(NOCConstants.AAI_NOC);
			if (DxfFileConstants.YES.equals(pl.getPlanInformation().getIsProjectNearOfCentrallyProtectedMonument())
					|| "Mandatory".equals(pl.getPlanInformation().getGisNMADecls()))
				list.add(NOCConstants.NMA_NOC);
//			if (DxfFileConstants.YES.equals(pl.getPlanInformation().getIsProjectNearOfStateProtectedMonument()))
//				list.add(NOCConstants.STATE_ARCHAEOLOGY);
//			if (DxfFileConstants.ENVIRONMENTALLY_SENSITIVE_ZONE.equals(pl.getPlanInformation().getLandUseZone()))
//				list.add(NOCConstants.DEPUTY_FORST_OFFICEER_NOC);

			OccupancyTypeHelper helper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
			String ocCode = "";
			String subOcCode = "";
			BigDecimal buildingHeight = OdishaUtill.getMaxBuildingHeight(pl);
			BigDecimal buildUpArea = BigDecimal.ZERO;
			if (helper != null && helper.getType() != null) {
				ocCode = helper.getType().getCode();
				if (helper.getSubtype() != null)
					subOcCode = helper.getSubtype().getCode();

				if (pl.getVirtualBuilding() != null)
					buildUpArea = pl.getVirtualBuilding().getTotalBuitUpArea();
			}
//			if (DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(ocCode)) {
//				if (buildUpArea.compareTo(new BigDecimal("150000")) > 0)
//					list.add(NOCConstants.ENVIRONMENT_CLEARANCE_NOC);
//			} else {
//				if (buildUpArea.compareTo(new BigDecimal("20000")) > 0)
//					list.add(NOCConstants.ENVIRONMENT_CLEARANCE_NOC);
//			}

//			if (buildUpArea.compareTo(new BigDecimal("500")) > 0)
//				list.add(NOCConstants.URBAN_LOCAL_BODIES_UNDER_H_AND_UD_NOC);
//
//			list.add(NOCConstants.PUBLIC_HEALTH_ENGINEERING_ORGANIZATION_NOC);

			// Need to handle from noc_approvel user if required need to send back to arch
			// and arch will trigger the noc
//			list.add(NOCConstants.CENTRAL_GROUND_WATER_AUTHORITY_NOC);

//			if (DxfFileConstants.YES.equals(pl.getPlanInformation().getProjectNearOfStrategicBuildings())
//					&& buildingHeight.compareTo(new BigDecimal("10")) > 0) {
//				list.add(NOCConstants.POLICE_UNDER_HOME_DEPARTMENT_NOC);
//			}

//			if (DxfFileConstants.YES.equals(pl.getPlanInformation().getIsProposedConstructionNextToFloodEmbankment())) {
//				list.add(NOCConstants.WATER_RESOURCES_DEPARTMENT_NOC);
//			}
//			if (DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(ocCode)) {
//				list.add(NOCConstants.ODISHA_STATE_POLLUTION_CONTROL_BOARD_NOC);
//			}

//			if (DxfFileConstants.YES
//					.equals(pl.getPlanInformation().getIsKisamOfLandRecordedAsAgricultureInRecordOfRights()))
//				list.add(NOCConstants.TEHSILDAR_OF_CONCERNED_MOUZA_UNDER_REVENUE_AND_DISASTER_MANGEMENT_NOC);
//
//			if (DxfFileConstants.YES
//					.equals(pl.getPlanInformation().getIsTheProjectAdjacentToHighwayAndHavingDirectAccessToIt())) {
//				list.add(NOCConstants.NHAI_NOC);
//			}

			if (DxfFileConstants.YES.equals(pl.getPlanInformation().getIsTheProjectCloseToTheCoastalRegion())) {
				list.add(NOCConstants.COASTAL_REGULATION_NOC);
			}

			// Fire noc
			if (ocCode.equals(DxfFileConstants.OC_RESIDENTIAL) || ocCode.equals(DxfFileConstants.OC_AGRICULTURE)) {
				if (buildingHeight.compareTo(new BigDecimal("15")) >= 0)
					list.add(NOCConstants.FIRE_SERVICE_NOC);
			} else if (DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION.equals(subOcCode)
					|| DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION.equals(subOcCode)
					|| DxfFileConstants.CNG_MOTHER_STATION.equals(subOcCode)
					|| DxfFileConstants.MULTIPLEX.equals(subOcCode)
					|| DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(ocCode)
					|| DxfFileConstants.AIRPORT.equals(subOcCode)) {
				list.add(NOCConstants.FIRE_SERVICE_NOC);
			} else if (DxfFileConstants.FIVE_STAR_HOTEL.equals(subOcCode) || DxfFileConstants.MOTELS.equals(subOcCode)
					|| DxfFileConstants.HOTEL.equals(subOcCode) || DxfFileConstants.GUEST_HOUSES.equals(subOcCode)
					|| DxfFileConstants.HOLIDAY_RESORT.equals(subOcCode)
					|| DxfFileConstants.BOARDING_AND_LODGING_HOUSES.equals(subOcCode)) {
				if (buildingHeight.compareTo(new BigDecimal("12")) >= 0
						|| buildUpArea.compareTo(new BigDecimal("500")) >= 0
						|| pl.getPlanInformation().getStartRatingForHotel() > 3)
					list.add(NOCConstants.FIRE_SERVICE_NOC);// one condition left
			} else if (DxfFileConstants.SERVICES_FOR_HOUSEHOLDS.equals(subOcCode)
					|| DxfFileConstants.BANK.equals(subOcCode) || DxfFileConstants.RESORTS.equals(subOcCode)
					|| DxfFileConstants.LAGOONS_AND_LAGOON_RESORT.equals(subOcCode)
					|| DxfFileConstants.AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS.equals(subOcCode)
					|| DxfFileConstants.FINANCIAL_SERVICES_AND_STOCK_EXCHANGES.equals(subOcCode)
					|| DxfFileConstants.COLD_STORAGE_AND_ICE_FACTORY.equals(subOcCode)
					|| DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX.equals(subOcCode)
					|| DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING.equals(subOcCode)
					|| DxfFileConstants.PROFESSIONAL_OFFICES.equals(subOcCode)
					|| DxfFileConstants.DEPARTMENTAL_STORE.equals(subOcCode)
					|| DxfFileConstants.WHOLESALE_MARKET.equals(subOcCode)
					|| DxfFileConstants.MEDIA_CENTRES.equals(subOcCode)
					|| DxfFileConstants.WEIGH_BRIDGES.equals(subOcCode)) {
				if (buildingHeight.compareTo(new BigDecimal("12")) >= 0
						|| buildUpArea.compareTo(new BigDecimal("500")) >= 0)
					list.add(NOCConstants.FIRE_SERVICE_NOC);
			} else if (DxfFileConstants.GAS_GODOWN.equals(subOcCode) || DxfFileConstants.GODOWNS.equals(subOcCode)
					|| DxfFileConstants.GOOD_STORAGE.equals(subOcCode)
					|| DxfFileConstants.WARE_HOUSE.equals(subOcCode)) {
				if (buildUpArea.compareTo(new BigDecimal("500")) >= 0)
					list.add(NOCConstants.FIRE_SERVICE_NOC);
			} else if (DxfFileConstants.RESTAURANT.equals(subOcCode) || DxfFileConstants.SHOWROOM.equals(subOcCode)
					|| DxfFileConstants.SUPERMARKETS.equals(subOcCode) || DxfFileConstants.FOOD_COURTS.equals(subOcCode)
					|| DxfFileConstants.AUDITORIUM.equals(subOcCode) || DxfFileConstants.CLUB.equals(subOcCode)
					|| DxfFileConstants.MUSIC_PAVILIONS.equals(subOcCode)
					|| DxfFileConstants.COMMUNITY_HALL.equals(subOcCode)
					|| DxfFileConstants.SCIENCE_CENTRE_OR_MUSEUM.equals(subOcCode)
					|| DxfFileConstants.CONFERNCE_HALL.equals(subOcCode)
					|| DxfFileConstants.CONVENTION_HALL.equals(subOcCode)
					|| DxfFileConstants.SCULPTURE_COMPLEX.equals(subOcCode)
					|| DxfFileConstants.CULTURAL_COMPLEX.equals(subOcCode)
					|| DxfFileConstants.EXHIBITION_CENTER.equals(subOcCode)
					|| DxfFileConstants.GYMNASIA.equals(subOcCode)
					|| DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP.equals(subOcCode)
					|| DxfFileConstants.MUSUEM.equals(subOcCode) || DxfFileConstants.PLACE_OF_WORKSHIP.equals(subOcCode)
					|| DxfFileConstants.PUBLIC_LIBRARIES.equals(subOcCode)
					|| DxfFileConstants.RECREATION_BLDG.equals(subOcCode) || DxfFileConstants.MUSUEM.equals(subOcCode)
					|| DxfFileConstants.SPORTS_COMPLEX.equals(subOcCode) || DxfFileConstants.STADIUM.equals(subOcCode)
					|| DxfFileConstants.THEATRE.equals(subOcCode) || DxfFileConstants.METRO_STATION.equals(subOcCode)
					|| DxfFileConstants.BUS_TERMINAL.equals(subOcCode) || DxfFileConstants.ISBT.equals(subOcCode)
					|| DxfFileConstants.RAILWAY_STATION.equals(subOcCode)
					|| DxfFileConstants.MULTI_LEVEL_CAR_PARKING.equals(subOcCode)
					|| DxfFileConstants.TRUCK_TERMINAL.equals(subOcCode)) {
				BigDecimal area = getMaxBuldAreaInFloor(pl);
				if (area.compareTo(new BigDecimal("500")) >= 0) {
					list.add(NOCConstants.FIRE_SERVICE_NOC);
				}
			} else if (DxfFileConstants.LOCAL_RETAIL_SHOPPING.equals(subOcCode)
					|| DxfFileConstants.SHOPPING_CENTER.equals(subOcCode)
					|| DxfFileConstants.SHOPPING_MALL.equals(subOcCode)) {

				if (buildingHeight.compareTo(new BigDecimal("9")) >= 0
						|| buildUpArea.compareTo(new BigDecimal("500")) >= 0)
					list.add(NOCConstants.FIRE_SERVICE_NOC);
			} else if (DxfFileConstants.CLINIC.equals(subOcCode) || DxfFileConstants.DISPENSARY.equals(subOcCode)
					|| DxfFileConstants.YOGA_CENTER.equals(subOcCode)
					|| DxfFileConstants.DIAGNOSTIC_CENTRE.equals(subOcCode)
					|| DxfFileConstants.GOVT_SEMI_GOVT_HOSPITAL.equals(subOcCode)
					|| DxfFileConstants.HEALTH_CENTRE.equals(subOcCode) || DxfFileConstants.HOSPITAL.equals(subOcCode)
					|| DxfFileConstants.LAB.equals(subOcCode) || DxfFileConstants.MATERNITY_HOME.equals(subOcCode)
					|| DxfFileConstants.MEDICAL_BUILDING.equals(subOcCode)
					|| DxfFileConstants.NURSING_HOME.equals(subOcCode) || DxfFileConstants.POLYCLINIC.equals(subOcCode)
					|| DxfFileConstants.REHABILITAION_CENTER.equals(subOcCode)
					|| DxfFileConstants.VETERINARY_HOSPITAL_FOR_PET_ANIMALS_AND_BIRDS.equals(subOcCode)) {
				if (buildingHeight.compareTo(new BigDecimal("12")) >= 0 || getMaxNumberOfFloor(pl) > 3
						|| pl.getPlanInformation().getNumberOfOccupantsOrUsers().compareTo(new BigDecimal("30")) > 0
						|| DxfFileConstants.YES.equals(pl.getPlanInformation().getDoesHospitalHaveCriticalCareUnit())) {
					list.add(NOCConstants.FIRE_SERVICE_NOC);
				}
			} else if (DxfFileConstants.OC_EDUCATION.equals(ocCode)) {
				if (buildingHeight.compareTo(new BigDecimal("12")) >= 0
						|| buildUpArea.compareTo(new BigDecimal("1000")) >= 0
						|| getMaxNumberOfFloorAboveGroundFloor(pl).compareTo(new BigDecimal("3")) >= 0) {
					list.add(NOCConstants.FIRE_SERVICE_NOC);
				}
			} else {
				if (buildingHeight.compareTo(new BigDecimal("12")) >= 0
						|| buildUpArea.compareTo(new BigDecimal("500")) >= 0)
					list.add(NOCConstants.FIRE_SERVICE_NOC);
			}

		}

		pl.getPlanInformation().setRequiredNOCs(list);

		return list;

	}

	private static BigDecimal getMaxBuldAreaInFloor(Plan pl) {
		BigDecimal area = BigDecimal.ZERO;
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				if (area.compareTo(floor.getArea()) > 0)
					area = floor.getArea();
			}
		}
		return area;
	}

	private static int getMaxNumberOfFloor(Plan pl) {
		int count = 0;

		for (Block block : pl.getBlocks()) {
			if (block.getBuilding() != null && block.getBuilding().getFloors() != null
					&& block.getBuilding().getFloors().size() > count)
				count = block.getBuilding().getFloors().size();
		}

		return count;
	}

	public static void updateDocuments(Plan pl) {
		if (pl == null || pl.getPlanInformation() == null) {
			return;
		}

		List<String> list = new ArrayList<>();

		if (isDocTypes1Required(pl))
			list.add(DocumentConstants.DOC_TYPES_1);
		if (isDocTypes2Required(pl))
			list.add(DocumentConstants.DOC_TYPES_2);
		if (isDocTypes3Required(pl))
			list.add(DocumentConstants.DOC_TYPES_3);
		if (isDocTypes4Required(pl))
			list.add(DocumentConstants.DOC_TYPES_4);
		if (isDocTypes5Required(pl))
			list.add(DocumentConstants.DOC_TYPES_5);
		if (isDocTypes6Required(pl))
			list.add(DocumentConstants.DOC_TYPES_6);
		if (isDocTypes7Required(pl))
			list.add(DocumentConstants.DOC_TYPES_7);

		pl.getPlanInformation().setAdditionalDocuments(list);
	}

	// DOC_TYPES_1
	private static boolean isDocTypes1Required(Plan pl) {
		boolean flage = false;
		if (DxfFileConstants.YES.equals(
				pl.getPlanInfoProperties().get(DocumentConstants.DOC_OSHB_OR_GA_OR_BDA_DEVELOPED_AND_ALLOTTED_PLOT)))
			flage = true;
		return flage;
	}

	// DOC_TYPES_2
	private static boolean isDocTypes2Required(Plan pl) {
		boolean flage = false;
		if (DxfFileConstants.YES
				.equals(pl.getPlanInfoProperties().get(DocumentConstants.DOC_PLOT_PART_OF_APPROVED_PRIVATE_LAYOUT)))
			flage = true;
		return flage;
	}

	// DOC_TYPES_3
	private static boolean isDocTypes3Required(Plan pl) {
		boolean flage = false;
		if (DxfFileConstants.YES
				.equals(pl.getPlanInfoProperties().get(DocumentConstants.DOC_DOES_PROJECT_REQUIRE_RERA_REGISTRATION)))
			flage = true;
		return flage;
	}

	// DOC_TYPES_4
	private static boolean isDocTypes4Required(Plan pl) {
		boolean flage = false;// benchmarkValuePerAcre
		if (pl.getPlanInformation().getBenchmarkValuePerAcre() != null
				&& pl.getPlanInformation().getBenchmarkValuePerAcre().compareTo(BigDecimal.ZERO) > 0)
			flage = true;
		return flage;
	}

	// DOC_TYPES_5
	private static boolean isDocTypes5Required(Plan pl) {
		boolean flage = false;
		if (DxfFileConstants.YES.equals(
				pl.getPlanInfoProperties().get(DocumentConstants.DOC_PLOT_PART_OF_UNAUTHORISED_LAYOUT_OR_REVENUE_PLOT)))
			flage = true;
		return flage;
	}

	// DOC_TYPES_6
	private static boolean isDocTypes6Required(Plan pl) {
		boolean flage = false;
		OccupancyTypeHelper helper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		String ocCode = "";
		String subOcCode = "";
		BigDecimal buildingHeight = OdishaUtill.getMaxBuildingHeight(pl);
		BigDecimal buildUpArea = BigDecimal.ZERO;
		if (helper != null && helper.getType() != null) {
			ocCode = helper.getType().getCode();
			if (helper.getSubtype() != null)
				subOcCode = helper.getSubtype().getCode();

			if (pl.getVirtualBuilding() != null)
				buildUpArea = pl.getVirtualBuilding().getTotalBuitUpArea();
		}
		// y| N | N -> m
		// Y | Y | Y -> m
		// y | Y | N -> nm
		if (DxfFileConstants.OC_RESIDENTIAL.equals(ocCode) || DxfFileConstants.OC_COMMERCIAL.equals(ocCode)
				|| buildingHeight.compareTo(new BigDecimal("15")) >= 0) {
			if (buildUpArea.compareTo(new BigDecimal("500")) >= 0) {
				if (DxfFileConstants.YES.equals(pl.getPlanInfoProperties()
						.get(DocumentConstants.DOC_IS_PROJECT_COMING_UNDER_THE_JURISDICTION_OF_PLANNING_AUTHORITIES))) {
					if (DxfFileConstants.YES.equals(pl.getPlanInfoProperties()
							.get(DocumentConstants.DOC_DOES_THE_PROJECT_HAVE_AFFORDABLE_HOUSING_COMPONENT))
							&& DxfFileConstants.YES.equals(pl.getPlanInfoProperties().get(
									DocumentConstants.DOC_DOES_THE_PROJECT_HAVE_MORE_THAN_500_SQM_BUILT_UP_AREA_EXCLUDING_THE_AFFORDABLE_HOUSING_COMPONENT)))
						flage = true;
					else if (DxfFileConstants.NO.equals(pl.getPlanInfoProperties()
							.get(DocumentConstants.DOC_DOES_THE_PROJECT_HAVE_AFFORDABLE_HOUSING_COMPONENT))
							&& DxfFileConstants.NO.equals(pl.getPlanInfoProperties().get(
									DocumentConstants.DOC_DOES_THE_PROJECT_HAVE_MORE_THAN_500_SQM_BUILT_UP_AREA_EXCLUDING_THE_AFFORDABLE_HOUSING_COMPONENT)))
						flage = true;
				}

			}
		}

		return flage;
	}

	// DOC_TYPES_7
	private static boolean isDocTypes7Required(Plan pl) {
		boolean flage = false;
		OccupancyTypeHelper helper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		String ocCode = "";
		String subOcCode = "";
		if (helper != null && helper.getType() != null) {
			ocCode = helper.getType().getCode();
			if (helper.getSubtype() != null)
				subOcCode = helper.getSubtype().getCode();
		}

		if (DxfFileConstants.APARTMENT_BUILDING.equals(subOcCode) || DxfFileConstants.HOUSING_PROJECT.equals(subOcCode)
				|| DxfFileConstants.STUDIO_APARTMENTS.equals(subOcCode) || DxfFileConstants.EWS.equals(subOcCode)
				|| DxfFileConstants.LOW_INCOME_HOUSING.equals(subOcCode)
				|| DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(subOcCode)) {
			if (getDuUnit(pl) > 8)
				flage = true;
		}

		return flage;
	}

	private static int getDuUnit(Plan pl) {
		int count = 0;
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				if (floor.getUnits() != null && !floor.getUnits().isEmpty())
					count = count + floor.getUnits().size();
			}
		}
		return count;
	}

	private static BigDecimal getMaxNumberOfFloorAboveGroundFloor(Plan pl) {
		BigDecimal count = BigDecimal.ZERO;

		for (Block block : pl.getBlocks()) {
			if (block.getBuilding() != null && block.getBuilding().getFloorsAboveGround() != null
					&& count.compareTo(block.getBuilding().getFloorsAboveGround()) < 0)
				count = block.getBuilding().getFloorsAboveGround();
		}

		return count;
	}
}
