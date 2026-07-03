/*
 * eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 * accountability and the service delivery of the government  organizations.
 *
 *  Copyright (C) <2019>  eGovernments Foundation
 *
 *  The updated version of eGov suite of products as by eGovernments Foundation
 *  is available at http://www.egovernments.org
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see http://www.gnu.org/licenses/ or
 *  http://www.gnu.org/licenses/gpl.html .
 *
 *  In addition to the terms of the GPL license to be adhered to in using this
 *  program, the following additional terms are to be complied with:
 *
 *      1) All versions of this program, verbatim or modified must carry this
 *         Legal Notice.
 *      Further, all user interfaces, including but not limited to citizen facing interfaces,
 *         Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any
 *         derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *      For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *      For any further queries on attribution, including queries on brand guidelines,
 *         please contact contact@egovernments.org
 *
 *      2) Any misrepresentation of the origin of the material is prohibited. It
 *         is required that all modified versions of this material be marked in
 *         reasonable ways as different from the original version.
 *
 *      3) This license does not grant any rights to any user of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.edcr.feature;

import static org.egov.edcr.constants.DxfFileConstants.A;
import static org.egov.edcr.constants.DxfFileConstants.F;
import static org.egov.edcr.constants.DxfFileConstants.F_H;
import static org.egov.edcr.constants.DxfFileConstants.G;
import static org.egov.edcr.constants.DxfFileConstants.PARKING_SLOT;
import static org.egov.edcr.utility.DcrConstants.SQMTRS;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.FloorUnit;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyPercentage;
import org.egov.common.entity.edcr.OccupancyType;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.OdishaParkingHelper;
import org.egov.common.entity.edcr.ParkingDetails;
import org.egov.common.entity.edcr.ParkingHelper;
import org.egov.common.entity.edcr.ParkingUnit;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.OdishaUlbs;
import org.egov.edcr.od.OdishaUtill;
import org.egov.edcr.utility.DcrConstants;
import org.egov.edcr.utility.Util;
import org.springframework.stereotype.Service;

@Service
public class Parking extends FeatureProcess {

	private static final Logger LOGGER = Logger.getLogger(Parking.class);

	private static final String OUT_OF = "Out of ";
	private static final String SLOT_HAVING_GT_4_PTS = " number of polygon not having only 4 points.";
	private static final String LOADING_UNLOADING_DESC = "Minimum required Loading/Unloading area";
	private static final String MINIMUM_AREA_OF_EACH_DA_PARKING = " Minimum width of Each Special parking";
	private static final String SP_PARKING_SLOT_AREA = "Special Parking Area";
	private static final String NO_VIOLATION_OF_AREA = "No violation of area in ";
	private static final String MIN_AREA_EACH_CAR_PARKING = " Minimum Area of Each ECS parking";
	private static final String PARKING_VIOLATED_MINIMUM_AREA = " parking violated minimum area.";
	private static final String PARKING = " parking ";
	private static final String NUMBERS = " Numbers ";
	private static final String MECHANICAL_PARKING = "Mechanical parking";
	private static final String MAX_ALLOWED_MECH_PARK = "Maximum allowed mechanical parking";
	private static final String TWO_WHEELER_PARK_AREA = "Two Wheeler Parking Area";
	private static final String LOADING_UNLOADING_AREA = "Loading Unloading Area";
	private static final String SP_PARKING = "Special parking";
	private static final String SUB_RULE_34_1_DESCRIPTION = "Parking Slots Area";
	private static final String PARKING_MIN_AREA = "5 M x 2 M";
	private static final double PARKING_SLOT_WIDTH = 2;
	private static final double PARKING_SLOT_HEIGHT = 5;
	private static final double SP_PARK_SLOT_MIN_SIDE = 3.6;
	private static final String DA_PARKING_MIN_AREA = " 3.60 M ";
	public static final String NO_OF_UNITS = "No of apartment units";
	private static final double TWO_WHEEL_PARKING_AREA_WIDTH = 1.5;
	private static final double TWO_WHEEL_PARKING_AREA_HEIGHT = 2.0;
	private static final double MECH_PARKING_WIDTH = 2.7;
	private static final double MECH_PARKING_HEIGHT = 5.5;

	private static final double OPEN_ECS = 23;
	private static final double COVER_ECS = 28;
	private static final double BSMNT_ECS = 32;
	private static final double PARK_A = 0.25;
	private static final double PARK_F = 0.30;
	private static final double PARK_VISITOR = 0.15;
	private static final String SUB_RULE_40 = "ODA Rule 37 (1-vi,8), 64(1-i,2)";
	private static final String SUB_RULE_40_2 = "ODA Rule 37 (1,12,15), 74 (7-i)";
	private static final String SUB_RULE_40_2_DESCRIPTION = "Parking space";
	private static final String SUB_RULE_40_10 = "ODA Rule 37 (12)";
	private static final String SUB_RULE_40_10_DESCRIPTION = "Visitor’s Parking";
	public static final String OPEN_PARKING_DIM_DESC = "Open parking ECS dimension ";
	public static final String COVER_PARKING_DIM_DESC = "Cover parking ECS dimension ";
	public static final String BSMNT_PARKING_DIM_DESC = "Basement parking ECS dimension ";
	public static final String VISITOR_PARKING = "Visitor parking";
	public static final String SPECIAL_PARKING_DIM_DESC = "Special parking ECS dimension ";
	public static final String TWO_WHEELER_DIM_DESC = "Two wheeler parking dimension ";
	public static final String MECH_PARKING_DESC = "Mechanical parking dimension ";
	public static final String MECH_PARKING_DIM_DESC = "All Mechanical parking polylines should have dimension 2.7*5.5 m²";
	public static final String MECH_PARKING_DIM_DESC_NA = " mechanical parking polyines does not have dimensions 2.7*5.5 m²";

	private static final String PARKING_VIOLATED_DIM = " parking violated dimension.";
	private static final String PARKING_AREA_DIM = "1.5 M x 2 M";

	private static final String EWS_OR_LIG_PARKING_DESC = "EWS/LIG Parking";
	private static final String MIG_PARKING_DESC = "MIG Parking";
	private static final String VISITOR_PARKING_DESC = "Visitor Parking";
	private static final String STILT_PARKING_DESC = "Stilt Parking";
	private static final String BASEMENT_PARKING_DESC = "Basement Parking";
	private static final String OPEN_PARKING_DESC = "Open Parking";
	private static final String DA_PARKING_AREA_DESC = "Special Parking Area";
	private static final String DA_PARKING_WIDTH_DESC = "Special Parking width";
	private static final String DA_PARKING_COUNT_DESC = "Special Parking count";
	private static final String DA_PARKING_MAX_DISTANCE_DESC = "Maximum distance from building entrance";
	private static final String TWO_WHEELER_PARKING_DESC = "Two Wheeler Parking";
	private static final String BICYCLE_PARKING_DESC = "Bicycle Parking";
	private static final String ROOF_TOP_PARKING_DESC = "Roof top parking";
	private static final String EV_CHARGING_POINT_DESC = "Ev charging point";
	private static final String STACK_PARKING_DESC = "Stack parking";
	private static final String STACK_PARKING_COUNT_DESC = "Stack Parking count";
	private static final String STAFF_PARKING_DESC = "Staff Parking";
	private static final String TOTAL_PARKING_DESC = "Total Parking";
	private static final String TOTAL_PARKING_UNITS_DESC = "Total Parking Units";
	private static final String OPEN_PARKING_UNITS_DESC = "Open Parking Units";
	private static final String BASEMENT_PARKING_UNITS_DESC = "Basement Parking Units";
	private static final String STILT_PARKING_UNITS_DESC = "Stilt Parking Units";
	private static final String ROOFTOP_PARKING_UNITS_DESC = "Rooftop Parking Units";

	private static final int COLOR_LAYER_SPECIAL_PARKING_EWS_OR_LIG_PARKING = 1;
	private static final int COLOR_LAYER_SPECIAL_PARKING_MIG_PARKING = 2;
	private static final int COLOR_LAYER_SPECIAL_PARKING_STILT_PARKING = 3;
	public static final int COLOR_LAYER_SPECIAL_PARKING_ROOF_TOP_PARKING = 4;
	private static final int COLOR_LAYER_SPECIAL_PARKING_STAFF_PARKING = 5;
	
	private static final int COLOR_LAYER_OPEN_PARKING_UNITS = 1;
	private static final int COLOR_LAYER_BASEMENT_PARKING_UNITS = 2;
	private static final int COLOR_LAYER_STILT_PARKING_UNITS = 3;
	public static final int COLOR_LAYER_ROOFTOP_PARKING_UNITS = 4;

	private static final int COLOR_LAYER_TWO_WHEELER_PARKING_TWO_WHEELER_PARKING = 1;
	private static final int COLOR_LAYER_TWO_WHEELER_PARKING_BICYCLE_PARKING = 2;

	public OdishaParkingHelper prepareParkingData(Plan pl) {
		OdishaParkingHelper helper = new OdishaParkingHelper();

		ParkingDetails details = pl.getParkingDetails();
		BigDecimal totalParking = BigDecimal.ZERO;
		if (details.getSpecial() != null && !details.getSpecial().isEmpty()) {
			for (Measurement measurement : details.getSpecial()) {
				switch (measurement.getColorCode()) {
				case COLOR_LAYER_SPECIAL_PARKING_EWS_OR_LIG_PARKING:
					helper.eWSOrLigParkingProvided = helper.eWSOrLigParkingProvided.add(measurement.getArea())
							.setScale(2, BigDecimal.ROUND_HALF_UP);
					// totalParking=totalParking.add(measurement.getArea());
					break;
				case COLOR_LAYER_SPECIAL_PARKING_MIG_PARKING:
					helper.mIGParkingProvided = helper.mIGParkingProvided.add(measurement.getArea()).setScale(2,
							BigDecimal.ROUND_HALF_UP);
//					if(DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(pl.getVirtualBuilding().getMostRestrictiveFarHelper().getSubtype().getCode()))
//					 totalParking=totalParking.add(measurement.getArea());
					break;
				case COLOR_LAYER_SPECIAL_PARKING_STILT_PARKING:
					helper.stiltParkingProvided = helper.stiltParkingProvided.add(measurement.getArea()).setScale(2,
							BigDecimal.ROUND_HALF_UP);
					totalParking = totalParking.add(measurement.getArea());
					break;
				case COLOR_LAYER_SPECIAL_PARKING_ROOF_TOP_PARKING:
					helper.roofTopParkingProvided = helper.roofTopParkingProvided.add(measurement.getArea()).setScale(2,
							BigDecimal.ROUND_HALF_UP);
					totalParking = totalParking.add(measurement.getArea());
					break;
				case COLOR_LAYER_SPECIAL_PARKING_STAFF_PARKING:
					helper.staffParkingProvided = helper.staffParkingProvided.add(measurement.getArea()).setScale(2,
							BigDecimal.ROUND_HALF_UP);
					// totalParking=totalParking.add(measurement.getArea());
					break;
				}
			}
		}

		if (details.getTwoWheelers() != null && !details.getTwoWheelers().isEmpty()) {
			for (Measurement measurement : details.getTwoWheelers()) {
				switch (measurement.getColorCode()) {
				case COLOR_LAYER_TWO_WHEELER_PARKING_TWO_WHEELER_PARKING:
					helper.twoWheelerParkingProvided = helper.twoWheelerParkingProvided.add(measurement.getArea())
							.setScale(2, BigDecimal.ROUND_HALF_UP);
					// totalParking=totalParking.add(measurement.getArea()).setScale(2,
					// BigDecimal.ROUND_HALF_UP);
					break;
				case COLOR_LAYER_TWO_WHEELER_PARKING_BICYCLE_PARKING:
					helper.bicycleParkingProvided = helper.bicycleParkingProvided.add(measurement.getArea()).setScale(2,
							BigDecimal.ROUND_HALF_UP);
					// totalParking=totalParking.add(measurement.getArea());
					break;
				}
			}
		}

		try {
			helper.visitorParkingProvided = pl.getParkingDetails().getVisitors().stream().map(Measurement::getArea)
					.reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, BigDecimal.ROUND_HALF_UP);
			// totalParking=totalParking.add(helper.visitorParkingProvided);
		} catch (Exception e) {
			// TODO: handle exception
		}

		try {
			helper.openParkingProvided = pl.getParkingDetails().getOpenCars().stream().map(Measurement::getArea)
					.reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, BigDecimal.ROUND_HALF_UP);
			totalParking = totalParking.add(helper.openParkingProvided);
		} catch (Exception e) {
			// TODO: handle exception
		}

		BigDecimal coverParkingArea = BigDecimal.ZERO;
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				try {
					coverParkingArea = coverParkingArea.add(floor.getParking().getCoverCars().stream()
							.map(Measurement::getArea).reduce(BigDecimal.ZERO, BigDecimal::add));
					helper.basementParkingProvided = helper.basementParkingProvided
							.add(floor.getParking().getBasementCars().stream().map(Measurement::getArea)
									.reduce(BigDecimal.ZERO, BigDecimal::add))
							.setScale(2, BigDecimal.ROUND_HALF_UP);
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		}
		totalParking = totalParking.add(helper.basementParkingProvided);

		validateDAPArking(pl, pl.getParkingDetails());

		try {
			helper.daPARKINGProvided = pl.getParkingDetails().getDisabledPersons().stream().map(Measurement::getArea)
					.reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, BigDecimal.ROUND_HALF_UP);
			// totalParking=totalParking.add(helper.daPARKINGProvided);
		} catch (Exception e) {
			// TODO: handle exception
		}

		try {
			helper.daPARKINGWidthProvided = pl.getParkingDetails().getDisabledPersons().stream()
					.map(Measurement::getWidth).reduce(BigDecimal::min).get().setScale(2, BigDecimal.ROUND_HALF_UP);
		} catch (Exception e) {
			// TODO: handle exception
		}

		try {
			helper.daPARKINGCountProvided = pl.getParkingDetails().getDisabledPersons().size();

			totalParking = totalParking.setScale(2, BigDecimal.ROUND_HALF_UP);
			helper.totalParkingProvided = totalParking;

			helper.distFromDAToMainEntranceProvided = details.getDistFromDAToMainEntrance();
			helper.offSiteParkingprovisionsProvided = pl.getPlanInformation().getOffSiteParkingprovisionsArea();
		} catch (Exception e) {
			// TODO: handle exception
		}

		// EV charging points
		if(pl.getEvCharging()!=null && pl.getEvCharging().getMeasurements() != null && pl.getEvCharging().getMeasurements().size()>0) {
			BigDecimal evChargingPoints = pl.getEvCharging().getMeasurements().stream().map(Measurement::getArea).reduce(BigDecimal::add).get().setScale(2, BigDecimal.ROUND_HALF_UP);
			helper.evChargingPointProvided = evChargingPoints;
		}
			
		// STACK_PARKING
		if(pl.getParkingDetails()!=null && pl.getParkingDetails().getMechParking()!=null && pl.getParkingDetails().getMechParking().size()>0) {
			BigDecimal totalStackParkingArea = pl.getParkingDetails().getMechParking().stream().map(Measurement::getArea).reduce(BigDecimal::add).get();
			if(totalStackParkingArea!=null && totalStackParkingArea.compareTo(BigDecimal.ZERO)>0) {
				helper.stackParkingProvided = totalStackParkingArea.multiply(new BigDecimal("2")).setScale(2,BigDecimal.ROUND_HALF_UP);
				helper.stackParkingCountProvided = pl.getParkingDetails().getMechParking().size();
				helper.totalParkingProvided = helper.totalParkingProvided.add(helper.stackParkingProvided);
			}
		}
		
		//Parking Units
		if (pl.getParkingDetails()!=null 
				&& pl.getParkingDetails().getParkingUnits()!=null
				&& pl.getParkingDetails().getParkingUnits().size()>0) {
			helper.totalParkingUnitsProvided = pl.getParkingDetails().getParkingUnits().size();
		}
		
	
		if (details.getParkingUnits()!=null && details.getParkingUnits().size()>0) {
			
			for (ParkingUnit pUnit : details.getParkingUnits()) {
				switch (pUnit.getColorCode()) {
					case COLOR_LAYER_OPEN_PARKING_UNITS:
						helper.openParkingUnits += 1;
						break;
					case COLOR_LAYER_BASEMENT_PARKING_UNITS:
						helper.basementParkingUnits += 1;
						break;
					case COLOR_LAYER_STILT_PARKING_UNITS:
						helper.stiltParkingUnits += 1;
						break;
					case COLOR_LAYER_ROOFTOP_PARKING_UNITS:
						helper.rooftopParkingUnits += 1;
						break;	
				}
		}
		
		}
		// update planInformation of permit order
		pl.getPlanInformation().setTotalParking(helper.totalParkingProvided);
		return helper;
	}

	private void validateDAPArking(Plan pl, ParkingDetails parkingDetails) {
		if (parkingDetails.getDistFromDAToMainEntrance().compareTo(new BigDecimal("30")) > 0)
			pl.addError("distFromDAToMainEntranceProvided",
					"Distance of DA Parking from building entrance should be maximum 30 meters.");

		for (Measurement measurement : parkingDetails.getDisabledPersons()) {
			if (measurement.getArea().compareTo(new BigDecimal("25")) < 0)
				pl.addError("DaParkingArea", "Area of DA parking Space should be Minimum 25 sqm each.");
			if (measurement.getWidth().compareTo(new BigDecimal("3.6")) < 0)
				pl.addError("DaParkingArea", "Width of DA parking Space should be Minimum 3.6 meters each.");

		}
	}

	private void setReport(String ruleNo, String description, String required, String provided, Result result,
			ScrutinyDetail scrutinyDetail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, description);
		details.put(REQUIRED, required);
		details.put(PROVIDED, provided);
		details.put(STATUS, result.getResultVal());
		scrutinyDetail.getDetail().add(details);
	}

	@Override
	public Plan validate(Plan pl) {
		validateParkingUnits(pl);
		// validateDimensions(pl);
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		validate(pl);
		OdishaParkingHelper helper = prepareParkingData(pl);

		updateRequiredParkingDetails(pl, helper);
		
		if (helper.totalParkingRequired.compareTo(BigDecimal.ZERO)>0 && pl.getParkingDetails().getParkingUnits().isEmpty()) {
			pl.addError("Parking_unit_mandatory", "Parking unit layer is not present in the drawing.");
		}

		if (helper.offSiteParkingprovisionsProvided.compareTo(BigDecimal.ZERO) > 0) {
			offSiteParking(pl, helper);
		} else {
			genralParking(pl, helper);
		}
		validateDAParking(pl, helper);
		
		parkingUnitsInReport(pl, helper);
		
		

		// For Haryana demo
		processHaryanaDemo(pl);
		return pl;
	}

	private void updateRequiredParkingDetails(Plan pl, OdishaParkingHelper helper) {
		String ocType = "";
		String subType = "";
		OccupancyTypeHelper typeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		OdishaUlbs ulb = OdishaUlbs.getUlb(pl.getThirdPartyUserTenantld());

		if (typeHelper != null && typeHelper.getType() != null)
			ocType = typeHelper.getType().getCode();

		if (typeHelper != null && typeHelper.getSubtype() != null)
			subType = typeHelper.getSubtype().getCode();

		BigDecimal plotArea = pl.getPlot().getArea();
		if(DxfFileConstants.OC_MIXED_USE.equals(ocType) || (pl.getPlanInformation().getOccupancyPercentages() != null && pl.getPlanInformation().getOccupancyPercentages().size()>1)) {
			updateRequiredParkingDetailsForMixedUse(pl, helper);
		}else if (DxfFileConstants.OC_RESIDENTIAL.equals(ocType)) {
			// Total required
			long totalNumberOfDu = pl.getPlanInformation().getTotalNoOfDwellingUnits();
			BigDecimal totalStiltArea = totalStiltArea(pl);
			BigDecimal totalStiltParkingArea = helper.stiltParkingProvided;

			if (plotArea.compareTo(new BigDecimal("750")) > 0 || (totalStiltArea.compareTo(BigDecimal.ZERO) <= 0
					&& totalStiltArea.compareTo(totalStiltParkingArea) != 0) || totalNumberOfDu <= 8) {
				if (totalNumberOfDu > 4 && totalNumberOfDu <= 8) {
					helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
							.multiply(new BigDecimal("0.20")).setScale(2, BigDecimal.ROUND_HALF_UP);
				} else if (totalNumberOfDu > 8) {
					if (DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING.equals(subType)
							|| DxfFileConstants.SEMI_DETACHED.equals(subType)
							|| DxfFileConstants.ROW_HOUSING.equals(subType)) {
						// NA
					} else if (DxfFileConstants.APARTMENT_BUILDING.equals(subType)
							|| DxfFileConstants.HOUSING_PROJECT.equals(subType)
							|| DxfFileConstants.STUDIO_APARTMENTS.equals(subType)
							|| DxfFileConstants.DHARMASALA.equals(subType) || DxfFileConstants.DORMITORY.equals(subType)
							|| DxfFileConstants.HOSTEL.equals(subType) || DxfFileConstants.SHELTER_HOUSE.equals(subType)
							|| DxfFileConstants.STAFF_QAURTER.equals(subType)
							|| DxfFileConstants.MIXED_USE.equals(subType)) {
						helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
								.multiply(new BigDecimal("0.30")).setScale(2, BigDecimal.ROUND_HALF_UP);

					} else if (DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(subType)) {
						helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
								.multiply(new BigDecimal("0.25")).setScale(2, BigDecimal.ROUND_HALF_UP);
					}
				}
			}
			// Roof top parking
			if (pl.getVirtualBuilding().getTotalCoverageArea().compareTo(new BigDecimal("2000")) < 0
					&& helper.roofTopParkingProvided.compareTo(BigDecimal.ZERO) > 0) {
				pl.addError("Roof top parking",
						"Roof top parking not allowed in case coverage area is less then 2000 sqm");
			}

			// EWS/LIG
			if (pl.getPlanInformation().getIsProjectOnlyLigOrEws() == null 
					|| !pl.getPlanInformation().getIsProjectOnlyLigOrEws().equalsIgnoreCase("YES")) {
				
				if (DxfFileConstants.APARTMENT_BUILDING.equals(subType)
						|| DxfFileConstants.HOUSING_PROJECT.equals(subType)
						|| DxfFileConstants.STUDIO_APARTMENTS.equals(subType) || DxfFileConstants.EWS.equals(subType)
						|| DxfFileConstants.LOW_INCOME_HOUSING.equals(subType)) {
					BigDecimal totalLigAndEWS = getTotalAreaOfLIGAndEWS(pl);
					if (totalLigAndEWS.compareTo(BigDecimal.ZERO) > 0) {
						helper.eWSOrLigParkingRequired = totalLigAndEWS.multiply(new BigDecimal("0.10")).setScale(2,
								BigDecimal.ROUND_HALF_UP);
					}
				}
			} else {

				if (DxfFileConstants.EWS.equals(subType)) {

					BigDecimal totalEWSflr = getTotalAreaOfEWSFlrArea(pl);
					if (totalEWSflr.compareTo(BigDecimal.ZERO) > 0) {
						helper.totalParkingRequired = totalEWSflr.multiply(new BigDecimal("0.10")).setScale(2,
								BigDecimal.ROUND_HALF_UP);
					}
				} else if (DxfFileConstants.LOW_INCOME_HOUSING.equals(subType)) {

					BigDecimal totalLigFlr = getTotalAreaOfLIGFlrArea(pl);
					if (totalLigFlr.compareTo(BigDecimal.ZERO) > 0) {
						helper.totalParkingRequired = totalLigFlr.multiply(new BigDecimal("0.10")).setScale(2,
								BigDecimal.ROUND_HALF_UP);
					}

				}

			}

			// MIG
			if (DxfFileConstants.APARTMENT_BUILDING.equals(subType) || DxfFileConstants.HOUSING_PROJECT.equals(subType)
					|| DxfFileConstants.STUDIO_APARTMENTS.equals(subType)) {
//				if(ulb.isSparitFlag()) {
//					System.out.println("Sparit  implementation2:");
//					helper.mIGParkingRequired=BigDecimal.ZERO; 
//				}
//				else {	
				BigDecimal totalMigAndEWS = getTotalAreaOfMIG1AndMIG2(pl);
				if (totalMigAndEWS.compareTo(BigDecimal.ZERO) > 0) {
					helper.mIGParkingRequired = totalMigAndEWS.multiply(new BigDecimal("0.25")).setScale(2,
							BigDecimal.ROUND_HALF_UP);
				}
			} else if (DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(subType)) {

				helper.mIGParkingRequired = pl.getVirtualBuilding().getTotalFloorArea().multiply(new BigDecimal("0.25"))
						.setScale(2, BigDecimal.ROUND_HALF_UP);
			}

			// DA parking
			if (plotArea.compareTo(new BigDecimal("2000")) > 0)
				if (totalNumberOfDu > 4) {
					helper.daPARKINGCountRequired = 2;
					helper.distFromDAToMainEntranceRequired = new BigDecimal("30");
				}

			// visitor
			if (DxfFileConstants.APARTMENT_BUILDING.equals(subType) || DxfFileConstants.HOUSING_PROJECT.equals(subType)
					|| DxfFileConstants.STUDIO_APARTMENTS.equals(subType)) {
				helper.visitorParkingRequired = helper.totalParkingRequired.multiply(new BigDecimal("0.10")).setScale(2,
						BigDecimal.ROUND_HALF_UP);

			}

		} // RS
//		else if (DxfFileConstants.OC_MIXED_USE.equals(ocType)) {
//			updateRequiredParkingDetailsForMixedUse(pl, helper);
//		} 
		else { // oTher then resid
			if (DxfFileConstants.COLD_STORAGE_AND_ICE_FACTORY.equals(subType)
					|| DxfFileConstants.GAS_GODOWN.equals(subType) || DxfFileConstants.GODOWNS.equals(subType)
					|| DxfFileConstants.GOOD_STORAGE.equals(subType)
					|| DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE.equals(subType)
					|| DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE.equals(subType)
					|| DxfFileConstants.STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT.equals(subType)
					|| DxfFileConstants.WARE_HOUSE.equals(subType)) {
				helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
						.multiply(new BigDecimal("0.20")).setScale(2, BigDecimal.ROUND_HALF_UP);
				if (plotArea.compareTo(new BigDecimal("2000")) > 0) {
					helper.daPARKINGCountRequired = 2;
					helper.distFromDAToMainEntranceRequired = new BigDecimal("30");
				}
				helper.staffParkingRequired = helper.totalParkingRequired.divide(new BigDecimal("3"), 2,
						BigDecimal.ROUND_HALF_UP);
				helper.twoWheelerParkingRequired = helper.totalParkingRequired.multiply(new BigDecimal("0.15"))
						.setScale(2, BigDecimal.ROUND_HALF_UP);
				helper.bicycleParkingRequired = helper.totalParkingRequired.multiply(new BigDecimal("0.02")).setScale(2,
						BigDecimal.ROUND_HALF_UP);
			} else if (DxfFileConstants.OC_PUBLIC_UTILITY.equals(ocType) || DxfFileConstants.OC_EDUCATION.equals(ocType)
					|| DxfFileConstants.GUEST_HOUSES.equals(subType)
					|| DxfFileConstants.BOARDING_AND_LODGING_HOUSES.equals(subType)
					|| DxfFileConstants.ORPHANAGE.equals(subType) || DxfFileConstants.OLD_AGE_HOME.equals(subType)
					|| DxfFileConstants.SCIENCE_CENTRE_OR_MUSEUM.equals(subType)
					|| DxfFileConstants.CONFERNCE_HALL.equals(subType) || DxfFileConstants.MUSUEM.equals(subType)
					|| DxfFileConstants.PLACE_OF_WORKSHIP.equals(subType)
					|| DxfFileConstants.PUBLIC_LIBRARIES.equals(subType)
					|| DxfFileConstants.RECREATION_BLDG.equals(subType)
					|| DxfFileConstants.ADMINISTRATIVE_BUILDINGS.equals(subType)
					|| DxfFileConstants.GOVERNMENT_OFFICES.equals(subType)
					|| DxfFileConstants.LOCAL_AND_SEMI_GOVERNMENT_OFFICES.equals(subType)
					|| DxfFileConstants.POLICE_OR_ARMY_OR_BARRACK.equals(subType)
					|| DxfFileConstants.RELIGIOUS_BUILDING.equals(subType)
					|| DxfFileConstants.SOCIAL_AND_WELFARE_CENTRES.equals(subType)
					|| DxfFileConstants.CLINIC.equals(subType) || DxfFileConstants.DISPENSARY.equals(subType)
					|| DxfFileConstants.YOGA_CENTER.equals(subType)
					|| DxfFileConstants.DIAGNOSTIC_CENTRE.equals(subType)
					|| DxfFileConstants.GOVT_SEMI_GOVT_HOSPITAL.equals(subType)
					|| DxfFileConstants.REGISTERED_TRUST.equals(subType)
					|| DxfFileConstants.HEALTH_CENTRE.equals(subType) || DxfFileConstants.HOSPITAL.equals(subType)
					|| DxfFileConstants.LAB.equals(subType) || DxfFileConstants.MATERNITY_HOME.equals(subType)
					|| DxfFileConstants.MEDICAL_BUILDING.equals(subType)
					|| DxfFileConstants.NURSING_HOME.equals(subType) || DxfFileConstants.POLYCLINIC.equals(subType)
					|| DxfFileConstants.REHABILITAION_CENTER.equals(subType)
					|| DxfFileConstants.VETERINARY_HOSPITAL_FOR_PET_ANIMALS_AND_BIRDS.equals(subType)
					|| DxfFileConstants.RESEARCH_AND_TRAINING_CENTER.equals(subType)
					|| DxfFileConstants.RESEARCH_AND_TRAINING_INSTITUTE.equals(subType)
					|| DxfFileConstants.POLICE_STATION.equals(subType) || DxfFileConstants.FIRE_STATION.equals(subType)
					|| DxfFileConstants.JAIL_OR_PRISON.equals(subType) || DxfFileConstants.POST_OFFICE.equals(subType)
					|| DxfFileConstants.INDUSTRIAL_BUILDINGS_FACTORIES_WORKSHOPS_ETC.equals(subType)
					|| DxfFileConstants.NON_POLLUTING_INDUSTRIAL.equals(subType)
					|| DxfFileConstants.SEZ_INDUSTRIAL.equals(subType)
					|| DxfFileConstants.LOADING_OR_UNLOADING_SPACES.equals(subType)
					|| DxfFileConstants.FLATTED_FACTORY.equals(subType)
					|| DxfFileConstants.SMALL_FACTORIES_AND_ETC_FALLS_IN_INDUSTRIAL.equals(subType)) {
				if (ulb.isSparitFlag()) {
					//System.out.println("Sparit  implementation2:");
					helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
							.multiply(new BigDecimal("0.25")).setScale(2, BigDecimal.ROUND_HALF_UP);

				} else {
					helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
							.multiply(new BigDecimal("0.30")).setScale(2, BigDecimal.ROUND_HALF_UP);
				}
				if (plotArea.compareTo(new BigDecimal("2000")) > 0) {
					helper.daPARKINGCountRequired = 2;
					helper.distFromDAToMainEntranceRequired = new BigDecimal("30");
				}
				helper.staffParkingRequired = helper.totalParkingRequired.divide(new BigDecimal("3"), 2,
						BigDecimal.ROUND_HALF_UP);
				helper.twoWheelerParkingRequired = helper.totalParkingRequired.multiply(new BigDecimal("0.15"))
						.setScale(2, BigDecimal.ROUND_HALF_UP);
				helper.bicycleParkingRequired = helper.totalParkingRequired.multiply(new BigDecimal("0.02")).setScale(2,
						BigDecimal.ROUND_HALF_UP);
			} else if (DxfFileConstants.HOTEL.equals(subType) || DxfFileConstants.FIVE_STAR_HOTEL.equals(subType)
					|| DxfFileConstants.MOTELS.equals(subType) || DxfFileConstants.BANK.equals(subType)
					|| DxfFileConstants.RESORTS.equals(subType)
					|| DxfFileConstants.LAGOONS_AND_LAGOON_RESORT.equals(subType)
					|| DxfFileConstants.AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS.equals(subType)
					|| DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX.equals(subType)
					|| DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING.equals(subType)
					|| DxfFileConstants.PROFESSIONAL_OFFICES.equals(subType)
					|| DxfFileConstants.DEPARTMENTAL_STORE.equals(subType)
					|| DxfFileConstants.HOLIDAY_RESORT.equals(subType) || DxfFileConstants.RESTAURANT.equals(subType)
					|| DxfFileConstants.LOCAL_RETAIL_SHOPPING.equals(subType)
					|| DxfFileConstants.SHOWROOM.equals(subType) || DxfFileConstants.SUPERMARKETS.equals(subType)
					|| DxfFileConstants.WHOLESALE_MARKET.equals(subType)
					|| DxfFileConstants.MEDIA_CENTRES.equals(subType) || DxfFileConstants.FOOD_COURTS.equals(subType)
					|| DxfFileConstants.MERCENTILE.equals(subType) || DxfFileConstants.MUSIC_PAVILIONS.equals(subType)
					|| DxfFileConstants.COMMUNITY_HALL.equals(subType)
					|| DxfFileConstants.CULTURAL_COMPLEX.equals(subType)
					|| DxfFileConstants.SCULPTURE_COMPLEX.equals(subType)
					|| DxfFileConstants.EXHIBITION_CENTER.equals(subType)
					|| DxfFileConstants.IT_ITES_BUILDINGS.equals(subType)) {
				if (ulb.isSparitFlag()) {
					//System.out.println("Sparit  implementation2:");
					helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
							.multiply(new BigDecimal("0.30")).setScale(2, BigDecimal.ROUND_HALF_UP);

				} else {
					if (DxfFileConstants.HOTEL.equals(subType) || DxfFileConstants.FIVE_STAR_HOTEL.equals(subType)
							|| DxfFileConstants.MOTELS.equals(subType) || DxfFileConstants.RESORTS.equals(subType)
							|| DxfFileConstants.HOLIDAY_RESORT.equals(subType)
							|| DxfFileConstants.LAGOONS_AND_LAGOON_RESORT.equals(subType)
							|| DxfFileConstants.AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS.equals(subType)) {
						helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
								.multiply(new BigDecimal("0.30")).setScale(2, BigDecimal.ROUND_HALF_UP);

					} else {
						helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
								.multiply(new BigDecimal("0.40")).setScale(2, BigDecimal.ROUND_HALF_UP);
					}

				}
				if (plotArea.compareTo(new BigDecimal("2000")) > 0) {
					helper.daPARKINGCountRequired = 2;
					helper.distFromDAToMainEntranceRequired = new BigDecimal("30");
				}
				helper.staffParkingRequired = helper.totalParkingRequired.divide(new BigDecimal("3"), 2,
						BigDecimal.ROUND_HALF_UP);
				helper.twoWheelerParkingRequired = helper.totalParkingRequired.multiply(new BigDecimal("0.15"))
						.setScale(2, BigDecimal.ROUND_HALF_UP);
				helper.bicycleParkingRequired = helper.totalParkingRequired.multiply(new BigDecimal("0.02")).setScale(2,
						BigDecimal.ROUND_HALF_UP);
			} else if (DxfFileConstants.SHOPPING_CENTER.equals(subType)
					|| DxfFileConstants.SHOPPING_MALL.equals(subType) || DxfFileConstants.AUDITORIUM.equals(subType)
					|| DxfFileConstants.BANQUET_HALL.equals(subType) || DxfFileConstants.CINEMA.equals(subType)
					|| DxfFileConstants.CLUB.equals(subType) || DxfFileConstants.CONVENTION_HALL.equals(subType)
					|| DxfFileConstants.GYMNASIA.equals(subType)
					|| DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP.equals(subType)
					|| DxfFileConstants.SPORTS_COMPLEX.equals(subType) || DxfFileConstants.STADIUM.equals(subType)
					|| DxfFileConstants.THEATRE.equals(subType)) {
				if (ulb.isSparitFlag()) {
					//System.out.println("Sparit  implementation2:");
					helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
							.multiply(new BigDecimal("0.40")).setScale(2, BigDecimal.ROUND_HALF_UP);
				} else {
					helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
							.multiply(new BigDecimal("0.50")).setScale(2, BigDecimal.ROUND_HALF_UP);
				}
				if (plotArea.compareTo(new BigDecimal("2000")) > 0) {
					helper.daPARKINGCountRequired = 2;
					helper.distFromDAToMainEntranceRequired = new BigDecimal("30");
				}
				helper.staffParkingRequired = helper.totalParkingRequired.divide(new BigDecimal("3"), 2,
						BigDecimal.ROUND_HALF_UP);
				helper.twoWheelerParkingRequired = helper.totalParkingRequired.multiply(new BigDecimal("0.15"))
						.setScale(2, BigDecimal.ROUND_HALF_UP);
				helper.bicycleParkingRequired = helper.totalParkingRequired.multiply(new BigDecimal("0.02")).setScale(2,
						BigDecimal.ROUND_HALF_UP);
			}

			// Roof top parking
			if (pl.getVirtualBuilding().getTotalCoverageArea().compareTo(new BigDecimal("2000")) < 0
					&& helper.roofTopParkingProvided.compareTo(BigDecimal.ZERO) > 0) {
				pl.addError("Roof top parking",
						"Roof top parking not allowed in case coverage area is less then 2000 sqm");
			}

			// staff parking not required to validate
			if (DxfFileConstants.OC_PUBLIC_UTILITY.equals(ocType)) {
				helper.staffParkingRequired = BigDecimal.ZERO;
			}

			// Two parking & bicycleParkingRequired not required to validate
			if (DxfFileConstants.OC_PUBLIC_UTILITY.equals(ocType) || DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(ocType)
					|| DxfFileConstants.OC_EDUCATION.equals(ocType)) {
				helper.twoWheelerParkingRequired = BigDecimal.ZERO;
				helper.bicycleParkingRequired = BigDecimal.ZERO;
			}
		}

		if (DxfFileConstants.OC_AGRICULTURE.equals(ocType)) {
			long totalNumberOfDu = pl.getPlanInformation().getTotalNoOfDwellingUnits();
			if (totalNumberOfDu > 4 && totalNumberOfDu <= 8) {
				helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
						.multiply(new BigDecimal("0.20")).setScale(2, BigDecimal.ROUND_HALF_UP);
			} else if (totalNumberOfDu > 8) {
				helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea()
						.multiply(new BigDecimal("0.30")).setScale(2, BigDecimal.ROUND_HALF_UP);
			}

			// DA parking
			if (totalNumberOfDu > 4) {
				helper.daPARKINGCountRequired = 2;
				helper.distFromDAToMainEntranceRequired = new BigDecimal("30");
			}

		}

		if (helper.daPARKINGCountRequired > 0) {
			helper.daPARKINGWidthRequired = new BigDecimal("3.6");
			helper.daPARKINGRequired = new BigDecimal("25");
		}
		
		BigDecimal buildingHeight = OdishaUtill.getMaxBuildingHeight(pl);
		
		// Ev charging points required
		if(plotArea.compareTo(new BigDecimal("4046.86"))>0 ||  buildingHeight.compareTo(new BigDecimal("15"))>0 || OdishaUtill.isOccupancyPersent(pl, DxfFileConstants.MULTI_LEVEL_CAR_PARKING)) {
			try {
				helper.evChargingPointRequired = helper.totalParkingRequired.multiply(new BigDecimal("0.3")).setScale(2,BigDecimal.ROUND_HALF_UP);
			}catch (Exception e) {
			}
		}
		
	}

	private BigDecimal getTotalAreaOfEWSFlrArea(Plan pl) {
		BigDecimal totalFlrArea = BigDecimal.ZERO;
		
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				for (Occupancy oc: floor.getOccupancies()) {
					if (oc.getTypeHelper().getSubtype().getCode().equals(DxfFileConstants.EWS)) {
						totalFlrArea = totalFlrArea.add(oc.getFloorArea());
					}
				}
			}
		}
		return totalFlrArea;
	}
	
	private BigDecimal getTotalAreaOfLIGFlrArea(Plan pl) {
		BigDecimal totalFlrArea = BigDecimal.ZERO;
		
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				for (Occupancy oc: floor.getOccupancies()) {
					if (oc.getTypeHelper().getSubtype().getCode().equals(DxfFileConstants.LOW_INCOME_HOUSING)) {
						totalFlrArea = totalFlrArea.add(oc.getFloorArea());
					}
				}
			}
		}
		return totalFlrArea;
	}

	private BigDecimal getTotalAreaOfMIG1AndMIG2(Plan pl) {
		BigDecimal totalArea = BigDecimal.ZERO;
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				for (FloorUnit unit : floor.getMig1Unit()) {
					totalArea = totalArea.add(unit.getArea());
				}
				for (FloorUnit unit : floor.getMig2Unit()) {
					totalArea = totalArea.add(unit.getArea());
				}
			}
		}
		return totalArea;
	}

	private BigDecimal getTotalAreaOfLIGAndEWS(Plan pl) {
		BigDecimal totalArea = BigDecimal.ZERO;
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				for (FloorUnit unit : floor.getLigUnit()) {
					totalArea = totalArea.add(unit.getArea());
				}
				for (FloorUnit unit : floor.getEwsUnit()) {
					totalArea = totalArea.add(unit.getArea());
				}
			}
		}
		return totalArea;
	}

	private BigDecimal totalStiltArea(Plan pl) {
		BigDecimal area = BigDecimal.ZERO;

		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				if (floor.getIsStiltFloor()) {
					area = area.add(floor.getTotalStiltArea());
				}
			}
		}

		return area;
	}

	private void genralParking(Plan pl, OdishaParkingHelper helper) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Parking Type");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);

		ScrutinyDetail scrutinyDetail1 = new ScrutinyDetail();
		scrutinyDetail1.setKey("Common_Parking Provision");
		scrutinyDetail1.addColumnHeading(1, RULE_NO);
		scrutinyDetail1.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail1.addColumnHeading(3, REQUIRED);
		scrutinyDetail1.addColumnHeading(4, PROVIDED);
		scrutinyDetail1.addColumnHeading(5, STATUS);

		validateTotalParking(helper, scrutinyDetail);
		validateOpenParking(helper, scrutinyDetail);
		validateBasementParking(helper, scrutinyDetail);
		validateStilledParking(helper, scrutinyDetail);
		validateRoofTopParking(helper, scrutinyDetail);
		validateStackParking(pl,helper, scrutinyDetail);
		
		validateStaffParking(helper, scrutinyDetail1);
		validateVisitorParking(helper, scrutinyDetail1);
		validateTwoWheelers(helper, scrutinyDetail1);
		validateBicycleParking(helper, scrutinyDetail1);
		validateEWSAndLiGParking(helper, scrutinyDetail1);
		validateMIGParking(helper, scrutinyDetail1);
		validateEvChargingPoint(helper, scrutinyDetail1);
		
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail1);

	}

	private void offSiteParking(Plan pl, OdishaParkingHelper helper) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Parking");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);

		setReport(SUB_RULE_40, "Off Site Parking", DxfFileConstants.NA,
				helper.offSiteParkingprovisionsProvided.toString(), Result.Verify, scrutinyDetail);
		setReport(SUB_RULE_40, "On Site Parking", DxfFileConstants.NA, helper.totalParkingProvided.toString(),
				Result.Verify, scrutinyDetail);

		BigDecimal totalProvidedParking = helper.totalParkingProvided.add(helper.offSiteParkingprovisionsProvided)
				.setScale(2, BigDecimal.ROUND_HALF_UP);

		setReport(SUB_RULE_40, "Total Parking", helper.totalParkingRequired.toString(),
				helper.totalParkingProvided.toString(),
				totalProvidedParking.compareTo(helper.totalParkingRequired) >= 0 ? Result.Accepted
						: Result.Not_Accepted,
				scrutinyDetail);

		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	private void validateDAParking(Plan pl, OdishaParkingHelper helper) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Special Parking");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);

		validateDAParkingCount(helper, scrutinyDetail);
		validateDAParkingArea(helper, scrutinyDetail);
		validateDAParkingWidth(helper, scrutinyDetail);
		validateDAParkingMinDistance(helper, scrutinyDetail);

		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	private void validateTotalParking(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.totalParkingRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.totalParkingRequired.toString();
		if (helper.totalParkingProvided.compareTo(helper.totalParkingRequired) >= 0)
			setReport("ODA Rule 37 (1,13), 41(3-iv),74(7-i)", TOTAL_PARKING_DESC, required, helper.totalParkingProvided.toString(),
					Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Rule 37 (1,13), 41(3-iv),74(7-i)", TOTAL_PARKING_DESC, required, helper.totalParkingProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateEWSAndLiGParking(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.eWSOrLigParkingRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.eWSOrLigParkingRequired.toString();
		if (helper.eWSOrLigParkingProvided.compareTo(helper.eWSOrLigParkingRequired) >= 0)
			setReport("ODA Rule 37 (1-iii)", EWS_OR_LIG_PARKING_DESC, required, helper.eWSOrLigParkingProvided.toString(),
					Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Rule 37 (1-iii)", EWS_OR_LIG_PARKING_DESC, required, helper.eWSOrLigParkingProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateMIGParking(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.mIGParkingRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.mIGParkingRequired.toString();
		if (helper.mIGParkingProvided.compareTo(helper.mIGParkingRequired) >= 0)
			setReport("ODA Rule 37 (1-iv), 74 (7-i)", MIG_PARKING_DESC, required, helper.mIGParkingProvided.toString(), Result.Accepted,
					scrutinyDetail);
		else
			setReport("ODA Rule 37 (1-iv), 74 (7-i)", MIG_PARKING_DESC, required, helper.mIGParkingProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateStilledParking(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.stiltParkingRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.stiltParkingRequired.toString();
		if (helper.stiltParkingProvided.compareTo(helper.stiltParkingRequired) >= 0)
			setReport("ODA Rule 37 (13)", STILT_PARKING_DESC, required, helper.stiltParkingProvided.toString(),
					Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Rule 37 (13)", STILT_PARKING_DESC, required, helper.stiltParkingProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateRoofTopParking(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.roofTopParkingRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.roofTopParkingRequired.toString();
		if (helper.roofTopParkingProvided.compareTo(helper.roofTopParkingRequired) >= 0)
			setReport("ODA Rule 37 (2-vii)", ROOF_TOP_PARKING_DESC, required, helper.roofTopParkingProvided.toString(),
					Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Rule 37 (2-vii)", ROOF_TOP_PARKING_DESC, required, helper.roofTopParkingProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateStaffParking(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.staffParkingRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.staffParkingRequired.toString() + " Max";
		if (helper.staffParkingProvided.compareTo(helper.staffParkingRequired) <= 0)
			setReport("ODA Rule 37 (12)", STAFF_PARKING_DESC, required, helper.staffParkingProvided.toString(),
					Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Rule 37 (12)", STAFF_PARKING_DESC, required, helper.staffParkingProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateBasementParking(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.basementParkingRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.basementParkingRequired.toString();
		if (helper.basementParkingProvided.compareTo(helper.basementParkingRequired) >= 0)
			setReport("ODA Rule 37 (2-vii), 41(3-iv)", BASEMENT_PARKING_DESC, required, helper.basementParkingProvided.toString(),
					Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Rule 37 (2-vii), 41(3-iv)", BASEMENT_PARKING_DESC, required, helper.basementParkingProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateOpenParking(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.openParkingRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.openParkingRequired.toString();
		if (helper.openParkingProvided.compareTo(helper.openParkingRequired) >= 0)
			setReport(SUB_RULE_40, OPEN_PARKING_DESC, required, helper.openParkingProvided.toString(), Result.Accepted,
					scrutinyDetail);
		else
			setReport(SUB_RULE_40, OPEN_PARKING_DESC, required, helper.openParkingProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateTwoWheelers(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.twoWheelerParkingRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.twoWheelerParkingRequired.toString();
		if (helper.twoWheelerParkingProvided.compareTo(helper.twoWheelerParkingRequired) >= 0)
			setReport("ODA Rule 37 (1-ii)", TWO_WHEELER_PARKING_DESC, required, helper.twoWheelerParkingProvided.toString(),
					Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Rule 37 (1-ii)", TWO_WHEELER_PARKING_DESC, required, helper.twoWheelerParkingProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateBicycleParking(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.bicycleParkingRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.bicycleParkingRequired.toString();
		if (helper.bicycleParkingProvided.compareTo(helper.bicycleParkingRequired) >= 0)
			setReport("ODA Rule 37 (1-ii)", BICYCLE_PARKING_DESC, required, helper.bicycleParkingProvided.toString(),
					Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Rule 37 (1-ii)", BICYCLE_PARKING_DESC, required, helper.bicycleParkingProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateVisitorParking(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.visitorParkingRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.visitorParkingRequired.toString();
		if (helper.visitorParkingProvided.compareTo(helper.visitorParkingRequired) >= 0)
			setReport("ODA Rule 37 (12)", VISITOR_PARKING_DESC, required, helper.visitorParkingProvided.toString(),
					Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Rule 37 (12)", VISITOR_PARKING_DESC, required, helper.visitorParkingProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateDAParkingArea(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.daPARKINGRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.daPARKINGRequired.toString() + " each";
		if (helper.daPARKINGProvided.compareTo(helper.daPARKINGRequired) >= 0)
			setReport("ODA Annexure VIII (3-ii)", DA_PARKING_AREA_DESC, required, helper.daPARKINGProvided.toString(), Result.Accepted,
					scrutinyDetail);
		else
			setReport("ODA Annexure VIII (3-ii)", DA_PARKING_AREA_DESC, required, helper.daPARKINGProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateDAParkingCount(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.daPARKINGCountRequired > 0)
			required = helper.daPARKINGCountRequired + "";
		if (helper.daPARKINGCountProvided >= helper.daPARKINGCountRequired)
			setReport("NBC Norms", DA_PARKING_COUNT_DESC, required, helper.daPARKINGCountProvided + "", Result.Accepted,
					scrutinyDetail);
		else
			setReport("NBC Norms", DA_PARKING_COUNT_DESC, required, helper.daPARKINGCountProvided + "",
					Result.Not_Accepted, scrutinyDetail);
	}
	
	
	private void validateTotalParkingUnits(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		setReport("NBC Norms", TOTAL_PARKING_UNITS_DESC, required, helper.totalParkingUnitsProvided + "",
					Result.Accepted, scrutinyDetail);
	}
	
	private void validateOpenParkingUnits(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		setReport("NBC Norms", OPEN_PARKING_UNITS_DESC, required, helper.openParkingUnits + "",
					Result.Accepted, scrutinyDetail);
	}
	
	private void validateBasementParkingUnits(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		setReport("NBC Norms", BASEMENT_PARKING_UNITS_DESC, required, helper.basementParkingUnits + "",
					Result.Accepted, scrutinyDetail);
	}
	
	private void validateStiltParkingUnits(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		setReport("NBC Norms", STILT_PARKING_UNITS_DESC, required, helper.stiltParkingUnits + "",
					Result.Accepted, scrutinyDetail);
	}
	
	private void validateRooftopParkingUnits(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		setReport("NBC Norms", ROOFTOP_PARKING_UNITS_DESC, required, helper.rooftopParkingUnits + "",
					Result.Accepted, scrutinyDetail);
	}
	
	

	private void validateDAParkingWidth(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.daPARKINGWidthRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.daPARKINGWidthRequired.toString();
		if (helper.daPARKINGWidthProvided.compareTo(helper.daPARKINGWidthRequired) >= 0)
			setReport("ODA Annexure VIII (3-ii)", DA_PARKING_WIDTH_DESC, required, helper.daPARKINGWidthProvided.toString(),
					Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Annexure VIII (3-ii)", DA_PARKING_WIDTH_DESC, required, helper.daPARKINGWidthProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}

	private void validateDAParkingMinDistance(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.distFromDAToMainEntranceRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.distFromDAToMainEntranceRequired.toString();
		else
			return;
		if (helper.distFromDAToMainEntranceProvided.compareTo(helper.distFromDAToMainEntranceRequired) <= 0)
			setReport("ODA Annexure VIII (3-i)", DA_PARKING_MAX_DISTANCE_DESC, required,
					helper.distFromDAToMainEntranceProvided.toString(), Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Annexure VIII (3-i)", DA_PARKING_MAX_DISTANCE_DESC, required,
					helper.distFromDAToMainEntranceProvided.toString(), Result.Not_Accepted, scrutinyDetail);
	}

	private void validateDimensions(Plan pl) {
		ParkingDetails parkDtls = pl.getParkingDetails();
		if (!parkDtls.getCars().isEmpty()) {
			int count = 0;
			for (Measurement m : parkDtls.getCars())
				if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0)
					count++;
			if (count > 0)
				pl.addError(PARKING_SLOT, PARKING_SLOT + count + SLOT_HAVING_GT_4_PTS);
		}

		if (!parkDtls.getOpenCars().isEmpty()) {
			int count = 0;
			for (Measurement m : parkDtls.getOpenCars())
				if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0)
					count++;
			if (count > 0)
				pl.addError(OPEN_PARKING_DIM_DESC, OPEN_PARKING_DIM_DESC + count + SLOT_HAVING_GT_4_PTS);
		}

		if (!parkDtls.getCoverCars().isEmpty()) {
			int count = 0;
			for (Measurement m : parkDtls.getCoverCars())
				if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0)
					count++;
			if (count > 0)
				pl.addError(COVER_PARKING_DIM_DESC, COVER_PARKING_DIM_DESC + count + SLOT_HAVING_GT_4_PTS);
		}

		if (!parkDtls.getCoverCars().isEmpty()) {
			int count = 0;
			for (Measurement m : parkDtls.getBasementCars())
				if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0)
					count++;
			if (count > 0)
				pl.addError(BSMNT_PARKING_DIM_DESC, BSMNT_PARKING_DIM_DESC + count + SLOT_HAVING_GT_4_PTS);
		}

		if (!parkDtls.getSpecial().isEmpty()) {
			int count = 0;
			for (Measurement m : parkDtls.getDisabledPersons())
				if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0)
					count++;
			if (count > 0)
				pl.addError(SPECIAL_PARKING_DIM_DESC, SPECIAL_PARKING_DIM_DESC + count
						+ " number of DA Parking slot polygon not having only 4 points.");
		}

		if (!parkDtls.getLoadUnload().isEmpty()) {
			int count = 0;
			for (Measurement m : parkDtls.getLoadUnload())
				if (m.getArea().compareTo(BigDecimal.valueOf(30)) < 0)
					count++;
			if (count > 0)
				pl.addError("load unload", count + " loading unloading parking spaces doesnt contain minimum of 30m2");
		}

		if (!parkDtls.getMechParking().isEmpty()) {
			int count = 0;
			for (Measurement m : parkDtls.getMechParking())
				if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0)
					count++;
			if (count > 0)
				pl.addError(MECHANICAL_PARKING,
						count + " number of Mechanical parking slot polygon not having only 4 points.");
		}

		if (!parkDtls.getTwoWheelers().isEmpty()) {
			int count = 0;
			for (Measurement m : parkDtls.getTwoWheelers())
				if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0)
					count++;
			if (count > 0)
				pl.addError(TWO_WHEELER_DIM_DESC, TWO_WHEELER_DIM_DESC + count
						+ " number of two wheeler Parking slot polygon not having only 4 points.");
		}
	}

	public void processParking(Plan pl) {
		ParkingHelper helper = new ParkingHelper();
		// checkDimensionForCarParking(pl, helper);

		OccupancyTypeHelper mostRestrictiveOccupancy = pl.getVirtualBuilding() != null
				? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
				: null;
		BigDecimal totalBuiltupArea = pl.getOccupancies().stream().map(Occupancy::getBuiltUpArea)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal coverParkingArea = BigDecimal.ZERO;
		BigDecimal basementParkingArea = BigDecimal.ZERO;
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				coverParkingArea = coverParkingArea.add(floor.getParking().getCoverCars().stream()
						.map(Measurement::getArea).reduce(BigDecimal.ZERO, BigDecimal::add));
				basementParkingArea = basementParkingArea.add(floor.getParking().getBasementCars().stream()
						.map(Measurement::getArea).reduce(BigDecimal.ZERO, BigDecimal::add));
			}
		}
		BigDecimal openParkingArea = pl.getParkingDetails().getOpenCars().stream().map(Measurement::getArea)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalProvidedCarParkArea = openParkingArea.add(coverParkingArea).add(basementParkingArea);
		helper.totalRequiredCarParking += openParkingArea.doubleValue() / OPEN_ECS;
		helper.totalRequiredCarParking += coverParkingArea.doubleValue() / COVER_ECS;
		helper.totalRequiredCarParking += basementParkingArea.doubleValue() / BSMNT_ECS;
		Double requiredCarParkArea = 0d;
		Double requiredVisitorParkArea = 0d;

		BigDecimal providedVisitorParkArea = BigDecimal.ZERO;

		validateSpecialParking(pl, helper, totalBuiltupArea);

		if (totalBuiltupArea != null && totalBuiltupArea.doubleValue() <= 300) {

			if (mostRestrictiveOccupancy != null && A.equals(mostRestrictiveOccupancy.getType().getCode())) {
				if (totalBuiltupArea.doubleValue() <= 200) {
					requiredCarParkArea += OPEN_ECS * 1;
				} else if (totalBuiltupArea.doubleValue() > 200 && totalBuiltupArea.doubleValue() <= 300) {
					requiredCarParkArea += OPEN_ECS * 2;
				}
			} else {
				BigDecimal builtupArea = totalBuiltupArea.subtract(totalBuiltupArea.multiply(BigDecimal.valueOf(0.15)));
				double requiredEcs = builtupArea.divide(BigDecimal.valueOf(100)).multiply(BigDecimal.valueOf(2))
						.setScale(0, RoundingMode.UP).doubleValue();
				if (openParkingArea.doubleValue() > 0 && coverParkingArea.doubleValue() > 0)
					requiredCarParkArea += COVER_ECS * requiredEcs;
				else if (openParkingArea.doubleValue() > 0 && basementParkingArea.doubleValue() > 0)
					requiredCarParkArea += BSMNT_ECS * requiredEcs;
				else if (coverParkingArea.doubleValue() > 0 && basementParkingArea.doubleValue() > 0)
					requiredCarParkArea += BSMNT_ECS * requiredEcs;
				else if (coverParkingArea.doubleValue() > 0)
					requiredCarParkArea += COVER_ECS * requiredEcs;
				else if (basementParkingArea.doubleValue() > 0)
					requiredCarParkArea += BSMNT_ECS * requiredEcs;
				else if (openParkingArea.doubleValue() > 0)
					requiredCarParkArea += OPEN_ECS * requiredEcs;
			}
		} else {
			providedVisitorParkArea = pl.getParkingDetails().getVisitors().stream().map(Measurement::getArea)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			if (mostRestrictiveOccupancy != null && A.equals(mostRestrictiveOccupancy.getType().getCode())) {
				requiredCarParkArea = totalBuiltupArea.doubleValue() * PARK_A;
				if (mostRestrictiveOccupancy != null && mostRestrictiveOccupancy.getSubtype() != null
						&& A.equals(mostRestrictiveOccupancy.getSubtype().getCode()))
					requiredVisitorParkArea = requiredCarParkArea * PARK_VISITOR;
			} else if (mostRestrictiveOccupancy != null && (F.equals(mostRestrictiveOccupancy.getType().getCode()))) {
				requiredCarParkArea = totalBuiltupArea.doubleValue() * PARK_F;
				if (mostRestrictiveOccupancy != null && mostRestrictiveOccupancy.getSubtype() != null
						&& (F_H.equals(mostRestrictiveOccupancy.getSubtype().getCode())
								|| F.equals(mostRestrictiveOccupancy.getSubtype().getCode())
								|| F.equals(mostRestrictiveOccupancy.getSubtype().getCode())
								|| F.equals(mostRestrictiveOccupancy.getSubtype().getCode())
								|| F.equals(mostRestrictiveOccupancy.getSubtype().getCode()))) {
					requiredCarParkArea = totalBuiltupArea.doubleValue() * PARK_F;
				}
			} else if (mostRestrictiveOccupancy != null && (G.equals(mostRestrictiveOccupancy.getType().getCode()))) {
				requiredCarParkArea = totalBuiltupArea.doubleValue() * PARK_F;
			}
		}

		BigDecimal requiredCarParkingArea = Util.roundOffTwoDecimal(BigDecimal.valueOf(requiredCarParkArea));
		BigDecimal totalProvidedCarParkingArea = Util.roundOffTwoDecimal(totalProvidedCarParkArea);
		BigDecimal requiredVisitorParkingArea = Util.roundOffTwoDecimal(BigDecimal.valueOf(requiredVisitorParkArea));
		BigDecimal providedVisitorParkingArea = Util.roundOffTwoDecimal(providedVisitorParkArea);

		// checkDimensionForTwoWheelerParking(pl, helper);
		// checkAreaForLoadUnloadSpaces(pl);
		if (totalProvidedCarParkArea.doubleValue() == 0) {
			pl.addError(SUB_RULE_40_2_DESCRIPTION,
					getLocaleMessage("msg.error.not.defined", SUB_RULE_40_2_DESCRIPTION));
		} else if (requiredCarParkArea > 0 && totalProvidedCarParkingArea.compareTo(requiredCarParkingArea) < 0) {
			setReportOutputDetails(pl, SUB_RULE_40_2, SUB_RULE_40_2_DESCRIPTION, requiredCarParkingArea + SQMTRS,
					totalProvidedCarParkingArea + SQMTRS, Result.Not_Accepted.getResultVal());
		} else {
			setReportOutputDetails(pl, SUB_RULE_40_2, SUB_RULE_40_2_DESCRIPTION, requiredCarParkingArea + SQMTRS,
					totalProvidedCarParkingArea + SQMTRS, Result.Accepted.getResultVal());
		}
		if (requiredVisitorParkArea > 0 && providedVisitorParkArea.compareTo(requiredVisitorParkingArea) < 0) {
			setReportOutputDetails(pl, SUB_RULE_40_10, SUB_RULE_40_10_DESCRIPTION, requiredVisitorParkingArea + SQMTRS,
					providedVisitorParkArea + SQMTRS, Result.Not_Accepted.getResultVal());
		} else if (requiredVisitorParkArea > 0) {
			setReportOutputDetails(pl, SUB_RULE_40_10, SUB_RULE_40_10_DESCRIPTION, requiredVisitorParkingArea + SQMTRS,
					providedVisitorParkingArea + SQMTRS, Result.Accepted.getResultVal());
		}

//		LOGGER.info("******************Require no of Car Parking***************" + helper.totalRequiredCarParking);
	}

	private void setReportOutputDetails(Plan pl, String ruleNo, String ruleDesc, String expected, String actual,
			String status) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status);
		scrutinyDetail.getDetail().add(details);
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	private void validateSpecialParking(Plan pl, ParkingHelper helper, BigDecimal totalBuiltupArea) {
		BigDecimal maxHeightOfBuilding = BigDecimal.ZERO;
		int failedCount = 0;
		int success = 0;
		if (!pl.getParkingDetails().getSpecial().isEmpty()) {
			for (Measurement m : pl.getParkingDetails().getSpecial()) {
				if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0)
					failedCount++;
				else
					success++;
			}
			if (failedCount > 0)
				pl.addError(SPECIAL_PARKING_DIM_DESC,
						SPECIAL_PARKING_DIM_DESC + failedCount + " number not having only 4 points.");
			pl.getParkingDetails().setValidSpecialSlots(success);
		}

		for (Block block : pl.getBlocks()) {
			if (block.getBuilding().getBuildingHeight().compareTo(maxHeightOfBuilding) > 0) {
				maxHeightOfBuilding = block.getBuilding().getBuildingHeight();
			}
		}
		if (maxHeightOfBuilding.compareTo(new BigDecimal(15)) >= 0
				|| (pl.getPlot() != null && pl.getPlot().getArea().compareTo(new BigDecimal(500)) > 0)) {
			if (pl.getParkingDetails().getValidSpecialSlots() == 0) {
				pl.addError("", getLocaleMessage(DcrConstants.OBJECTNOTDEFINED, SP_PARKING));
			} else {
				for (Measurement m : pl.getParkingDetails().getSpecial()) {
					if (m.getMinimumSide().compareTo(new BigDecimal(0)) > 0
							&& m.getMinimumSide().compareTo(new BigDecimal(3.6)) >= 0) {
						setReportOutputDetails(pl, "ODA Annexure VIII (3-ii)", SP_PARKING, 1 + NUMBERS,
								pl.getParkingDetails().getValidSpecialSlots() + NUMBERS,
								Result.Accepted.getResultVal());
					} else if (m.getMinimumSide().compareTo(new BigDecimal(0)) > 0) {
						setReportOutputDetails(pl, "ODA Annexure VIII (3-ii)", SP_PARKING, 1 + NUMBERS,
								pl.getParkingDetails().getValidSpecialSlots() + NUMBERS,
								Result.Not_Accepted.getResultVal());
					}
				}
			}
		}

	}

	private void processTwoWheelerParking(Plan pl, ParkingHelper helper) {
		helper.twoWheelerParking = BigDecimal.valueOf(0.25 * helper.totalRequiredCarParking * 2.70 * 5.50)
				.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
		double providedArea = 0;
		for (Measurement measurement : pl.getParkingDetails().getTwoWheelers()) {
			providedArea = providedArea + measurement.getArea().doubleValue();
		}
		if (providedArea < helper.twoWheelerParking) {
			setReportOutputDetails(pl, "ODA Rule 37 (1-ii)", TWO_WHEELER_PARK_AREA,
					helper.twoWheelerParking + " " + DcrConstants.SQMTRS,
					BigDecimal.valueOf(providedArea).setScale(2, BigDecimal.ROUND_HALF_UP) + " " + DcrConstants.SQMTRS,
					Result.Not_Accepted.getResultVal());
		} else {
			setReportOutputDetails(pl, "ODA Rule 37 (1-ii)", TWO_WHEELER_PARK_AREA,
					helper.twoWheelerParking + " " + DcrConstants.SQMTRS,
					BigDecimal.valueOf(providedArea).setScale(2, BigDecimal.ROUND_HALF_UP) + " " + DcrConstants.SQMTRS,
					Result.Accepted.getResultVal());
		}
	}

	private void processMechanicalParking(Plan pl) {
		int count = 0;
		for (Measurement m : pl.getParkingDetails().getMechParking())
			if (m.getWidth().compareTo(BigDecimal.valueOf(MECH_PARKING_WIDTH)) < 0
					|| m.getHeight().compareTo(BigDecimal.valueOf(MECH_PARKING_HEIGHT)) < 0)
				count++;
		if (count > 0) {
			setReportOutputDetails(pl, "ODA Rule 37 (2-vi)", MECH_PARKING_DESC, MECH_PARKING_DIM_DESC,
					count + MECH_PARKING_DIM_DESC_NA, Result.Not_Accepted.getResultVal());
		} else {
			setReportOutputDetails(pl, "ODA Rule 37 (2-vi)", MECH_PARKING_DESC, MECH_PARKING_DIM_DESC,
					count + MECH_PARKING_DIM_DESC_NA, Result.Accepted.getResultVal());
		}
	}

	/*
	 * private double processMechanicalParking(Plan pl, ParkingHelper helper) {
	 * Integer noOfMechParkingFromPlInfo =
	 * pl.getPlanInformation().getNoOfMechanicalParking(); Integer providedSlots =
	 * pl.getParkingDetails().getMechParking().size(); double maxAllowedMechPark =
	 * BigDecimal.valueOf(helper.totalRequiredCarParking / 2).setScale(0,
	 * RoundingMode.UP) .intValue(); if (noOfMechParkingFromPlInfo > 0) { if
	 * (noOfMechParkingFromPlInfo > 0 && providedSlots == 0) {
	 * setReportOutputDetails(pl, SUB_RULE_34_2, MECHANICAL_PARKING, 1 + NUMBERS,
	 * providedSlots + NUMBERS, Result.Not_Accepted.getResultVal()); } else if
	 * (noOfMechParkingFromPlInfo > 0 && providedSlots > 0 &&
	 * noOfMechParkingFromPlInfo > maxAllowedMechPark) { setReportOutputDetails(pl,
	 * SUB_RULE_34_2, MAX_ALLOWED_MECH_PARK, maxAllowedMechPark + NUMBERS,
	 * noOfMechParkingFromPlInfo + NUMBERS, Result.Not_Accepted.getResultVal()); }
	 * else if (noOfMechParkingFromPlInfo > 0 && providedSlots > 0) {
	 * setReportOutputDetails(pl, SUB_RULE_34_2, MECHANICAL_PARKING, "",
	 * noOfMechParkingFromPlInfo + NUMBERS, Result.Accepted.getResultVal()); } }
	 * return 0; }
	 */

	/*
	 * private void buildResultForYardValidation(Plan Plan, BigDecimal
	 * parkSlotAreaInFrontYard, BigDecimal maxAllowedArea, String type) {
	 * Plan.reportOutput .add(buildRuleOutputWithSubRule(DcrConstants.RULE34,
	 * SUB_RULE_34_1,
	 * "Parking space should not exceed 50% of the area of mandatory " + type,
	 * "Parking space should not exceed 50% of the area of mandatory " + type,
	 * "Maximum allowed area for parking in " + type +" " + maxAllowedArea +
	 * DcrConstants.SQMTRS, "Parking provided in more than the allowed area " +
	 * parkSlotAreaInFrontYard + DcrConstants.SQMTRS, Result.Not_Accepted, null)); }
	 * private BigDecimal validateParkingSlotsAreWithInYard(Plan Plan, Polygon
	 * yardPolygon) { BigDecimal area = BigDecimal.ZERO; for (Measurement
	 * parkingSlot : Plan.getParkingDetails().getCars()) { Iterator parkSlotIterator
	 * = parkingSlot.getPolyLine().getVertexIterator(); while
	 * (parkSlotIterator.hasNext()) { DXFVertex dxfVertex = (DXFVertex)
	 * parkSlotIterator.next(); Point point = dxfVertex.getPoint(); if
	 * (rayCasting.contains(point, yardPolygon)) { area =
	 * area.add(parkingSlot.getArea()); } } } return area; }
	 */

	private void checkDimensionForCarParking(Plan pl, ParkingHelper helper) {

		/*
		 * for (Block block : Plan.getBlocks()) { for (SetBack setBack :
		 * block.getSetBacks()) { if (setBack.getFrontYard() != null &&
		 * setBack.getFrontYard().getPresentInDxf()) { Polygon frontYardPolygon =
		 * ProcessHelper.getPolygon(setBack.getFrontYard().getPolyLine()); BigDecimal
		 * parkSlotAreaInFrontYard = validateParkingSlotsAreWithInYard(Plan,
		 * frontYardPolygon); BigDecimal maxAllowedArea =
		 * setBack.getFrontYard().getArea().divide(BigDecimal.valueOf(2),
		 * DcrConstants.DECIMALDIGITS_MEASUREMENTS, RoundingMode.HALF_UP); if
		 * (parkSlotAreaInFrontYard.compareTo(maxAllowedArea) > 0) {
		 * buildResultForYardValidation(Plan, parkSlotAreaInFrontYard, maxAllowedArea,
		 * "front yard space"); } } if (setBack.getRearYard() != null &&
		 * setBack.getRearYard().getPresentInDxf()) { Polygon rearYardPolygon =
		 * ProcessHelper.getPolygon(setBack.getRearYard().getPolyLine()); BigDecimal
		 * parkSlotAreaInRearYard = validateParkingSlotsAreWithInYard(Plan,
		 * rearYardPolygon); BigDecimal maxAllowedArea =
		 * setBack.getRearYard().getArea().divide(BigDecimal.valueOf(2),
		 * DcrConstants.DECIMALDIGITS_MEASUREMENTS, RoundingMode.HALF_UP); if
		 * (parkSlotAreaInRearYard.compareTo(maxAllowedArea) > 0) {
		 * buildResultForYardValidation(Plan, parkSlotAreaInRearYard, maxAllowedArea,
		 * "rear yard space"); } } if (setBack.getSideYard1() != null &&
		 * setBack.getSideYard1().getPresentInDxf()) { Polygon sideYard1Polygon =
		 * ProcessHelper.getPolygon(setBack.getSideYard1().getPolyLine()); BigDecimal
		 * parkSlotAreaInSideYard1 = validateParkingSlotsAreWithInYard(Plan,
		 * sideYard1Polygon); BigDecimal maxAllowedArea =
		 * setBack.getSideYard1().getArea().divide(BigDecimal.valueOf(2),
		 * DcrConstants.DECIMALDIGITS_MEASUREMENTS, RoundingMode.HALF_UP); if
		 * (parkSlotAreaInSideYard1.compareTo(maxAllowedArea) > 0) {
		 * buildResultForYardValidation(Plan, parkSlotAreaInSideYard1, maxAllowedArea,
		 * "side yard1 space"); } } if (setBack.getSideYard2() != null &&
		 * setBack.getSideYard2().getPresentInDxf()) { Polygon sideYard2Polygon =
		 * ProcessHelper.getPolygon(setBack.getSideYard2().getPolyLine()); BigDecimal
		 * parkSlotAreaInFrontYard = validateParkingSlotsAreWithInYard(Plan,
		 * sideYard2Polygon); BigDecimal maxAllowedArea =
		 * setBack.getSideYard2().getArea().divide(BigDecimal.valueOf(2),
		 * DcrConstants.DECIMALDIGITS_MEASUREMENTS, RoundingMode.HALF_UP); if
		 * (parkSlotAreaInFrontYard.compareTo(maxAllowedArea) > 0) {
		 * buildResultForYardValidation(Plan, parkSlotAreaInFrontYard, maxAllowedArea,
		 * "side yard2 space"); } } } }
		 */

		int parkingCount = pl.getParkingDetails().getCars().size();
		int failedCount = 0;
		int success = 0;
		for (Measurement slot : pl.getParkingDetails().getCars()) {
			if (slot.getHeight().setScale(2, RoundingMode.UP).doubleValue() >= PARKING_SLOT_HEIGHT
					&& slot.getWidth().setScale(2, RoundingMode.UP).doubleValue() >= PARKING_SLOT_WIDTH)
				success++;
			else
				failedCount++;
		}
		pl.getParkingDetails().setValidCarParkingSlots(parkingCount - failedCount);
		if (parkingCount > 0)
			if (failedCount > 0) {
				if (helper.totalRequiredCarParking.intValue() == pl.getParkingDetails().getValidCarParkingSlots()) {
					setReportOutputDetails(pl, SUB_RULE_40, SUB_RULE_34_1_DESCRIPTION,
							PARKING_MIN_AREA + MIN_AREA_EACH_CAR_PARKING,
							OUT_OF + parkingCount + PARKING + failedCount + PARKING_VIOLATED_MINIMUM_AREA,
							Result.Accepted.getResultVal());
				} else {
					setReportOutputDetails(pl, SUB_RULE_40, SUB_RULE_34_1_DESCRIPTION,
							PARKING_MIN_AREA + MIN_AREA_EACH_CAR_PARKING,
							OUT_OF + parkingCount + PARKING + failedCount + PARKING_VIOLATED_MINIMUM_AREA,
							Result.Not_Accepted.getResultVal());
				}
			} else {
				setReportOutputDetails(pl, SUB_RULE_40, SUB_RULE_34_1_DESCRIPTION,
						PARKING_MIN_AREA + MIN_AREA_EACH_CAR_PARKING, NO_VIOLATION_OF_AREA + parkingCount + PARKING,
						Result.Accepted.getResultVal());
			}
		int openParkCount = pl.getParkingDetails().getOpenCars().size();
		int openFailedCount = 0;
		int openSuccess = 0;
		for (Measurement slot : pl.getParkingDetails().getOpenCars()) {
			if (slot.getHeight().setScale(2, RoundingMode.UP).doubleValue() >= PARKING_SLOT_HEIGHT
					&& slot.getWidth().setScale(2, RoundingMode.UP).doubleValue() >= PARKING_SLOT_WIDTH)
				openSuccess++;
			else
				openFailedCount++;
		}
		pl.getParkingDetails().setValidOpenCarSlots(openParkCount - openFailedCount);
		if (openParkCount > 0)
			if (openFailedCount > 0) {
				if (helper.totalRequiredCarParking.intValue() == pl.getParkingDetails().getValidOpenCarSlots()) {
					setReportOutputDetails(pl, SUB_RULE_40, OPEN_PARKING_DIM_DESC,
							PARKING_MIN_AREA + MIN_AREA_EACH_CAR_PARKING,
							OUT_OF + openParkCount + PARKING + openFailedCount + PARKING_VIOLATED_MINIMUM_AREA,
							Result.Accepted.getResultVal());
				} else {
					setReportOutputDetails(pl, SUB_RULE_40, OPEN_PARKING_DIM_DESC,
							PARKING_MIN_AREA + MIN_AREA_EACH_CAR_PARKING,
							OUT_OF + openParkCount + PARKING + openFailedCount + PARKING_VIOLATED_MINIMUM_AREA,
							Result.Not_Accepted.getResultVal());
				}
			} else {
				setReportOutputDetails(pl, SUB_RULE_40, OPEN_PARKING_DIM_DESC,
						PARKING_MIN_AREA + MIN_AREA_EACH_CAR_PARKING, NO_VIOLATION_OF_AREA + openParkCount + PARKING,
						Result.Accepted.getResultVal());
			}

		int coverParkCount = pl.getParkingDetails().getCoverCars().size();
		int coverFailedCount = 0;
		int coverSuccess = 0;
		for (Measurement slot : pl.getParkingDetails().getCoverCars()) {
			if (slot.getHeight().setScale(2, RoundingMode.UP).doubleValue() >= PARKING_SLOT_HEIGHT
					&& slot.getWidth().setScale(2, RoundingMode.UP).doubleValue() >= PARKING_SLOT_WIDTH)
				coverSuccess++;
			else
				coverFailedCount++;
		}
		pl.getParkingDetails().setValidCoverCarSlots(coverParkCount - coverFailedCount);
		if (coverParkCount > 0)
			if (coverFailedCount > 0) {
				if (helper.totalRequiredCarParking.intValue() == pl.getParkingDetails().getValidCoverCarSlots()) {
					setReportOutputDetails(pl, SUB_RULE_40, COVER_PARKING_DIM_DESC,
							PARKING_MIN_AREA + MIN_AREA_EACH_CAR_PARKING,
							OUT_OF + coverParkCount + PARKING + coverFailedCount + PARKING_VIOLATED_MINIMUM_AREA,
							Result.Accepted.getResultVal());
				} else {
					setReportOutputDetails(pl, SUB_RULE_40, COVER_PARKING_DIM_DESC,
							PARKING_MIN_AREA + MIN_AREA_EACH_CAR_PARKING,
							OUT_OF + coverParkCount + PARKING + coverFailedCount + PARKING_VIOLATED_MINIMUM_AREA,
							Result.Not_Accepted.getResultVal());
				}
			} else {
				setReportOutputDetails(pl, SUB_RULE_40, COVER_PARKING_DIM_DESC,
						PARKING_MIN_AREA + MIN_AREA_EACH_CAR_PARKING, NO_VIOLATION_OF_AREA + coverParkCount + PARKING,
						Result.Accepted.getResultVal());
			}

		// Validate dimension of basement
		int bsmntParkCount = pl.getParkingDetails().getBasementCars().size();
		int bsmntFailedCount = 0;
		int bsmntSuccess = 0;
		for (Measurement slot : pl.getParkingDetails().getBasementCars()) {
			if (slot.getHeight().setScale(2, RoundingMode.UP).doubleValue() >= PARKING_SLOT_HEIGHT
					&& slot.getWidth().setScale(2, RoundingMode.UP).doubleValue() >= PARKING_SLOT_WIDTH)
				bsmntSuccess++;
			else
				bsmntFailedCount++;
		}
		pl.getParkingDetails().setValidBasementCarSlots(bsmntParkCount - bsmntFailedCount);
		if (bsmntParkCount > 0)
			if (bsmntFailedCount > 0) {
				if (helper.totalRequiredCarParking.intValue() == pl.getParkingDetails().getValidBasementCarSlots()) {
					setReportOutputDetails(pl, SUB_RULE_40, BSMNT_PARKING_DIM_DESC,
							PARKING_MIN_AREA + MIN_AREA_EACH_CAR_PARKING,
							OUT_OF + bsmntParkCount + PARKING + bsmntFailedCount + PARKING_VIOLATED_MINIMUM_AREA,
							Result.Accepted.getResultVal());
				} else {
					setReportOutputDetails(pl, SUB_RULE_40, BSMNT_PARKING_DIM_DESC,
							PARKING_MIN_AREA + MIN_AREA_EACH_CAR_PARKING,
							OUT_OF + bsmntParkCount + PARKING + bsmntFailedCount + PARKING_VIOLATED_MINIMUM_AREA,
							Result.Not_Accepted.getResultVal());
				}
			} else {
				setReportOutputDetails(pl, SUB_RULE_40, BSMNT_PARKING_DIM_DESC,
						PARKING_MIN_AREA + MIN_AREA_EACH_CAR_PARKING, NO_VIOLATION_OF_AREA + bsmntParkCount + PARKING,
						Result.Accepted.getResultVal());
			}

	}

	private void checkDimensionForSpecialParking(Plan pl, ParkingHelper helper) {

		int success = 0;
		int specialFailedCount = 0;
		int specialParkCount = pl.getParkingDetails().getSpecial().size();
		for (Measurement spParkSlot : pl.getParkingDetails().getSpecial()) {
			if (spParkSlot.getMinimumSide().doubleValue() >= SP_PARK_SLOT_MIN_SIDE)
				success++;
			else
				specialFailedCount++;
		}
		pl.getParkingDetails().setValidSpecialSlots(specialParkCount - specialFailedCount);
		if (specialParkCount > 0)
			if (specialFailedCount > 0) {
				if (helper.daParking.intValue() == pl.getParkingDetails().getValidSpecialSlots()) {
					setReportOutputDetails(pl, "ODA Annexure VIII (3-ii)", SP_PARKING_SLOT_AREA,
							DA_PARKING_MIN_AREA + MINIMUM_AREA_OF_EACH_DA_PARKING,
							NO_VIOLATION_OF_AREA + pl.getParkingDetails().getValidSpecialSlots() + PARKING,
							Result.Accepted.getResultVal());
				} else {
					setReportOutputDetails(pl, "ODA Annexure VIII (3-ii)", SP_PARKING_SLOT_AREA,
							DA_PARKING_MIN_AREA + MINIMUM_AREA_OF_EACH_DA_PARKING,
							OUT_OF + specialParkCount + PARKING + specialFailedCount + PARKING_VIOLATED_MINIMUM_AREA,
							Result.Not_Accepted.getResultVal());
				}
			} else {
				setReportOutputDetails(pl, "ODA Annexure VIII (3-ii)", SP_PARKING_SLOT_AREA,
						DA_PARKING_MIN_AREA + MINIMUM_AREA_OF_EACH_DA_PARKING,
						NO_VIOLATION_OF_AREA + specialParkCount + PARKING, Result.Accepted.getResultVal());
			}
	}

	private void checkDimensionForTwoWheelerParking(Plan pl, ParkingHelper helper) {
		double providedArea = 0;
		int twoWheelParkingCount = pl.getParkingDetails().getTwoWheelers().size();
		int failedTwoWheelCount = 0;
		helper.twoWheelerParking = BigDecimal.valueOf(0.25 * helper.totalRequiredCarParking * 2.70 * 5.50)
				.setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
		if (!pl.getParkingDetails().getTwoWheelers().isEmpty()) {
			for (Measurement m : pl.getParkingDetails().getTwoWheelers()) {
				if (m.getWidth().setScale(2, RoundingMode.UP).doubleValue() < TWO_WHEEL_PARKING_AREA_WIDTH
						|| m.getHeight().setScale(2, RoundingMode.UP).doubleValue() < TWO_WHEEL_PARKING_AREA_HEIGHT)
					failedTwoWheelCount++;

				providedArea = providedArea + m.getArea().doubleValue();
			}
		}

		if (providedArea < helper.twoWheelerParking) {
			setReportOutputDetails(pl, "ODA Rule 37 (1-ii)", TWO_WHEELER_PARK_AREA,
					helper.twoWheelerParking + " " + DcrConstants.SQMTRS,
					BigDecimal.valueOf(providedArea).setScale(2, BigDecimal.ROUND_HALF_UP) + " " + DcrConstants.SQMTRS,
					Result.Not_Accepted.getResultVal());
		} else {
			setReportOutputDetails(pl, "ODA Rule 37 (1-ii)", TWO_WHEELER_PARK_AREA,
					helper.twoWheelerParking + " " + DcrConstants.SQMTRS,
					BigDecimal.valueOf(providedArea).setScale(2, BigDecimal.ROUND_HALF_UP) + " " + DcrConstants.SQMTRS,
					Result.Accepted.getResultVal());
		}

		if (providedArea >= helper.twoWheelerParking && failedTwoWheelCount >= 0) {
			setReportOutputDetails(pl, SUB_RULE_40, TWO_WHEELER_DIM_DESC, PARKING_AREA_DIM,
					OUT_OF + twoWheelParkingCount + PARKING + failedTwoWheelCount + PARKING_VIOLATED_DIM,
					Result.Accepted.getResultVal());
		} else {
			setReportOutputDetails(pl, SUB_RULE_40, TWO_WHEELER_DIM_DESC, PARKING_AREA_DIM,
					OUT_OF + twoWheelParkingCount + PARKING + failedTwoWheelCount + PARKING_VIOLATED_DIM,
					Result.Not_Accepted.getResultVal());
		}
	}

	private BigDecimal getTotalCarpetAreaByOccupancy(Plan pl, OccupancyType type) {
		BigDecimal totalArea = BigDecimal.ZERO;
		for (Block b : pl.getBlocks())
			for (Occupancy occupancy : b.getBuilding().getTotalArea())
				if (occupancy.getType().equals(type))
					totalArea = totalArea.add(occupancy.getCarpetArea());
		return totalArea;
	}

	private void checkAreaForLoadUnloadSpaces(Plan pl) {
		double providedArea = 0;
		BigDecimal totalBuiltupArea = pl.getOccupancies().stream().map(Occupancy::getBuiltUpArea)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		double requiredArea = Math.abs(((totalBuiltupArea.doubleValue() - 700) / 1000) * 30);
		if (!pl.getParkingDetails().getLoadUnload().isEmpty()) {
			for (Measurement m : pl.getParkingDetails().getLoadUnload()) {
				if (m.getArea().compareTo(BigDecimal.valueOf(30)) >= 0)
					providedArea = providedArea + m.getArea().doubleValue();
			}
		}
		if (providedArea < requiredArea) {
			setReportOutputDetails(pl, SUB_RULE_40, LOADING_UNLOADING_AREA, requiredArea + " " + DcrConstants.SQMTRS,
					BigDecimal.valueOf(providedArea).setScale(2, BigDecimal.ROUND_HALF_UP) + " " + DcrConstants.SQMTRS,
					Result.Not_Accepted.getResultVal());
		} else {
			setReportOutputDetails(pl, SUB_RULE_40, LOADING_UNLOADING_AREA, requiredArea + " " + DcrConstants.SQMTRS,
					BigDecimal.valueOf(providedArea).setScale(2, BigDecimal.ROUND_HALF_UP) + " " + DcrConstants.SQMTRS,
					Result.Accepted.getResultVal());
		}
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

	// For Haryana demo
	private void processHaryanaDemo(Plan pl) {
		ScrutinyDetail scrutinyDetail1 = new ScrutinyDetail();
		scrutinyDetail1.setKey("Common_Open Parking");
		scrutinyDetail1.addColumnHeading(1, RULE_NO);
		scrutinyDetail1.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail1.addColumnHeading(3, REQUIRED);
		scrutinyDetail1.addColumnHeading(4, PROVIDED);
		scrutinyDetail1.addColumnHeading(5, STATUS);

		BigDecimal totalFrontParking = BigDecimal.ZERO;
		BigDecimal totalRearParking = BigDecimal.ZERO;

		if (pl.getParkingDetails() != null && pl.getParkingDetails().getFrontParking() != null) {
			for (Measurement measurement : pl.getParkingDetails().getFrontParking()) {
				totalFrontParking = totalFrontParking.add(measurement.getArea()).setScale(2, BigDecimal.ROUND_HALF_UP);
			}
		}

		if (pl.getParkingDetails() != null && pl.getParkingDetails().getRearParking() != null) {
			for (Measurement measurement : pl.getParkingDetails().getRearParking()) {
				totalRearParking = totalRearParking.add(measurement.getArea()).setScale(2, BigDecimal.ROUND_HALF_UP);
			}
		}

		if (totalFrontParking.compareTo(BigDecimal.ZERO) > 0)
			setReport("ODA Rule 37 (1-vi,8), 64(1-i,2)", "Open parking in front side", "NA", totalFrontParking.toString(), Result.Verify,
					scrutinyDetail1);

		if (totalRearParking.compareTo(BigDecimal.ZERO) > 0)
			setReport("ODA Rule 37 (1-vi,8), 64(1-i,2)", "Open parking in rear side", "NA", totalRearParking.toString(), Result.Verify,
					scrutinyDetail1);

		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail1);
	}

	private void updateRequiredParkingDetailsForMixedUse(Plan pl, OdishaParkingHelper helper) {
		
		if(pl.getPlanInformation().getIsProjectWorkCumResidential()) {
			helper.totalParkingRequired = pl.getVirtualBuilding().getTotalFloorArea().multiply(new BigDecimal("0.30")).setScale(2,
					BigDecimal.ROUND_HALF_UP);
			return;
		}
		
		OdishaUlbs ulb = OdishaUlbs.getUlb(pl.getThirdPartyUserTenantld());
		BigDecimal plotArea = pl.getPlot().getArea();
		
		for (OccupancyPercentage occupancyPercentage : pl.getPlanInformation().getOccupancyPercentages().values()) {
			// copy code
			String ocTypeCode = occupancyPercentage.getOccupancyCode();
			String subOcTypeCode = occupancyPercentage.getSubOccupancyCode();

			if (DxfFileConstants.OC_RESIDENTIAL.equals(ocTypeCode)) {
				// Total required
				long totalNumberOfDu = pl.getPlanInformation().getTotalNoOfDwellingUnits();
				BigDecimal totalParkingRequired = BigDecimal.ZERO;
				if (totalNumberOfDu > 4 && totalNumberOfDu <= 8) {
					totalParkingRequired = occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.20")).setScale(2,
							BigDecimal.ROUND_HALF_UP);
//					helper.totalParkingRequired = helper.totalParkingRequired
//							.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.20")).setScale(2,
//									BigDecimal.ROUND_HALF_UP));
				} else if (totalNumberOfDu > 8) {
					if (DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING.equals(subOcTypeCode)
							|| DxfFileConstants.SEMI_DETACHED.equals(subOcTypeCode)
							|| DxfFileConstants.ROW_HOUSING.equals(subOcTypeCode)) {
						totalParkingRequired = occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.30"))
								.setScale(2, BigDecimal.ROUND_HALF_UP);
//						helper.totalParkingRequired = helper.totalParkingRequired
//								.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.30"))
//										.setScale(2, BigDecimal.ROUND_HALF_UP));
					} else if (DxfFileConstants.APARTMENT_BUILDING.equals(subOcTypeCode)
							|| DxfFileConstants.HOUSING_PROJECT.equals(subOcTypeCode)
							|| DxfFileConstants.STUDIO_APARTMENTS.equals(subOcTypeCode)
							|| DxfFileConstants.DHARMASALA.equals(subOcTypeCode)
							|| DxfFileConstants.DORMITORY.equals(subOcTypeCode)
							|| DxfFileConstants.HOSTEL.equals(subOcTypeCode)
							|| DxfFileConstants.SHELTER_HOUSE.equals(subOcTypeCode)
							|| DxfFileConstants.STAFF_QAURTER.equals(subOcTypeCode)
							|| DxfFileConstants.MIXED_USE.equals(subOcTypeCode)) {
						totalParkingRequired = occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.30"))
								.setScale(2, BigDecimal.ROUND_HALF_UP);
//						helper.totalParkingRequired = helper.totalParkingRequired
//								.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.30"))
//										.setScale(2, BigDecimal.ROUND_HALF_UP));

					} else if (DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(subOcTypeCode)) {
						totalParkingRequired = occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.25"))
								.setScale(2, BigDecimal.ROUND_HALF_UP);
//						helper.totalParkingRequired = helper.totalParkingRequired
//								.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.25"))
//										.setScale(2, BigDecimal.ROUND_HALF_UP));
					}
				}

				// Roof top parking
				if (pl.getVirtualBuilding().getTotalCoverageArea().compareTo(new BigDecimal("2000")) < 0
						&& helper.roofTopParkingProvided.compareTo(BigDecimal.ZERO) > 0) {
					pl.addError("Roof top parking",
							"Roof top parking not allowed in case coverage area is less then 2000 sqm");
				}

				// EWS
				if (pl.getPlanInformation().getIsProjectOnlyLigOrEws() == null
						|| !pl.getPlanInformation().getIsProjectOnlyLigOrEws().equalsIgnoreCase("YES")) {

					if (DxfFileConstants.APARTMENT_BUILDING.equals(subOcTypeCode)
							|| DxfFileConstants.HOUSING_PROJECT.equals(subOcTypeCode)
							|| DxfFileConstants.STUDIO_APARTMENTS.equals(subOcTypeCode)
							|| DxfFileConstants.EWS.equals(subOcTypeCode)
							|| DxfFileConstants.LOW_INCOME_HOUSING.equals(subOcTypeCode)) {
						BigDecimal totalLigAndEWS = getTotalAreaOfLIGAndEWS(pl);
						if (totalLigAndEWS.compareTo(BigDecimal.ZERO) > 0) {
							helper.eWSOrLigParkingRequired = helper.eWSOrLigParkingRequired.add(totalLigAndEWS
									.multiply(new BigDecimal("0.10")).setScale(2, BigDecimal.ROUND_HALF_UP));
						}
					}

				} else {
					if (DxfFileConstants.EWS.equals(subOcTypeCode)) {

						BigDecimal totalEWSflr = getTotalAreaOfEWSFlrArea(pl);
						if (totalEWSflr.compareTo(BigDecimal.ZERO) > 0) {
							helper.totalParkingRequired = helper.totalParkingRequired.add(
									totalEWSflr.multiply(new BigDecimal("0.10")).setScale(2, BigDecimal.ROUND_HALF_UP));
						}
					} else if (DxfFileConstants.LOW_INCOME_HOUSING.equals(subOcTypeCode)) {

						BigDecimal totalLigFlr = getTotalAreaOfLIGFlrArea(pl);
						if (totalLigFlr.compareTo(BigDecimal.ZERO) > 0) {
							helper.totalParkingRequired = helper.totalParkingRequired.add(
									totalLigFlr.multiply(new BigDecimal("0.10")).setScale(2, BigDecimal.ROUND_HALF_UP));
						}

					}

				}

				// MIG
				if (DxfFileConstants.APARTMENT_BUILDING.equals(subOcTypeCode)
						|| DxfFileConstants.HOUSING_PROJECT.equals(subOcTypeCode)
						|| DxfFileConstants.STUDIO_APARTMENTS.equals(subOcTypeCode)) {
					BigDecimal totalMigAndEWS = getTotalAreaOfMIG1AndMIG2(pl);
					if (totalMigAndEWS.compareTo(BigDecimal.ZERO) > 0) {
						helper.mIGParkingRequired = helper.mIGParkingRequired.add(
								totalMigAndEWS.multiply(new BigDecimal("0.25")).setScale(2, BigDecimal.ROUND_HALF_UP));
					}
				} else if (DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(subOcTypeCode)) {

					helper.mIGParkingRequired = helper.mIGParkingRequired.add(occupancyPercentage.getTotalFloorArea()
							.multiply(new BigDecimal("0.25")).setScale(2, BigDecimal.ROUND_HALF_UP));

					if (pl.getPlanInformation().getIsProjectOnlyLigOrEws() != null
							&& pl.getPlanInformation().getIsProjectOnlyLigOrEws().equalsIgnoreCase("YES")) {
						helper.totalParkingRequired = helper.totalParkingRequired
								.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.25"))
										.setScale(2, BigDecimal.ROUND_HALF_UP));

					}
				}

				// DA parking
				if (plotArea.compareTo(new BigDecimal("2000")) > 0)
					if (totalNumberOfDu > 4) {
						helper.daPARKINGCountRequired = helper.daPARKINGCountRequired + 2;
						helper.distFromDAToMainEntranceRequired = helper.distFromDAToMainEntranceRequired
								.add(new BigDecimal("30"));
					}

				// visitor
				if (DxfFileConstants.APARTMENT_BUILDING.equals(subOcTypeCode)
						|| DxfFileConstants.HOUSING_PROJECT.equals(subOcTypeCode)
						|| DxfFileConstants.STUDIO_APARTMENTS.equals(subOcTypeCode)) {
					helper.visitorParkingRequired = helper.visitorParkingRequired.add(totalParkingRequired
							.multiply(new BigDecimal("0.10")).setScale(2, BigDecimal.ROUND_HALF_UP));

				}
				
				helper.totalParkingRequired = helper.totalParkingRequired.add(totalParkingRequired);

			}

			if (DxfFileConstants.COLD_STORAGE_AND_ICE_FACTORY.equals(subOcTypeCode)
					|| DxfFileConstants.GAS_GODOWN.equals(subOcTypeCode)
					|| DxfFileConstants.GODOWNS.equals(subOcTypeCode)
					|| DxfFileConstants.GOOD_STORAGE.equals(subOcTypeCode)
					|| DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE.equals(subOcTypeCode)
					|| DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE.equals(subOcTypeCode)
					|| DxfFileConstants.STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT.equals(subOcTypeCode)
					|| DxfFileConstants.WARE_HOUSE.equals(subOcTypeCode)) {
				BigDecimal totalParkingRequired = BigDecimal.ZERO;
				totalParkingRequired = occupancyPercentage.getTotalFloorArea()
						.multiply(new BigDecimal("0.20")).setScale(2, BigDecimal.ROUND_HALF_UP);
//				helper.totalParkingRequired = helper.totalParkingRequired.add(occupancyPercentage.getTotalFloorArea()
//						.multiply(new BigDecimal("0.20")).setScale(2, BigDecimal.ROUND_HALF_UP));
				if (plotArea.compareTo(new BigDecimal("2000")) > 0) {
					helper.daPARKINGCountRequired = helper.daPARKINGCountRequired + 2;
					helper.distFromDAToMainEntranceRequired = helper.distFromDAToMainEntranceRequired
							.add(new BigDecimal("30"));
				}
				helper.staffParkingRequired = totalParkingRequired.divide(new BigDecimal("3"), 2,
						BigDecimal.ROUND_HALF_UP);
				helper.twoWheelerParkingRequired = helper.twoWheelerParkingRequired.add(totalParkingRequired
						.multiply(new BigDecimal("0.15")).setScale(2, BigDecimal.ROUND_HALF_UP));
				helper.bicycleParkingRequired = helper.bicycleParkingRequired.add(totalParkingRequired
						.multiply(new BigDecimal("0.02")).setScale(2, BigDecimal.ROUND_HALF_UP));
				helper.totalParkingRequired = helper.totalParkingRequired.add(totalParkingRequired);
			} else if (DxfFileConstants.OC_PUBLIC_UTILITY.equals(ocTypeCode)
					|| DxfFileConstants.OC_EDUCATION.equals(ocTypeCode)
					|| DxfFileConstants.GUEST_HOUSES.equals(subOcTypeCode)
					|| DxfFileConstants.BOARDING_AND_LODGING_HOUSES.equals(subOcTypeCode)
					|| DxfFileConstants.ORPHANAGE.equals(subOcTypeCode)
					|| DxfFileConstants.OLD_AGE_HOME.equals(subOcTypeCode)
					|| DxfFileConstants.SCIENCE_CENTRE_OR_MUSEUM.equals(subOcTypeCode)
					|| DxfFileConstants.CONFERNCE_HALL.equals(subOcTypeCode)
					|| DxfFileConstants.MUSUEM.equals(subOcTypeCode)
					|| DxfFileConstants.PLACE_OF_WORKSHIP.equals(subOcTypeCode)
					|| DxfFileConstants.PUBLIC_LIBRARIES.equals(subOcTypeCode)
					|| DxfFileConstants.RECREATION_BLDG.equals(subOcTypeCode)
					|| DxfFileConstants.ADMINISTRATIVE_BUILDINGS.equals(subOcTypeCode)
					|| DxfFileConstants.GOVERNMENT_OFFICES.equals(subOcTypeCode)
					|| DxfFileConstants.LOCAL_AND_SEMI_GOVERNMENT_OFFICES.equals(subOcTypeCode)
					|| DxfFileConstants.POLICE_OR_ARMY_OR_BARRACK.equals(subOcTypeCode)
					|| DxfFileConstants.RELIGIOUS_BUILDING.equals(subOcTypeCode)
					|| DxfFileConstants.SOCIAL_AND_WELFARE_CENTRES.equals(subOcTypeCode)
					|| DxfFileConstants.CLINIC.equals(subOcTypeCode)
					|| DxfFileConstants.DISPENSARY.equals(subOcTypeCode)
					|| DxfFileConstants.YOGA_CENTER.equals(subOcTypeCode)
					|| DxfFileConstants.DIAGNOSTIC_CENTRE.equals(subOcTypeCode)
					|| DxfFileConstants.GOVT_SEMI_GOVT_HOSPITAL.equals(subOcTypeCode)
					|| DxfFileConstants.REGISTERED_TRUST.equals(subOcTypeCode)
					|| DxfFileConstants.HEALTH_CENTRE.equals(subOcTypeCode)
					|| DxfFileConstants.HOSPITAL.equals(subOcTypeCode) || DxfFileConstants.LAB.equals(subOcTypeCode)
					|| DxfFileConstants.MATERNITY_HOME.equals(subOcTypeCode)
					|| DxfFileConstants.MEDICAL_BUILDING.equals(subOcTypeCode)
					|| DxfFileConstants.NURSING_HOME.equals(subOcTypeCode)
					|| DxfFileConstants.POLYCLINIC.equals(subOcTypeCode)
					|| DxfFileConstants.REHABILITAION_CENTER.equals(subOcTypeCode)
					|| DxfFileConstants.VETERINARY_HOSPITAL_FOR_PET_ANIMALS_AND_BIRDS.equals(subOcTypeCode)
					|| DxfFileConstants.RESEARCH_AND_TRAINING_CENTER.equals(subOcTypeCode)
					|| DxfFileConstants.RESEARCH_AND_TRAINING_INSTITUTE.equals(subOcTypeCode)
					|| DxfFileConstants.POLICE_STATION.equals(subOcTypeCode)
					|| DxfFileConstants.FIRE_STATION.equals(subOcTypeCode)
					|| DxfFileConstants.JAIL_OR_PRISON.equals(subOcTypeCode)
					|| DxfFileConstants.POST_OFFICE.equals(subOcTypeCode)
					|| DxfFileConstants.INDUSTRIAL_BUILDINGS_FACTORIES_WORKSHOPS_ETC.equals(subOcTypeCode)
					|| DxfFileConstants.NON_POLLUTING_INDUSTRIAL.equals(subOcTypeCode)
					|| DxfFileConstants.SEZ_INDUSTRIAL.equals(subOcTypeCode)
					|| DxfFileConstants.LOADING_OR_UNLOADING_SPACES.equals(subOcTypeCode)
					|| DxfFileConstants.FLATTED_FACTORY.equals(subOcTypeCode)
					|| DxfFileConstants.SMALL_FACTORIES_AND_ETC_FALLS_IN_INDUSTRIAL.equals(subOcTypeCode)) {
				BigDecimal totalParkingRequired = BigDecimal.ZERO;
				if (ulb.isSparitFlag()) {
//					helper.totalParkingRequired = helper.totalParkingRequired
//							.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.25")).setScale(2,
//									BigDecimal.ROUND_HALF_UP));
					totalParkingRequired = occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.25")).setScale(2,
							BigDecimal.ROUND_HALF_UP);

				} else {
//					helper.totalParkingRequired = helper.totalParkingRequired
//							.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.30")).setScale(2,
//									BigDecimal.ROUND_HALF_UP));
					totalParkingRequired = occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.30")).setScale(2,
							BigDecimal.ROUND_HALF_UP);
				}
				if (plotArea.compareTo(new BigDecimal("2000")) > 0) {
					helper.daPARKINGCountRequired = helper.daPARKINGCountRequired + 2;
					helper.distFromDAToMainEntranceRequired = helper.distFromDAToMainEntranceRequired
							.add(new BigDecimal("30"));
				}
