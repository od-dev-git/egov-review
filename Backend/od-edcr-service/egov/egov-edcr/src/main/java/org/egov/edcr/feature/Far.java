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
import static org.egov.edcr.constants.DxfFileConstants.A_R;
import static org.egov.edcr.constants.DxfFileConstants.A_SA;
import static org.egov.edcr.constants.DxfFileConstants.F;
import static org.egov.edcr.constants.DxfFileConstants.G;
import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED;
import static org.egov.edcr.utility.DcrConstants.PLOT_AREA;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.egov.common.entity.ApplicationType;
import org.egov.common.entity.dcr.helper.OccupancyHelperDetail;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Building;
import org.egov.common.entity.edcr.FarDetails;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.FloorUnit;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyPercentage;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.ServiceFloor;
import org.egov.common.entity.edcr.StiltFloor;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaMixedUseUtill;
import org.egov.edcr.od.OdishaUtill;
import org.egov.edcr.service.ProcessPrintHelper;
import org.egov.edcr.utility.DcrConstants;
import org.egov.infra.microservice.models.TechnicalPersonRole;
import org.egov.infra.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class Far extends FeatureProcess {

	private static final Logger LOG = Logger.getLogger(Far.class);

	private static final String VALIDATION_NEGATIVE_FLOOR_AREA = "msg.error.negative.floorarea.occupancy.floor";
	private static final String VALIDATION_NEGATIVE_EXISTING_FLOOR_AREA = "msg.error.negative.existing.floorarea.occupancy.floor";
	private static final String VALIDATION_NEGATIVE_BUILTUP_AREA = "msg.error.negative.builtuparea.occupancy.floor";
	private static final String VALIDATION_NEGATIVE_EXISTING_BUILTUP_AREA = "msg.error.negative.existing.builtuparea.occupancy.floor";
	public static final String RULE_31_1 = "31-1";
	public static final String RULE_38 = "ODA Rule 35, 36(3,4), 40(4), 43(vi), 50(7), 57(1), 59(ii), 61(3,6), 67(6-ii), 68(2), 69(2), 74(7-ii), 75(4-viii),76(4-ii), 90(3-ii & iii,6), Annexure II(2-vi)";

	private static final BigDecimal POINTTWO = BigDecimal.valueOf(0.2);
	private static final BigDecimal POINTTHREETHREE = BigDecimal.valueOf(0.33);
	private static final BigDecimal POINTFOUR = BigDecimal.valueOf(0.4);
	private static final BigDecimal POINTFIVE = BigDecimal.valueOf(0.5);
	private static final BigDecimal POINTSIX = BigDecimal.valueOf(0.6);
	private static final BigDecimal POINTSEVEN = BigDecimal.valueOf(0.7);
	private static final BigDecimal ONE = BigDecimal.valueOf(1);
	private static final BigDecimal ONE_POINTTWO = BigDecimal.valueOf(1.2);
	private static final BigDecimal ONE_POINTFIVE = BigDecimal.valueOf(1.5);
	private static final BigDecimal ONE_POINTSEVENFIVE = BigDecimal.valueOf(1.75);
	private static final BigDecimal ONE_POINTEIGHT = BigDecimal.valueOf(1.8);
	private static final BigDecimal TWO = BigDecimal.valueOf(2);
	private static final BigDecimal TWO_POINTTWOFIVE = BigDecimal.valueOf(2.25);
	private static final BigDecimal TWO_POINTFIVE = BigDecimal.valueOf(2.5);
	private static final BigDecimal TWO_POINTSEVENFIVE = BigDecimal.valueOf(2.75);
	private static final BigDecimal THREE = BigDecimal.valueOf(3);
	private static final BigDecimal THREE_POINTTWOFIVE = BigDecimal.valueOf(3.25);
	private static final BigDecimal THREE_POINTFIVE = BigDecimal.valueOf(3.5);
	private static final BigDecimal FIFTEEN = BigDecimal.valueOf(15);
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	
	private static final BigDecimal ROAD_WIDTH_TWO_POINTFOUR = BigDecimal.valueOf(2.4);
	private static final BigDecimal ROAD_WIDTH_TWO_POINTFOURFOUR = BigDecimal.valueOf(2.44);
	private static final BigDecimal ROAD_WIDTH_THREE_POINTSIX = BigDecimal.valueOf(3.6);
	private static final BigDecimal ROAD_WIDTH_FOUR_POINTEIGHT = BigDecimal.valueOf(4.8);
	private static final BigDecimal ROAD_WIDTH_SIX_POINTONE = BigDecimal.valueOf(6.1);
	private static final BigDecimal ROAD_WIDTH_NINE_POINTONE = BigDecimal.valueOf(9.1);
	private static final BigDecimal ROAD_WIDTH_TWELVE_POINTTWO = BigDecimal.valueOf(12.2);
	private static final BigDecimal ROAD_WIDTH_SIX = BigDecimal.valueOf(6);
	private static final BigDecimal ROAD_WIDTH_NINE = BigDecimal.valueOf(9);
	private static final BigDecimal ROAD_WIDTH_TWELVE = BigDecimal.valueOf(12);
	private static final BigDecimal ROAD_WIDTH_FIFTEEN = BigDecimal.valueOf(15);
	private static final BigDecimal ROAD_WIDTH_EIGHTEEN = BigDecimal.valueOf(18);
	private static final BigDecimal ROAD_WIDTH_THIRTY = BigDecimal.valueOf(30);

	private static final BigDecimal ROAD_WIDTH_EIGHTEEN_POINTTHREE = BigDecimal.valueOf(18.3);
	private static final BigDecimal ROAD_WIDTH_TWENTYFOUR_POINTFOUR = BigDecimal.valueOf(24.4);
	private static final BigDecimal ROAD_WIDTH_TWENTYSEVEN_POINTFOUR = BigDecimal.valueOf(27.4);
	private static final BigDecimal ROAD_WIDTH_THIRTY_POINTFIVE = BigDecimal.valueOf(30.5);
	
	private static final int COLOR_OWNERS_SOCIETY_OFFICE = 8;

	public static final String OLD = "OLD";
	public static final String NEW = "NEW";
	public static final String OLD_AREA_ERROR = "road width old area";
	public static final String NEW_AREA_ERROR = "road width new area";
	public static final String OLD_AREA_ERROR_MSG = "No construction shall be permitted if the road width is less than 2.4m for old area.";
	public static final String NEW_AREA_ERROR_MSG = "No construction shall be permitted if the road width is less than 6.1m for new area.";
	
	@Autowired
	private AdditionalMixedUseFeature additionalMixedUseFeature;
	
	@Autowired
	private OdishaMixedUseUtill odishaMixedUseUtill;
	
	@Value("${od.mixed-use.enabled:true}")
	private boolean isMixedUseEnable;
	
	@Override
	public Plan validate(Plan pl) {
		if (pl.getPlot() == null || (pl.getPlot() != null
				&& (pl.getPlot().getArea() == null || pl.getPlot().getArea().doubleValue() == 0))) {
			pl.addError(PLOT_AREA, getLocaleMessage(OBJECTNOTDEFINED, PLOT_AREA));
		}
		return pl;
	}
	
	private void updatePlan(Plan pl) {
		
	}

	private List<Occupancy> getOccupanciesFromRegularRomms(Floor floor) {
		return floor.getRegularRooms().stream().flatMap(room -> room.getMezzanineAreas().stream()).collect(Collectors.toList());
	}
	
	@Override
	public Plan process(Plan pl) {
		decideNocIsRequired(pl);
		HashMap<String, String> errorMsgs = new HashMap<>();
		int errors = pl.getErrors().size();
		validate(pl);
		int validatedErrors = pl.getErrors().size();
		if (validatedErrors > errors) {
			return pl;
		}
		BigDecimal totalExistingBuiltUpArea = BigDecimal.ZERO;
		BigDecimal totalExistingFloorArea = BigDecimal.ZERO;
		BigDecimal totalBuiltUpArea = BigDecimal.ZERO;
		BigDecimal totalFloorArea = BigDecimal.ZERO;
		BigDecimal totalCarpetArea = BigDecimal.ZERO;
		BigDecimal totalExistingCarpetArea = BigDecimal.ZERO;
		Set<OccupancyTypeHelper> distinctOccupancyTypesHelper = new HashSet<>();
		for (Block blk : pl.getBlocks()) {
			BigDecimal flrArea = BigDecimal.ZERO;
			BigDecimal bltUpArea = BigDecimal.ZERO;
			BigDecimal existingFlrArea = BigDecimal.ZERO;
			BigDecimal existingBltUpArea = BigDecimal.ZERO;
			BigDecimal carpetArea = BigDecimal.ZERO;
			BigDecimal existingCarpetArea = BigDecimal.ZERO;
			Building building = blk.getBuilding();
			for (Floor flr : building.getFloors()) {
				// set data for stilled floor and service floor
				OdishaUtill.validateServiceFloor(pl, blk, flr);
				OdishaUtill.validateStilledFloor(pl, blk, flr);				
				if (pl.getPlanInformation().getAlterationSubService()!=null) {
					OdishaUtill.validateExistingServiceFloor(pl, blk, flr);
					OdishaUtill.validateExistingStilledFloor(pl, blk, flr);
				}		
				OdishaUtill.validateHeightOfTheCeilingOfUpperBasementDeduction(pl, blk, flr);
				List<Occupancy> occupancies = flr.getOccupancies();
				occupancies.addAll(getOccupanciesFromRegularRomms(flr));
				
				// if(!flr.getIsStiltFloor()) {
				
				
				for (Occupancy occupancy : occupancies) {					
					validate2(pl, blk, flr, occupancy);
					/*
					 * occupancy.setCarpetArea(occupancy.getFloorArea().multiply
					 * (BigDecimal.valueOf(0.80))); occupancy
					 * .setExistingCarpetArea(occupancy.getExistingFloorArea().
					 * multiply(BigDecimal.valueOf(0.80)));
					 */
					bltUpArea = bltUpArea.add(
							occupancy.getBuiltUpArea() == null ? BigDecimal.valueOf(0) : occupancy.getBuiltUpArea());
					
					existingBltUpArea = existingBltUpArea
							.add(occupancy.getExistingBuiltUpArea() == null ? BigDecimal.valueOf(0)
									: occupancy.getExistingBuiltUpArea());
					
					flrArea = flrArea.add(occupancy.getFarAreaDeductExisting() == null ? occupancy.getFloorArea()
							: (occupancy.getFloorArea().subtract(occupancy.getFarAreaDeductExisting())
									.compareTo(BigDecimal.ZERO) < 0) ? occupancy.getFloorArea()
											: occupancy.getFloorArea().subtract(occupancy.getFarAreaDeductExisting()));

					existingFlrArea = existingFlrArea.add(occupancy.getExistingFloorArea());

					carpetArea = carpetArea.add(occupancy.getCarpetArea());
					existingCarpetArea = existingCarpetArea.add(occupancy.getExistingCarpetArea());
				}
				// }
			}
			building.setTotalFloorArea(flrArea);
			building.setTotalBuitUpArea(bltUpArea);
			building.setTotalExistingBuiltUpArea(existingBltUpArea);
			building.setTotalExistingFloorArea(existingFlrArea);

			// check block is completely existing building or not.
			if (existingBltUpArea.compareTo(bltUpArea) == 0)
				blk.setCompletelyExisting(Boolean.TRUE);

			totalFloorArea = totalFloorArea.add(flrArea);
			totalBuiltUpArea = totalBuiltUpArea.add(bltUpArea);
			totalExistingBuiltUpArea = totalExistingBuiltUpArea.add(existingBltUpArea);
			totalExistingFloorArea = totalExistingFloorArea.add(existingFlrArea);
			totalCarpetArea = totalCarpetArea.add(carpetArea);
			totalExistingCarpetArea = totalExistingCarpetArea.add(existingCarpetArea);

			// Find Occupancies by block and add
			Set<OccupancyTypeHelper> occupancyByBlock = new HashSet<>();
			for (Floor flr : building.getFloors()) {
				for (Occupancy occupancy : flr.getOccupancies()) {
					if (occupancy.getTypeHelper() != null)
						occupancyByBlock.add(occupancy.getTypeHelper());
				}
			}

			List<Map<String, Object>> listOfMapOfAllDtls = new ArrayList<>();
			List<OccupancyTypeHelper> listOfOccupancyTypes = new ArrayList<>();

			for (OccupancyTypeHelper occupancyType : occupancyByBlock) {

				Map<String, Object> allDtlsMap = new HashMap<>();
				BigDecimal blockWiseFloorArea = BigDecimal.ZERO;
				BigDecimal blockWiseBuiltupArea = BigDecimal.ZERO;
				BigDecimal blockWiseExistingFloorArea = BigDecimal.ZERO;
				BigDecimal blockWiseExistingBuiltupArea = BigDecimal.ZERO;
				for (Floor flr : blk.getBuilding().getFloors()) {
					for (Occupancy occupancy : flr.getOccupancies()) {
						if (occupancyType.getType() != null && occupancyType.getType().getCode() != null
								&& occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getType() != null
								&& occupancy.getTypeHelper().getType().getCode() != null && occupancy.getTypeHelper()
										.getType().getCode().equals(occupancyType.getType().getCode())) {
							blockWiseFloorArea = blockWiseFloorArea.add(occupancy.getFloorArea());
							blockWiseBuiltupArea = blockWiseBuiltupArea
									.add(occupancy.getBuiltUpArea() == null ? BigDecimal.valueOf(0)
											: occupancy.getBuiltUpArea());
							blockWiseExistingFloorArea = blockWiseExistingFloorArea
									.add(occupancy.getExistingFloorArea());
							blockWiseExistingBuiltupArea = blockWiseExistingBuiltupArea
									.add(occupancy.getExistingBuiltUpArea() == null ? BigDecimal.valueOf(0)
											: occupancy.getExistingBuiltUpArea());

						}
					}
				}
				Occupancy occupancy = new Occupancy();
				occupancy.setBuiltUpArea(blockWiseBuiltupArea);
				occupancy.setFloorArea(blockWiseFloorArea);
				occupancy.setExistingFloorArea(blockWiseExistingFloorArea);
				occupancy.setExistingBuiltUpArea(blockWiseExistingBuiltupArea);
				occupancy.setCarpetArea(blockWiseFloorArea.multiply(BigDecimal.valueOf(.80)));
				occupancy.setTypeHelper(occupancyType);
				building.getTotalArea().add(occupancy);

				allDtlsMap.put("occupancy", occupancyType);
				allDtlsMap.put("totalFloorArea", blockWiseFloorArea);
				allDtlsMap.put("totalBuiltUpArea", blockWiseBuiltupArea);
				allDtlsMap.put("existingFloorArea", blockWiseExistingFloorArea);
				allDtlsMap.put("existingBuiltUpArea", blockWiseExistingBuiltupArea);

				listOfOccupancyTypes.add(occupancyType);

				listOfMapOfAllDtls.add(allDtlsMap);
			}
			Set<OccupancyTypeHelper> setOfOccupancyTypes = new HashSet<>(listOfOccupancyTypes);

			List<Occupancy> listOfOccupanciesOfAParticularblock = new ArrayList<>();
			// for each distinct converted occupancy types
			for (OccupancyTypeHelper occupancyType : setOfOccupancyTypes) {
				if (occupancyType != null) {
					Occupancy occupancy = new Occupancy();
					BigDecimal totalFlrArea = BigDecimal.ZERO;
					BigDecimal totalBltUpArea = BigDecimal.ZERO;
					BigDecimal totalExistingFlrArea = BigDecimal.ZERO;
					BigDecimal totalExistingBltUpArea = BigDecimal.ZERO;

					for (Map<String, Object> dtlsMap : listOfMapOfAllDtls) {
						if (occupancyType.equals(dtlsMap.get("occupancy"))) {
							totalFlrArea = totalFlrArea.add((BigDecimal) dtlsMap.get("totalFloorArea"));
							totalBltUpArea = totalBltUpArea.add((BigDecimal) dtlsMap.get("totalBuiltUpArea"));

							totalExistingBltUpArea = totalExistingBltUpArea
									.add((BigDecimal) dtlsMap.get("existingBuiltUpArea"));
							totalExistingFlrArea = totalExistingFlrArea
									.add((BigDecimal) dtlsMap.get("existingFloorArea"));

						}
					}
					occupancy.setTypeHelper(occupancyType);
					occupancy.setBuiltUpArea(totalBltUpArea);
					occupancy.setFloorArea(totalFlrArea);
					occupancy.setExistingBuiltUpArea(totalExistingBltUpArea);
					occupancy.setExistingFloorArea(totalExistingFlrArea);
					occupancy.setExistingCarpetArea(totalExistingFlrArea.multiply(BigDecimal.valueOf(0.80)));
					occupancy.setCarpetArea(totalFlrArea.multiply(BigDecimal.valueOf(0.80)));

					listOfOccupanciesOfAParticularblock.add(occupancy);
				}
			}
			blk.getBuilding().setOccupancies(listOfOccupanciesOfAParticularblock);

			if (!listOfOccupanciesOfAParticularblock.isEmpty()) {
				// listOfOccupanciesOfAParticularblock already converted. In
				// case of professional building type, converted into A1
				// type
				boolean singleFamilyBuildingTypeOccupancyPresent = false;
				boolean otherThanSingleFamilyOccupancyTypePresent = false;

				for (Occupancy occupancy : listOfOccupanciesOfAParticularblock) {
					if (occupancy.getTypeHelper().getSubtype() != null
							&& A_R.equals(occupancy.getTypeHelper().getSubtype().getCode()))
						singleFamilyBuildingTypeOccupancyPresent = true;
					else {
						otherThanSingleFamilyOccupancyTypePresent = true;
						break;
					}
				}
				blk.setSingleFamilyBuilding(
						!otherThanSingleFamilyOccupancyTypePresent && singleFamilyBuildingTypeOccupancyPresent);
				int allResidentialOccTypes = 0;
				int allResidentialOrCommercialOccTypes = 0;

				for (Occupancy occupancy : listOfOccupanciesOfAParticularblock) {
					if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getType() != null) {
						// setting residentialBuilding
						int residentialOccupancyType = 0;
						if (A.equals(occupancy.getTypeHelper().getType().getCode())) {
							residentialOccupancyType = 1;
						}
						if (residentialOccupancyType == 0) {
							allResidentialOccTypes = 0;
							break;
						} else {
							allResidentialOccTypes = 1;
						}
					}
				}
				blk.setResidentialBuilding(allResidentialOccTypes == 1);
				for (Occupancy occupancy : listOfOccupanciesOfAParticularblock) {
					if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getType() != null) {
						// setting residentialOrCommercial Occupancy Type
						int residentialOrCommercialOccupancyType = 0;
						if (A.equals(occupancy.getTypeHelper().getType().getCode())
								|| F.equals(occupancy.getTypeHelper().getType().getCode())) {
							residentialOrCommercialOccupancyType = 1;
						}
						if (residentialOrCommercialOccupancyType == 0) {
							allResidentialOrCommercialOccTypes = 0;
							break;
						} else {
							allResidentialOrCommercialOccTypes = 1;
						}
					}
				}
				blk.setResidentialOrCommercialBuilding(allResidentialOrCommercialOccTypes == 1);
			}

			if (blk.getBuilding().getFloors() != null && !blk.getBuilding().getFloors().isEmpty()) {
				BigDecimal noOfFloorsAboveGround = BigDecimal.ZERO;
				for (Floor floor : blk.getBuilding().getFloors()) {
					if (floor.getNumber() != null && floor.getNumber() >= 0) {
						noOfFloorsAboveGround = noOfFloorsAboveGround.add(BigDecimal.valueOf(1));
					}
				}

				boolean hasTerrace = blk.getBuilding().getFloors().stream()
						.anyMatch(floor -> floor.getTerrace().equals(Boolean.TRUE));

				noOfFloorsAboveGround = hasTerrace ? noOfFloorsAboveGround.subtract(BigDecimal.ONE)
						: noOfFloorsAboveGround;

				blk.getBuilding().setMaxFloor(noOfFloorsAboveGround);
				blk.getBuilding().setFloorsAboveGround(noOfFloorsAboveGround);
				blk.getBuilding().setTotalFloors(BigDecimal.valueOf(blk.getBuilding().getFloors().size()));
			}

		}

		for (Block blk : pl.getBlocks()) {
			Building building = blk.getBuilding();
			List<OccupancyTypeHelper> blockWiseOccupancyTypes = new ArrayList<>();
			for (Occupancy occupancy : blk.getBuilding().getOccupancies()) {
				if (occupancy.getTypeHelper() != null)
					blockWiseOccupancyTypes.add(occupancy.getTypeHelper());
			}
			Set<OccupancyTypeHelper> setOfBlockDistinctOccupancyTypes = new HashSet<>(blockWiseOccupancyTypes);
			
			OccupancyTypeHelper mostRestrictiveFar = getMostRestrictiveFar(setOfBlockDistinctOccupancyTypes,pl,true);
			blk.getBuilding().setMostRestrictiveFarHelper(mostRestrictiveFar);

			for (Floor flr : building.getFloors()) {
				BigDecimal flrArea = BigDecimal.ZERO;
				BigDecimal existingFlrArea = BigDecimal.ZERO;
				BigDecimal carpetArea = BigDecimal.ZERO;
				BigDecimal existingCarpetArea = BigDecimal.ZERO;
				BigDecimal existingBltUpArea = BigDecimal.ZERO;
				for (Occupancy occupancy : flr.getOccupancies()) {
					flrArea = flrArea.add(
							(occupancy.getExistingFloorArea().compareTo(BigDecimal.ZERO) > 0 
									&& !(flr.getNumber() < 0) 
									&& !(occupancy.getExistingBuiltUpAreaAdd().compareTo(BigDecimal.ZERO) > 0))
							? occupancy.getFloorArea()
									.subtract(occupancy.getExistingFloorArea())
									.subtract(occupancy.getFarAreaDeductExisting())
							: occupancy.getFloorArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
					existingFlrArea = existingFlrArea.add(occupancy.getExistingFloorArea());
					carpetArea = carpetArea.add(
							occupancy.getExistingCarpetArea().compareTo(BigDecimal.ZERO) > 0
							? occupancy.getCarpetArea()
									.subtract(occupancy.getExistingCarpetArea())
							: occupancy.getCarpetArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
					existingCarpetArea = existingCarpetArea.add(occupancy.getExistingCarpetArea());
					
				}

				List<Occupancy> occupancies = flr.getOccupancies();
				for (Occupancy occupancy : occupancies) {
					existingBltUpArea = existingBltUpArea
							.add(occupancy.getExistingBuiltUpArea() != null ? occupancy.getExistingBuiltUpArea()
									: BigDecimal.ZERO);
					
					//FOR EWS, "Floor Area > Carpet Area" condition should not be checked.
					if (!flr.getIsStiltFloor())					
					if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
							&& !(DxfFileConstants.EWS.equals(occupancy.getTypeHelper().getSubtype().getCode())
							|| DxfFileConstants.LOW_INCOME_HOUSING.equals(occupancy.getTypeHelper().getSubtype().getCode()))) {
						
						

						if (flrArea
								.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS, DcrConstants.ROUNDMODE_MEASUREMENTS)
								.compareTo(carpetArea.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
										DcrConstants.ROUNDMODE_MEASUREMENTS)) < 0) {
							pl.addError("Floor area in block " + blk.getNumber() + "floor " + flr.getNumber(),
									"Floor area is less than carpet area in block " + blk.getNumber() + "floor "
											+ flr.getNumber());
						}

						if (existingBltUpArea.compareTo(BigDecimal.ZERO) > 0 && existingFlrArea
								.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS, DcrConstants.ROUNDMODE_MEASUREMENTS)
								.compareTo(existingCarpetArea.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
										DcrConstants.ROUNDMODE_MEASUREMENTS)) < 0) {
							pl.addError("Existing floor area in block " + blk.getNumber() + "floor " + flr.getNumber(),
									"Existing Floor area is less than carpet area in block " + blk.getNumber()
											+ "floor " + flr.getNumber());
				}
				
							}

				if (mostRestrictiveFar != null && mostRestrictiveFar.getConvertedSubtype() != null
						&& !A_R.equals(mostRestrictiveFar.getSubtype().getCode())) {
					if (carpetArea.compareTo(BigDecimal.ZERO) == 0) {
						pl.addError("Carpet area in block " + blk.getNumber() + "floor " + flr.getNumber(),
								"Carpet area is not defined in block " + blk.getNumber() + "floor " + flr.getNumber());
					}

					if (existingBltUpArea.compareTo(BigDecimal.ZERO) > 0
							&& existingCarpetArea.compareTo(BigDecimal.ZERO) == 0) {
						pl.addError("Existing Carpet area in block " + blk.getNumber() + "floor " + flr.getNumber(),
								"Existing Carpet area is not defined in block " + blk.getNumber() + "floor "
										+ flr.getNumber());
					}
				}
				

					
				}

			}
		}

		List<OccupancyTypeHelper> plotWiseOccupancyTypes = new ArrayList<>();
		for (Block block : pl.getBlocks()) {
			for (Occupancy occupancy : block.getBuilding().getOccupancies()) {
				if (occupancy.getTypeHelper() != null)
					plotWiseOccupancyTypes.add(occupancy.getTypeHelper());
			}
		}

		Set<OccupancyTypeHelper> setOfDistinctOccupancyTypes = new HashSet<>(plotWiseOccupancyTypes);

		distinctOccupancyTypesHelper.addAll(setOfDistinctOccupancyTypes);

		List<Occupancy> occupanciesForPlan = new ArrayList<>();

		for (OccupancyTypeHelper occupancyType : setOfDistinctOccupancyTypes) {
			if (occupancyType != null) {
				BigDecimal totalFloorAreaForAllBlks = BigDecimal.ZERO;
				BigDecimal totalBuiltUpAreaForAllBlks = BigDecimal.ZERO;
				BigDecimal totalCarpetAreaForAllBlks = BigDecimal.ZERO;
				BigDecimal totalExistBuiltUpAreaForAllBlks = BigDecimal.ZERO;
				BigDecimal totalExistFloorAreaForAllBlks = BigDecimal.ZERO;
				BigDecimal totalExistCarpetAreaForAllBlks = BigDecimal.ZERO;
				Occupancy occupancy = new Occupancy();
				for (Block block : pl.getBlocks()) {
					for (Occupancy buildingOccupancy : block.getBuilding().getOccupancies()) {
						if (occupancyType.equals(buildingOccupancy.getTypeHelper())) {
							totalFloorAreaForAllBlks = totalFloorAreaForAllBlks.add(buildingOccupancy.getFloorArea());
							totalBuiltUpAreaForAllBlks = totalBuiltUpAreaForAllBlks
									.add(buildingOccupancy.getBuiltUpArea());
							totalCarpetAreaForAllBlks = totalCarpetAreaForAllBlks
									.add(buildingOccupancy.getCarpetArea());
							totalExistBuiltUpAreaForAllBlks = totalExistBuiltUpAreaForAllBlks
									.add(buildingOccupancy.getExistingBuiltUpArea());
							totalExistFloorAreaForAllBlks = totalExistFloorAreaForAllBlks
									.add(buildingOccupancy.getExistingFloorArea());
							totalExistCarpetAreaForAllBlks = totalExistCarpetAreaForAllBlks
									.add(buildingOccupancy.getExistingCarpetArea());
						}
					}
				}
				occupancy.setTypeHelper(occupancyType);
				occupancy.setBuiltUpArea(totalBuiltUpAreaForAllBlks);
				occupancy.setCarpetArea(totalCarpetAreaForAllBlks);
				occupancy.setFloorArea(totalFloorAreaForAllBlks);
				occupancy.setExistingBuiltUpArea(totalExistBuiltUpAreaForAllBlks);
				occupancy.setExistingFloorArea(totalExistFloorAreaForAllBlks);
				occupancy.setExistingCarpetArea(totalExistCarpetAreaForAllBlks);
				occupanciesForPlan.add(occupancy);
			}
		}

		pl.setOccupancies(occupanciesForPlan);
		pl.getVirtualBuilding().setTotalFloorArea(totalFloorArea);
		pl.getVirtualBuilding().setTotalCarpetArea(totalCarpetArea);
		pl.getVirtualBuilding().setTotalExistingBuiltUpArea(totalExistingBuiltUpArea);
		pl.getVirtualBuilding().setTotalExistingFloorArea(totalExistingFloorArea);
		pl.getVirtualBuilding().setTotalExistingCarpetArea(totalExistingCarpetArea);
		pl.getVirtualBuilding().setOccupancyTypes(distinctOccupancyTypesHelper);
		pl.getVirtualBuilding().setTotalBuitUpArea(totalBuiltUpArea);
		pl.getVirtualBuilding().setMostRestrictiveFarHelper(getMostRestrictiveFar(setOfDistinctOccupancyTypes,pl,false));
		
		if (!distinctOccupancyTypesHelper.isEmpty()) {
			int allResidentialOccTypesForPlan = 0;
			for (OccupancyTypeHelper occupancy : distinctOccupancyTypesHelper) {
				//LOG.info("occupancy :" + occupancy);
				// setting residentialBuilding
				int residentialOccupancyType = 0;
				if (occupancy.getType() != null && A.equals(occupancy.getType().getCode())) {
					residentialOccupancyType = 1;
				}
				if (residentialOccupancyType == 0) {
					allResidentialOccTypesForPlan = 0;
					break;
				} else {
					allResidentialOccTypesForPlan = 1;
				}
			}
			pl.getVirtualBuilding().setResidentialBuilding(allResidentialOccTypesForPlan == 1);
			int allResidentialOrCommercialOccTypesForPlan = 0;
			for (OccupancyTypeHelper occupancyType : distinctOccupancyTypesHelper) {
				int residentialOrCommercialOccupancyTypeForPlan = 0;
				if (occupancyType.getType() != null && (A.equals(occupancyType.getType().getCode())
						|| F.equals(occupancyType.getType().getCode()))) {
					residentialOrCommercialOccupancyTypeForPlan = 1;
				}
				if (residentialOrCommercialOccupancyTypeForPlan == 0) {
					allResidentialOrCommercialOccTypesForPlan = 0;
					break;
				} else {
					allResidentialOrCommercialOccTypesForPlan = 1;
				}
			}
			pl.getVirtualBuilding().setResidentialOrCommercialBuilding(allResidentialOrCommercialOccTypesForPlan == 1);
		}
		
		OccupancyTypeHelper mostRestrictiveOccupancy = pl.getVirtualBuilding() != null
				? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
				: null;

