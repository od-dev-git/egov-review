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

import static org.egov.edcr.constants.DxfFileConstants.ENVIRONMENTALLY_SENSITIVE_ZONE;
import static org.egov.edcr.constants.DxfFileConstants.OPEN_SPACE_USE_ZONE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.OccupancyType;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.NOCConstants;
import org.egov.edcr.constants.OdishaUlbs;
import org.egov.edcr.od.NocAndDocumentsUtill;
import org.egov.edcr.od.OdishaUtill;
import org.egov.edcr.utility.DcrConstants;
import org.egov.infra.utils.StringUtils;
import org.kabeja.dxf.DXFConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Coverage extends FeatureProcess {
	// private static final String OCCUPANCY2 = "OCCUPANCY";

	private static final Logger LOG = Logger.getLogger(Coverage.class);

	// private static final String RULE_NAME_KEY = "coverage.rulename";
	private static final String RULE_DESCRIPTION_KEY = "coverage.description";
	private static final String RULE_EXPECTED_KEY = "coverage.expected";
	private static final String RULE_ACTUAL_KEY = "coverage.actual";
	// private static final BigDecimal ThirtyFive = BigDecimal.valueOf(35);
	private static final BigDecimal Forty = BigDecimal.valueOf(40);
	
	@Value("${cwwcFeeFlag.mandatory:false}")
    private boolean isCwwcFeeMandatoryFlag;
	
	/*
	 * private static final BigDecimal FortyFive = BigDecimal.valueOf(45); private
	 * static final BigDecimal Sixty = BigDecimal.valueOf(60); private static final
	 * BigDecimal SixtyFive = BigDecimal.valueOf(65); private static final
	 * BigDecimal Seventy = BigDecimal.valueOf(70); private static final BigDecimal
	 * SeventyFive = BigDecimal.valueOf(75); private static final BigDecimal Eighty
	 * = BigDecimal.valueOf(80);
	 */
	public static final String RULE_38 = "ODA Rule 33(1), 50(7),57(1),59(1),60(1,5-i), 61(6), 67(6,8), 68(2), 69(2), 90(6)";
	private static final BigDecimal ROAD_WIDTH_TWELVE_POINTTWO = BigDecimal.valueOf(12.2);
	private static final BigDecimal ROAD_WIDTH_THIRTY_POINTFIVE = BigDecimal.valueOf(30.5);

	@Override
	public Plan validate(Plan pl) {
		List<Block> list = pl.getBlocks();
		list.addAll(pl.getOuthouse());
		for (Block block : list) {
			if (block.getCoverage().isEmpty()) {
				pl.addError("coverageArea" + block.getNumber(),
						"Coverage Area for block " + block.getNumber() + " not Provided");
			}
		}
		OdishaUtill.additionalValidation(pl);
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		init(pl);
		validate(pl);
		BigDecimal totalCoverage = BigDecimal.ZERO;
		BigDecimal totalCoverageArea = BigDecimal.ZERO;
		//LOG.info("use zones:"+pl.getPlanInformation().getLandUseZone());	
		totalCoverageArea = updatedAmmenityArea(pl);
		List<Block> blocks = new ArrayList<>();
		blocks.addAll(pl.getBlocks());
		blocks.addAll(pl.getOuthouse());
		for (Block block : pl.getBlocks()) {
			BigDecimal coverageAreaWithoutDeduction = BigDecimal.ZERO;
			BigDecimal coverageDeductionArea = BigDecimal.ZERO;

			for (Measurement coverage : block.getCoverage()) {
				coverageAreaWithoutDeduction = coverageAreaWithoutDeduction.add(coverage.getArea());
			}
			for (Measurement deduct : block.getCoverageDeductions()) {
				coverageDeductionArea = coverageDeductionArea.add(deduct.getArea());
			}
			if (block.getBuilding() != null) {
				block.getBuilding().setCoverageArea(coverageAreaWithoutDeduction.subtract(coverageDeductionArea));
				BigDecimal coverage = BigDecimal.ZERO;
				if (pl.getPlot().getArea().doubleValue() > 0)
					coverage = block.getBuilding().getCoverageArea().multiply(BigDecimal.valueOf(100)).divide(
							pl.getPlanInformation().getPlotArea(), DcrConstants.DECIMALDIGITS_MEASUREMENTS,
							DcrConstants.ROUNDMODE_MEASUREMENTS);

				block.getBuilding().setCoverage(coverage);

				totalCoverageArea = totalCoverageArea.add(block.getBuilding().getCoverageArea());
				// totalCoverage =
				// totalCoverage.add(block.getBuilding().getCoverage());
			}

		}

		// pl.setCoverageArea(totalCoverageArea);
		// use plotBoundaryArea
		if (pl.getPlot() != null && pl.getPlot().getArea().doubleValue() > 0)
			totalCoverage = totalCoverageArea.multiply(BigDecimal.valueOf(100)).divide(
					pl.getPlanInformation().getPlotArea(), DcrConstants.DECIMALDIGITS_MEASUREMENTS,
					DcrConstants.ROUNDMODE_MEASUREMENTS);
		pl.setCoverage(totalCoverage);
		if (pl.getVirtualBuilding() != null) {
			pl.getVirtualBuilding().setTotalCoverageArea(totalCoverageArea);
		}

		BigDecimal roadWidth = pl.getPlanInformation().getTotalRoadWidth();
		
		//LOG.info("use zones:"+pl.getPlanInformation().getLandUseZone());
		BigDecimal requiredCoverage = getPermissibleGroundCoverage(pl);
		// if(requiredCoverage.compareTo(BigDecimal.ZERO)>0)
		processCoverage(pl, StringUtils.EMPTY, totalCoverage, requiredCoverage);

		return pl;
	}

	private BigDecimal updatedAmmenityArea(Plan pl) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Amenity in open space");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		BigDecimal totalArea = BigDecimal.ZERO;
		String required = "Ground Coverage Exemption till 10 sqm";
		if (pl.getAmmenity().getGuardRooms().size() > 0) {
			BigDecimal guardRoomArea = pl.getAmmenity().getGuardRooms().stream().map(m -> m.getArea())
					.reduce(BigDecimal::add).get();
			guardRoomArea = guardRoomArea.setScale(2, BigDecimal.ROUND_HALF_UP);
			if (guardRoomArea.compareTo(new BigDecimal("10")) > 0)
				totalArea = totalArea.add(guardRoomArea);
			addDetails(scrutinyDetail, "ODA Rule 40(5), Annexure II(2-iv)", "Guard room", required, guardRoomArea.toString(),
					Result.Accepted.getResultVal());
		}
		if (pl.getAmmenity().getElectricCabins().size() > 0) {
			BigDecimal electricCabinArea = pl.getAmmenity().getElectricCabins().stream().map(m -> m.getArea())
					.reduce(BigDecimal::add).get();
			electricCabinArea = electricCabinArea.setScale(2, BigDecimal.ROUND_HALF_UP);
			if (electricCabinArea.compareTo(new BigDecimal("10")) > 0)
				totalArea = totalArea.add(electricCabinArea);
			addDetails(scrutinyDetail, "ODA Rule 35(7-ii), 40(5)", "Electric cabin", required, electricCabinArea.toString(),
					Result.Accepted.getResultVal());
		}
		if (pl.getAmmenity().getSubStations().size() > 0) {
			BigDecimal subStationArea = pl.getAmmenity().getSubStations().stream().map(m -> m.getArea())
					.reduce(BigDecimal::add).get();
			subStationArea = subStationArea.setScale(2, BigDecimal.ROUND_HALF_UP);
			if (subStationArea.compareTo(new BigDecimal("10")) > 0)
				totalArea = totalArea.add(subStationArea);
			addDetails(scrutinyDetail, "ODA Rule 35(7-ii), 75(4-vii), Annexure IX(7-vi)", "Sub-Station", required, subStationArea.toString(),
					Result.Accepted.getResultVal());
		}
		if (pl.getAmmenity().getAreaForGeneratorSet().size() > 0) {
			BigDecimal AreaForGeneratorSetArea = pl.getAmmenity().getAreaForGeneratorSet().stream()
					.map(m -> m.getArea()).reduce(BigDecimal::add).get();
			AreaForGeneratorSetArea = AreaForGeneratorSetArea.setScale(2, BigDecimal.ROUND_HALF_UP);
			if (AreaForGeneratorSetArea.compareTo(new BigDecimal("10")) > 0)
				totalArea = totalArea.add(AreaForGeneratorSetArea);
			addDetails(scrutinyDetail, "ODA Rule 35(11), 40(5)", "Area for generator set", required, AreaForGeneratorSetArea.toString(),
					Result.Accepted.getResultVal());
		}
		if (pl.getAmmenity().getAtms().size() > 0) {
			BigDecimal atmArea = pl.getAmmenity().getAtms().stream().map(m -> m.getArea()).reduce(BigDecimal::add)
					.get();
			atmArea = atmArea.setScale(2, BigDecimal.ROUND_HALF_UP);
			if (atmArea.compareTo(new BigDecimal("10")) > 0)
				totalArea = totalArea.add(atmArea);
			addDetails(scrutinyDetail, "ODA Rule 40(5), Annexure II(2-I,iii,v)", "ATM", required, atmArea.toString(), Result.Accepted.getResultVal());
		}
		if (pl.getAmmenity().getOtherAmmenities().size() > 0) {
			BigDecimal otherAmmenitieArea = pl.getAmmenity().getOtherAmmenities().stream().map(m -> m.getArea())
					.reduce(BigDecimal::add).get();
			otherAmmenitieArea = otherAmmenitieArea.setScale(2, BigDecimal.ROUND_HALF_UP);
			if (otherAmmenitieArea.compareTo(new BigDecimal("10")) > 0)
				totalArea = totalArea.add(otherAmmenitieArea);
			addDetails(scrutinyDetail, "ODA Rule 71(3)", "Other Ammenities", required, otherAmmenitieArea.toString(),
					Result.Accepted.getResultVal());
		}
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		return totalArea;
	}

	private void addDetails(ScrutinyDetail scrutinyDetail, String rule, String description, String required,
			String provided, String status) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, rule);
		details.put(DESCRIPTION, description);
		details.put(REQUIRED, required);
		details.put(PROVIDED, provided);
		details.put(STATUS, status);
		scrutinyDetail.getDetail().add(details);
	}

	private void processCoverage(Plan pl, String occupancy, BigDecimal coverage, BigDecimal upperLimit) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Coverage");
		scrutinyDetail.setHeading("Coverage in Percentage");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		// scrutinyDetail.addColumnHeading(3, OCCUPANCY);
		scrutinyDetail.addColumnHeading(3, PERMISSIBLE);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);

		String desc = getLocaleMessage(RULE_DESCRIPTION_KEY, upperLimit.toString());
		String actualResult = getLocaleMessage(RULE_ACTUAL_KEY, coverage.toString());
		String expectedResult = getLocaleMessage(RULE_EXPECTED_KEY, upperLimit.toString());

		String serviceType = pl.getPlanInformation().getServiceType();

		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, RULE_38);
		details.put(DESCRIPTION, "Coverage");

		details.put(PROVIDED, coverage.toString());
		
		//TODO: 11 City level customization for Puri code starts here.
		try {
			if (pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea() != null
					&& !pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea().equals(DxfFileConstants.NA)
					&& Integer.parseInt(pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea()) == 3
					&& !pl.getThirdPartyUserTenantld().equals("od.nimapara")) {

				if (pl.getThirdPartyUserTenantld().equals("od.puri")
						|| pl.getThirdPartyUserTenantld().equals("od.purikonarkdevelopmentauthority")
						|| pl.getThirdPartyUserTenantld().equals("od.konark")) {

					details.put(PERMISSIBLE, "Max 33");
					BigDecimal THIRTYTHREE = BigDecimal.valueOf(33);
					if (coverage.doubleValue() > THIRTYTHREE.doubleValue()) {
						details.put(STATUS, Result.Not_Accepted.getResultVal());
					} else {
						details.put(STATUS, Result.Accepted.getResultVal());
					}
				}
			} else {
		// City level customization for Puri code ends here.
		
		if (DxfFileConstants.NEW_CONSTRUCTION.equals(serviceType)) {
			if (upperLimit.compareTo(BigDecimal.ZERO) > 0) {
				details.put(PERMISSIBLE, upperLimit.toString());
				if (coverage.doubleValue() <= upperLimit.doubleValue()) {
					details.put(STATUS, Result.Accepted.getResultVal());
				} else {
					details.put(STATUS, Result.Not_Accepted.getResultVal());
				}

			} else {
				details.put(PERMISSIBLE, DxfFileConstants.NA);
				details.put(STATUS, Result.Accepted.getResultVal());
			}
		}else if(DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType)) {
			if (upperLimit.compareTo(BigDecimal.ZERO) > 0) {
				details.put(PERMISSIBLE, upperLimit.toString());
				if (coverage.doubleValue() <= upperLimit.doubleValue()) {
					details.put(STATUS, Result.Accepted.getResultVal());
				} else {
					details.put(STATUS, Result.Not_Accepted.getResultVal());
				}

			} else {
				details.put(PERMISSIBLE, DxfFileConstants.NA);
				details.put(STATUS, Result.Accepted.getResultVal());
			}
		}
	}
} catch (Exception e) {
	e.printStackTrace();
}

		scrutinyDetail.getDetail().add(details);
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		
		
		ScrutinyDetail scrutinyDetail2 = new ScrutinyDetail();
		scrutinyDetail2.setKey("Common_EIDP Details");
		scrutinyDetail2.setHeading("EIDP Details");
		scrutinyDetail2.addColumnHeading(1, RULE_NO);
		scrutinyDetail2.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail2.addColumnHeading(3, PROVIDED);
		scrutinyDetail2.addColumnHeading(4, STATUS);
		
		Map<String, String> details2 = new HashMap<>();
		details2.put(RULE_NO, "");
		details2.put(DESCRIPTION, "Is the EIDP fee exempted for the project?");
		details2.put(PROVIDED, pl.getPlanInformation().getIsEidpFeeExemptedForTheProject());
		details2.put(STATUS, Result.Verify.getResultVal());
		
		scrutinyDetail2.getDetail().add(details2);
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail2);
		
		if(isCwwcFeeMandatoryFlag) {
			ScrutinyDetail scrutinyDetail3 = new ScrutinyDetail();
			scrutinyDetail3.setKey("Common_CWWC Fee Details");
			scrutinyDetail3.setHeading("CWWC Fee Details");
			scrutinyDetail3.addColumnHeading(1, RULE_NO);
			scrutinyDetail3.addColumnHeading(2, DESCRIPTION);
			scrutinyDetail3.addColumnHeading(3, PROVIDED);
			scrutinyDetail3.addColumnHeading(4, STATUS);
			
			Map<String, String> details3 = new HashMap<>();
			details3.put(RULE_NO, "");
			details3.put(DESCRIPTION, "Is the CWWC Fee already paid?");
			details3.put(PROVIDED, pl.getPlanInformation().getIsTheCwwcFeeAlreadyPaid());
			details3.put(STATUS, Result.Verify.getResultVal());
			
			scrutinyDetail3.getDetail().add(details3);
			pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail3);
		}
		
		
		

	}

	protected OccupancyType getMostRestrictiveCoverage(EnumSet<OccupancyType> distinctOccupancyTypes) {

		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_B1))
			return OccupancyType.OCCUPANCY_B1;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_B2))
			return OccupancyType.OCCUPANCY_B2;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_B3))
			return OccupancyType.OCCUPANCY_B3;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_D))
			return OccupancyType.OCCUPANCY_D;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_D1))
			return OccupancyType.OCCUPANCY_D1;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_I2))
			return OccupancyType.OCCUPANCY_I2;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_I1))
			return OccupancyType.OCCUPANCY_I1;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_C))
			return OccupancyType.OCCUPANCY_C;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_A1))
			return OccupancyType.OCCUPANCY_A1;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_A4))
			return OccupancyType.OCCUPANCY_A4;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_A2))
			return OccupancyType.OCCUPANCY_A2;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_G1))
			return OccupancyType.OCCUPANCY_G1;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_E))
			return OccupancyType.OCCUPANCY_E;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_F))
			return OccupancyType.OCCUPANCY_F;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_F4))
			return OccupancyType.OCCUPANCY_F4;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_G2))
			return OccupancyType.OCCUPANCY_G2;
		if (distinctOccupancyTypes.contains(OccupancyType.OCCUPANCY_H))
			return OccupancyType.OCCUPANCY_H;

		else
			return null;
	}

	private BigDecimal getPermissibleGroundCoverage(Plan pl) {
		//LOG.info("use zone1:"+pl.getPlanInformation().getLandUseZone());
		
		OdishaUlbs ulb = OdishaUlbs.getUlb(pl.getThirdPartyUserTenantld());
		
		updateSpecialBuilding(pl);

		if (checkLowRiskBuildingCriteria(pl))
			return BigDecimal.ZERO;

		BigDecimal maxPermissibleGroundCoverage = BigDecimal.ZERO;
		
		//LOG.info("use zone10:");	
		switch (pl.getPlanInformation().getLandUseZone()) {
			//OPEN SPACE USE ZONE
			
		case OPEN_SPACE_USE_ZONE:
			//SPARIT AND ODA BOTH
//			if(ulb.isSparitFlag() && getPublicOpenSpace(pl).compareTo(new BigDecimal("40")) < 0) {
//				maxPermissibleGroundCoverage = new BigDecimal("50");	
//			}
			
			//LOG.info("use zone11:");
			if (getPublicOpenSpace(pl).compareTo(new BigDecimal("40")) < 0) { 
				maxPermissibleGroundCoverage = new BigDecimal("50");
				//LOG.info("Check Sprit LUZ: "+maxPermissibleGroundCoverage);
			}
			    break;
			    
			    
		case ENVIRONMENTALLY_SENSITIVE_ZONE:
			maxPermissibleGroundCoverage = new BigDecimal("40");
			break;
		}

		if (maxPermissibleGroundCoverage.compareTo(BigDecimal.ZERO) > 0)
			return maxPermissibleGroundCoverage;

		// General Criteria
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
//		System.out.println(occupancyTypeHelper);
//		System.out.println(occupancyTypeHelper + " - " + occupancyTypeHelper.getSubtype());
//		System.out.println(occupancyTypeHelper + " - " + occupancyTypeHelper.getSubtype() + " - "
//				+ occupancyTypeHelper.getSubtype().getCode());
		if (DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION
						.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.CNG_MOTHER_STATION.equals(occupancyTypeHelper.getSubtype().getCode()))
			maxPermissibleGroundCoverage = new BigDecimal("20");
		else if (DxfFileConstants.FARM_HOUSE.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.COUNTRY_HOMES.equals(occupancyTypeHelper.getSubtype().getCode()))
			maxPermissibleGroundCoverage = new BigDecimal("15");
		else
			maxPermissibleGroundCoverage = getGeneralCriteria(pl);

		return maxPermissibleGroundCoverage;
	}

	private BigDecimal getGeneralCriteria(Plan pl) {
		BigDecimal buildingHeight = OdishaUtill.getMaxBuildingHeight(pl);
		BigDecimal maxPermissibleGroundCoverage = BigDecimal.ZERO;
		if (buildingHeight.compareTo(new BigDecimal("15")) < 0)
			maxPermissibleGroundCoverage = BigDecimal.ZERO;
		else if (buildingHeight.compareTo(new BigDecimal("15")) >= 0
				&& buildingHeight.compareTo(new BigDecimal("18")) < 0)
			maxPermissibleGroundCoverage = new BigDecimal("50");
		else if (buildingHeight.compareTo(new BigDecimal("18")) >= 0
				&& buildingHeight.compareTo(new BigDecimal("40")) <= 0)
			maxPermissibleGroundCoverage = new BigDecimal("40");
		else
			maxPermissibleGroundCoverage = new BigDecimal("40");
		return maxPermissibleGroundCoverage;
	}

	private BigDecimal getPublicOpenSpace(Plan pl) {
		BigDecimal totalPublicOpenSace = BigDecimal.ZERO;
		BigDecimal inPercentage = BigDecimal.ZERO;
		for (Block block : pl.getBlocks()) {
			for (Measurement measurement : block.getPlantationGreenStripes()) {
				totalPublicOpenSace = totalPublicOpenSace.add(measurement.getArea());
				////LOG.info(inPercentage);
			}
		}
		//LOG.info("totalPublicOpenSace"+totalPublicOpenSace);
		inPercentage = totalPublicOpenSace.divide(pl.getPlot().getArea()).multiply(new BigDecimal("100"));
		return inPercentage;
	}

	private static void init(Plan pl) {
		OdishaUtill.updateDUnitInPlan(pl);
		OdishaUtill.updateAmmenity(pl);
		OdishaUtill.updateBlock(pl);
		OdishaUtill.setPlanInfoBlkWise(pl, DxfFileConstants.NUMBER_OF_OCCUPANTS_OR_USERS_OR_BED_BLK);
		//OdishaUtill.validateRestricatedOccupancies(pl);
	}

	private boolean checkLowRiskBuildingCriteria(Plan pl) {
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		boolean isLowRiskBuilding = false;
		//LOG.info("inside low risk:");
		// TODO: 13 updated low risk building criteria.
		if (!(DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.COLD_STORAGE_AND_ICE_FACTORY.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.GAS_GODOWN.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.GODOWNS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.GOOD_STORAGE.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT
						.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.WARE_HOUSE.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.WHOLESALE_MARKET.equals(occupancyTypeHelper.getSubtype().getCode())
				|| (pl.getPlanInformation().getBuildingUnderHazardousOccupancyCategory() != null
						&& pl.getPlanInformation().getBuildingUnderHazardousOccupancyCategory()
								.equalsIgnoreCase(DxfFileConstants.YES))
				|| (pl.getPlanInformation().getSpecialBuilding() != null
						&& pl.getPlanInformation().getSpecialBuilding().equalsIgnoreCase(DxfFileConstants.YES))
				|| pl.getPlanInformation().isAssemblyBuilding())) {

			List<String> nocList = NocAndDocumentsUtill.updateNoc(pl);
			if (!nocList.contains(NOCConstants.FIRE_SERVICE_NOC)) {

				if (pl.getPlot().getArea().compareTo(new BigDecimal("500")) <= 0 && DxfFileConstants.YES
						.equalsIgnoreCase(pl.getPlanInformation().getApprovedLayoutDeclaration())) {
					if (!checkIsBeasment(pl)
							&& OdishaUtill.getMaxBuildingHeight(pl).compareTo(new BigDecimal("10")) <= 0) {
						isLowRiskBuilding = true;
					}
				}
			}
		}
		pl.getPlanInformation().setLowRiskBuilding(isLowRiskBuilding);

		if (isLowRiskBuilding) {
			//LOG.info("inside low risk:true");
			pl.getPlanInformation().setRiskType(DxfFileConstants.LOW);
			pl.getPlanInformation().setRiskTypeDes(DxfFileConstants.LOW);
		} else {
			//LOG.info("inside low risk:false");
			pl.getPlanInformation().setRiskType(DxfFileConstants.HIGH);
			pl.getPlanInformation().setRiskTypeDes(DxfFileConstants.OTHER_THAN_LOW);
		}

		return isLowRiskBuilding;
	}

	private boolean checkIsBeasment(Plan pl) {
		boolean flage = false;
		if (pl.getBlocks() != null) {
			for (Block b : pl.getBlocks()) {
				if (b.getBuilding() != null && b.getBuilding().getFloors() != null
						&& !b.getBuilding().getFloors().isEmpty()) {
					for (Floor f : b.getBuilding().getFloors()) {
						if (f.getNumber() == -1) {
							flage = true;
							break;
						}

					}
				}
				if (flage)
					break;
			}
		}
		return flage;
	}
	
	private static void updateSpecialBuilding(Plan pl) {
		boolean specialBuilding = false;

		boolean isAssemblyBuilding = false;
		for (Block block : pl.getBlocks()) {
			if (block.isAssemblyBuilding()) {
				isAssemblyBuilding = true;
				break;
			}
		}

		boolean isHazardousBuildings = false;
		if (DxfFileConstants.YES.equals(pl.getPlanInformation().getBuildingUnderHazardousOccupancyCategory()))
			isHazardousBuildings = true;

		boolean isBuildingCentrallyAirConditioned = false;
		if (DxfFileConstants.YES.equals(pl.getPlanInformation().getBuildingCentrallyAirConditioned())) {
			if (pl.getVirtualBuilding().getTotalBuitUpArea().compareTo(new BigDecimal("500")) > 0)
				isBuildingCentrallyAirConditioned = true;
		}

		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		boolean isSplOccupancy = false;
		if (DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.WHOLESALE_MARKET.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.HOTEL.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.FIVE_STAR_HOTEL.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.MIXED_USE.equals(occupancyTypeHelper.getSubtype().getCode()))
			isSplOccupancy = true;

		boolean isMixedOccupancies = false;// need to add condition

		if (isAssemblyBuilding || isHazardousBuildings || isBuildingCentrallyAirConditioned || isSplOccupancy
				|| isMixedOccupancies)
			specialBuilding = true;

		pl.getPlanInformation().setSpecialBuilding(specialBuilding ? DxfFileConstants.YES : DxfFileConstants.NO);
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}