//				helper.staffParkingRequired = helper.staffParkingRequired
//						.add(helper.totalParkingRequired.divide(new BigDecimal("3"), 2, BigDecimal.ROUND_HALF_UP));
//				helper.twoWheelerParkingRequired = helper.staffParkingRequired.add(helper.totalParkingRequired
//						.multiply(new BigDecimal("0.15")).setScale(2, BigDecimal.ROUND_HALF_UP));
//				helper.bicycleParkingRequired = helper.bicycleParkingRequired.add(helper.totalParkingRequired
//						.multiply(new BigDecimal("0.02")).setScale(2, BigDecimal.ROUND_HALF_UP));
				helper.staffParkingRequired = helper.staffParkingRequired
						.add(totalParkingRequired.divide(new BigDecimal("3"), 2, BigDecimal.ROUND_HALF_UP));
				helper.twoWheelerParkingRequired = helper.staffParkingRequired.add(totalParkingRequired
						.multiply(new BigDecimal("0.15")).setScale(2, BigDecimal.ROUND_HALF_UP));
				helper.bicycleParkingRequired = helper.bicycleParkingRequired.add(totalParkingRequired
						.multiply(new BigDecimal("0.02")).setScale(2, BigDecimal.ROUND_HALF_UP));
				helper.totalParkingRequired = helper.totalParkingRequired.add(totalParkingRequired).setScale(2, BigDecimal.ROUND_HALF_UP);
			} else if (DxfFileConstants.HOTEL.equals(subOcTypeCode)
					|| DxfFileConstants.FIVE_STAR_HOTEL.equals(subOcTypeCode)
					|| DxfFileConstants.MOTELS.equals(subOcTypeCode) || DxfFileConstants.BANK.equals(subOcTypeCode)
					|| DxfFileConstants.RESORTS.equals(subOcTypeCode)
					|| DxfFileConstants.LAGOONS_AND_LAGOON_RESORT.equals(subOcTypeCode)
					|| DxfFileConstants.AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS.equals(subOcTypeCode)
					|| DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX.equals(subOcTypeCode)
					|| DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING.equals(subOcTypeCode)
					|| DxfFileConstants.PROFESSIONAL_OFFICES.equals(subOcTypeCode)
					|| DxfFileConstants.DEPARTMENTAL_STORE.equals(subOcTypeCode)
					|| DxfFileConstants.HOLIDAY_RESORT.equals(subOcTypeCode)
					|| DxfFileConstants.RESTAURANT.equals(subOcTypeCode)
					|| DxfFileConstants.LOCAL_RETAIL_SHOPPING.equals(subOcTypeCode)
					|| DxfFileConstants.SHOWROOM.equals(subOcTypeCode)
					|| DxfFileConstants.SUPERMARKETS.equals(subOcTypeCode)
					|| DxfFileConstants.WHOLESALE_MARKET.equals(subOcTypeCode)
					|| DxfFileConstants.MEDIA_CENTRES.equals(subOcTypeCode)
					|| DxfFileConstants.FOOD_COURTS.equals(subOcTypeCode)
					|| DxfFileConstants.MERCENTILE.equals(subOcTypeCode)
					|| DxfFileConstants.MUSIC_PAVILIONS.equals(subOcTypeCode)
					|| DxfFileConstants.COMMUNITY_HALL.equals(subOcTypeCode)
					|| DxfFileConstants.CULTURAL_COMPLEX.equals(subOcTypeCode)
					|| DxfFileConstants.SCULPTURE_COMPLEX.equals(subOcTypeCode)
					|| DxfFileConstants.EXHIBITION_CENTER.equals(subOcTypeCode)
					|| DxfFileConstants.IT_ITES_BUILDINGS.equals(subOcTypeCode)) {
				BigDecimal totalParkingRequired = BigDecimal.ZERO;
				if (ulb.isSparitFlag()) {
					totalParkingRequired = occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.30")).setScale(2,
							BigDecimal.ROUND_HALF_UP);
//					helper.totalParkingRequired = helper.totalParkingRequired
//							.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.30")).setScale(2,
//									BigDecimal.ROUND_HALF_UP));

				} else {
					if (DxfFileConstants.HOTEL.equals(subOcTypeCode)
							|| DxfFileConstants.FIVE_STAR_HOTEL.equals(subOcTypeCode)
							|| DxfFileConstants.MOTELS.equals(subOcTypeCode)
							|| DxfFileConstants.RESORTS.equals(subOcTypeCode)
							|| DxfFileConstants.HOLIDAY_RESORT.equals(subOcTypeCode)
							|| DxfFileConstants.LAGOONS_AND_LAGOON_RESORT.equals(subOcTypeCode)
							|| DxfFileConstants.AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS.equals(subOcTypeCode)) {
//						helper.totalParkingRequired = helper.totalParkingRequired
//								.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.30"))
//										.setScale(2, BigDecimal.ROUND_HALF_UP));
						totalParkingRequired = occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.30"))
								.setScale(2, BigDecimal.ROUND_HALF_UP);

					} else {
//						helper.totalParkingRequired = helper.totalParkingRequired
//								.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.40"))
//										.setScale(2, BigDecimal.ROUND_HALF_UP));
						totalParkingRequired = occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.40"))
								.setScale(2, BigDecimal.ROUND_HALF_UP);
					}

				}
				if (plotArea.compareTo(new BigDecimal("2000")) > 0) {
					helper.daPARKINGCountRequired = helper.daPARKINGCountRequired + 2;
					helper.distFromDAToMainEntranceRequired = helper.distFromDAToMainEntranceRequired
							.add(new BigDecimal("30"));
				}