//		occupancy limit
//		if (!(pl.getVirtualBuilding().getResidentialOrCommercialBuilding()
//				|| (mostRestrictiveOccupancy != null && mostRestrictiveOccupancy.getType() != null
//						&& DxfFileConstants.G.equalsIgnoreCase(mostRestrictiveOccupancy.getType().getCode())))) {
//			pl.getErrors().put(DxfFileConstants.OCCUPANCY_ALLOWED_KEY, DxfFileConstants.OCCUPANCY_ALLOWED);
//			return pl;
//		}

		Set<String> occupancyCodes = new HashSet<>();
		for (OccupancyTypeHelper oth : pl.getVirtualBuilding().getOccupancyTypes()) {
			if (oth.getSubtype() != null) {
				occupancyCodes.add(oth.getSubtype().getCode());
			}
		}

//                if (occupancyCodes.size() == 1 && occupancyCodes.contains(DxfFileConstants.A_PO)) {
//                    pl.getErrors().put(DxfFileConstants.OCCUPANCY_PO_NOT_ALLOWED_KEY, DxfFileConstants.OCCUPANCY_PO_NOT_ALLOWED);
//                    return pl;
//                }

		OccupancyTypeHelper mostRestrictiveOccupancyType = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		BigDecimal providedFar = BigDecimal.ZERO;
		BigDecimal surrenderRoadArea = BigDecimal.ZERO;

		if (!pl.getSurrenderRoads().isEmpty()) {
			for (Measurement measurement : pl.getSurrenderRoads()) {
				surrenderRoadArea = surrenderRoadArea.add(measurement.getArea());
			}
		}

		pl.setTotalSurrenderRoadArea(surrenderRoadArea.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
				DcrConstants.ROUNDMODE_MEASUREMENTS));
