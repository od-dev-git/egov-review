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

import static org.egov.edcr.constants.DxfFileConstants.COLOR_GENERATOR_ROOM;
import static org.egov.edcr.constants.DxfFileConstants.COLOR_LAUNDRY_ROOM;
import static org.egov.edcr.constants.DxfFileConstants.COLOR_LIFT_LOBBY;
import static org.egov.edcr.constants.DxfFileConstants.COLOR_MEP_ROOM;
import static org.egov.edcr.constants.DxfFileConstants.COLOR_RESIDENTIAL_ROOM_MECHANICALLY_VENTILATED;
import static org.egov.edcr.constants.DxfFileConstants.COLOR_RESIDENTIAL_ROOM_NATURALLY_VENTILATED;
import static org.egov.edcr.constants.DxfFileConstants.COLOR_STILT_FLOOR;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.Room;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.springframework.stereotype.Service;

@Service
public class Basement extends FeatureProcess {

	private static final Logger LOG = Logger.getLogger(Basement.class);
	private static final String RULE_46_6C = "ODA Rule 39 (v), 41 (8-iii)";
	public static final String BASEMENT_REQUIRED = "Basement required";
	public static final String BASEMENT_DESCRIPTION_ONE = "Height from the floor to the soffit of the roof slab or ceiling";
	public static final String BASEMENT_DESCRIPTION_TWO = "Minimum height of the ceiling of upper basement above ground level";

	@Override
	public Plan validate(Plan pl) {
		validateAllowedRoomInBasment(pl);
		OccupancyTypeHelper helper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		BigDecimal plotArea = pl.getPlot().getArea();

		for (Block block : pl.getBlocks()) {
			int totalNoOfBasement = noOfBasement(pl, block);
			int maxAllowedBasement = 0;
			BigDecimal maxAreaAllowed = BigDecimal.ZERO;
			
			if(!DxfFileConstants.OC_MIXED_USE.equals(helper.getType().getCode())) {
				helper = OdishaUtill.getMostRestrictiveBlockMaxBuildupArea(block);
			}
			
			// TODO: 11. City level customization for Puri code starts here.

			// POINT: 2 [Auto scrutiny feature update for Puri and 3 other ULBs.]
			// POINT 2: In Puri, if the project falls under 100m of Grant Road, height
			// restriction of 12 meters is applicable
			// and no basement is allowed.

			if (pl.getThirdPartyUserTenantld().equals("od.puri")) {
				if (pl.getPlanInformation().getWhetherPlotFallsUnder100mOfGrantRoad() != null && pl.getPlanInformation()
						.getWhetherPlotFallsUnder100mOfGrantRoad().equalsIgnoreCase(DxfFileConstants.YES)
						&& totalNoOfBasement != 0) {
					pl.addError("Basement error", "Basement is not allowed within 100 meters of Grant Road.");

				}
			}
			// City level customization for Puri code ends here.

			if (DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING
					.equals(helper.getSubtype().getCode())
					|| DxfFileConstants.SEMI_DETACHED.equals(helper.getSubtype().getCode())
					|| DxfFileConstants.ROW_HOUSING.equals(helper.getSubtype().getCode())) {
				maxAllowedBasement = 1;
				if (plotArea.compareTo(new BigDecimal("500")) <= 0) {
					if (block.getBuilding().getCoverageArea() != null)
						maxAreaAllowed = block.getBuilding().getCoverageArea().multiply(new BigDecimal("0.5"));
				}

				if (totalNoOfBasement > maxAllowedBasement)
					pl.addError("Basement error", "Maximum one basement is allowed");

				if (totalAreaOfBasement(block).compareTo(maxAreaAllowed) > 0)
					pl.addError("Basement ARea", "Maximum of 50% of the covered area is alowed");
			} else if (DxfFileConstants.OC_RESIDENTIAL.equals(helper.getType().getCode())
					|| DxfFileConstants.PUBLIC_AND_SEMI_PUBLIC_USE_ZONES.equals(helper.getType().getCode())
					|| DxfFileConstants.PUBLIC_UTILITY_BLDG.equals(helper.getType().getCode())
					|| DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(helper.getType().getCode())
					|| DxfFileConstants.OC_EDUCATION.equals(helper.getType().getCode())
					|| DxfFileConstants.OC_TRANSPORTATION.equals(helper.getType().getCode())
					|| DxfFileConstants.OC_AGRICULTURE.equals(helper.getType().getCode())
					|| DxfFileConstants.OC_MIXED_USE.equals(helper.getType().getCode())) {

				if (plotArea.compareTo(new BigDecimal("500")) < 0) {
					if (totalNoOfBasement > 0)
						pl.addError("Basement error", "basement is not allowed");
				} else if (plotArea.compareTo(new BigDecimal("500")) >= 0
						&& plotArea.compareTo(new BigDecimal("1000")) <= 0) {
					if (totalNoOfBasement > 1)
						pl.addError("Basement error", "Maximum one basement is not allowed");
				}
			}

			if (DxfFileConstants.OC_COMMERCIAL.equals(helper.getType().getCode())) {
				maxAllowedBasement = 1;
				if (plotArea.compareTo(new BigDecimal("500")) <= 0) {
					if (totalNoOfBasement > 1)
						pl.addError("Basement error", "Maximum one basement is allowed");

					if (block.getBuilding().getCoverageArea() != null)
						maxAreaAllowed = block.getBuilding().getCoverageArea().multiply(new BigDecimal("0.5"));

					if (totalAreaOfBasement(block).compareTo(maxAreaAllowed) > 0)
						pl.addError("Basement ARea", "Maximum of 50% of the covered area is alowed");
				} else if (plotArea.compareTo(new BigDecimal("500")) >= 0
						&& plotArea.compareTo(new BigDecimal("1000")) <= 0) {
					if (totalNoOfBasement > 1)
						pl.addError("Basement error", "Maximum one basement is allowed");
				}

			}

		}

		return pl;
	}

