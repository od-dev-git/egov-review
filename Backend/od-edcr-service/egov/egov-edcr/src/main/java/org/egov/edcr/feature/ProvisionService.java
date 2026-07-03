package org.egov.edcr.feature;

import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Building;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyPercentage;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.stereotype.Service;

@Service
public class ProvisionService extends FeatureProcess {

	public static final int SCALE = 4;
	public static final BigDecimal PLOT_AREA_FOUR_ACRE = BigDecimal.valueOf(4);
	public static final BigDecimal ACRE_TO_SQ_MT = BigDecimal.valueOf(4046.86);
	public static final BigDecimal MIN_EWS_BUA_PERCENTAGE = BigDecimal.valueOf(0.1);
	public static final BigDecimal MIN_RESERVE_NS_AND_CF_PERCENTAGE = BigDecimal.valueOf(0.05);
	public static final BigDecimal MIN_RESERVE_NS_PERCENTAGE = BigDecimal.valueOf(0.03);
	public static final BigDecimal MIN_PLOT_SIZE_FOR_EWS = BigDecimal.valueOf(2000);
//	public static final BigDecimal MAX_FAR_ALLOWED = BigDecimal.valueOf(3.5);
	public static final BigDecimal COMERCIAL_ACTIVITY_MAX_PERCENTAGE = BigDecimal.valueOf(5);
	public static final BigDecimal MAX_ACCOMODATION_FOR_FARM_HOUSE = BigDecimal.valueOf(100);
	public static final BigDecimal MAX_ACCOMODATION_FOR_COUNTRY_HOMES = BigDecimal.valueOf(100);
	
	public static final BigDecimal FIVE = BigDecimal.valueOf(5);
	public static final BigDecimal NINE = BigDecimal.valueOf(9);
	public static final BigDecimal TWELVE = BigDecimal.valueOf(12);
	public static final BigDecimal EIGHTEEN = BigDecimal.valueOf(18);
	public static final BigDecimal TWENTY_FOUR = BigDecimal.valueOf(24);
	public static final BigDecimal THIRTY = BigDecimal.valueOf(30);
	public static final int ALL_FLOOR = 99;

	public static final String PROVIDED_WITHIN_5KM = "Provided within 5KM from project site";