//		BigDecimal plotArea = pl.getPlot() != null ? pl.getPlot().getArea().add(surrenderRoadArea) : BigDecimal.ZERO;
//		if (plotArea.doubleValue() > 0)
//			providedFar = pl.getVirtualBuilding().getTotalFloorArea().divide(plotArea, DECIMALDIGITS_MEASUREMENTS,
//					ROUNDMODE_MEASUREMENTS);
		
		providedFar=calculateFar(pl,surrenderRoadArea);

		pl.setFarDetails(new FarDetails());
		pl.getFarDetails().setProvidedFar(providedFar.doubleValue());
		String typeOfArea = pl.getPlanInformation().getTypeOfArea();
		BigDecimal roadWidth = pl.getPlanInformation().getTotalRoadWidth();

//		if (mostRestrictiveOccupancyType != null && StringUtils.isNotBlank(typeOfArea) && roadWidth != null
//				&& !processFarForSpecialOccupancy(pl, mostRestrictiveOccupancyType, providedFar, typeOfArea, roadWidth,
//						errorMsgs)) {
		if (mostRestrictiveOccupancyType != null) {

			processFar(pl, mostRestrictiveOccupancyType, providedFar, typeOfArea, roadWidth, errorMsgs);
//                        if ((mostRestrictiveOccupancyType.getType() != null
//                                        && DxfFileConstants.A.equalsIgnoreCase(mostRestrictiveOccupancyType.getType().getCode()))
//                                        || (mostRestrictiveOccupancyType.getSubtype() != null
//                                                        && (A_R.equalsIgnoreCase(mostRestrictiveOccupancyType.getSubtype().getCode())
//                                                        || A_AF.equalsIgnoreCase(mostRestrictiveOccupancyType.getSubtype().getCode())))) {
//                                processFarResidential(pl, mostRestrictiveOccupancyType, providedFar, typeOfArea, roadWidth, errorMsgs);
//                        }
//                        if (mostRestrictiveOccupancyType.getType() != null
//                                        && (DxfFileConstants.G.equalsIgnoreCase(mostRestrictiveOccupancyType.getType().getCode())
//                                                        || DxfFileConstants.B.equalsIgnoreCase(mostRestrictiveOccupancyType.getType().getCode())
//                                                        || DxfFileConstants.D.equalsIgnoreCase(mostRestrictiveOccupancyType.getType().getCode()))) {
//                                processFarForGBDOccupancy(pl, mostRestrictiveOccupancyType, providedFar, typeOfArea, roadWidth,
//                                                errorMsgs);
//                        }
//                        if (mostRestrictiveOccupancyType.getType() != null
//                                        && DxfFileConstants.I.equalsIgnoreCase(mostRestrictiveOccupancyType.getType().getCode())) {
//                                processFarHaazardous(pl, mostRestrictiveOccupancyType, providedFar, typeOfArea, roadWidth, errorMsgs);
//                        } 
//                        if (mostRestrictiveOccupancyType.getType() != null
//                                && DxfFileConstants.F.equalsIgnoreCase(mostRestrictiveOccupancyType.getType().getCode())) { 
//                                processFarNonResidential(pl, mostRestrictiveOccupancyType, providedFar, typeOfArea, roadWidth,
//                                                errorMsgs);
//                        }
		}
		ProcessPrintHelper.print(pl);
		validateRoles(pl);
		
		if(!(DxfFileConstants.EWS.equals(pl.getVirtualBuilding().getMostRestrictiveFarHelper().getSubtype().getCode())
				|| DxfFileConstants.LOW_INCOME_HOUSING.equals(pl.getVirtualBuilding().getMostRestrictiveFarHelper().getSubtype().getCode()))) {
			
			if(pl.getPlanInformation().getIsProjectUnderAffordableHousingScheme() != null && pl.getPlanInformation().getIsProjectUnderAffordableHousingScheme().equalsIgnoreCase("YES")) {
				pl.addError("invalidAffordableHousingScheme","'IS_THE_PROJECT_COMING_UNDER_AFFORDABLE_HOUSING_SCHEME_AS_PER_HOUSING_FOR_ALL_POLICY_2022' cannot be 'Yes' for the given plan.");
			}
			
		}
