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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.Room;
import org.egov.common.entity.edcr.RoomHeight;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.egov.edcr.service.ProcessHelper;
import org.springframework.stereotype.Service;

@Service
public class HeightOfRoom extends FeatureProcess {

	private static final String SUBRULE_41_II_A = "ODA Rule 2 (lxviii)";
	private static final String SUBRULE_41_II_B = "41-ii-b";

	private static final String SUBRULE_41_II_A_AC_DESC = "Minimum height of ac room";
	private static final String SUBRULE_41_II_A_REGULAR_DESC = "Minimum height of regular room";
	private static final String SUBRULE_41_II_B_AREA_DESC = "Total area of rooms";
	private static final String SUBRULE_41_II_B_TOTAL_WIDTH = "Minimum Width of room";
	private static final String SUBRULE_41_II_B_TOTAL_DIMENSION = "Minimum dimension of room";

	public static final BigDecimal MINIMUM_HEIGHT_3_6 = BigDecimal.valueOf(3.6);
	public static final BigDecimal MINIMUM_HEIGHT_3 = BigDecimal.valueOf(3);
	public static final BigDecimal MINIMUM_HEIGHT_2_75 = BigDecimal.valueOf(2.75);
	public static final BigDecimal MINIMUM_HEIGHT_2_4 = BigDecimal.valueOf(2.4);
	public static final BigDecimal MINIMUM_AREA_9 = BigDecimal.valueOf(9);// 9.29
	public static final BigDecimal MINIMUM_WIDTH_2_4 = BigDecimal.valueOf(2.4);
	public static final BigDecimal MINIMUM_WIDTH_2_1 = BigDecimal.valueOf(2.1);
	private static final String ROOM_HEIGHT_NOTDEFINED = "Room height is not defined in layer ";
	private static final String LAYER_ROOM_HEIGHT = "BLK_%s_FLR_%s_%s";

	@Override
	public Plan validate(Plan pl) {
		Map<String, Integer> heightOfRoomFeaturesColor = pl.getSubFeatureColorCodesMaster().get("HeightOfRoom");
		validateRoomLayer(pl);
		for (Block block : pl.getBlocks()) {
			int regularRoomCount = 0;
			for (Floor floor : block.getBuilding().getFloors()) {
				// regularRoomCount=floor.getRegularRoom()!=null?regularRoomCount+floor.getRegularRoom().getRooms().size():regularRoomCount;
				List<Room> rooms = getRegularRoom(pl, floor.getRegularRooms());
				if (!rooms.isEmpty()) {
					regularRoomCount = rooms.size() + regularRoomCount;
				}

			}
			if (isRoomMandatory(pl)) {
				if (regularRoomCount < 1)
					pl.addError("roomRequired" + block.getNumber(),
							"Habitable room is mandatory, Block " + block.getNumber());
			}
		}

		return pl;
	}