	/* Error Key */
	private static final String COMMERCIAL_ACTIVITY = "Commercial Activity";
	private static final String COMMERCIAL_ACTIVITY_DEPTH = "Commercial Activity Depth";
	private static final String SHOP_CUM_RESIDENTIAL_FLOOR_AREA = "Shop Cum Residential Floor Area";
	private static final String SHOP_CUM_RESIDENTIAL_GROUND_FLOOR = "Shop Cum Residential Ground Floor";
	/* Error Description */
	public static final String COMMERCIAL_ACTIVITY_NOT_ALLOWED = "Commercial activity not allowed in Block %s Floor %s as per available road width";
	public static final String OTHER_ACTIVITY_NOT_ALLOWED = "%s sub-occupancy not allowed";
	public static final String SHOP_CUM_RESIDENTIAL_FLOOR_AREA_DESC = "Shop Cum Residential floor area is allowed upto 2/3 of total floor area";
	public static final String SHOP_CUM_RESIDENTIAL_GROUND_FLOOR_DESC = "Entire ground floor is not allowed as residential in block %s";
	public static final String SHOP_CUM_RESIDENTIAL_GROUND_FLOOR_DEPTH_DESC = "Depth below 10 meters are nt allowed in shop-cum-residetial ground floor";
	private static final String COMMERCIAL_ACTIVITY_DEPTH_DESC = "Depth allow till 10 meters from the front setback line in block %s";

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
//		validateCommercialActivity(pl);
		processEwsProvision(pl);
		otherProvisions(pl);
		return pl;
	}

	private void otherProvisions(Plan pl) {
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

//		if (DxfFileConstants.OC_COMMERCIAL.equals(occupancyTypeHelper.getType().getCode())) {
//			provisionForCommercial(pl);
//		} else if (DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL
//				.equals(occupancyTypeHelper.getType().getCode())) {
//			provisionsForPublicSemiOrInstitutional(pl);
//		} else if (DxfFileConstants.OC_AGRICULTURE.equals(occupancyTypeHelper.getType().getCode())) {
//			provisionsForAgriculture(pl);
//		}

	}

	public boolean provisionsForAgriculture(Plan pl,OccupancyPercentage occupancyPercentage) {
		//OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		boolean isUnderTheProvision = false;
		BigDecimal maxAccomodation = BigDecimal.ZERO;
		if (DxfFileConstants.FARM_HOUSE.equals(occupancyPercentage.getSubOccupancyCode())) {
			maxAccomodation = MAX_ACCOMODATION_FOR_FARM_HOUSE;
		} else if (DxfFileConstants.COUNTRY_HOMES.equals(occupancyPercentage.getSubOccupancyCode())) {
			maxAccomodation = MAX_ACCOMODATION_FOR_COUNTRY_HOMES;
		}

		BigDecimal providedAccomodation = calculateBuildUpArea(pl,
				Arrays.asList(DxfFileConstants.ACCOMODATION_OF_WATCH_AND_WARD_MAINTENANCE_STAFF));
		if (maxAccomodation.compareTo(providedAccomodation) < 0) {
			isUnderTheProvision = true;
		}else {
			isUnderTheProvision = false;
		}
		return isUnderTheProvision;
	}


	public boolean provisionsForPublicSemiOrInstitutional(Plan pl,OccupancyPercentage occupancyPercentage) {
		//OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		boolean isUnderTheProvision = false;
		if (DxfFileConstants.CINEMA.equals(occupancyPercentage.getSubOccupancyCode())) {
			BigDecimal cinemaBUA = calculateBuildUpArea(pl, Arrays.asList(DxfFileConstants.CINEMA));
			BigDecimal cinemaPercentage = cinemaBUA.divide(pl.getVirtualBuilding().getTotalBuitUpArea(), 4)
					.multiply(BigDecimal.valueOf(100)).setScale(SCALE, BigDecimal.ROUND_HALF_UP);
			if (BigDecimal.valueOf(90).compareTo(cinemaPercentage) > 0) {
				// less than 90%. considered as Mixed use building
				isUnderTheProvision = false;
				return isUnderTheProvision;
			}else {
				isUnderTheProvision = true;
			}

			BigDecimal commercialBua = calculateBuildUpArea(pl, getCommercialSubOccupancies());
			BigDecimal commercialPercentage = commercialBua.divide(pl.getVirtualBuilding().getTotalBuitUpArea(), 4)
					.multiply(BigDecimal.valueOf(100)).setScale(SCALE, BigDecimal.ROUND_HALF_UP);
			if (BigDecimal.valueOf(10).compareTo(commercialPercentage) < 0) {
				// More than 10%. considered as Mixed use building
				isUnderTheProvision = false;
			}else {
				isUnderTheProvision = true;
			}
		}
		return isUnderTheProvision;
	}

	public boolean provisionForCommercial(Plan pl,OccupancyPercentage occupancyPercentage) {
		//OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		boolean isUnderTheProvision = false;
		if (DxfFileConstants.HOTEL.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.FIVE_STAR_HOTEL.equals(occupancyPercentage.getSubOccupancyCode())) {
			BigDecimal comOfcAndRetAndSerShopFloorArea = BigDecimal.ZERO;
			for (Block b : pl.getBlocks()) {
				for (Floor fl : b.getBuilding().getFloors()) {
					for (Occupancy type : fl.getOccupancies()) {
						if (!getOtherAllowedSubOccupanciesInOcCommercial()
								.contains(type.getTypeHelper().getSubtype().getCode())
								&& !(DxfFileConstants.HOTEL.equals(type.getTypeHelper().getSubtype().getCode())
										|| DxfFileConstants.FIVE_STAR_HOTEL
												.equals(type.getTypeHelper().getSubtype().getCode()))) {
							/* Other than allowed occupancy present. considered as Mixed use building */
							isUnderTheProvision = false;
							return isUnderTheProvision;
						}else {
							isUnderTheProvision = true;
						}

						if (getCommercialOfficeAndRetailServiceList()
								.contains(type.getTypeHelper().getSubtype().getCode())) {
							comOfcAndRetAndSerShopFloorArea = comOfcAndRetAndSerShopFloorArea.add(type.getFloorArea());
						}
					}
				}
			}

			BigDecimal percentage = comOfcAndRetAndSerShopFloorArea
					.divide(pl.getVirtualBuilding().getTotalFloorArea(), 4).multiply(BigDecimal.valueOf(100))
					.setScale(SCALE, BigDecimal.ROUND_HALF_UP);
			if (BigDecimal.valueOf(20).compareTo(percentage) <= 0) {
				// More than 20%. Considered as Mixed use building
				isUnderTheProvision = true;
			}else {
				isUnderTheProvision = false;
			}
		} 
//		else if (DxfFileConstants.SHOP_CUM_RESIDENTIAL.equals(occupancyPercentage.getSubOccupancyCode())) {
//			BigDecimal shopCumResidentialFloorArea = calculateTotalDeductedBuildupArea(pl,
//					Arrays.asList(DxfFileConstants.SHOP_CUM_RESIDENTIAL));
//			if (pl.getVirtualBuilding().getTotalFloorArea().multiply(BigDecimal.valueOf(2))
//					.divide(BigDecimal.valueOf(3), 4).compareTo(shopCumResidentialFloorArea) < 0) {
//				pl.addError(SHOP_CUM_RESIDENTIAL_FLOOR_AREA, String.format(SHOP_CUM_RESIDENTIAL_FLOOR_AREA_DESC));
//			}
//
//			if (isOtherThanAllowedSubOccupanciesPresent(pl)) {
//				// Mixed Occupancies
//				isUnderTheProvision = false;
//			} else {
//				isUnderTheProvision = true;
//				checkingGoundFloor(pl);
//			}
//		} 
		else if (DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING
				.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.DEPARTMENTAL_STORE.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.LOCAL_RETAIL_SHOPPING.equals(occupancyPercentage.getSubOccupancyCode())
				|| DxfFileConstants.RESTAURANT.equals(occupancyPercentage.getSubOccupancyCode())) {

			Map<String, OccupancyPercentage> occupancyPercentages = pl.getPlanInformation().getOccupancyPercentages();

			BigDecimal totalFloorAreaOfCommercial = occupancyPercentages.get(occupancyPercentage.getSubOccupancy())
					.getTotalFloorArea();

			if (totalFloorAreaOfCommercial.compareTo(pl.getVirtualBuilding().getTotalFloorArea()
					.multiply(BigDecimal.valueOf(2)).divide(BigDecimal.valueOf(3), 4)) >= 0) {

				if (isOtherThanAllowedSubOccupanciesPresent(pl)) {
					isUnderTheProvision = false;
				} else {
					isUnderTheProvision = true;
					
					if ((occupancyPercentages.containsKey("Convenience and Neighborhood Shopping")
							|| occupancyPercentages.containsKey("Departmental store")
							|| occupancyPercentages.containsKey("Local(retail) shopping")
							|| occupancyPercentages.containsKey("Restaurant"))

							&& (occupancyPercentages.containsKey("Residential Plotted")
									|| occupancyPercentages.containsKey("Semi-detached")
									|| occupancyPercentages.containsKey("Row housing"))) {
						pl.getPlanInformation().setIsProjectShopCumResidential(true);						
					}
					
					
				}
			} else {
				isUnderTheProvision = false;
			}

		}
		return isUnderTheProvision;
	}

	private void checkingGoundFloor(Plan pl) {
//		for (Block b : pl.getBlocks()) {
//			for (Floor fl : b.getBuilding().getFloors()) {
//				if (fl.getNumber() == 0) {
//					boolean entireFloorShopCumResidential = true;
//					boolean entireFloorResidential = true;
//					for (Occupancy type : fl.getOccupancies()) {
//						if (DxfFileConstants.SHOP_CUM_RESIDENTIAL.equals(type.getTypeHelper().getSubtype().getCode())) {
//							entireFloorResidential = false;
//						} else {
//							entireFloorShopCumResidential = false;
//						}
//					}
//					if (entireFloorResidential) {
//						// Error
//						pl.addError(SHOP_CUM_RESIDENTIAL_GROUND_FLOOR,
//								String.format(SHOP_CUM_RESIDENTIAL_GROUND_FLOOR_DESC, b.getNumber()));
//					} else if (!entireFloorShopCumResidential) {
//						// Layer mandate
//						BigDecimal commercialDepth = b.getBuilding().getDistanceFromSetBackToBuildingLine().isEmpty()
//								? BigDecimal.ZERO
//								: Collections.max(b.getBuilding().getDistanceFromSetBackToBuildingLine());
//						if (BigDecimal.valueOf(10).compareTo(commercialDepth) < 0) {
//							// Error
//							pl.addError(SHOP_CUM_RESIDENTIAL_GROUND_FLOOR,
//									String.format(SHOP_CUM_RESIDENTIAL_GROUND_FLOOR_DEPTH_DESC));
//						}
//					}
//				}
//
//			}
//		}

	}

	private boolean isOtherThanAllowedSubOccupanciesPresent(Plan pl) {
		List<String> allowedSubOccupancies = Arrays.asList(
				DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING, DxfFileConstants.SEMI_DETACHED,
				DxfFileConstants.ROW_HOUSING, DxfFileConstants.LOCAL_RETAIL_SHOPPING,
				DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING, DxfFileConstants.DEPARTMENTAL_STORE,
				DxfFileConstants.RESTAURANT);
		for (Block b : pl.getBlocks()) {
			for (Floor fl : b.getBuilding().getFloors()) {
				for (Occupancy type : fl.getOccupancies()) {
					if (!allowedSubOccupancies.contains(type.getTypeHelper().getSubtype().getCode())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public boolean isCommercialActivityPermisibleForRes(Plan pl) {
		boolean flage = false;
		BigDecimal commercialBua = calculateBuildUpArea(pl, getCommercialSubOccupancies());
		BigDecimal commercialActivityPercentage = commercialBua
				.divide(pl.getVirtualBuilding().getTotalBuitUpArea(), 4).multiply(BigDecimal.valueOf(100))
				.setScale(SCALE, BigDecimal.ROUND_HALF_UP);
		if (COMERCIAL_ACTIVITY_MAX_PERCENTAGE.compareTo(commercialActivityPercentage) >= 0) {
			// not a Mixed Use
			flage = true;
		}
		
		if(commercialActivityPercentage.compareTo(FIVE) > 0) {
			validateCommercialActivityWithRoadWidth(pl);
		}	
		
		if(commercialActivityPercentage.compareTo(BigDecimal.ZERO)>0) {
			
			commercialBua = calculateBuildUpArea(pl, getCommercialSubOccupancies());
			commercialActivityPercentage = commercialBua
					.divide(pl.getVirtualBuilding().getTotalBuitUpArea(), 4).multiply(BigDecimal.valueOf(100))
					.setScale(SCALE, BigDecimal.ROUND_HALF_UP);
			
			ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
			scrutinyDetail.addColumnHeading(1, RULE_NO);
			scrutinyDetail.addColumnHeading(2, DESCRIPTION);
			scrutinyDetail.addColumnHeading(3, REQUIRED);
			scrutinyDetail.addColumnHeading(4, PROVIDED);
			scrutinyDetail.addColumnHeading(5, STATUS);
			scrutinyDetail.setKey("Common_Commercial Activity Provisions");

			Map<String, String> details = new HashMap<>();
			details.put(RULE_NO, "ODA Rule 64(1,2,3), 65(2,3,4), 75(4-iii), 76(4-ii)");
			details.put(DESCRIPTION, "Commercial activity percentage");
			details.put(REQUIRED, "Max 5%");
			details.put(PROVIDED, commercialActivityPercentage.toString());
			details.put(STATUS, Result.Accepted.getResultVal());
			scrutinyDetail.getDetail().add(details);
			pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		}
		return flage;
	}
	
	private void validateCommercialActivity(Plan pl) {
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		if (occupancyTypeHelper != null) {
			if (DxfFileConstants.OC_RESIDENTIAL.equals(occupancyTypeHelper.getType().getCode())
					&& (DxfFileConstants.APARTMENT_BUILDING.equals(occupancyTypeHelper.getSubtype().getCode())
							|| DxfFileConstants.HOUSING_PROJECT.equals(occupancyTypeHelper.getSubtype().getCode()))) {
				validateCommercialActivityWithRoadWidth(pl);
				BigDecimal commercialBua = calculateBuildUpArea(pl, getCommercialSubOccupancies());
				BigDecimal commercialActivityPercentage = commercialBua
						.divide(pl.getVirtualBuilding().getTotalBuitUpArea(), 4).multiply(BigDecimal.valueOf(100))
						.setScale(SCALE, BigDecimal.ROUND_HALF_UP);
				if (COMERCIAL_ACTIVITY_MAX_PERCENTAGE.compareTo(commercialActivityPercentage) <= 0) {
					// Mixed Use
				}

				ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
				scrutinyDetail.addColumnHeading(1, RULE_NO);
				scrutinyDetail.addColumnHeading(2, DESCRIPTION);
				scrutinyDetail.addColumnHeading(3, REQUIRED);
				scrutinyDetail.addColumnHeading(4, PROVIDED);
				scrutinyDetail.addColumnHeading(5, STATUS);
				scrutinyDetail.setKey("Common_Commercial Activity Provisions");

				Map<String, String> details = new HashMap<>();
				details.put(RULE_NO, "ODA Rule 64(1,2,3), 65(2,3,4), 75(4-iii), 76(4-ii)");
				details.put(DESCRIPTION, "Commercial activity percentage");
				details.put(REQUIRED, "Max 5%");
				details.put(PROVIDED, commercialActivityPercentage.toString());
				details.put(STATUS, Result.Accepted.getResultVal());
				scrutinyDetail.getDetail().add(details);
				pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
			} else if (DxfFileConstants.OC_MIXED_USE.equals(occupancyTypeHelper.getType().getCode())) {
				validateCommercialActivityWithRoadWidth(pl);
			}
		}

	}

	private void validateCommercialActivityWithRoadWidth(Plan pl) {

		BigDecimal roadWidth = pl.getPlanInformation().getTotalRoadWidth();
		int allowedTillFloor = -1;
		boolean needDepthCheck = false;
		if (THIRTY.compareTo(roadWidth) <= 0) {
			allowedTillFloor = ALL_FLOOR;
		} else if (TWENTY_FOUR.compareTo(roadWidth) >= 0 && EIGHTEEN.compareTo(roadWidth) < 0) {
			allowedTillFloor = 2;
		} else if (TWELVE.compareTo(roadWidth) < 0) {
			allowedTillFloor = 1;
		} else if (NINE.compareTo(roadWidth) < 0) {
			allowedTillFloor = 0;
			needDepthCheck = true;
		}

		for (Block b : pl.getBlocks()) {
			for (Floor fl : b.getBuilding().getFloors()) {
				boolean hasCommercialOccupancy = false;
				for (Occupancy type : fl.getOccupancies()) {
					if (DxfFileConstants.HOTEL.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.FIVE_STAR_HOTEL.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.MOTELS.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.SERVICES_FOR_HOUSEHOLDS
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.BANK.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.RESORTS.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.LAGOONS_AND_LAGOON_RESORT
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.FINANCIAL_SERVICES_AND_STOCK_EXCHANGES
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.COLD_STORAGE_AND_ICE_FACTORY
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.PROFESSIONAL_OFFICES.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.DEPARTMENTAL_STORE.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.GAS_GODOWN.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.GODOWNS.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.GOOD_STORAGE.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.GUEST_HOUSES.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.HOLIDAY_RESORT.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.BOARDING_AND_LODGING_HOUSES
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.CNG_MOTHER_STATION.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.RESTAURANT.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.LOCAL_RETAIL_SHOPPING
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.SHOPPING_CENTER.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.SHOPPING_MALL.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.SHOWROOM.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT
									.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.SUPERMARKETS.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.WARE_HOUSE.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.WHOLESALE_MARKET.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.MEDIA_CENTRES.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.FOOD_COURTS.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.WEIGH_BRIDGES.equals(type.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.MERCENTILE.equals(type.getTypeHelper().getSubtype().getCode())) {
						hasCommercialOccupancy = true;
					}
				}
				if (hasCommercialOccupancy && allowedTillFloor < ALL_FLOOR && allowedTillFloor < fl.getNumber()) {
					pl.addError(COMMERCIAL_ACTIVITY,
							String.format(COMMERCIAL_ACTIVITY_NOT_ALLOWED, b.getNumber(), fl.getNumber()));
				}
			}
			if (needDepthCheck) {
				BigDecimal commercialDepth = b.getBuilding().getDistanceFromSetBackToBuildingLine().isEmpty()
						? BigDecimal.ZERO
						: Collections.max(b.getBuilding().getDistanceFromSetBackToBuildingLine());
				if (BigDecimal.valueOf(10).compareTo(commercialDepth) < 0) {
					pl.addError(COMMERCIAL_ACTIVITY_DEPTH,
							String.format(COMMERCIAL_ACTIVITY_DEPTH_DESC, b.getNumber()));
				}
			}
		}

	}

	private void processEwsProvision(Plan pl) {

		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setHeading("EWS and LIH Provisions");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		scrutinyDetail.setKey("Common_EWS Provisions");

		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		if (DxfFileConstants.OC_RESIDENTIAL.equals(occupancyTypeHelper.getType().getCode())
				&& (DxfFileConstants.APARTMENT_BUILDING.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.HOUSING_PROJECT.equals(occupancyTypeHelper.getSubtype().getCode()))) {

			if (pl != null && pl.getPlanInformation().getPlotArea().compareTo(MIN_PLOT_SIZE_FOR_EWS) >= 0) {

				BigDecimal plotArea = pl.getPlanInformation().getPlotArea();
				BigDecimal plotAreaInAcre = pl.getPlanInformation().getPlotArea().divide(ACRE_TO_SQ_MT,
						DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);

				BigDecimal totalBua = calculateTotalDeductedBuildupArea(pl, getSubOccupanciesForEWS());
				BigDecimal mandatoryEWSBUA = totalBua.multiply(MIN_EWS_BUA_PERCENTAGE).setScale(SCALE,
						BigDecimal.ROUND_HALF_UP);
				
				BigDecimal carpetWithoutEWSandLIG = calculateCarpetAreaWithoutEWSandLIG(pl);
				BigDecimal mandatoryCarpet = carpetWithoutEWSandLIG.multiply(MIN_EWS_BUA_PERCENTAGE).setScale(SCALE,
						BigDecimal.ROUND_HALF_UP);
				
				

				boolean status = false;
				boolean hasProvidevEWSWithin5Km = false;
				boolean isShelterFeeRequired = false;
				if (plotArea.compareTo(new BigDecimal("2000")) > 0
						&& plotAreaInAcre.compareTo(PLOT_AREA_FOUR_ACRE) <= 0) {
					if (calculateCarpetAreaOfEWSandLIG(pl).compareTo(mandatoryCarpet) >= 0) {
						status = true;
					} else if (DxfFileConstants.YES.equalsIgnoreCase(pl.getPlanInfoProperties().get(
							DxfFileConstants.HAS_PROJECT_PROVIDED_MIN_10_PER_BUA_FOR_EWS_WITHIN_5_KM_FROM_PROJECT_SITE))) {
						hasProvidevEWSWithin5Km = true;
						status = true;
					} else {
						status = true;
						isShelterFeeRequired = true;
					}
				} else if (plotAreaInAcre.compareTo(PLOT_AREA_FOUR_ACRE) > 0) {
					
					//put setScale to get precision of 2 in the report only
					
					BigDecimal mandateNsAndCf = calculateCarpetAreaOfEWSandLIG(pl).multiply(MIN_RESERVE_NS_AND_CF_PERCENTAGE)
							.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);
					
					BigDecimal mandateNs = calculateCarpetAreaOfEWSandLIG(pl).multiply(MIN_RESERVE_NS_PERCENTAGE)
							.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);
					BigDecimal providedNsAndCf = calculateTotalCarpetArea(pl, get5PercentSubOccupancyList()).setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);
					BigDecimal providedNs = calculateTotalCarpetArea(pl, get3PercentSubOccupancyList()).setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);

					if (calculateCarpetAreaOfEWSandLIG(pl).compareTo(mandatoryCarpet) >= 0) {
						status = true;
					} else {
						status = true;
						isShelterFeeRequired = true;
					}

					Map<String, String> detailNsCf = new HashMap<>();
					detailNsCf.put(RULE_NO, "ODA Rule 64 (1,3)");
					detailNsCf.put(DESCRIPTION, "Neighbourhood shopping and community facilities");	
					detailNsCf.put(REQUIRED, mandateNsAndCf.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).toString());
					detailNsCf.put(PROVIDED, providedNsAndCf.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).toString());
					
					if (providedNsAndCf.compareTo(mandateNsAndCf) >= 0) {
						detailNsCf.put(STATUS, Result.Accepted.getResultVal());
					} else {
						detailNsCf.put(STATUS, Result.Not_Accepted.getResultVal());
					}
					scrutinyDetail.getDetail().add(detailNsCf);

					Map<String, String> detailsNs = new HashMap<>();
					detailsNs.put(RULE_NO, "ODA Rule 64 (1,3)");
					detailsNs.put(DESCRIPTION, "neighbourhood shopping facilities");
					detailsNs.put(REQUIRED, mandateNs.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).toString());
					detailsNs.put(PROVIDED, providedNs.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).toString());
					if (providedNs.compareTo(mandateNs) >= 0) {
						detailsNs.put(STATUS, Result.Accepted.getResultVal());
					} else {
						detailsNs.put(STATUS, Result.Not_Accepted.getResultVal());
					}
					scrutinyDetail.getDetail().add(detailsNs);

				}

				// FAR calculation with EWS
				Double farWithEWS = calculateFar(pl);
				BigDecimal farWithEWSBD = new BigDecimal(farWithEWS).setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);
				Double deltaFar = farWithEWS - pl.getFarDetails().getProvidedFar() ;
				Double maxAllowed = pl.getFarDetails().getPermissableFar() + deltaFar;
				BigDecimal maxAllowedBD = new BigDecimal(maxAllowed).setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS); 
				
				Map<String, String> detailsFar = new HashMap<>();
				detailsFar.put(RULE_NO, "ODA Rule 84, 74(1,2,3,4,5,9)");
				detailsFar.put(DESCRIPTION, "FAR including EWS and LIH provisions");
				detailsFar.put(REQUIRED, "Max " + maxAllowedBD.toString());
				detailsFar.put(PROVIDED, farWithEWSBD.toString());
				if (farWithEWS <= maxAllowed) {
					detailsFar.put(STATUS, Result.Accepted.getResultVal());
				} else {
					detailsFar.put(STATUS, Result.Not_Accepted.getResultVal());
				}
				scrutinyDetail.getDetail().add(detailsFar);

				Map<String, String> details = new HashMap<>();
				details.put(RULE_NO, "ODA Rule 75(4-vi)");
				details.put(DESCRIPTION, "Mandatory 10% of Carpet Area");
				details.put(REQUIRED,
						mandatoryCarpet.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).toString());

				// Changed built up area to carpet area in the report. Provided value will be carpet area of the 
				// EWS occupancy. Required value is total carpet area excluding the EWS occupancy.
				
				String provided = null;
				if (hasProvidevEWSWithin5Km)
					provided = PROVIDED_WITHIN_5KM;