//				helper.staffParkingRequired = helper.staffParkingRequired
//						.add(helper.totalParkingRequired.divide(new BigDecimal("3"), 2, BigDecimal.ROUND_HALF_UP));
//				helper.twoWheelerParkingRequired = helper.twoWheelerParkingRequired.add(helper.totalParkingRequired
//						.multiply(new BigDecimal("0.15")).setScale(2, BigDecimal.ROUND_HALF_UP));
//				helper.bicycleParkingRequired = helper.bicycleParkingRequired.add(helper.totalParkingRequired
//						.multiply(new BigDecimal("0.02")).setScale(2, BigDecimal.ROUND_HALF_UP));
				
				helper.staffParkingRequired = helper.staffParkingRequired
						.add(totalParkingRequired.divide(new BigDecimal("3"), 2, BigDecimal.ROUND_HALF_UP));
				helper.twoWheelerParkingRequired = helper.twoWheelerParkingRequired.add(totalParkingRequired
						.multiply(new BigDecimal("0.15")).setScale(2, BigDecimal.ROUND_HALF_UP));
				helper.bicycleParkingRequired = helper.bicycleParkingRequired.add(totalParkingRequired
						.multiply(new BigDecimal("0.02")).setScale(2, BigDecimal.ROUND_HALF_UP));
				
				helper.totalParkingRequired = helper.totalParkingRequired.add(totalParkingRequired);
				
			} else if (DxfFileConstants.SHOPPING_CENTER.equals(subOcTypeCode)
					|| DxfFileConstants.SHOPPING_MALL.equals(subOcTypeCode)
					|| DxfFileConstants.AUDITORIUM.equals(subOcTypeCode)
					|| DxfFileConstants.BANQUET_HALL.equals(subOcTypeCode)
					|| DxfFileConstants.CINEMA.equals(subOcTypeCode) || DxfFileConstants.CLUB.equals(subOcTypeCode)
					|| DxfFileConstants.CONVENTION_HALL.equals(subOcTypeCode)
					|| DxfFileConstants.GYMNASIA.equals(subOcTypeCode)
					|| DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP.equals(subOcTypeCode)
					|| DxfFileConstants.SPORTS_COMPLEX.equals(subOcTypeCode)
					|| DxfFileConstants.STADIUM.equals(subOcTypeCode)
					|| DxfFileConstants.THEATRE.equals(subOcTypeCode)) {
				BigDecimal totalParkingRequired = BigDecimal.ZERO;
				if (ulb.isSparitFlag()) {
//					helper.totalParkingRequired = helper.totalParkingRequired
//							.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.40")).setScale(2,
//									BigDecimal.ROUND_HALF_UP));
					totalParkingRequired = occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.40")).setScale(2,
							BigDecimal.ROUND_HALF_UP);
				} else {
//					helper.totalParkingRequired = helper.totalParkingRequired
//							.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.50")).setScale(2,
//									BigDecimal.ROUND_HALF_UP));
					totalParkingRequired = occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.50")).setScale(2,
							BigDecimal.ROUND_HALF_UP);
				}
				if (plotArea.compareTo(new BigDecimal("2000")) > 0) {
					helper.daPARKINGCountRequired = helper.daPARKINGCountRequired + 2;
					helper.distFromDAToMainEntranceRequired = helper.distFromDAToMainEntranceRequired
							.add(new BigDecimal("30"));
				}