//		//computeOccupancyPercentage
//		OdishaUtill.computeOccupancyPercentage(pl);
		
		//validate shop/work cum residential occupancy
		validateShopCumResidential(pl);
		validateWorkCumResidential(pl);
		
		//Mixed use case
		additionalMixedUseFeature.process(pl);
		return pl;
	}
	
	private void validateWorkCumResidential(Plan pl) {
		Map<String, OccupancyPercentage> occupancyPercentages = pl.getPlanInformation().getOccupancyPercentages();

		if (occupancyPercentages.containsKey("Professional offices")
				&& (occupancyPercentages.containsKey("Residential Plotted")
						|| occupancyPercentages.containsKey("Semi-detached")
						|| occupancyPercentages.containsKey("Row housing"))) {

			if (pl.getPlanInformation().getIsProjectComingUnderWorkCumResidential() == null) {

				pl.addError("Work cum residential",
						"Plan info IS_PROJECT_COMING_UNDER_WORK_CUM_RESIDENTIAL_OCCUPANCY is mandatory.");

			} else if (pl.getPlanInformation().getIsProjectComingUnderWorkCumResidential().equalsIgnoreCase("YES")) {

				pl.getPlanInformation().setIsProjectWorkCumResidential(true);
			}

		}
	}

	private void validateShopCumResidential(Plan pl) {

		Map<String, OccupancyPercentage> occupancyPercentages = pl.getPlanInformation().getOccupancyPercentages();

		if ((occupancyPercentages.containsKey("Convenience and Neighborhood Shopping")
				|| occupancyPercentages.containsKey("Departmental store")
				|| occupancyPercentages.containsKey("Local(retail) shopping")
				|| occupancyPercentages.containsKey("Restaurant"))

				&& (occupancyPercentages.containsKey("Residential Plotted")
						|| occupancyPercentages.containsKey("Semi-detached")
						|| occupancyPercentages.containsKey("Row housing"))) {

			if (pl.getPlanInformation().getIsProjectComingUnderShopCumResidential() == null) {

				pl.addError("Shop cum residential",
						"Plan info IS_PROJECT_COMING_UNDER_SHOP_CUM_RESIDENTIAL_OCCUPANCY is mandatory.");

			} else if (pl.getPlanInformation().getIsProjectComingUnderShopCumResidential().equalsIgnoreCase("YES")) {

				if (pl.getPlanInformation().getAreThePlotsAllotedInRow() == null
						|| pl.getPlanInformation().getAreThePlotsAllotedInRow().equalsIgnoreCase("NO")) {

					pl.addError("Shop cum residential",
							"Shop-cum-residential is allowed only where plots are allotted in a row.");
				}
			}

		}

	}

	private void validateRoles(Plan pl) {
		for (TechnicalPersonRole role : pl.getRoles()) {
			if (role.getCode().equalsIgnoreCase("BPA_SUPERVISORTP")) {
				if (pl.getPlot().getArea().compareTo(ONE_HUNDRED) > 0 || getMaxNumberOfFloor(pl) > 2) {
					pl.addError("UNAUTHORIZED_SUPERVISOR",
							"Supervisors are only allowed to scrutinize residential buildings on plot up to 100sqm and up to 2 stories");
				}
			}
		}	
	}
	private void decideNocIsRequired(Plan pl) {
		Boolean isHighRise = false;
		for (Block b : pl.getBlocks()) {
			if ((b.getBuilding() != null/*
										 * && b.getBuilding().getIsHighRise() != null && b.getBuilding().getIsHighRise()
										 */ && b.getBuilding().getBuildingHeight().compareTo(new BigDecimal(5)) > 0)
					|| (b.getBuilding() != null && b.getBuilding().getCoverageArea() != null
							&& b.getBuilding().getCoverageArea().compareTo(new BigDecimal(500)) > 0)) {
				isHighRise = true;

			}
		}
		if (isHighRise) {
			pl.getPlanInformation().setNocFireDept("YES");
		}

		if (StringUtils.isNotBlank(pl.getPlanInformation().getBuildingNearMonument())
				&& "YES".equalsIgnoreCase(pl.getPlanInformation().getBuildingNearMonument())) {
			BigDecimal minDistanceFromMonument = BigDecimal.ZERO;
			List<BigDecimal> distancesFromMonument = pl.getDistanceToExternalEntity().getMonuments();
			if (!distancesFromMonument.isEmpty()) {

				minDistanceFromMonument = distancesFromMonument.stream().reduce(BigDecimal::min).get();

				if (minDistanceFromMonument.compareTo(BigDecimal.valueOf(300)) > 0) {
					pl.getPlanInformation().setNocNearMonument("YES");
				}
			}

		}

	}

	private void validate2(Plan pl, Block blk, Floor flr, Occupancy occupancy) {
		String occupancyTypeHelper = StringUtils.EMPTY;
		String occupancySubTypeHelperCode= StringUtils.EMPTY;
		if (occupancy.getTypeHelper() != null) {
			if (occupancy.getTypeHelper().getType() != null) {
				occupancyTypeHelper = occupancy.getTypeHelper().getType().getName();
			} else if (occupancy.getTypeHelper().getSubtype() != null) {
				occupancyTypeHelper = occupancy.getTypeHelper().getSubtype().getName();
			}
			
			if(occupancy.getTypeHelper().getSubtype() != null)
				occupancySubTypeHelperCode=occupancy.getTypeHelper().getSubtype().getCode();
		}
		
		//remove from far calculation
		if(DxfFileConstants.PUBLIC_WASHROOMS.equals(occupancySubTypeHelperCode) 
				|| DxfFileConstants.INFO_COMMS_SYSTEM.equals(occupancySubTypeHelperCode)) {
			return;
		}
				
		
		boolean onlyOwnerSocietyFlag = false;

		Set<Integer> colorcodes = flr.getUnits().stream().map(units -> units.getColorCode())
				.collect(Collectors.toSet());
		int size = colorcodes.size();

		if (size == 1) {
			if (colorcodes.toArray(new Integer[1])[0] == COLOR_OWNERS_SOCIETY_OFFICE)
				onlyOwnerSocietyFlag = true;
		}
		
		// validate the carpet area for OC_RESIDENTIAL	
		if (!onlyOwnerSocietyFlag && !(flr.getNumber() < 0) && !flr.getIsStiltFloor() && !occupancy.getIsMezzanine()) {
			if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getType() != null
					&& DxfFileConstants.OC_RESIDENTIAL.equals(occupancy.getTypeHelper().getType().getCode())) {
				if (occupancy.getCarpetArea().compareTo(BigDecimal.ZERO) <= 0
						&& occupancy.getExistingCarpetArea().compareTo(BigDecimal.ZERO) <= 0) {
					pl.addError("carpet_missing_" + flr.getColorCode() + occupancySubTypeHelperCode,
							"Carpet area is mandatory for block " + blk.getNumber() + " floor "
									+ flr.getNumber());
				}
			}
		}
		
		
		
		if (occupancy.getBuiltUpArea() != null && occupancy.getBuiltUpArea().compareTo(BigDecimal.valueOf(0)) < 0) {
			pl.addError(VALIDATION_NEGATIVE_BUILTUP_AREA, getLocaleMessage(VALIDATION_NEGATIVE_BUILTUP_AREA,
					blk.getNumber(), flr.getNumber().toString(), occupancyTypeHelper));
		}
		if (occupancy.getExistingBuiltUpArea() != null
				&& occupancy.getExistingBuiltUpArea().compareTo(BigDecimal.valueOf(0)) < 0) {
			pl.addError(VALIDATION_NEGATIVE_EXISTING_BUILTUP_AREA,
					getLocaleMessage(VALIDATION_NEGATIVE_EXISTING_BUILTUP_AREA, blk.getNumber(),
							flr.getNumber().toString(), occupancyTypeHelper));
		}
		
		if(occupancy.getBuiltUpArea() != null && occupancy.getBuiltUpArea().compareTo(BigDecimal.ZERO)>0) {
			occupancy.setBuiltUpArea(occupancy.getBuiltUpArea().subtract(occupancy.getDeduction() == null ? BigDecimal.ZERO : occupancy.getDeduction())
					.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
		}
		
		if (occupancy.getExistingBuiltUpArea() != null
				&& occupancy.getExistingBuiltUpArea().compareTo(BigDecimal.ZERO) > 0) {
			occupancy.setExistingBuiltUpArea(occupancy.getExistingBuiltUpArea().subtract(
					occupancy.getExistingDeduction() == null ? BigDecimal.ZERO : occupancy.getExistingDeduction())
					.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
		}

		if (!Arrays.asList(new String[] { DxfFileConstants.EWS, DxfFileConstants.LOW_INCOME_HOUSING })
				.contains(occupancySubTypeHelperCode)
				|| (pl.getPlanInformation().getIsProjectOnlyLigOrEws() != null
						&& pl.getPlanInformation().getIsProjectOnlyLigOrEws().equalsIgnoreCase("YES"))) {
						
			occupancy.setFloorArea(occupancy.getBuiltUpArea() == null ? BigDecimal.ZERO : occupancy.getBuiltUpArea());
			occupancy.setExistingFloorArea(
					occupancy.getExistingBuiltUpArea() == null ? BigDecimal.ZERO : occupancy.getExistingBuiltUpArea());
					
			deductStiltAndService(pl, blk, flr, occupancy);
			
		} else if (DxfFileConstants.EWS.equals(occupancySubTypeHelperCode)) {
			BigDecimal ewsArea = pl.getTotalEWSAreaInPlot();
			ewsArea = ewsArea.add(occupancy.getBuiltUpArea());
			pl.setTotalEWSAreaInPlot(ewsArea.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
		}
		if (occupancy.getFloorArea() != null && occupancy.getFloorArea().compareTo(BigDecimal.valueOf(0)) < 0) {
			pl.addError(VALIDATION_NEGATIVE_FLOOR_AREA, getLocaleMessage(VALIDATION_NEGATIVE_FLOOR_AREA,
					blk.getNumber(), flr.getNumber().toString(), occupancyTypeHelper));
		}
//		occupancy.setExistingFloorArea(
//				(occupancy.getExistingBuiltUpArea() == null ? BigDecimal.ZERO : occupancy.getExistingBuiltUpArea()));
//						.subtract(occupancy.getExistingDeduction() == null ? BigDecimal.ZERO
//								: occupancy.getExistingDeduction()));
		if (occupancy.getExistingFloorArea() != null
				&& occupancy.getExistingFloorArea().compareTo(BigDecimal.valueOf(0)) < 0) {
			pl.addError(VALIDATION_NEGATIVE_EXISTING_FLOOR_AREA,
					getLocaleMessage(VALIDATION_NEGATIVE_EXISTING_FLOOR_AREA, blk.getNumber(),
							flr.getNumber().toString(), occupancyTypeHelper));
		}
		
		if(occupancy.getCarpetArea()!=null && occupancy.getCarpetAreaDeduction()!=null) {
			occupancy.setCarpetArea(occupancy.getCarpetArea().subtract(occupancy.getCarpetAreaDeduction() == null ? BigDecimal.ZERO : occupancy.getCarpetAreaDeduction())
					.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
		}
		
		if(occupancy.getExistingCarpetArea()!=null && occupancy.getExistingCarpetAreaDeduction()!=null) {
			occupancy.setExistingCarpetArea(occupancy.getExistingCarpetArea().subtract(occupancy.getExistingCarpetAreaDeduction() == null ? BigDecimal.ZERO : occupancy.getExistingCarpetAreaDeduction())
					.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
		}
		
		//remove floor area if floor is negative
		if(flr.getNumber()<0) {
			occupancy.setFloorArea(BigDecimal.ZERO);
			occupancy.setExistingFloorArea(BigDecimal.ZERO);
		}
		
		//Buildup area new layer work
		if(flr.getNumber()<0) {
			if(occupancy.getBuiltUpAreaAdd()!=null && occupancy.getBuiltUpArea()!=null) {
				occupancy.setFloorArea(occupancy.getFloorArea().add(occupancy.getBuiltUpAreaAdd() == null ? BigDecimal.ZERO : occupancy.getBuiltUpAreaAdd().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
			}
			
			if(occupancy.getExistingBuiltUpAreaAdd()!=null && occupancy.getExistingBuiltUpArea()!=null) {
				occupancy.setExistingFloorArea(occupancy.getExistingFloorArea().add(occupancy.getExistingBuiltUpAreaAdd() == null ? BigDecimal.ZERO : occupancy.getExistingBuiltUpAreaAdd().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
				occupancy.setFloorArea(occupancy.getFloorArea().compareTo(BigDecimal.valueOf(0)) <= 0 ? occupancy.getExistingFloorArea() : occupancy.getFloorArea());
			}
		}
		
		//FAR Area Deduct layer
		if(occupancy.getFloorArea() != null  && occupancy.getFarAreaDeduct() != null) {
			occupancy.setFloorArea(occupancy.getFloorArea().subtract(occupancy.getFarAreaDeduct() ==null ? BigDecimal.ZERO : occupancy.getFarAreaDeduct().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
		}
		
		if(occupancy.getExistingFloorArea() != null  && occupancy.getFarAreaDeductExisting() != null) {
			occupancy.setExistingFloorArea(occupancy.getExistingFloorArea().subtract(occupancy.getFarAreaDeductExisting() ==null ? BigDecimal.ZERO : occupancy.getFarAreaDeductExisting().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
		}

	}

	protected OccupancyTypeHelper getMostRestrictiveFar(Set<OccupancyTypeHelper> distinctOccupancyTypes,Plan pl,boolean isBuildingLevel) {
		if(!isBuildingLevel) {
			//computeOccupancyPercentage
			OdishaUtill.computeOccupancyPercentage(pl);
			if(distinctOccupancyTypes!=null && distinctOccupancyTypes.size()>1) {
				OccupancyTypeHelper occupancyTypeHelper = checkMostRestrictiveFarForMixedUse(distinctOccupancyTypes, pl);
				if(occupancyTypeHelper!=null) {
					if(!isMixedUseEnable) {
						pl.addError("isMixedUseEnable", "Mixed use is currently under development.");
					}
					return occupancyTypeHelper;
				}
			}
		}
		
		
		Set<String> codes = new HashSet<>();
		Map<String, OccupancyTypeHelper> codesMap = new HashMap<>();
		for (OccupancyTypeHelper typeHelper : distinctOccupancyTypes) {

			if (typeHelper.getType() != null)
				codesMap.put(typeHelper.getType().getCode(), typeHelper);
			if (typeHelper.getSubtype() != null)
				codesMap.put(typeHelper.getSubtype().getCode(), typeHelper);
		}
		codes = codesMap.keySet();
		if(!isBuildingLevel) {
			OccupancyPercentage occupancyPercentage = odishaMixedUseUtill.getMostOccupancyPercentage(
					pl.getPlanInformation().getOccupancyPercentages());
		
			if(codes.contains(occupancyPercentage.getSubOccupancyCode()))
				return codesMap.get(occupancyPercentage.getSubOccupancyCode());
		}
		
		if (codes.contains(DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING))
			return codesMap.get(DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING);
		else if (codes.contains(DxfFileConstants.SEMI_DETACHED))
			return codesMap.get(DxfFileConstants.SEMI_DETACHED);
		else if (codes.contains(DxfFileConstants.ROW_HOUSING))
			return codesMap.get(DxfFileConstants.ROW_HOUSING);
		else if (codes.contains(DxfFileConstants.APARTMENT_BUILDING))
			return codesMap.get(DxfFileConstants.APARTMENT_BUILDING);
		else if (codes.contains(DxfFileConstants.HOUSING_PROJECT))
			return codesMap.get(DxfFileConstants.HOUSING_PROJECT);
		else if (codes.contains(DxfFileConstants.STUDIO_APARTMENTS))
			return codesMap.get(DxfFileConstants.STUDIO_APARTMENTS);
		else if (codes.contains(DxfFileConstants.DHARMASALA))
			return codesMap.get(DxfFileConstants.DHARMASALA);
		else if (codes.contains(DxfFileConstants.DORMITORY))
			return codesMap.get(DxfFileConstants.DORMITORY);
		else if (codes.contains(DxfFileConstants.EWS))
			return codesMap.get(DxfFileConstants.EWS);
		else if (codes.contains(DxfFileConstants.LOW_INCOME_HOUSING))
			return codesMap.get(DxfFileConstants.LOW_INCOME_HOUSING);
		else if (codes.contains(DxfFileConstants.MEDIUM_INCOME_HOUSING))
			return codesMap.get(DxfFileConstants.MEDIUM_INCOME_HOUSING);
		else if (codes.contains(DxfFileConstants.HOSTEL))
			return codesMap.get(DxfFileConstants.HOSTEL);
		else if (codes.contains(DxfFileConstants.SHELTER_HOUSE))
			return codesMap.get(DxfFileConstants.SHELTER_HOUSE);
		else if (codes.contains(DxfFileConstants.STAFF_QAURTER))
			return codesMap.get(DxfFileConstants.STAFF_QAURTER);
		else if (codes.contains(DxfFileConstants.HOTEL))
			return codesMap.get(DxfFileConstants.HOTEL);
		else if (codes.contains(DxfFileConstants.FIVE_STAR_HOTEL))
			return codesMap.get(DxfFileConstants.FIVE_STAR_HOTEL);
		else if (codes.contains(DxfFileConstants.MOTELS))
			return codesMap.get(DxfFileConstants.MOTELS);
		else if (codes.contains(DxfFileConstants.SERVICES_FOR_HOUSEHOLDS))
			return codesMap.get(DxfFileConstants.SERVICES_FOR_HOUSEHOLDS);
		else if (codes.contains(DxfFileConstants.BANK))
			return codesMap.get(DxfFileConstants.BANK);
		else if (codes.contains(DxfFileConstants.RESORTS))
			return codesMap.get(DxfFileConstants.RESORTS);
		else if (codes.contains(DxfFileConstants.LAGOONS_AND_LAGOON_RESORT))
			return codesMap.get(DxfFileConstants.LAGOONS_AND_LAGOON_RESORT);
		else if (codes.contains(DxfFileConstants.AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS))
			return codesMap.get(DxfFileConstants.AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS);
		else if (codes.contains(DxfFileConstants.FINANCIAL_SERVICES_AND_STOCK_EXCHANGES))
			return codesMap.get(DxfFileConstants.FINANCIAL_SERVICES_AND_STOCK_EXCHANGES);
		else if (codes.contains(DxfFileConstants.COLD_STORAGE_AND_ICE_FACTORY))
			return codesMap.get(DxfFileConstants.COLD_STORAGE_AND_ICE_FACTORY);
		else if (codes.contains(DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX))
			return codesMap.get(DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX);
		else if (codes.contains(DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING))
			return codesMap.get(DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING);
		else if (codes.contains(DxfFileConstants.PROFESSIONAL_OFFICES))
			return codesMap.get(DxfFileConstants.PROFESSIONAL_OFFICES);
		else if (codes.contains(DxfFileConstants.DEPARTMENTAL_STORE))
			return codesMap.get(DxfFileConstants.DEPARTMENTAL_STORE);
		else if (codes.contains(DxfFileConstants.GAS_GODOWN))
			return codesMap.get(DxfFileConstants.GAS_GODOWN);
		else if (codes.contains(DxfFileConstants.GODOWNS))
			return codesMap.get(DxfFileConstants.GODOWNS);
		else if (codes.contains(DxfFileConstants.GOOD_STORAGE))
			return codesMap.get(DxfFileConstants.GOOD_STORAGE);
		else if (codes.contains(DxfFileConstants.GUEST_HOUSES))
			return codesMap.get(DxfFileConstants.GUEST_HOUSES);
		else if (codes.contains(DxfFileConstants.HOLIDAY_RESORT))
			return codesMap.get(DxfFileConstants.HOLIDAY_RESORT);
		else if (codes.contains(DxfFileConstants.BOARDING_AND_LODGING_HOUSES))
			return codesMap.get(DxfFileConstants.BOARDING_AND_LODGING_HOUSES);
		else if (codes.contains(DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION))
			return codesMap.get(DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION);
		else if (codes.contains(DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION))
			return codesMap.get(DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION);
		else if (codes.contains(DxfFileConstants.CNG_MOTHER_STATION))
			return codesMap.get(DxfFileConstants.CNG_MOTHER_STATION);
		else if (codes.contains(DxfFileConstants.RESTAURANT))
			return codesMap.get(DxfFileConstants.RESTAURANT);
		else if (codes.contains(DxfFileConstants.LOCAL_RETAIL_SHOPPING))
			return codesMap.get(DxfFileConstants.LOCAL_RETAIL_SHOPPING);
		else if (codes.contains(DxfFileConstants.SHOPPING_CENTER))
			return codesMap.get(DxfFileConstants.SHOPPING_CENTER);
		else if (codes.contains(DxfFileConstants.SHOPPING_MALL))
			return codesMap.get(DxfFileConstants.SHOPPING_MALL);
		else if (codes.contains(DxfFileConstants.SHOWROOM))
			return codesMap.get(DxfFileConstants.SHOWROOM);
		else if (codes.contains(DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE))
			return codesMap.get(DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE);
		else if (codes.contains(DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE))
			return codesMap.get(DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE);
		else if (codes.contains(DxfFileConstants.STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT))
			return codesMap.get(DxfFileConstants.STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT);
		else if (codes.contains(DxfFileConstants.SUPERMARKETS))
			return codesMap.get(DxfFileConstants.SUPERMARKETS);
		else if (codes.contains(DxfFileConstants.WARE_HOUSE))
			return codesMap.get(DxfFileConstants.WARE_HOUSE);
		else if (codes.contains(DxfFileConstants.WHOLESALE_MARKET))
			return codesMap.get(DxfFileConstants.WHOLESALE_MARKET);
		else if (codes.contains(DxfFileConstants.MEDIA_CENTRES))
			return codesMap.get(DxfFileConstants.MEDIA_CENTRES);
		else if (codes.contains(DxfFileConstants.FOOD_COURTS))
			return codesMap.get(DxfFileConstants.FOOD_COURTS);
		else if (codes.contains(DxfFileConstants.WEIGH_BRIDGES))
			return codesMap.get(DxfFileConstants.WEIGH_BRIDGES);
		else if (codes.contains(DxfFileConstants.MERCENTILE))
			return codesMap.get(DxfFileConstants.MERCENTILE);
		else if (codes.contains(DxfFileConstants.AUDITORIUM))
			return codesMap.get(DxfFileConstants.AUDITORIUM);
		else if (codes.contains(DxfFileConstants.BANQUET_HALL))
			return codesMap.get(DxfFileConstants.BANQUET_HALL);
		else if (codes.contains(DxfFileConstants.CINEMA))
			return codesMap.get(DxfFileConstants.CINEMA);
		else if (codes.contains(DxfFileConstants.CLUB))
			return codesMap.get(DxfFileConstants.CLUB);
		else if (codes.contains(DxfFileConstants.MUSIC_PAVILIONS))
			return codesMap.get(DxfFileConstants.MUSIC_PAVILIONS);
		else if (codes.contains(DxfFileConstants.COMMUNITY_HALL))
			return codesMap.get(DxfFileConstants.COMMUNITY_HALL);
		else if (codes.contains(DxfFileConstants.ORPHANAGE))
			return codesMap.get(DxfFileConstants.ORPHANAGE);
		else if (codes.contains(DxfFileConstants.OLD_AGE_HOME))
			return codesMap.get(DxfFileConstants.OLD_AGE_HOME);
		else if (codes.contains(DxfFileConstants.SCIENCE_CENTRE_OR_MUSEUM))
			return codesMap.get(DxfFileConstants.SCIENCE_CENTRE_OR_MUSEUM);
		else if (codes.contains(DxfFileConstants.CONFERNCE_HALL))
			return codesMap.get(DxfFileConstants.CONFERNCE_HALL);
		else if (codes.contains(DxfFileConstants.CONVENTION_HALL))
			return codesMap.get(DxfFileConstants.CONVENTION_HALL);
		else if (codes.contains(DxfFileConstants.SCULPTURE_COMPLEX))
			return codesMap.get(DxfFileConstants.SCULPTURE_COMPLEX);
		else if (codes.contains(DxfFileConstants.CULTURAL_COMPLEX))
			return codesMap.get(DxfFileConstants.CULTURAL_COMPLEX);
		else if (codes.contains(DxfFileConstants.EXHIBITION_CENTER))
			return codesMap.get(DxfFileConstants.EXHIBITION_CENTER);
		else if (codes.contains(DxfFileConstants.GYMNASIA))
			return codesMap.get(DxfFileConstants.GYMNASIA);
		else if (codes.contains(DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP))
			return codesMap.get(DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP);
		else if (codes.contains(DxfFileConstants.MULTIPLEX))
			return codesMap.get(DxfFileConstants.MULTIPLEX);
		else if (codes.contains(DxfFileConstants.MUSUEM))
			return codesMap.get(DxfFileConstants.MUSUEM);
		else if (codes.contains(DxfFileConstants.PLACE_OF_WORKSHIP))
			return codesMap.get(DxfFileConstants.PLACE_OF_WORKSHIP);
		else if (codes.contains(DxfFileConstants.PUBLIC_LIBRARIES))
			return codesMap.get(DxfFileConstants.PUBLIC_LIBRARIES);
		else if (codes.contains(DxfFileConstants.RECREATION_BLDG))
			return codesMap.get(DxfFileConstants.RECREATION_BLDG);
		else if (codes.contains(DxfFileConstants.SPORTS_COMPLEX))
			return codesMap.get(DxfFileConstants.SPORTS_COMPLEX);
		else if (codes.contains(DxfFileConstants.STADIUM))
			return codesMap.get(DxfFileConstants.STADIUM);
		else if (codes.contains(DxfFileConstants.THEATRE))
			return codesMap.get(DxfFileConstants.THEATRE);
		else if (codes.contains(DxfFileConstants.ADMINISTRATIVE_BUILDINGS))
			return codesMap.get(DxfFileConstants.ADMINISTRATIVE_BUILDINGS);
		else if (codes.contains(DxfFileConstants.GOVERNMENT_OFFICES))
			return codesMap.get(DxfFileConstants.GOVERNMENT_OFFICES);
		else if (codes.contains(DxfFileConstants.LOCAL_AND_SEMI_GOVERNMENT_OFFICES))
			return codesMap.get(DxfFileConstants.LOCAL_AND_SEMI_GOVERNMENT_OFFICES);
		else if (codes.contains(DxfFileConstants.POLICE_OR_ARMY_OR_BARRACK))
			return codesMap.get(DxfFileConstants.POLICE_OR_ARMY_OR_BARRACK);
		else if (codes.contains(DxfFileConstants.RELIGIOUS_BUILDING))
			return codesMap.get(DxfFileConstants.RELIGIOUS_BUILDING);
		else if (codes.contains(DxfFileConstants.SOCIAL_AND_WELFARE_CENTRES))
			return codesMap.get(DxfFileConstants.SOCIAL_AND_WELFARE_CENTRES);
		else if (codes.contains(DxfFileConstants.CLINIC))
			return codesMap.get(DxfFileConstants.CLINIC);
		else if (codes.contains(DxfFileConstants.DISPENSARY))
			return codesMap.get(DxfFileConstants.DISPENSARY);
		else if (codes.contains(DxfFileConstants.DIAGNOSTIC_CENTRE))
			return codesMap.get(DxfFileConstants.DIAGNOSTIC_CENTRE);
		else if (codes.contains(DxfFileConstants.GOVT_SEMI_GOVT_HOSPITAL))
			return codesMap.get(DxfFileConstants.GOVT_SEMI_GOVT_HOSPITAL);
		else if (codes.contains(DxfFileConstants.REGISTERED_TRUST))
			return codesMap.get(DxfFileConstants.REGISTERED_TRUST);
		else if (codes.contains(DxfFileConstants.HEALTH_CENTRE))
			return codesMap.get(DxfFileConstants.HEALTH_CENTRE);
		else if (codes.contains(DxfFileConstants.HOSPITAL))
			return codesMap.get(DxfFileConstants.HOSPITAL);
		else if (codes.contains(DxfFileConstants.LAB))
			return codesMap.get(DxfFileConstants.LAB);
		else if (codes.contains(DxfFileConstants.MATERNITY_HOME))
			return codesMap.get(DxfFileConstants.MATERNITY_HOME);
		else if (codes.contains(DxfFileConstants.MEDICAL_BUILDING))
			return codesMap.get(DxfFileConstants.MEDICAL_BUILDING);
		else if (codes.contains(DxfFileConstants.NURSING_HOME))
			return codesMap.get(DxfFileConstants.NURSING_HOME);
		else if (codes.contains(DxfFileConstants.POLYCLINIC))
			return codesMap.get(DxfFileConstants.POLYCLINIC);
		else if (codes.contains(DxfFileConstants.REHABILITAION_CENTER))
			return codesMap.get(DxfFileConstants.REHABILITAION_CENTER);
		else if (codes.contains(DxfFileConstants.VETERINARY_HOSPITAL_FOR_PET_ANIMALS_AND_BIRDS))
			return codesMap.get(DxfFileConstants.VETERINARY_HOSPITAL_FOR_PET_ANIMALS_AND_BIRDS);
		else if (codes.contains(DxfFileConstants.RESEARCH_AND_TRAINING_INSTITUTE))
			return codesMap.get(DxfFileConstants.RESEARCH_AND_TRAINING_INSTITUTE);
		else if (codes.contains(DxfFileConstants.POLICE_STATION))
			return codesMap.get(DxfFileConstants.POLICE_STATION);
		else if (codes.contains(DxfFileConstants.FIRE_STATION))
			return codesMap.get(DxfFileConstants.FIRE_STATION);
		else if (codes.contains(DxfFileConstants.JAIL_OR_PRISON))
			return codesMap.get(DxfFileConstants.JAIL_OR_PRISON);
		else if (codes.contains(DxfFileConstants.POST_OFFICE))
			return codesMap.get(DxfFileConstants.POST_OFFICE);
		else if (codes.contains(DxfFileConstants.BILL_COLLECTION_CENTER))
			return codesMap.get(DxfFileConstants.BILL_COLLECTION_CENTER);
		else if (codes.contains(DxfFileConstants.BROADCASTING_TRANSMISSION_CENTRE))
			return codesMap.get(DxfFileConstants.BROADCASTING_TRANSMISSION_CENTRE);
		else if (codes.contains(DxfFileConstants.BURIAL_AND_CREMATION_GROUNDS))
			return codesMap.get(DxfFileConstants.BURIAL_AND_CREMATION_GROUNDS);
		else if (codes.contains(DxfFileConstants.PUBLIC_DISTRIBUTION_SYSTEM_SHOP))
			return codesMap.get(DxfFileConstants.PUBLIC_DISTRIBUTION_SYSTEM_SHOP);
		else if (codes.contains(DxfFileConstants.PUBLIC_TOILETS_IN_PUBLIC_AREA))
			return codesMap.get(DxfFileConstants.PUBLIC_TOILETS_IN_PUBLIC_AREA);
		else if (codes.contains(DxfFileConstants.PUBLIC_UTILITY_BLDG))
			return codesMap.get(DxfFileConstants.PUBLIC_UTILITY_BLDG);
		else if (codes.contains(DxfFileConstants.SUB_STATION))
			return codesMap.get(DxfFileConstants.SUB_STATION);
		else if (codes.contains(DxfFileConstants.TELECOMMUNICATION))
			return codesMap.get(DxfFileConstants.TELECOMMUNICATION);
		else if (codes.contains(DxfFileConstants.WATER_PUMPING_STATIONS))
			return codesMap.get(DxfFileConstants.WATER_PUMPING_STATIONS);
		else if (codes.contains(DxfFileConstants.SERVICE_AND_STORAGE_YARDS))
			return codesMap.get(DxfFileConstants.SERVICE_AND_STORAGE_YARDS);
		else if (codes.contains(DxfFileConstants.ELECTRICAL_DISTRIBUTION_DEPOTS))
			return codesMap.get(DxfFileConstants.ELECTRICAL_DISTRIBUTION_DEPOTS);
		else if (codes.contains(DxfFileConstants.INDUSTRIAL_BUILDINGS_FACTORIES_WORKSHOPS_ETC))
			return codesMap.get(DxfFileConstants.INDUSTRIAL_BUILDINGS_FACTORIES_WORKSHOPS_ETC);
		else if (codes.contains(DxfFileConstants.NON_POLLUTING_INDUSTRIAL))
			return codesMap.get(DxfFileConstants.NON_POLLUTING_INDUSTRIAL);
		else if (codes.contains(DxfFileConstants.IT_ITES_BUILDINGS))
			return codesMap.get(DxfFileConstants.IT_ITES_BUILDINGS);
		else if (codes.contains(DxfFileConstants.SEZ_INDUSTRIAL))
			return codesMap.get(DxfFileConstants.SEZ_INDUSTRIAL);
		else if (codes.contains(DxfFileConstants.LOADING_OR_UNLOADING_SPACES))
			return codesMap.get(DxfFileConstants.LOADING_OR_UNLOADING_SPACES);
		else if (codes.contains(DxfFileConstants.FLATTED_FACTORY))
			return codesMap.get(DxfFileConstants.FLATTED_FACTORY);
		else if (codes.contains(DxfFileConstants.SMALL_FACTORIES_AND_ETC_FALLS_IN_INDUSTRIAL))
			return codesMap.get(DxfFileConstants.SMALL_FACTORIES_AND_ETC_FALLS_IN_INDUSTRIAL);
		else if (codes.contains(DxfFileConstants.COACHING_CENTRE))
			return codesMap.get(DxfFileConstants.COACHING_CENTRE);
		else if (codes.contains(DxfFileConstants.COMMERCIAL_INSTITUTE))
			return codesMap.get(DxfFileConstants.COMMERCIAL_INSTITUTE);
		else if (codes.contains(DxfFileConstants.COLLEGE))
			return codesMap.get(DxfFileConstants.COLLEGE);
		else if (codes.contains(DxfFileConstants.COMPUTER_TRAINING_INSTITUTE))
			return codesMap.get(DxfFileConstants.COMPUTER_TRAINING_INSTITUTE);
		else if (codes.contains(DxfFileConstants.NURSERY_SCHOOL))
			return codesMap.get(DxfFileConstants.NURSERY_SCHOOL);
		else if (codes.contains(DxfFileConstants.PRIMARY_SCHOOL))
			return codesMap.get(DxfFileConstants.PRIMARY_SCHOOL);
		else if (codes.contains(DxfFileConstants.HOSTEL_CAPTIVE))
			return codesMap.get(DxfFileConstants.HOSTEL_CAPTIVE);
		else if (codes.contains(DxfFileConstants.HIGH_SCHOOL))
			return codesMap.get(DxfFileConstants.HIGH_SCHOOL);
		else if (codes.contains(DxfFileConstants.PLAY_SCHOOL))
			return codesMap.get(DxfFileConstants.PLAY_SCHOOL);
		else if (codes.contains(DxfFileConstants.CRECHE))
			return codesMap.get(DxfFileConstants.CRECHE);
		else if (codes.contains(DxfFileConstants.SCHOOL_FOR_MENTALLY_CHALLENGED))
			return codesMap.get(DxfFileConstants.SCHOOL_FOR_MENTALLY_CHALLENGED);
		else if (codes.contains(DxfFileConstants.ART_ACADEMY))
			return codesMap.get(DxfFileConstants.ART_ACADEMY);
		else if (codes.contains(DxfFileConstants.TECHNICAL_COLLEGE))
			return codesMap.get(DxfFileConstants.TECHNICAL_COLLEGE);
		else if (codes.contains(DxfFileConstants.SPORTS_TRAINING_CENTERS))
			return codesMap.get(DxfFileConstants.SPORTS_TRAINING_CENTERS);
		else if (codes.contains(DxfFileConstants.TRAINING_INSTITUTE))
			return codesMap.get(DxfFileConstants.TRAINING_INSTITUTE);
		else if (codes.contains(DxfFileConstants.VETERINARY_INSTITUTE))
			return codesMap.get(DxfFileConstants.VETERINARY_INSTITUTE);
		else if (codes.contains(DxfFileConstants.MEDICAL_COLLEGE))
			return codesMap.get(DxfFileConstants.MEDICAL_COLLEGE);
		else if (codes.contains(DxfFileConstants.RESEARCH_AND_TRAINING_CENTER))
			return codesMap.get(DxfFileConstants.RESEARCH_AND_TRAINING_CENTER);
		else if (codes.contains(DxfFileConstants.AIRPORT))
			return codesMap.get(DxfFileConstants.AIRPORT);
		else if (codes.contains(DxfFileConstants.AUTO_STAND))
			return codesMap.get(DxfFileConstants.AUTO_STAND);
		else if (codes.contains(DxfFileConstants.METRO_STATION))
			return codesMap.get(DxfFileConstants.METRO_STATION);
		else if (codes.contains(DxfFileConstants.BUS_STAND))
			return codesMap.get(DxfFileConstants.BUS_STAND);
		else if (codes.contains(DxfFileConstants.BUS_TERMINAL))
			return codesMap.get(DxfFileConstants.BUS_TERMINAL);
		else if (codes.contains(DxfFileConstants.ISBT))
			return codesMap.get(DxfFileConstants.ISBT);
		else if (codes.contains(DxfFileConstants.RAILWAY_STATION))
			return codesMap.get(DxfFileConstants.RAILWAY_STATION);
		else if (codes.contains(DxfFileConstants.TAXI_STAND))
			return codesMap.get(DxfFileConstants.TAXI_STAND);
		else if (codes.contains(DxfFileConstants.MULTI_LEVEL_CAR_PARKING))
			return codesMap.get(DxfFileConstants.MULTI_LEVEL_CAR_PARKING);
		else if (codes.contains(DxfFileConstants.PUBLIC_PARKING))
			return codesMap.get(DxfFileConstants.PUBLIC_PARKING);
		else if (codes.contains(DxfFileConstants.TOLL_PLAZA))
			return codesMap.get(DxfFileConstants.TOLL_PLAZA);
		else if (codes.contains(DxfFileConstants.TRUCK_TERMINAL))
			return codesMap.get(DxfFileConstants.TRUCK_TERMINAL);
		else if (codes.contains(DxfFileConstants.AGRICULTURE_FARM))
			return codesMap.get(DxfFileConstants.AGRICULTURE_FARM);
		else if (codes.contains(DxfFileConstants.AGRO_GODOWN))
			return codesMap.get(DxfFileConstants.AGRO_GODOWN);
		else if (codes.contains(DxfFileConstants.AGRO_RESEARCH_FARM))
			return codesMap.get(DxfFileConstants.AGRO_RESEARCH_FARM);
		else if (codes.contains(DxfFileConstants.FARM_HOUSE))
			return codesMap.get(DxfFileConstants.FARM_HOUSE);
		else if (codes.contains(DxfFileConstants.COUNTRY_HOMES))
			return codesMap.get(DxfFileConstants.COUNTRY_HOMES);
		else if (codes.contains(DxfFileConstants.NURSERY_AND_GREEN_HOUSES))
			return codesMap.get(DxfFileConstants.NURSERY_AND_GREEN_HOUSES);
		else if (codes.contains(DxfFileConstants.POLUTRY_DIARY_AND_SWINE_OR_GOAT_OR_HORSE))
			return codesMap.get(DxfFileConstants.POLUTRY_DIARY_AND_SWINE_OR_GOAT_OR_HORSE);
		else if (codes.contains(DxfFileConstants.HORTICULTURE))
			return codesMap.get(DxfFileConstants.HORTICULTURE);
		else if (codes.contains(DxfFileConstants.SERI_CULTURE))
			return codesMap.get(DxfFileConstants.SERI_CULTURE);
		else
			return null;

	}

	protected OccupancyTypeHelper checkMostRestrictiveFarForMixedUse(Set<OccupancyTypeHelper> distinctOccupancyTypes,Plan pl) {
		OccupancyTypeHelper occupancyTypeHelper = null;
		if(distinctOccupancyTypes==null || distinctOccupancyTypes.size()<1) {
			return occupancyTypeHelper;
		}
		boolean isMixesUseCase = false;
		if(!odishaMixedUseUtill.allowedProvision(distinctOccupancyTypes, pl)) {
			if(!odishaMixedUseUtill.checkAdditionalAllowedProvision(distinctOccupancyTypes, pl))
				isMixesUseCase = true;
		}
		if(isMixesUseCase)
			occupancyTypeHelper = odishaMixedUseUtill.getMisxedUseOccupancyTypeHelper(pl);
		return occupancyTypeHelper;
	}
	
	//	private Boolean processFarForSpecialOccupancy(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal far,
//			String typeOfArea, BigDecimal roadWidth, HashMap<String, String> errors) {
//		boolean flage = false;
//		if (!flage)
//			return flage;
//
//		OccupancyTypeHelper mostRestrictiveOccupancyType = pl.getVirtualBuilding() != null
//				? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
//				: null;
//		String expectedResult = StringUtils.EMPTY;
//		boolean isAccepted = false;
//		if (mostRestrictiveOccupancyType != null && mostRestrictiveOccupancyType.getSubtype() != null) {
//			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_ECFG)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(A_FH)) {
//				isAccepted = far.compareTo(POINTTWO) <= 0;
//				expectedResult = "<= 0.2";
//				return true;
//			}
//
//			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_SAS)) {
//				isAccepted = far.compareTo(POINTFOUR) <= 0;
//				expectedResult = "<= 0.4";
//				return true;
//			}
//
//			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(D_B)) {
//				isAccepted = far.compareTo(POINTFIVE) <= 0;
//				expectedResult = "<= 0.5";
//				return true;
//			}
//
//			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(D_C)) {
//				isAccepted = far.compareTo(POINTSIX) <= 0;
//				expectedResult = "<= 0.6";
//				return true;
//			}
//
//			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(D_A)) {
//				isAccepted = far.compareTo(POINTSEVEN) <= 0;
//				expectedResult = "<= 0.7";
//				return true;
//			}
//
//			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(H_PP)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_NS)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(M_DFPAB)) {
//				isAccepted = far.compareTo(ONE) <= 0;
//				expectedResult = "<= 1";
//				return true;
//			}
//
//			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_PS)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_SFMC)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_SFDAP)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_EARC)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_MCH)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_BH)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_CRC)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_CA)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_SC)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(S_ICC)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(A2)) {
//				isAccepted = far.compareTo(ONE_POINTTWO) <= 0;
//				expectedResult = "<= 1.2";
//				return true;
//			}
//
//			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(B2)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_CLG)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(M_OHF)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(M_VH)
//					|| mostRestrictiveOccupancyType.getSubtype().getCode().equals(M_NAPI)) {
//				isAccepted = far.compareTo(ONE_POINTFIVE) <= 0;
//				expectedResult = "<= 1.5";
//				return true;
//			}
//
//			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(A_SA)) {
//				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
//				expectedResult = "<= 2.5";
//				return true;
//			}
//
//			if (mostRestrictiveOccupancyType.getSubtype().getCode().equals(E_SACA)) {
//				isAccepted = far.compareTo(FIFTEEN) <= 0;
//				expectedResult = "<= 15";
//				return true;
//			}
//
//		}
//
//		String occupancyName = occupancyType.getSubtype() != null ? occupancyType.getSubtype().getName()
//				: occupancyType.getType().getName();
//
//		if (StringUtils.isNotBlank(expectedResult)) {
//			buildResult(pl, occupancyName, far, typeOfArea, roadWidth, expectedResult, isAccepted);
//		}
//
//		return false;
//	}

	private void processFar(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal far, String typeOfArea,
			BigDecimal roadWidth, HashMap<String, String> errors) {

		String expectedResult = StringUtils.EMPTY;
		boolean isAccepted = false;

		pl.getFarDetails().setProvidedFar(far.doubleValue());

		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		// TODO: 11. Adding Far restrictions for Below ULBs.

		// City level customization for Puri code starts here.
		try {

			if (pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea() != null
					&& !pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea().equals(DxfFileConstants.NA)
					&& Integer.parseInt(pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea()) == 2
					&& !(DxfFileConstants.D.equals(occupancyType.getType().getCode())
							|| DxfFileConstants.H.equals(occupancyType.getType().getCode())
							|| DxfFileConstants.M.equals(occupancyType.getType().getCode())
							|| DxfFileConstants.AF.equals(occupancyType.getType().getCode()))) {

				if (pl.getThirdPartyUserTenantld().equals("od.puri")
						|| pl.getThirdPartyUserTenantld().equals("od.purikonarkdevelopmentauthority")
						|| pl.getThirdPartyUserTenantld().equals("od.konark")
						|| pl.getThirdPartyUserTenantld().equals("od.nimapara")) {

					// POINT 5: a)
					if (DxfFileConstants.A.equals(occupancyType.getType().getCode())
							|| DxfFileConstants.B.equals(occupancyType.getType().getCode())) {

						if (roadWidth.compareTo(BigDecimal.valueOf(0)) >= 0
								&& roadWidth.compareTo(ROAD_WIDTH_SIX) < 0) {

							isAccepted = far.compareTo(ONE) <= 0;
							pl.getFarDetails().setBaseFar(ONE.doubleValue());
							pl.getFarDetails().setPermissableFar(ONE.doubleValue());
							expectedResult = "<= 1.00";

						} else if (roadWidth.compareTo(ROAD_WIDTH_SIX) >= 0
								&& roadWidth.compareTo(ROAD_WIDTH_NINE) < 0) {

							isAccepted = far.compareTo(ONE_POINTFIVE) <= 0;
							pl.getFarDetails().setBaseFar(ONE_POINTFIVE.doubleValue());
							pl.getFarDetails().setPermissableFar(ONE_POINTFIVE.doubleValue());
							expectedResult = "<= 1.50";

						} else if (roadWidth.compareTo(ROAD_WIDTH_NINE) >= 0
								&& roadWidth.compareTo(ROAD_WIDTH_TWELVE) < 0) {

							isAccepted = far.compareTo(ONE_POINTSEVENFIVE) <= 0;
							pl.getFarDetails().setBaseFar(ONE_POINTSEVENFIVE.doubleValue());
							pl.getFarDetails().setPermissableFar(ONE_POINTSEVENFIVE.doubleValue());
							expectedResult = "<= 1.75";

						} else if (roadWidth.compareTo(ROAD_WIDTH_TWELVE) >= 0
								&& roadWidth.compareTo(ROAD_WIDTH_FIFTEEN) < 0) {

							isAccepted = far.compareTo(TWO) <= 0;
							pl.getFarDetails().setBaseFar(TWO.doubleValue());
							pl.getFarDetails().setPermissableFar(TWO.doubleValue());
							expectedResult = "<= 2.00";

						} else if (roadWidth.compareTo(ROAD_WIDTH_FIFTEEN) >= 0
								&& roadWidth.compareTo(ROAD_WIDTH_EIGHTEEN) < 0) {

							isAccepted = far.compareTo(TWO_POINTTWOFIVE) <= 0;
							pl.getFarDetails().setBaseFar(TWO.doubleValue());
							pl.getFarDetails().setPermissableFar(TWO_POINTTWOFIVE.doubleValue());
							expectedResult = "<= 2.25";

						} else if (roadWidth.compareTo(ROAD_WIDTH_EIGHTEEN) >= 0
								&& roadWidth.compareTo(ROAD_WIDTH_THIRTY) < 0) {

							isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
							pl.getFarDetails().setBaseFar(TWO.doubleValue());
							pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
							expectedResult = "<= 2.50";

						} else if (roadWidth.compareTo(ROAD_WIDTH_THIRTY) >= 0) {

							isAccepted = far.compareTo(TWO_POINTSEVENFIVE) <= 0;
							pl.getFarDetails().setBaseFar(TWO.doubleValue());
							pl.getFarDetails().setPermissableFar(TWO_POINTSEVENFIVE.doubleValue());
							expectedResult = "<= 2.75";

						}
					}

					// POINT 5: b)
					if (DxfFileConstants.C.equals(occupancyType.getType().getCode())
							|| pl.getPlanInformation().isAssemblyBuilding()
							|| DxfFileConstants.F.equals(occupancyType.getType().getCode())) {

						if (pl.getPlanInformation().getPlotArea().compareTo(new BigDecimal("1000")) <= 0) {
							pl.getFarDetails().setBaseFar(ONE_POINTFIVE.doubleValue());
							pl.getFarDetails().setPermissableFar(ONE_POINTFIVE.doubleValue());
						} else if (pl.getPlanInformation().getPlotArea().compareTo(new BigDecimal("1000")) >= 0) {
							pl.getFarDetails().setBaseFar(ONE_POINTSEVENFIVE.doubleValue());
							pl.getFarDetails().setPermissableFar(ONE_POINTSEVENFIVE.doubleValue());
						}
					}

					// POINT 5: c)
					if (DxfFileConstants.G.equals(occupancyType.getType().getCode())) {
						pl.getFarDetails().setBaseFar(ONE.doubleValue());
						pl.getFarDetails().setPermissableFar(ONE.doubleValue());
					}

					// POINT 5: d)
					if (DxfFileConstants.E.equals(occupancyType.getType().getCode())) {
						pl.getFarDetails().setBaseFar(ONE.doubleValue());
						pl.getFarDetails().setPermissableFar(ONE.doubleValue());
					}

				}

			} else if (pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea() != null
					&& !pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea().equals(DxfFileConstants.NA)
					&& Integer.parseInt(pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea()) == 3
					&& (occupancyType.getSubtype().getColor() == 30 || occupancyType.getSubtype().getColor() == 31
							|| occupancyType.getSubtype().getColor() == 32
							|| occupancyType.getSubtype().getColor() == 36
							|| occupancyType.getSubtype().getColor() == 37)) {

				if (getMaxNumberOfFloor(pl) > 2) {
					pl.addError("Max_Floor_exceeded", "Maximum 2 floors are allowed in CRZ 3 area.");
				}
				pl.getFarDetails().setBaseFar(POINTTHREETHREE.doubleValue());
				pl.getFarDetails().setPermissableFar(POINTTHREETHREE.doubleValue());

			}

			else {

				// City level customization for Puri ends here.


		if (DxfFileConstants.EWS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.LOW_INCOME_HOUSING.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL
						.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.OC_PUBLIC_UTILITY.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.OC_EDUCATION.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.OC_TRANSPORTATION.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.AGRICULTURE_FARM.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.AGRO_GODOWN.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.AGRO_RESEARCH_FARM.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.NURSERY_AND_GREEN_HOUSES.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.POLUTRY_DIARY_AND_SWINE_OR_GOAT_OR_HORSE
						.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.HORTICULTURE.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.SERI_CULTURE.equals(occupancyTypeHelper.getSubtype().getCode())) {
			generalCriteriasFar(pl.getFarDetails(), roadWidth);
		} else if (DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION
				.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION
						.equals(occupancyTypeHelper.getSubtype().getCode())) {
			pl.getFarDetails().setBaseFar(2d);
			pl.getFarDetails().setPermissableFar(2d);
		} else if (DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING
				.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.SEMI_DETACHED.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.APARTMENT_BUILDING.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.STUDIO_APARTMENTS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.DHARMASALA.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.DORMITORY.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.HOSTEL.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.SHELTER_HOUSE.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.STAFF_QAURTER.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.OC_COMMERCIAL.equals(occupancyTypeHelper.getType().getCode())) {

			BigDecimal buildingHeight = BigDecimal.ZERO;
			buildingHeight = OdishaUtill.getMaxBuildingHeight(pl);
			
			//TODO:11 Else rule is not written. Basefar is set as NA in report.
			if (buildingHeight.compareTo(new BigDecimal("10")) > 0
					|| pl.getPlot().getArea().compareTo(new BigDecimal("115")) > 0) {
				generalCriteriasFar(pl.getFarDetails(), roadWidth);
			}

		} else if (DxfFileConstants.ROW_HOUSING.equals(occupancyTypeHelper.getSubtype().getCode())) {

			if (roadWidth.compareTo(new BigDecimal("6")) < 0) {
				pl.getFarDetails().setBaseFar(1.5);
				pl.getFarDetails().setPermissableFar(1.5);
			} else {
				pl.getFarDetails().setBaseFar(1.5);
				pl.getFarDetails().setPermissableFar(1.5);
			}
		} else if (DxfFileConstants.HOUSING_PROJECT.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.APARTMENT_BUILDING.equals(occupancyTypeHelper.getSubtype().getCode())) {
			BigDecimal buildingHeight = BigDecimal.ZERO;
			buildingHeight = OdishaUtill.getMaxBuildingHeight(pl);
			if (buildingHeight.compareTo(new BigDecimal("10")) > 0
					|| pl.getPlot().getArea().compareTo(new BigDecimal("115")) > 0) {
				generalCriteriasFar(pl.getFarDetails(), roadWidth);
			}
		} else if (DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(occupancyTypeHelper.getSubtype().getCode())) {
			BigDecimal buildingHeight = BigDecimal.ZERO;
			buildingHeight = OdishaUtill.getMaxBuildingHeight(pl);
			if (buildingHeight.compareTo(new BigDecimal("10")) > 0
					|| pl.getPlot().getArea().compareTo(new BigDecimal("115")) > 0) {
				generalCriteriasFar(pl.getFarDetails(), roadWidth);
//				pl.getFarDetails().setPermissableFar(pl.getFarDetails().getPermissableFar() + 0.25);
				pl.getFarDetails().setPermissableFar(pl.getFarDetails().getPermissableFar());
			}

		} else if (DxfFileConstants.FARM_HOUSE.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.COUNTRY_HOMES.equals(occupancyTypeHelper.getSubtype().getCode())) {

			validateBuilupArea(pl);
		} else if(DxfFileConstants.MIXED_USE.equals(occupancyTypeHelper.getSubtype().getCode())) {
			generalCriteriasFar(pl.getFarDetails(), roadWidth);
		}
			}
		// far validation
		BigDecimal permissableFar=BigDecimal.ZERO;
		if (pl.getFarDetails() != null && pl.getFarDetails().getPermissableFar() != null
				&& pl.getFarDetails().getPermissableFar() > 0) {
			permissableFar=new BigDecimal(pl.getFarDetails().getPermissableFar());
			permissableFar=permissableFar.setScale(2,BigDecimal.ROUND_HALF_UP);
		}
		
		if(ApplicationType.OCCUPANCY_CERTIFICATE.equals(pl.getApplicationType())) {
			permissableFar=permissableFar.multiply(new BigDecimal("1.10"));
		}
		
		if (pl.getFarDetails() != null && pl.getFarDetails().getPermissableFar() != null
				&& pl.getFarDetails().getPermissableFar() > 0) {
			isAccepted = far.compareTo(permissableFar) <= 0;
		} else
			isAccepted = true;
		
		String occupancyName = occupancyType.getType().getName();
		buildResult(pl, occupancyName, far, typeOfArea, roadWidth, expectedResult, isAccepted);
		checkFarBanchMarkValue(pl);
		
		// TDR calculation
		if(pl.getPlanInformation().getAdditionalTdr() != null && pl.getPlanInformation().getAdditionalTdr().compareTo(BigDecimal.ZERO) > 0) {
			updateAdditionalTdr(pl);
		}
	} catch (Exception e) {
		e.printStackTrace();
	}
}
	

	private void checkFarBanchMarkValue(Plan pl) {
		FarDetails farDetails=pl.getFarDetails();
		
		if(farDetails!=null && farDetails.getBaseFar()!=null &&farDetails.getBaseFar()>0) {
			if(farDetails.getProvidedFar()>farDetails.getBaseFar() && pl.getPlanInformation().getBenchmarkValuePerAcre().compareTo(BigDecimal.ZERO)<=0)
				pl.addError("benchmarkValuePerAcre", "PER_ACRE_BENCHMARK_VALUE_OF_LAND Should be greater than zero.");
		}
		
	}

	private void validateBuilupArea(Plan pl) {
		BigDecimal totalBuildUpArea = pl.getVirtualBuilding().getTotalBuitUpArea();
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		BigDecimal plotArea = pl.getPlot().getArea();

		if (DxfFileConstants.FARM_HOUSE.equals(occupancyTypeHelper.getSubtype().getCode())) {
			int multipler = plotArea.divide(new BigDecimal("10000"), 1).intValue();
			if (multipler < 0)
				multipler = 1;
			BigDecimal AllowedBuildUpArea = new BigDecimal("500").multiply(new BigDecimal(multipler));
			if (totalBuildUpArea.compareTo(AllowedBuildUpArea) > 0)
				pl.addError("Far", "Max " + AllowedBuildUpArea + " SQM BuildUpArea is Allowed");
		}

		if (DxfFileConstants.FARM_HOUSE.equals(occupancyTypeHelper.getSubtype().getCode())) {
			int multipler = plotArea.divide(new BigDecimal("2000"), 1).intValue();
			if (multipler < 0)
				multipler = 1;
			BigDecimal AllowedBuildUpArea = new BigDecimal("250").multiply(new BigDecimal(multipler));
			if (totalBuildUpArea.compareTo(AllowedBuildUpArea) > 0)
				pl.addError("Far", "Max " + AllowedBuildUpArea + " SQM BuildUpArea is Allowed");
		}
	}

	private void generalCriteriasFar(FarDetails farDetails, BigDecimal roadWidth) {
		if (roadWidth.compareTo(new BigDecimal("6")) < 0) {
			farDetails.setBaseFar(1.5);
			farDetails.setPermissableFar(1.5);
		} else if (roadWidth.compareTo(new BigDecimal("6")) >= 0 && roadWidth.compareTo(new BigDecimal("9")) < 0) {
			farDetails.setBaseFar(2d);
			farDetails.setPermissableFar(2d);
		} else if (roadWidth.compareTo(new BigDecimal("9")) >= 0 && roadWidth.compareTo(new BigDecimal("12")) < 0) {
			farDetails.setBaseFar(2d);
			farDetails.setPermissableFar(3d);
		} else if (roadWidth.compareTo(new BigDecimal("12")) >= 0 && roadWidth.compareTo(new BigDecimal("18")) < 0) {
			farDetails.setBaseFar(2d);
			farDetails.setPermissableFar(4d);
		} else if (roadWidth.compareTo(new BigDecimal("18")) >= 0 && roadWidth.compareTo(new BigDecimal("30")) < 0) {
			farDetails.setBaseFar(2d);
			farDetails.setPermissableFar(5d);
		} else if (roadWidth.compareTo(new BigDecimal("30")) >= 0 && roadWidth.compareTo(new BigDecimal("60")) < 0) {
			farDetails.setBaseFar(2d);
			farDetails.setPermissableFar(6d);
		} else {
			farDetails.setBaseFar(2d);
			farDetails.setPermissableFar(7d);
		}

	}

	private void processFarResidential(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal far, String typeOfArea,
			BigDecimal roadWidth, HashMap<String, String> errors) {

		String expectedResult = StringUtils.EMPTY;
		boolean isAccepted = true;

		if (typeOfArea.equalsIgnoreCase(OLD)) {
			if (roadWidth.compareTo(ROAD_WIDTH_TWO_POINTFOUR) < 0) {
				errors.put(OLD_AREA_ERROR, OLD_AREA_ERROR_MSG);
				pl.addErrors(errors);
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWO_POINTFOURFOUR) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_THREE_POINTSIX) < 0) {
				isAccepted = far.compareTo(ONE_POINTTWO) <= 0;
				pl.getFarDetails().setPermissableFar(ONE_POINTTWO.doubleValue());
				expectedResult = "<= 1.2";
			} else if (roadWidth.compareTo(ROAD_WIDTH_THREE_POINTSIX) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_FOUR_POINTEIGHT) < 0) {
				isAccepted = far.compareTo(ONE_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(ONE_POINTFIVE.doubleValue());
				expectedResult = "<= 1.5";
			} else if (roadWidth.compareTo(ROAD_WIDTH_FOUR_POINTEIGHT) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) < 0) {
				isAccepted = far.compareTo(ONE_POINTEIGHT) <= 0;
				pl.getFarDetails().setPermissableFar(ONE_POINTEIGHT.doubleValue());
				expectedResult = "<= 1.8";
			} else if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_NINE_POINTONE) < 0) {
				isAccepted = far.compareTo(TWO) <= 0;
				pl.getFarDetails().setPermissableFar(TWO.doubleValue());
				expectedResult = "<= 2";
			} else if (roadWidth.compareTo(ROAD_WIDTH_NINE_POINTONE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_TWELVE_POINTTWO) < 0) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
				expectedResult = "<= 2.5";
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWELVE_POINTTWO) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_EIGHTEEN_POINTTHREE) < 0) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
				expectedResult = "<= 2.5";
			} else if (roadWidth.compareTo(ROAD_WIDTH_EIGHTEEN_POINTTHREE) >= 0) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
				expectedResult = "<= 2.5";
			}

		}

		if (typeOfArea.equalsIgnoreCase(NEW)) {
			if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) < 0) {
				errors.put(NEW_AREA_ERROR, NEW_AREA_ERROR_MSG);
				pl.addErrors(errors);
			} else if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_NINE_POINTONE) < 0) {
				isAccepted = far.compareTo(TWO) <= 0;
				pl.getFarDetails().setPermissableFar(TWO.doubleValue());
				expectedResult = "<= 2";
			} else if (roadWidth.compareTo(ROAD_WIDTH_NINE_POINTONE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_TWELVE_POINTTWO) < 0) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
				expectedResult = "<= 2.5";
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWELVE_POINTTWO) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_EIGHTEEN_POINTTHREE) < 0) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
				expectedResult = "<= 2.5";
			} else if (roadWidth.compareTo(ROAD_WIDTH_EIGHTEEN_POINTTHREE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_TWENTYFOUR_POINTFOUR) < 0) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
				expectedResult = "<= 2.5";
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWENTYFOUR_POINTFOUR) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_TWENTYSEVEN_POINTFOUR) < 0) {
				isAccepted = far.compareTo(THREE) <= 0;
				pl.getFarDetails().setPermissableFar(THREE.doubleValue());
				expectedResult = "<= 3";
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWENTYSEVEN_POINTFOUR) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_THIRTY_POINTFIVE) < 0) {
				isAccepted = far.compareTo(THREE_POINTTWOFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(THREE_POINTTWOFIVE.doubleValue());
				expectedResult = "<= 3.25";
			} else if (roadWidth.compareTo(ROAD_WIDTH_THIRTY_POINTFIVE) >= 0) {
				isAccepted = far.compareTo(THREE_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(THREE_POINTFIVE.doubleValue());
				expectedResult = "<= 3.5";
			}

		}

		String occupancyName = occupancyType.getType().getName();
		if (errors.isEmpty() && StringUtils.isNotBlank(expectedResult)) {
			buildResult(pl, occupancyName, far, typeOfArea, roadWidth, expectedResult, isAccepted);
		}
	}

	private void processFarNonResidential(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal far, String typeOfArea,
			BigDecimal roadWidth, HashMap<String, String> errors) {

		String expectedResult = StringUtils.EMPTY;
		boolean isAccepted = false;

		if (typeOfArea.equalsIgnoreCase(OLD)) {
			if (roadWidth.compareTo(ROAD_WIDTH_TWO_POINTFOUR) < 0) {
				errors.put(OLD_AREA_ERROR, OLD_AREA_ERROR_MSG);
				pl.addErrors(errors);
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWO_POINTFOURFOUR) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_THREE_POINTSIX) < 0) {
				isAccepted = far.compareTo(ONE_POINTTWO) <= 0;
				pl.getFarDetails().setPermissableFar(TWO.doubleValue());
				expectedResult = "<= 1.2";
			} else if (roadWidth.compareTo(ROAD_WIDTH_THREE_POINTSIX) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_FOUR_POINTEIGHT) < 0) {
				isAccepted = far.compareTo(BigDecimal.ZERO) >= 0;
				pl.getFarDetails().setPermissableFar(BigDecimal.ZERO.doubleValue());
				expectedResult = "0";
			} else if (roadWidth.compareTo(ROAD_WIDTH_FOUR_POINTEIGHT) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) < 0) {
				isAccepted = far.compareTo(BigDecimal.ZERO) >= 0;
				pl.getFarDetails().setPermissableFar(BigDecimal.ZERO.doubleValue());
				expectedResult = "0";
			} else if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_NINE_POINTONE) < 0) {
				isAccepted = far.compareTo(BigDecimal.ZERO) >= 0;
				pl.getFarDetails().setPermissableFar(BigDecimal.ZERO.doubleValue());
				expectedResult = "0";
			} else if (roadWidth.compareTo(ROAD_WIDTH_NINE_POINTONE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_TWELVE_POINTTWO) < 0) {
				isAccepted = far.compareTo(BigDecimal.ZERO) >= 0;
				pl.getFarDetails().setPermissableFar(BigDecimal.ZERO.doubleValue());
				expectedResult = "0";
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWELVE_POINTTWO) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_EIGHTEEN_POINTTHREE) < 0) {
				isAccepted = far.compareTo(TWO) <= 0;
				pl.getFarDetails().setPermissableFar(TWO.doubleValue());
				expectedResult = "<= 2";
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWENTYFOUR_POINTFOUR) >= 0) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
				expectedResult = "<= 2.5";
			}

		}

		if (typeOfArea.equalsIgnoreCase(NEW)) {
			if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) < 0) {
				errors.put(NEW_AREA_ERROR, NEW_AREA_ERROR_MSG);
				pl.addErrors(errors);
			} else if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_NINE_POINTONE) < 0) {
				isAccepted = far.compareTo(BigDecimal.ZERO) >= 0;
				pl.getFarDetails().setPermissableFar(BigDecimal.ZERO.doubleValue());
				expectedResult = "0";
			} else if (roadWidth.compareTo(ROAD_WIDTH_NINE_POINTONE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_TWELVE_POINTTWO) < 0) {
				isAccepted = far.compareTo(BigDecimal.ZERO) >= 0;
				pl.getFarDetails().setPermissableFar(BigDecimal.ZERO.doubleValue());
				expectedResult = "0";
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWELVE_POINTTWO) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_EIGHTEEN_POINTTHREE) < 0) {
				isAccepted = far.compareTo(TWO) <= 0;
				pl.getFarDetails().setPermissableFar(TWO.doubleValue());
				expectedResult = "<= 2";
			} else if (roadWidth.compareTo(ROAD_WIDTH_EIGHTEEN_POINTTHREE) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_TWENTYFOUR_POINTFOUR) < 0) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
				expectedResult = "<= 2.5";
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWENTYFOUR_POINTFOUR) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_TWENTYSEVEN_POINTFOUR) < 0) {
				isAccepted = far.compareTo(TWO_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(TWO_POINTFIVE.doubleValue());
				expectedResult = "<= 2.5";
			} else if (roadWidth.compareTo(ROAD_WIDTH_TWENTYSEVEN_POINTFOUR) >= 0
					&& roadWidth.compareTo(ROAD_WIDTH_THIRTY_POINTFIVE) < 0) {
				isAccepted = far.compareTo(THREE) <= 0;
				pl.getFarDetails().setPermissableFar(THREE.doubleValue());
				expectedResult = "<= 3";
			} else if (roadWidth.compareTo(ROAD_WIDTH_THIRTY_POINTFIVE) >= 0) {
				isAccepted = far.compareTo(THREE_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(THREE_POINTFIVE.doubleValue());
				expectedResult = "<= 3";
			}

		}

		String occupancyName = occupancyType.getType().getName();

		if (errors.isEmpty() && StringUtils.isNotBlank(expectedResult)) {
			buildResult(pl, occupancyName, far, typeOfArea, roadWidth, expectedResult, isAccepted);
		}
	}

	private void processFarForGBDOccupancy(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal far,
			String typeOfArea, BigDecimal roadWidth, HashMap<String, String> errors) {

		String expectedResult = StringUtils.EMPTY;
		boolean isAccepted = false;

		if (typeOfArea.equalsIgnoreCase(OLD)) {
			if (roadWidth.compareTo(ROAD_WIDTH_TWO_POINTFOUR) < 0) {
				errors.put(OLD_AREA_ERROR, OLD_AREA_ERROR_MSG);
				pl.addErrors(errors);
				return;
			} else {
				isAccepted = far.compareTo(ONE_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(ONE_POINTFIVE.doubleValue());
				expectedResult = "<=" + ONE_POINTFIVE;
			}

		}

		if (typeOfArea.equalsIgnoreCase(NEW)) {
			if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) < 0) {
				errors.put(NEW_AREA_ERROR, NEW_AREA_ERROR_MSG);
				pl.addErrors(errors);
				return;
			} else {
				isAccepted = far.compareTo(ONE_POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(ONE_POINTFIVE.doubleValue());
				expectedResult = "<=" + ONE_POINTFIVE;
			}

		}

		String occupancyName = occupancyType.getType().getName();

		if (occupancyType.getSubtype() != null) {
			OccupancyHelperDetail subtype = occupancyType.getSubtype();
			occupancyName = subtype.getName();
			String code = subtype.getCode();

//                    if (G_PHI.equalsIgnoreCase(code)) {
//                        isAccepted = far.compareTo(POINTFIVE) <= 0;
//                        pl.getFarDetails().setPermissableFar(POINTFIVE.doubleValue());
//                        expectedResult = "<=" + POINTFIVE;
//                    } else if (G_NPHI.equalsIgnoreCase(code)) {
//                        isAccepted = far.compareTo(ONE_POINTFIVE) <= 0;
//                        pl.getFarDetails().setPermissableFar(ONE_POINTFIVE.doubleValue());
//                        expectedResult = "<=" + ONE_POINTFIVE;
//                    }
		}

		if (errors.isEmpty() && StringUtils.isNotBlank(expectedResult)) {
			buildResult(pl, occupancyName, far, typeOfArea, roadWidth, expectedResult, isAccepted);
		}
	}

	private void processFarHaazardous(Plan pl, OccupancyTypeHelper occupancyType, BigDecimal far, String typeOfArea,
			BigDecimal roadWidth, HashMap<String, String> errors) {

		String expectedResult = StringUtils.EMPTY;
		boolean isAccepted = false;

		if (typeOfArea.equalsIgnoreCase(OLD)) {
			if (roadWidth.compareTo(ROAD_WIDTH_TWO_POINTFOUR) < 0) {
				errors.put(OLD_AREA_ERROR, OLD_AREA_ERROR_MSG);
				pl.addErrors(errors);
			} else {
				isAccepted = far.compareTo(POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(POINTFIVE.doubleValue());
				expectedResult = "<=" + POINTFIVE;
			}

		}

		if (typeOfArea.equalsIgnoreCase(NEW)) {
			if (roadWidth.compareTo(ROAD_WIDTH_SIX_POINTONE) < 0) {
				errors.put(NEW_AREA_ERROR, NEW_AREA_ERROR_MSG);
				pl.addErrors(errors);
			} else {
				isAccepted = far.compareTo(POINTFIVE) <= 0;
				pl.getFarDetails().setPermissableFar(POINTFIVE.doubleValue());
				expectedResult = "<=" + POINTFIVE;
			}

		}

		String occupancyName = occupancyType.getType().getName();

		if (errors.isEmpty() && StringUtils.isNotBlank(expectedResult)) {
			buildResult(pl, occupancyName, far, typeOfArea, roadWidth, expectedResult, isAccepted);
		}
	}

	private void buildResult(Plan pl, String occupancyName, BigDecimal far, String typeOfArea, BigDecimal roadWidth,
			String expectedResult, boolean isAccepted) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		// scrutinyDetail.addColumnHeading(2, OCCUPANCY);
		// scrutinyDetail.addColumnHeading(3, AREA_TYPE);
		// scrutinyDetail.addColumnHeading(4, ROAD_WIDTH);
		scrutinyDetail.addColumnHeading(2, BASE_FAR);
		scrutinyDetail.addColumnHeading(3, MAX_PERMISSIBLE);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		scrutinyDetail.setKey("Common_FAR");

		String actualResult = far.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).toString();
		expectedResult = "";
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, RULE_38);
		// details.put(OCCUPANCY, occupancyName);
		// details.put(AREA_TYPE, typeOfArea);
		// details.put(ROAD_WIDTH, roadWidth.toString());
		//LOG.info("Plan in FAR " + pl.getFarDetails().getBaseFar());
		
		
		//If base far is not set in the generalcriteriasfar method, then it will be set as NA in report. 
		BigDecimal baseFARBD = new BigDecimal(0);
		BigDecimal maxPermissibleBD = new BigDecimal(0);
				
		if (pl.getFarDetails() != null && pl.getFarDetails().getBaseFar() != null) {
		baseFARBD = new BigDecimal(pl.getFarDetails().getBaseFar())
				.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);
		
		maxPermissibleBD = new BigDecimal(pl.getFarDetails().getPermissableFar())
				.setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);
		}
		
		if (pl.getFarDetails() != null && pl.getFarDetails().getBaseFar() != null)
			details.put(BASE_FAR, baseFARBD.toString());
		
		else
			details.put(BASE_FAR, DxfFileConstants.NA);
		if (pl.getFarDetails() != null && pl.getFarDetails().getPermissableFar() != null) {
			String str=" (10% deviation allowed)";
			if(ApplicationType.OCCUPANCY_CERTIFICATE.equals(pl.getApplicationType()))
				details.put(MAX_PERMISSIBLE, maxPermissibleBD.toString()+str);
			else
				details.put(MAX_PERMISSIBLE, maxPermissibleBD.toString());
		}
		else
			details.put(MAX_PERMISSIBLE, DxfFileConstants.NA);

		details.put(PROVIDED, actualResult);
		details.put(STATUS, isAccepted ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());
		// details.put(STATUS, Result.Verify.getResultVal());

		scrutinyDetail.getDetail().add(details);
		
		Map<String, String> details2 = new HashMap<>();
		details2.put(RULE_NO, "ODA rule 8 (2-vi)");
		details2.put(BASE_FAR, "Declaration by Architect");
		details2.put(MAX_PERMISSIBLE,
				"Per acre benchmark value of land needed if project is having purchasable FAR component");
		details2.put(PROVIDED,
				pl.getPlanInformation().getBenchmarkValuePerAcre().compareTo(BigDecimal.ZERO) > 0
						? pl.getPlanInformation().getBenchmarkValuePerAcre() + ""
						: "NA");
		details2.put(STATUS, Result.Verify.getResultVal());

		scrutinyDetail.getDetail().add(details2);
		
		
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	private ScrutinyDetail getFarScrutinyDetail(String key) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, "Area Type");
		scrutinyDetail.addColumnHeading(3, "Road Width");
		scrutinyDetail.addColumnHeading(4, PERMISSIBLE);
		scrutinyDetail.addColumnHeading(5, PROVIDED);
		scrutinyDetail.addColumnHeading(6, STATUS);
		scrutinyDetail.setKey(key);
		return scrutinyDetail;
	}
	
	private BigDecimal calculateFar(Plan pl,BigDecimal surrenderRoadArea) {
		BigDecimal providedFar=BigDecimal.ZERO;
		BigDecimal plotArea = pl.getPlot() != null ? pl.getPlot().getArea().add(surrenderRoadArea) : BigDecimal.ZERO;
		if (plotArea.doubleValue() > 0)
			providedFar = pl.getVirtualBuilding().getTotalFloorArea().divide(plotArea, 3,
					ROUNDMODE_MEASUREMENTS);
		
		return providedFar;
	}
	

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
	
	public void updateAdditionalTdr(Plan pl) {
		BigDecimal giftedArea=pl.getPlanInformation().getAdditionalTdr();
		//calculate tdrFar
		BigDecimal tdrFarRelaxation= pl.getVirtualBuilding().getTotalFloorArea().divide(giftedArea.add(pl.getPlot().getArea()), 3,
				ROUNDMODE_MEASUREMENTS);
		tdrFarRelaxation= giftedArea.divide(pl.getPlot().getArea(),3,ROUNDMODE_MEASUREMENTS);
		pl.getFarDetails().setTdrFarRelaxation(tdrFarRelaxation.doubleValue());
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, "TDR Area");
		scrutinyDetail.addColumnHeading(3, "TDR Far");
		scrutinyDetail.addColumnHeading(4, STATUS);
		scrutinyDetail.setKey("Common_FAR TDR");
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, RULE_38);
		details.put("TDR Area", giftedArea.toString());
		details.put("TDR Far", tdrFarRelaxation.toString());
		details.put(STATUS, Result.Verify.getResultVal());
		scrutinyDetail.getDetail().add(details);
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	// TODO: 11 // City level customization for Puri code starts here.
	private static int getMaxNumberOfFloor(Plan pl) {
		int count = 0;

		for (Block block : pl.getBlocks()) {
			if (block.getBuilding() != null && block.getBuilding().getFloors() != null
					&& block.getBuilding().getFloors().size() > count) {
				count = block.getBuilding().getFloorsAboveGround().intValue();
			}
		}

		return count;
	}
	// City level customization for Puri code ends here.
	
	
	
	private static void deductStiltAndService(Plan pl, Block blk, Floor flr, Occupancy occupancy) {

		if (flr.getIsStiltFloor() && flr.getIsServiceFloor()) {

			for (StiltFloor stiltFloor : flr.getStiltFloors()) {

				if (stiltFloor.getColorCode() == occupancy.getTypeHelper().getSubtype().getColor()) {
					occupancy.setFloorArea(occupancy.getFloorArea()
							.subtract(stiltFloor.getArea() == null ? BigDecimal.ZERO : stiltFloor.getArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
				}

			}

			for (ServiceFloor serviceFloor : flr.getServiceFloors()) {

				if (serviceFloor.getColorCode() == occupancy.getTypeHelper().getSubtype().getColor()) {
					occupancy.setFloorArea(occupancy.getFloorArea()
							.subtract(serviceFloor.getArea() == null ? BigDecimal.ZERO : serviceFloor.getArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
				}

			}

		} else if (flr.isExistingStiltFloor() && flr.getIsExistingServiceFloor()) {
			for (StiltFloor stiltFloor : flr.getExistingStiltFloors()) {

				if (stiltFloor.getColorCode() == occupancy.getTypeHelper().getSubtype().getColor()) {
					occupancy.setExistingFloorArea(occupancy.getExistingFloorArea()
							.subtract(stiltFloor.getArea() == null ? BigDecimal.ZERO : stiltFloor.getArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
				}

			}

			for (ServiceFloor serviceFloor : flr.getExistingServiceFloors()) {

				if (serviceFloor.getColorCode() == occupancy.getTypeHelper().getSubtype().getColor()) {
					occupancy.setExistingFloorArea(occupancy.getExistingFloorArea()
							.subtract(serviceFloor.getArea() == null ? BigDecimal.ZERO : serviceFloor.getArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
				}

			}
		} else if (flr.getIsStiltFloor() || flr.getIsServiceFloor() || flr.isExistingStiltFloor()
				|| flr.getIsExistingServiceFloor()) {

			if (flr.getIsStiltFloor()) {
				for (StiltFloor stiltFloor : flr.getStiltFloors()) {
					if (stiltFloor.getColorCode() == occupancy.getTypeHelper().getSubtype().getColor()) {
						occupancy.setFloorArea(occupancy.getFloorArea()
								.subtract(stiltFloor.getArea() == null ? BigDecimal.ZERO : stiltFloor.getArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));

					}
				}

			}

			if (flr.isExistingStiltFloor()) {
				for (StiltFloor stiltFloor : flr.getExistingStiltFloors()) {
					if (stiltFloor.getColorCode() == occupancy.getTypeHelper().getSubtype().getColor()) {
						occupancy.setExistingFloorArea(occupancy.getExistingFloorArea()
								.subtract(stiltFloor.getArea() == null ? BigDecimal.ZERO : stiltFloor.getArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
						
						occupancy.setFloorArea(occupancy.getFloorArea()
								.subtract(stiltFloor.getArea() == null ? BigDecimal.ZERO : stiltFloor.getArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
					}
				}

			}

			if (flr.getIsServiceFloor()) {
				for (ServiceFloor serviceFloor : flr.getServiceFloors()) {
					if (serviceFloor.getColorCode() == occupancy.getTypeHelper().getSubtype().getColor()) {
						occupancy.setFloorArea(occupancy.getFloorArea()
								.subtract(serviceFloor.getArea() == null ? BigDecimal.ZERO : serviceFloor.getArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
					}
				}

			}

			if (flr.getIsExistingServiceFloor()) {
				for (ServiceFloor serviceFloor : flr.getExistingServiceFloors()) {
					if (serviceFloor.getColorCode() == occupancy.getTypeHelper().getSubtype().getColor()) {
						occupancy.setExistingFloorArea(occupancy.getExistingFloorArea()
								.subtract(serviceFloor.getArea() == null ? BigDecimal.ZERO : serviceFloor.getArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
						
						occupancy.setFloorArea(occupancy.getFloorArea()
								.subtract(serviceFloor.getArea() == null ? BigDecimal.ZERO : serviceFloor.getArea().setScale(DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS)));
					}
				}

			}

		}

	}
}