//				else if (isShelterFeeRequired) {
//					provided = "Shelter Fee Applicable (Provided - " + pl.getTotalEWSAreaInPlot()
//							.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).toString() + " )";
//				} else {
//					provided = pl.getTotalEWSAreaInPlot().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).toString();
//				}
				else if (isShelterFeeRequired) {
					provided = "Shelter Fee Applicable (Provided - " + calculateCarpetAreaOfEWSandLIG(pl)
							.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).toString() + " )";
				} else {
					provided = calculateCarpetAreaOfEWSandLIG(pl).setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)
							.toString();
				}
				
				

//				details.put(PROVIDED, hasProvidevEWSWithin5Km ? PROVIDED_WITHIN_5KM : pl.getTotalEWSAreaInPlot().toString());
				details.put(PROVIDED, provided);
				details.put(STATUS, status ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());
				scrutinyDetail.getDetail().add(details);

				List<Map<String, String>> list = scrutinyDetail.getDetail();
				Collections.reverse(list);
				scrutinyDetail.setDetail(list);

				pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
			}
		}

	}

	private Double calculateFar(Plan pl) {
		BigDecimal surrenderRoadArea = pl.getTotalSurrenderRoadArea();
		BigDecimal plotArea = pl.getPlot() != null ? pl.getPlot().getArea().add(surrenderRoadArea) : BigDecimal.ZERO;
		BigDecimal totalBuaArea = pl.getVirtualBuilding().getTotalFloorArea().add(calculateBuiltupAreaOfEWSandLIG(pl));
		return totalBuaArea.divide(plotArea, 3, ROUNDMODE_MEASUREMENTS).doubleValue();
	}

	private BigDecimal calculateTotalDeductedBuildupArea(Plan pl, List<String> subOccupancies) {
		BigDecimal totalFloorArea = BigDecimal.ZERO;
		for (Block b : pl.getBlocks()) {
			for (Floor fl : b.getBuilding().getFloors()) {
				for (Occupancy type : fl.getOccupancies()) {
					if (subOccupancies.contains(type.getTypeHelper().getSubtype().getCode())) {
						totalFloorArea = totalFloorArea.add(type.getFloorArea());
					}
				}
			}
		}
		return totalFloorArea;
	}
	
	private BigDecimal calculateTotalCarpetArea(Plan pl, List<String> subOccupancies) {
		BigDecimal totalCarpetArea = BigDecimal.ZERO;
		for (Block b : pl.getBlocks()) {
			for (Floor fl : b.getBuilding().getFloors()) {
				for (Occupancy type : fl.getOccupancies()) {
					if (subOccupancies.contains(type.getTypeHelper().getSubtype().getCode())) {
						totalCarpetArea = totalCarpetArea.add(type.getCarpetArea());
					}
				}
			}
		}
		return totalCarpetArea;
	}
	
	
	private BigDecimal calculateCarpetAreaWithoutEWSandLIG(Plan pl) {

		BigDecimal totalCarpetAreaWithoutEWSandLIG = BigDecimal.ZERO;
		for (Block blk : pl.getBlocks()) {
			Building building = blk.getBuilding();

			for (Floor flr : building.getFloors()) {
				List<Occupancy> occupancies = flr.getOccupancies();
				for (Occupancy occupancy : occupancies) {

					if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
							&& !(DxfFileConstants.EWS.equals(occupancy.getTypeHelper().getSubtype().getCode())
									|| DxfFileConstants.LOW_INCOME_HOUSING
									.equals(occupancy.getTypeHelper().getSubtype().getCode()))) {

						totalCarpetAreaWithoutEWSandLIG = totalCarpetAreaWithoutEWSandLIG
								.add(occupancy.getCarpetArea() != null ? occupancy.getCarpetArea() : BigDecimal.ZERO);

					}

				}
			}
		}
		return totalCarpetAreaWithoutEWSandLIG;
	}
	
	private BigDecimal calculateCarpetAreaOfEWSandLIG(Plan pl) {

		BigDecimal totalCarpetAreaOfEWS = BigDecimal.ZERO;
		for (Block blk : pl.getBlocks()) {
			Building building = blk.getBuilding();

			for (Floor flr : building.getFloors()) {
				List<Occupancy> occupancies = flr.getOccupancies();
				for (Occupancy occupancy : occupancies) {

					if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
							&& (DxfFileConstants.EWS.equals(occupancy.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.LOW_INCOME_HOUSING
							.equals(occupancy.getTypeHelper().getSubtype().getCode()))) {

						totalCarpetAreaOfEWS = totalCarpetAreaOfEWS
								.add(occupancy.getCarpetArea() != null ? occupancy.getCarpetArea() : BigDecimal.ZERO);

					}

				}
			}
		}
		return totalCarpetAreaOfEWS;
	}
	
	private BigDecimal calculateBuiltupAreaOfEWSandLIG(Plan pl) {

		BigDecimal totalBuildupAreaOfEWSandLIG = BigDecimal.ZERO;
		for (Block blk : pl.getBlocks()) {
			Building building = blk.getBuilding();

			for (Floor flr : building.getFloors()) {
				List<Occupancy> occupancies = flr.getOccupancies();
				for (Occupancy occupancy : occupancies) {

					if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
							&& (DxfFileConstants.EWS.equals(occupancy.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.LOW_INCOME_HOUSING
							.equals(occupancy.getTypeHelper().getSubtype().getCode()))) {

						totalBuildupAreaOfEWSandLIG = totalBuildupAreaOfEWSandLIG
								.add(occupancy.getBuiltUpArea() != null ? occupancy.getBuiltUpArea() : BigDecimal.ZERO);

					}

				}
			}
		}
		return totalBuildupAreaOfEWSandLIG;
	}
	
	
	

	private BigDecimal calculateBuildUpArea(Plan pl, List<String> subOccupancies) {
		BigDecimal buildUpArea = BigDecimal.ZERO;
		for (Block b : pl.getBlocks()) {
			for (Floor fl : b.getBuilding().getFloors()) {
				for (Occupancy type : fl.getOccupancies()) {
					if (subOccupancies.contains(type.getTypeHelper().getSubtype().getCode())) {
						buildUpArea = buildUpArea.add(type.getBuiltUpArea());
					}
				}
			}
		}
		return buildUpArea;
	}

	public List<String> getCommercialSubOccupancies() {
		return Arrays.asList(DxfFileConstants.HOTEL, DxfFileConstants.FIVE_STAR_HOTEL, DxfFileConstants.MOTELS, DxfFileConstants.BANK,
				DxfFileConstants.RESORTS, DxfFileConstants.LAGOONS_AND_LAGOON_RESORT,
				DxfFileConstants.AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS,
				DxfFileConstants.FINANCIAL_SERVICES_AND_STOCK_EXCHANGES, DxfFileConstants.COLD_STORAGE_AND_ICE_FACTORY,
				DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX,
				DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING, DxfFileConstants.PROFESSIONAL_OFFICES,
				DxfFileConstants.DEPARTMENTAL_STORE, DxfFileConstants.GAS_GODOWN, DxfFileConstants.GODOWNS,
				DxfFileConstants.GOOD_STORAGE, DxfFileConstants.GUEST_HOUSES, DxfFileConstants.HOLIDAY_RESORT,
				DxfFileConstants.BOARDING_AND_LODGING_HOUSES, DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION,
				DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION, DxfFileConstants.CNG_MOTHER_STATION,
				DxfFileConstants.RESTAURANT, DxfFileConstants.LOCAL_RETAIL_SHOPPING, DxfFileConstants.SHOPPING_CENTER,
				DxfFileConstants.SHOPPING_MALL, DxfFileConstants.SHOWROOM,
				DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE, DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE,
				DxfFileConstants.STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT, DxfFileConstants.SUPERMARKETS,
				DxfFileConstants.WARE_HOUSE, DxfFileConstants.WHOLESALE_MARKET, DxfFileConstants.MEDIA_CENTRES,
				DxfFileConstants.FOOD_COURTS, DxfFileConstants.WEIGH_BRIDGES, DxfFileConstants.MERCENTILE,
				DxfFileConstants.CLUB,DxfFileConstants.COMMUNITY_HALL,DxfFileConstants.CONFERNCE_HALL,
				DxfFileConstants.CONVENTION_HALL,DxfFileConstants.CULTURAL_COMPLEX,DxfFileConstants.GYMNASIA,
				DxfFileConstants.PUBLIC_LIBRARIES,DxfFileConstants.RECREATION_BLDG,DxfFileConstants.SPORTS_COMPLEX,
				DxfFileConstants.HEALTH_CENTRE,DxfFileConstants.LAB);

	}
	
	public List<String> getWorkCumResidentialOccupancies() {
		return Arrays.asList(DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING,
				DxfFileConstants.ROW_HOUSING,
				DxfFileConstants.SEMI_DETACHED,
				DxfFileConstants.PROFESSIONAL_OFFICES);

	}

	private List<String> get5PercentSubOccupancyList() {
		return Arrays.asList(DxfFileConstants.SERVICES_FOR_HOUSEHOLDS,
				DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING, DxfFileConstants.PROFESSIONAL_OFFICES,
				DxfFileConstants.DEPARTMENTAL_STORE, DxfFileConstants.RESTAURANT,
				DxfFileConstants.LOCAL_RETAIL_SHOPPING, DxfFileConstants.MERCENTILE, DxfFileConstants.BANQUET_HALL,
				DxfFileConstants.COMMUNITY_HALL, DxfFileConstants.GYMNASIA, DxfFileConstants.CLINIC);
	}

	private List<String> get3PercentSubOccupancyList() {
		return Arrays.asList(DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING,
				DxfFileConstants.LOCAL_RETAIL_SHOPPING);
	}

	public List<String> getOtherAllowedSubOccupanciesInOcCommercial() {
		return Arrays.asList(DxfFileConstants.APARTMENT_BUILDING, DxfFileConstants.STUDIO_APARTMENTS,
				DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX,
				DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING, DxfFileConstants.PROFESSIONAL_OFFICES,
				DxfFileConstants.RESTAURANT, DxfFileConstants.LOCAL_RETAIL_SHOPPING, DxfFileConstants.SHOPPING_CENTER,
				DxfFileConstants.FOOD_COURTS, DxfFileConstants.MERCENTILE, DxfFileConstants.BANQUET_HALL,
				DxfFileConstants.CLUB, DxfFileConstants.CONFERNCE_HALL, DxfFileConstants.GYMNASIA,
				DxfFileConstants.HEALTH_CENTRE);
	}

	public List<String> getCommercialOfficeAndRetailServiceList() {
		return Arrays.asList(DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX,
				DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING, DxfFileConstants.PROFESSIONAL_OFFICES,
				DxfFileConstants.LOCAL_RETAIL_SHOPPING, DxfFileConstants.SHOPPING_CENTER, DxfFileConstants.MERCENTILE);
	}

	private List<String> getSubOccupanciesForEWS() {
		return Arrays.asList(DxfFileConstants.APARTMENT_BUILDING, DxfFileConstants.HOUSING_PROJECT);

	}
	
	public List<String> getShopsList() {
		return Arrays.asList(
				DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING,
				DxfFileConstants.LOCAL_RETAIL_SHOPPING, 
				DxfFileConstants.DEPARTMENTAL_STORE, 
				DxfFileConstants.RESTAURANT);
	}
	
	public List<String> getOtherAllowedSuboccupanciesWithShops() {
		return Arrays.asList(
				DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING,
				DxfFileConstants.SEMI_DETACHED, 
				DxfFileConstants.ROW_HOUSING);
	}
}