//				helper.staffParkingRequired = helper.staffParkingRequired
//						.add(helper.totalParkingRequired.divide(new BigDecimal("3"), 2, BigDecimal.ROUND_HALF_UP));
//				helper.twoWheelerParkingRequired = helper.twoWheelerParkingRequired.add(helper.totalParkingRequired
//						.multiply(new BigDecimal("0.15")).setScale(2, BigDecimal.ROUND_HALF_UP));
//				helper.bicycleParkingRequired = helper.bicycleParkingRequired.add(helper.totalParkingRequired
//						.multiply(new BigDecimal("0.02")).setScale(2, BigDecimal.ROUND_HALF_UP));
				
				helper.staffParkingRequired = helper.staffParkingRequired
						.add(totalParkingRequired.divide(new BigDecimal("3"), 2, BigDecimal.ROUND_HALF_UP));
				helper.twoWheelerParkingRequired = helper.twoWheelerParkingRequired.add(totalParkingRequired
						.multiply(new BigDecimal("0.15")).setScale(2, BigDecimal.ROUND_HALF_UP));
				helper.bicycleParkingRequired = helper.bicycleParkingRequired.add(totalParkingRequired
						.multiply(new BigDecimal("0.02")).setScale(2, BigDecimal.ROUND_HALF_UP));
				
				helper.totalParkingRequired = helper.totalParkingRequired.add(totalParkingRequired);
			}

			// Roof top parking
			if (pl.getVirtualBuilding().getTotalCoverageArea().compareTo(new BigDecimal("2000")) < 0
					&& helper.roofTopParkingProvided.compareTo(BigDecimal.ZERO) > 0) {
				pl.addError("Roof top parking",
						"Roof top parking not allowed in case coverage area is less then 2000 sqm");
			}

			// staff parking not required to validate
			if (DxfFileConstants.OC_PUBLIC_UTILITY.equals(ocTypeCode)) {
				helper.staffParkingRequired = helper.staffParkingRequired.add(BigDecimal.ZERO);
			}

			// Two parking & bicycleParkingRequired not required to validate
			if (DxfFileConstants.OC_PUBLIC_UTILITY.equals(ocTypeCode)
					|| DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(ocTypeCode)
					|| DxfFileConstants.OC_EDUCATION.equals(ocTypeCode)) {
				helper.twoWheelerParkingRequired = helper.twoWheelerParkingRequired.add(BigDecimal.ZERO);
				helper.bicycleParkingRequired = helper.bicycleParkingRequired.add(BigDecimal.ZERO);
			}

			if (DxfFileConstants.OC_AGRICULTURE.equals(ocTypeCode)) {
				BigDecimal totalParkingRequired = BigDecimal.ZERO;
				long totalNumberOfDu = pl.getPlanInformation().getTotalNoOfDwellingUnits();
				if (totalNumberOfDu > 4 && totalNumberOfDu <= 8) {
//					helper.totalParkingRequired = helper.totalParkingRequired
//							.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.20")).setScale(2,
//									BigDecimal.ROUND_HALF_UP));
					totalParkingRequired = totalParkingRequired
							.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.20")).setScale(2,
									BigDecimal.ROUND_HALF_UP));
				} else if (totalNumberOfDu > 8) {
//					helper.totalParkingRequired = helper.totalParkingRequired
//							.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.30")).setScale(2,
//									BigDecimal.ROUND_HALF_UP));
					totalParkingRequired = totalParkingRequired
							.add(occupancyPercentage.getTotalFloorArea().multiply(new BigDecimal("0.30")).setScale(2,
									BigDecimal.ROUND_HALF_UP));
				}
				
				helper.totalParkingRequired = helper.totalParkingRequired.add(totalParkingRequired);

				// DA parking
				if (totalNumberOfDu > 4) {
					helper.daPARKINGCountRequired = helper.daPARKINGCountRequired + 2;
					helper.distFromDAToMainEntranceRequired = helper.distFromDAToMainEntranceRequired
							.add(new BigDecimal("30"));
				}

			}

		}
	}
	
	private void validateEvChargingPoint(OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.evChargingPointRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.evChargingPointRequired.toString();
		if (helper.evChargingPointProvided.compareTo(helper.evChargingPointRequired) >= 0)
			setReport("ODA Rule 37 (15)", EV_CHARGING_POINT_DESC, required, helper.evChargingPointProvided.toString(),
					Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Rule 37 (15)", EV_CHARGING_POINT_DESC, required, helper.evChargingPointProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
	}
	
	private void validateStackParking(Plan pl,OdishaParkingHelper helper, ScrutinyDetail scrutinyDetail) {
		String required = DxfFileConstants.NA;
		if (helper.stackParkingRequired.compareTo(BigDecimal.ZERO) > 0)
			required = helper.stackParkingRequired.toString();
		if (helper.stackParkingProvided.compareTo(helper.stackParkingRequired) >= 0)
			setReport("ODA Rule 37 (2-vi)", STACK_PARKING_DESC, required, helper.stackParkingProvided.toString(),
					Result.Accepted, scrutinyDetail);
		else
			setReport("ODA Rule 37 (2-vi)", STACK_PARKING_DESC, required, helper.stackParkingProvided.toString(),
					Result.Not_Accepted, scrutinyDetail);
		
		if(helper.stackParkingCountProvided>0)
			addStackParkingTable(pl,helper);
		
	}
	
	private void addStackParkingTable(Plan pl,OdishaParkingHelper helper) {
		
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Stack Parking");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		
		if (helper.stackParkingProvided.compareTo(BigDecimal.ZERO) > 0) {
			String required = DxfFileConstants.NA;
			if (helper.stackParkingRequired.compareTo(BigDecimal.ZERO) > 0)
				required = helper.stackParkingRequired.toString();
			if (helper.stackParkingProvided.compareTo(helper.stackParkingRequired) >= 0)
				setReport("ODA Rule 37 (2-vi)", STACK_PARKING_DESC, required, helper.stackParkingProvided.toString(),
						Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 37 (2-vi)", STACK_PARKING_DESC, required, helper.stackParkingProvided.toString(),
						Result.Not_Accepted, scrutinyDetail);
		}
		
		if(helper.stackParkingCountProvided>0) {
			String required = DxfFileConstants.NA;
			if (helper.stackParkingCountRequired> 0)
				required = helper.stackParkingCountRequired+"";
			if (helper.stackParkingCountProvided >= helper.stackParkingCountRequired)
				setReport("NA", STACK_PARKING_COUNT_DESC, required, helper.stackParkingCountProvided+"",
						Result.Accepted, scrutinyDetail);
			else
				setReport("NA", STACK_PARKING_COUNT_DESC, required, helper.stackParkingCountProvided+"",
						Result.Not_Accepted, scrutinyDetail);
		}
		
		
		
		if(helper.stackParkingCountProvided>0)
			validateStackParkingDimension(pl);
		
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}
	
	private void validateStackParkingDimension(Plan pl) {
		if(pl.getParkingDetails()!=null &&  pl.getParkingDetails().getMechParking()!=null
				&& pl.getParkingDetails().getMechParking().size()>0) {
			for(Measurement measurement:pl.getParkingDetails().getMechParking()) {
				if(!(measurement.getLength().compareTo(new BigDecimal("5"))>=0 
						&& measurement.getWidth().compareTo(new BigDecimal("3"))>=0)) {
					pl.addError("STACK_PARKING_Dimension_Error", "Length and width of each stack parking should be minimum 5m and 3m.");
					break;
				}
			}
		}
	}
	
	private void validateParkingUnits(Plan pl) {
		
		
		if (pl.getParkingDetails()!=null 
				&& pl.getParkingDetails().getParkingUnits() !=null
				&& pl.getParkingDetails().getParkingUnits().size()>0) {
			for(ParkingUnit pUnit : pl.getParkingDetails().getParkingUnits()) {
				if (!(pUnit.getLength().compareTo(BigDecimal.valueOf(5))>=0
						&& pUnit.getWidth().compareTo(BigDecimal.valueOf(2.75))>=0)) {
					pl.addError("Parking_Unit_Dimension_Error", "Length and width of each parking unit should be minimum 5m and 2.75m.");
				}
			}
		}
		
	}
	
	private void parkingUnitsInReport(Plan pl, OdishaParkingHelper helper) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Parking Units Count");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		
		validateTotalParkingUnits(helper, scrutinyDetail);
		validateOpenParkingUnits(helper, scrutinyDetail);
		validateBasementParkingUnits(helper, scrutinyDetail);
		validateStiltParkingUnits(helper, scrutinyDetail);
		validateRooftopParkingUnits(helper, scrutinyDetail);

		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}
}