	private void validateAllowedRoomInBasment(Plan pl) {
		Map<Integer, String> allowedRooms = getAllowedRoomList(pl);

		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				if (floor.getNumber() < 0) {
					for (Room room : floor.getRegularRooms()) {
						for (Measurement measurement : room.getRooms()) {
							if (!allowedRooms.keySet().contains(measurement.getColorCode())) {
								pl.addError("BasmentNotAllowedRoom"+measurement.getColorCode(),"Prohibited Room is present in Basment, Block "+block.getNumber()+" floor "+floor.getNumber()+" colorcode "+measurement.getColorCode());
							}
						}
					}
				}
			}
		}
	}

	private Map<Integer, String> getAllowedRoomList(Plan pl) {
		Map<Integer, String> allowedRooms = new HashMap<>();
		Map<String, Integer> heightOfRoomFeaturesColor = pl.getSubFeatureColorCodesMaster().get("HeightOfRoom");
		OccupancyTypeHelper typeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		if (DxfFileConstants.OC_RESIDENTIAL.equals(typeHelper.getType().getCode())
				|| DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL.equals(typeHelper.getType().getCode())
				|| DxfFileConstants.OC_EDUCATION.equals(typeHelper.getType().getCode())
				|| DxfFileConstants.OC_AGRICULTURE.equals(typeHelper.getType().getCode())) {
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_STUDY_ROOM),
					DxfFileConstants.COLOR_STUDY_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_LIBRARY_ROOM),
					DxfFileConstants.COLOR_LIBRARY_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_GAME_ROOM),
					DxfFileConstants.COLOR_GAME_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_STORE_ROOM),
					DxfFileConstants.COLOR_STORE_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_CCTV_ROOM),
					DxfFileConstants.COLOR_CCTV_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_SERVICE_ROOM),
					DxfFileConstants.COLOR_SERVICE_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_MEP_ROOM),
					DxfFileConstants.COLOR_MEP_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_LAUNDRY_ROOM),
					DxfFileConstants.COLOR_LAUNDRY_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_LIFT_LOBBY),
					DxfFileConstants.COLOR_LIFT_LOBBY);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_GUARD_ROOM),
					DxfFileConstants.COLOR_GUARD_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_ELECTRIC_CABIN_ROOM),
					DxfFileConstants.COLOR_ELECTRIC_CABIN_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_SUB_STATION_ROOM),
					DxfFileConstants.COLOR_SUB_STATION_ROOM);
		} else {
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_STORE_ROOM),
					DxfFileConstants.COLOR_STORE_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_CCTV_ROOM),
					DxfFileConstants.COLOR_CCTV_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_SERVICE_ROOM),
					DxfFileConstants.COLOR_SERVICE_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_MEP_ROOM),
					DxfFileConstants.COLOR_MEP_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_LAUNDRY_ROOM),
					DxfFileConstants.COLOR_LAUNDRY_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_LIFT_LOBBY),
					DxfFileConstants.COLOR_LIFT_LOBBY);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_GUARD_ROOM),
					DxfFileConstants.COLOR_GUARD_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_ELECTRIC_CABIN_ROOM),
					DxfFileConstants.COLOR_ELECTRIC_CABIN_ROOM);
			allowedRooms.put(heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_SUB_STATION_ROOM),
					DxfFileConstants.COLOR_SUB_STATION_ROOM);
		}

		return allowedRooms;
	}

	private BigDecimal totalAreaOfBasement(Block block) {
		BigDecimal area = BigDecimal.ZERO;

		for (Floor floor : block.getBuilding().getFloors()) {
			area = area.add(floor.getArea());
		}

		return area;
	}

	private int noOfBasement(Plan pl, Block block) {
		int count = 0;
		for (Floor floor : block.getBuilding().getFloors()) {
			if (floor.getNumber() < 0)
				count++;
		}
		return count;
	}

	@Override
	public Plan process(Plan pl) {
		validate(pl);
		HashMap<String, String> errors = new HashMap<>();
		OccupancyTypeHelper mostRestrictiveFarHelper = pl.getVirtualBuilding() != null
				? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
				: null;
		for (Block b : pl.getBlocks()) {

			ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
			scrutinyDetail.setKey("Block_" + b.getNumber() + "_" + "Basement");
			scrutinyDetail.addColumnHeading(1, RULE_NO);
			scrutinyDetail.addColumnHeading(2, DESCRIPTION);
			scrutinyDetail.addColumnHeading(3, REQUIRED);
			scrutinyDetail.addColumnHeading(4, PROVIDED);
			scrutinyDetail.addColumnHeading(5, STATUS);

			Map<String, String> details = new HashMap<>();
			BigDecimal minLength = BigDecimal.ZERO;

			if (b.getBuilding() != null && b.getBuilding().getFloors() != null
					&& !b.getBuilding().getFloors().isEmpty()) {
				for (Floor f : b.getBuilding().getFloors()) {

					if (f != null && f.getNumber() == -1) {

						// apply rule

						if (f.getHeightFromTheFloorToCeiling() != null
								&& !f.getHeightFromTheFloorToCeiling().isEmpty()) {

							minLength = f.getHeightFromTheFloorToCeiling().stream().reduce(BigDecimal::min).get();

							if (minLength.compareTo(BigDecimal.valueOf(2.5)) >= 0) {
								details.put(RULE_NO, "ODA Rule 41 (8-i)");
								details.put(DESCRIPTION, BASEMENT_DESCRIPTION_ONE);
								details.put(REQUIRED, ">= 2.5");
								details.put(PROVIDED, minLength.toString());
								details.put(STATUS, Result.Accepted.getResultVal());
								scrutinyDetail.getDetail().add(details);

							} else {
								details = new HashMap<>();
								details.put(RULE_NO, "ODA Rule 39 (v), 41 (8-iii)");
								details.put(DESCRIPTION, BASEMENT_DESCRIPTION_ONE);
								details.put(REQUIRED, ">= 2.5");
								details.put(PROVIDED, minLength.toString());
								details.put(STATUS, Result.Not_Accepted.getResultVal());
								scrutinyDetail.getDetail().add(details);
							}
						}
						minLength = BigDecimal.ZERO;
						if (f.getHeightOfTheCeilingOfUpperBasement() != null
								&& !f.getHeightOfTheCeilingOfUpperBasement().isEmpty()) {

							minLength = f.getHeightOfTheCeilingOfUpperBasement().stream().reduce(BigDecimal::min).get();

							BigDecimal minRequired = BigDecimal.ZERO;
							BigDecimal maxRequired = new BigDecimal("1.5");

							if (isGroundFloorCommercialOrMultiParkingOrStilt(b)) {
								minRequired = new BigDecimal("0.3");
							} else {
								minRequired = new BigDecimal("0.9");
							}

							if (minLength.compareTo(minRequired) >= 0 && minLength.compareTo(maxRequired) <= 0) {
								details = new HashMap<>();
								details.put(RULE_NO, RULE_46_6C);
								details.put(DESCRIPTION, BASEMENT_DESCRIPTION_TWO);
								details.put(REQUIRED, "Between " + minRequired.toString() + " to 1.5");
								details.put(PROVIDED, minLength.toString());
								details.put(STATUS, Result.Accepted.getResultVal());
								scrutinyDetail.getDetail().add(details);

							} else {
								details = new HashMap<>();
								details.put(RULE_NO, RULE_46_6C);
								details.put(DESCRIPTION, BASEMENT_DESCRIPTION_TWO);
								details.put(REQUIRED, "Between " + minRequired.toString() + " to 1.5");
								details.put(PROVIDED, minLength.toString());
								details.put(STATUS, Result.Not_Accepted.getResultVal());
								scrutinyDetail.getDetail().add(details);
							}
						}

					}

				}
			}
			pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		}

		if (errors.size() > 0)
			pl.addErrors(errors);

		return pl;
	}
	
	private boolean isGroundFloorCommercialOrMultiParkingOrStilt(Block block) {
		if (block.getBuilding() != null && block.getBuilding().getFloors() != null) {
			for (Floor floor : block.getBuilding().getFloors()) {
				if (floor.getNumber() == 0) {

					if (floor.getIsStiltFloor()) {
						return true;
					}

					if (isItCommercial(floor)) {
						return true;
					}

					if (isItMultiLevelParking(floor)) {
						return true;
					}

				}

			}
		}
		return false;
	}

	private boolean isItMultiLevelParking(Floor floor) {
		for (Occupancy occ : floor.getOccupancies()) {
			if (!occ.getTypeHelper().getSubtype().getCode()
					.equalsIgnoreCase(DxfFileConstants.MULTI_LEVEL_CAR_PARKING)) {
				return false;
			}
		}
		return true;
	}

	private boolean isItCommercial(Floor floor) {
		for (Occupancy occ : floor.getOccupancies()) {
			if (!occ.getTypeHelper().getType().getCode().equalsIgnoreCase(DxfFileConstants.OC_COMMERCIAL)) {
				return false;
			}
		}
		return true;
	}

//	private int allowedNoOfBesment(Plan pl,OccupancyTypeHelper occupancyTypeHelper) {
//		BigDecimal plot=pl.getPlot().getArea();
//	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

}