	private void validateRoomLayer(Plan pl) {
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				int regularRoomCount = 0;
				for(Room room:floor.getRegularRooms()) {
					regularRoomCount++;
					if(room.getRooms()==null)
						pl.addError("RoomNotDefind"+block.getNumber()+"f"+floor.getNumber()+"r"+regularRoomCount, "Polygon is not definded block "+block.getColorCode()+" floor "+floor.getNumber()+" room "+regularRoomCount);
					else if(room.getRooms().size()>1)
						pl.addError("RoomMoreDefindeb"+block.getNumber()+"f"+floor.getNumber()+"r"+regularRoomCount, "Multiple Polygons with same layer name not allowed for Regular Rooms in block "+block.getColorCode()+" floor "+floor.getNumber()+" room "+regularRoomCount);
				}
			}
		}
	}
	

	@Override
	public Plan process(Plan pl) {
		//disabled as per client request 30-Apr-2021
		boolean flage=true;
		if(flage) {
			return pl;
		}
		Map<String, Integer> heightOfRoomFeaturesColor = pl.getSubFeatureColorCodesMaster().get("HeightOfRoom");
		validate(pl);

		HashMap<String, String> errors = new HashMap<>();
		if (pl != null && pl.getBlocks() != null) {
			OccupancyTypeHelper mostRestrictiveOccupancy = pl.getVirtualBuilding() != null
					? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
					: null;
			if (mostRestrictiveOccupancy != null && mostRestrictiveOccupancy.getType() != null
					&& mostRestrictiveOccupancy.getSubtype() != null

			) {
				for (Block block : pl.getBlocks()) {
					if (block.getBuilding() != null && !block.getBuilding().getFloors().isEmpty()) {
						scrutinyDetail = new ScrutinyDetail();
						scrutinyDetail.addColumnHeading(1, RULE_NO);
						scrutinyDetail.addColumnHeading(2, DESCRIPTION);
						scrutinyDetail.addColumnHeading(3, FLOOR);
						scrutinyDetail.addColumnHeading(4, ROOM);
						scrutinyDetail.addColumnHeading(5, REQUIRED);
						scrutinyDetail.addColumnHeading(6, PROVIDED);
						scrutinyDetail.addColumnHeading(7, STATUS);

						scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "Habitable Room");

						int roomCount = 0;

						for (Floor floor : block.getBuilding().getFloors()) {
							if (floor.getNumber() < 0)
								continue;
							List<BigDecimal> roomAreas = new ArrayList<>();
							List<BigDecimal> roomWidths = new ArrayList<>();
							BigDecimal minimumHeight = BigDecimal.ZERO;
							BigDecimal totalArea = BigDecimal.ZERO;
							BigDecimal minWidth = BigDecimal.ZERO;
							String subRule = null;
							String subRuleDesc = null;
							String color = "";

							color = DxfFileConstants.COLOR_RESIDENTIAL_ROOM;

							if (!floor.getRegularRooms().isEmpty()) {
								List<BigDecimal> residentialRoomHeights = new ArrayList<>();

								for (Room room : floor.getRegularRooms()) {
									for (RoomHeight roomHeight : room.getHeights()) {
										if (heightOfRoomFeaturesColor.get(color) == roomHeight.getColorCode()) {
											residentialRoomHeights.add(roomHeight.getHeight());
										}
									}
								}
								List<Room> habitableRooms = getRegularRoom(pl, floor.getRegularRooms());

//                                for (Measurement room : habitableRooms.) {
//                                    if (heightOfRoomFeaturesColor.get(color) == room.getColorCode()) {
//                                        roomAreas.add(room.getArea());
//                                        roomWidths.add(room.getWidth());
//                                    }
//                                }
								int roomNumber = 1;
								for (Room r : habitableRooms) {
									if (r.getRooms() != null && !r.getRooms().isEmpty() && r.getRooms().size() >= 1) {
										Measurement room = r.getRooms().get(0);
										minimumHeight = MINIMUM_HEIGHT_2_75;
										BigDecimal minimumArea = MINIMUM_AREA_9;
										minWidth = MINIMUM_WIDTH_2_4;
										BigDecimal providedHeight = BigDecimal.ZERO;
										try {
											providedHeight = r.getHeights().stream().map(RoomHeight::getHeight)
													.reduce(BigDecimal::min).get();
										} catch (Exception e) {

										}

										subRule = SUBRULE_41_II_A;
										subRuleDesc = SUBRULE_41_II_B_AREA_DESC;

										boolean valid = false;
										boolean isTypicalRepititiveFloor = false;
										Map<String, Object> typicalFloorValues = ProcessHelper
												.getTypicalFloorValues(block, floor, isTypicalRepititiveFloor);

										buildResultArea(pl, floor, roomNumber, minimumArea, subRule, subRuleDesc,
												room.getArea(), valid, typicalFloorValues);

										subRuleDesc = SUBRULE_41_II_B_TOTAL_WIDTH;
										buildResult(pl, floor, roomNumber, minWidth, subRule, subRuleDesc,
												room.getWidth(), valid, typicalFloorValues);

										subRuleDesc = SUBRULE_41_II_A_REGULAR_DESC;
										buildResult(pl, floor, roomNumber, minimumHeight, subRule, subRuleDesc,
												providedHeight, valid, typicalFloorValues);

										roomNumber++;
									}
								}

							}

						}
					}
					pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
				}

			}
		}
		return pl;

	}

	private List<Measurement> getRoom(Room room, int colorCode) {
		List<Measurement> spcRoom = new ArrayList<Measurement>();
		if (room != null) {
			for (Measurement r : room.getRooms()) {
				if (colorCode == r.getColorCode()) {
					spcRoom.add(r);
				}
			}
		}
		return spcRoom;
	}

	private List<Room> getRegularRoom(Plan pl, List<Room> rooms) {
//		Map<String, Integer> heightOfRoomFeaturesColor = pl.getSubFeatureColorCodesMaster().get("HeightOfRoom");
//		List<Room> spcRoom = new ArrayList<Room>();
//		if (rooms != null) {
//			for (Room room : rooms) {
//				Room room2=new Room();
//				room2.setNumber(room.getNumber());
//				room2.setClosed(room.getClosed());
//				List<Measurement> measurements = new ArrayList<>();
//				if (room.getRooms() != null && !room.getRooms().isEmpty() && room.getRooms().size() >= 1) {
//					//Measurement r = room.getRooms().get(0);
//					for(Measurement r:room.getRooms()) {
//						if (heightOfRoomFeaturesColor.get(DxfFileConstants.COLOR_RESIDENTIAL_ROOM_NATURALLY_VENTILATED) == r
//								.getColorCode()
//								|| heightOfRoomFeaturesColor
//										.get(DxfFileConstants.COLOR_RESIDENTIAL_ROOM_MECHANICALLY_VENTILATED) == r
//												.getColorCode()) {
//							measurements.add(r);
//						}
//					}
//				}
//				spcRoom.add(room2);
//			}
//		}
		Set<String> allowedRooms = new HashSet();
		allowedRooms.add(DxfFileConstants.COLOR_RESIDENTIAL_ROOM_NATURALLY_VENTILATED);
		allowedRooms.add(DxfFileConstants.COLOR_RESIDENTIAL_ROOM_MECHANICALLY_VENTILATED);
		List<Room> spcRoom = OdishaUtill.getRegularRoom(pl, rooms, allowedRooms);
		return spcRoom;
	}

	private boolean isRoomMandatory(Plan plan) {
		boolean flage = false;
		OccupancyTypeHelper occupancyTypeHelper = plan.getVirtualBuilding().getMostRestrictiveFarHelper() != null
				? plan.getVirtualBuilding().getMostRestrictiveFarHelper()
				: null;
		if (DxfFileConstants.OC_RESIDENTIAL.equals(occupancyTypeHelper.getType().getCode()))
			flage = true;

		return flage;
	}

	private void buildResult(Plan pl, Floor floor, int roomNumber, BigDecimal expected, String subRule,
			String subRuleDesc, BigDecimal actual, boolean valid, Map<String, Object> typicalFloorValues) {
		if (!(Boolean) typicalFloorValues.get("isTypicalRepititiveFloor")
				&& expected.compareTo(BigDecimal.valueOf(0)) > 0 && subRule != null && subRuleDesc != null) {

			String value = typicalFloorValues.get("typicalFloors") != null
					? (String) typicalFloorValues.get("typicalFloors")
					: " floor " + floor.getNumber();

			actual = actual.setScale(2, BigDecimal.ROUND_HALF_UP);

			if (actual.compareTo(expected) >= 0) {
				valid = true;
			}
			if (valid) {
				setReportOutputDetails(pl, subRule, subRuleDesc, value, roomNumber, expected.toString(),
						actual.toString(), Result.Accepted.getResultVal());
			} else {
				setReportOutputDetails(pl, subRule, subRuleDesc, value, roomNumber, expected.toString(),
						actual.toString(), Result.Not_Accepted.getResultVal());
			}
		}
	}

	private void buildResultArea(Plan pl, Floor floor, int room, BigDecimal expected, String subRule,
			String subRuleDesc, BigDecimal actual, boolean valid, Map<String, Object> typicalFloorValues) {
		if (!(Boolean) typicalFloorValues.get("isTypicalRepititiveFloor")
				&& expected.compareTo(BigDecimal.valueOf(0)) > 0 && subRule != null && subRuleDesc != null) {

			actual = actual.setScale(2, BigDecimal.ROUND_HALF_UP);

			if (actual.compareTo(expected) >= 0) {
				valid = true;
			}
			String value = typicalFloorValues.get("typicalFloors") != null
					? (String) typicalFloorValues.get("typicalFloors")
					: " floor " + floor.getNumber();
			if (valid) {
				setReportOutputDetails(pl, subRule, subRuleDesc, value, room, expected.toString(), actual.toString(),
						Result.Accepted.getResultVal());
			} else {
				setReportOutputDetails(pl, subRule, subRuleDesc, value, room, expected.toString(), actual.toString(),
						Result.Not_Accepted.getResultVal());
			}
		}
	}

	private void setReportOutputDetails(Plan pl, String ruleNo, String ruleDesc, String floor, int room,
			String expected, String actual, String status) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
		details.put(FLOOR, floor);
		details.put(ROOM, room + "");
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status);
		scrutinyDetail.getDetail().add(details);
		// pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}