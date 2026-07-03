package org.egov.edcr.feature;

import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.HEIGHTNOTDEFINED;
import static org.egov.edcr.utility.DcrConstants.IN_METER;
import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED_DESC;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.SQMTRS;

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
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.Room;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.springframework.stereotype.Service;

@Service
public class MezzanineFloorService extends FeatureProcess {
	private static final String SUBRULE_46 = "ODA Rule 43 (vi)";
	private static final String RULE46_MAXAREA_DESC = "Maximum allowed area of mezzanine floor";
	private static final String RULE46_DIM_DESC = "Minimum height of mezzanine floor";
	private static final String FLOOR = "Floor";
	public static final String HALL_NUMBER = "Hall Number";
	private static final BigDecimal HEIGHT_2_POINT_2 = BigDecimal.valueOf(2.2);

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		validate(pl);
		String subRule = SUBRULE_46;
		if (pl != null && !pl.getBlocks().isEmpty()) {
			for (Block block : pl.getBlocks()) {
				scrutinyDetail = new ScrutinyDetail();
				scrutinyDetail.addColumnHeading(1, RULE_NO);
				scrutinyDetail.addColumnHeading(2, DESCRIPTION);
				scrutinyDetail.addColumnHeading(3, FLOOR);
				scrutinyDetail.addColumnHeading(4, REQUIRED);
				scrutinyDetail.addColumnHeading(5, PROVIDED);
				scrutinyDetail.addColumnHeading(6, STATUS);
				scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "Mezzanine Floor");
				if (block.getBuilding() != null && !block.getBuilding().getFloors().isEmpty()) {
					for (Floor floor : block.getBuilding().getFloors()) {
						for(Room room:getRegularRoom(pl, floor.getRegularRooms())) {
							
							if(room.getMezzanineAreas()==null || room.getMezzanineAreas().size()==0)
								continue;

							BigDecimal roomArea = BigDecimal.ZERO;
							for (Measurement measurement : room.getRooms()) {
								if (measurement.getArea() != null)
									roomArea = roomArea.add(measurement.getArea());
							}
							BigDecimal mezzanineFloorArea = BigDecimal.ZERO;
							List<BigDecimal> heights=new ArrayList<>();
							for (Occupancy eachOccupancy : room.getMezzanineAreas()) {
								if (eachOccupancy.getIsMezzanine()) {
									if (eachOccupancy.getBuiltUpArea() != null) {
										mezzanineFloorArea = mezzanineFloorArea.add(eachOccupancy.getBuiltUpArea()
												.subtract(eachOccupancy.getDeduction())) ;
										heights.add(eachOccupancy.getHeight());
									}
								}
							}
							
							if(room.getMezzanineAreas()!=null && room.getMezzanineAreas().size()>0) {
								mezzanineFloorArea=mezzanineFloorArea.setScale(2,BigDecimal.ROUND_HALF_UP);
								boolean valid = false;
								BigDecimal oneThirdOfBuiltup = roomArea.divide(BigDecimal.valueOf(3),
										DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);
								if (mezzanineFloorArea.doubleValue() > 0
										&& mezzanineFloorArea.compareTo(oneThirdOfBuiltup) <= 0) {
									valid = true;
								}
								String floorNo = " floor " + floor.getNumber();

								BigDecimal height = BigDecimal.ZERO;
								if(heights.size()>0) {
									height=heights.stream().reduce(BigDecimal::min).get();
								}
								if (height.compareTo(BigDecimal.ZERO) == 0) {
									pl.addError(RULE46_DIM_DESC,
											getLocaleMessage(HEIGHTNOTDEFINED,
													"Mezzanine floor " + floor.getNumber(),
													block.getName(), String.valueOf(floor.getNumber())));
								} else if (height.compareTo(HEIGHT_2_POINT_2) >= 0) {
									setReportOutputDetails(pl, subRule,
											RULE46_DIM_DESC , floorNo,
											HEIGHT_2_POINT_2.toString(), height.toString(),
											Result.Accepted.getResultVal());
								} else {
									setReportOutputDetails(pl, subRule,
											RULE46_DIM_DESC, floorNo,
											HEIGHT_2_POINT_2 .toString(), height.toString() ,
											Result.Not_Accepted.getResultVal());
								}
								if (valid) {
									setReportOutputDetails(pl, subRule,
											RULE46_MAXAREA_DESC, floorNo,
											oneThirdOfBuiltup.toString(), mezzanineFloorArea.toString(),
											Result.Accepted.getResultVal());
								} else {
									setReportOutputDetails(pl, subRule,
											RULE46_MAXAREA_DESC , floorNo,
											oneThirdOfBuiltup .toString(), mezzanineFloorArea.toString(),
											Result.Not_Accepted.getResultVal());
								}

							}
							
						
						}
					}
				}
			}
		}
		return pl;
	}

	private List<Room> getRegularRoom(Plan pl, List<Room> rooms) {
		Set<String> allowedRooms=new HashSet();
		allowedRooms.add(DxfFileConstants.COLOR_RESIDENTIAL_ROOM_NATURALLY_VENTILATED);
		allowedRooms.add(DxfFileConstants.COLOR_RESIDENTIAL_ROOM_MECHANICALLY_VENTILATED);
		List<Room> spcRoom=OdishaUtill.getRegularRoom(pl, rooms, allowedRooms);
		return spcRoom;
	}
	private void setReportOutputDetails(Plan pl, String ruleNo, String ruleDesc, String floor, String expected,
			String actual, String status) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
		details.put(FLOOR, floor);
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status);
		scrutinyDetail.getDetail().add(details);
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}