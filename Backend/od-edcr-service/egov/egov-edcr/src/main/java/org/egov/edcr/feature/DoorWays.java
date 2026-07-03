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
import org.egov.common.entity.edcr.Door;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.Room;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.TypicalFloor;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.springframework.stereotype.Service;

@Service
public class DoorWays extends FeatureProcess {

	private static final int GENERAL_DOOR_COLOR_CODE = 1;
	private static final int BATHROOMS_WATER_CLOSET_AND_STORES_DOOR_COLOR_CODE = 2;
	private static final int FIRE_DOOR_COLOR_CODE = 3;
	private static final int DA_DOOR_COLOR_CODE = 4;

	@Override
	public Map<String, Date> getAmendments() {
		// TODO Auto-generated method stub
		return new LinkedHashMap<>();
	}

	@Override
	public Plan validate(Plan pl) {
		// TODO Auto-generated method stub
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		addTypicalFloor(pl);
		String serviceType = pl.getPlanInformation().getServiceType();
		for (Block b : pl.getBlocks()) {
			ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
			scrutinyDetail.setKey("Block_" + b.getNumber() + "_" + "Doorways");
			scrutinyDetail.addColumnHeading(1, RULE_NO);
			scrutinyDetail.addColumnHeading(2, DESCRIPTION);
			scrutinyDetail.addColumnHeading(3, FLOOR);
			scrutinyDetail.addColumnHeading(4, REQUIRED);
			scrutinyDetail.addColumnHeading(5, PROVIDED);
			scrutinyDetail.addColumnHeading(6, STATUS);

			for (Floor floor : b.getBuilding().getFloors()) {

				Map<Integer, List<BigDecimal>> doorWays = getDoors(floor.getDoors());

				// GeneralDoorways
				boolean isGeneralDoorwaysRequired = isGeneralDoorwaysRequired(pl, floor, serviceType);

				if (isGeneralDoorwaysRequired) {
					List<BigDecimal> genralDoorways = doorWays.get(GENERAL_DOOR_COLOR_CODE);
					setReportOutputDetails(pl, scrutinyDetail, "NA", "Genral Doorways Count", floor.getNumber(), "1",
							genralDoorways.size() + "", genralDoorways.size() > 0 ? Result.Accepted.getResultVal()
									: Result.Not_Accepted.getResultVal());

					if (genralDoorways.size() > 0) {
						BigDecimal requiredWidth = new BigDecimal("1");
						BigDecimal providedWidth = genralDoorways.stream().reduce(BigDecimal::min).get();
						setReportOutputDetails(pl, scrutinyDetail, "ODA Annexure IX(1-ii)", "Genral Doorways Width", floor.getNumber(),
								requiredWidth.toString(), providedWidth.toString(),
								providedWidth.compareTo(requiredWidth) >= 0 ? Result.Accepted.getResultVal()
										: Result.Not_Accepted.getResultVal());

					}

				}

				// BATHROOMS_WATER_CLOSET_AND_STORES_DOOR_COLOR_CODE
				int bathroomDoorwaysCountRequired = bathRoomAndStoreCount(pl, floor);

				if (bathroomDoorwaysCountRequired > 0) {
					List<BigDecimal> bathDoorways = doorWays.get(BATHROOMS_WATER_CLOSET_AND_STORES_DOOR_COLOR_CODE);
					setReportOutputDetails(pl, scrutinyDetail, "ODA Annexure IX(1-ii)", "Bathroom Doorways Count", floor.getNumber(),
							bathroomDoorwaysCountRequired + "", bathDoorways.size() + "",
							bathDoorways.size() >= bathroomDoorwaysCountRequired ? Result.Accepted.getResultVal()
									: Result.Not_Accepted.getResultVal());

					if (bathDoorways.size() > 0) {
						BigDecimal requiredWidth = new BigDecimal("0.75");
						BigDecimal providedWidth = bathDoorways.stream().reduce(BigDecimal::min).get();
						setReportOutputDetails(pl, scrutinyDetail, "", "Bathroom Doorways Width", floor.getNumber(),
								requiredWidth.toString(), providedWidth.toString(),
								providedWidth.compareTo(requiredWidth) >= 0 ? Result.Accepted.getResultVal()
										: Result.Not_Accepted.getResultVal());

					}
				}

				// fireDoorways
				int fireDoorwaysCountRequired = isFireDoorwaysRequiredCount(pl, floor);

				if (fireDoorwaysCountRequired > 0) {
					List<BigDecimal> fireDoorways = doorWays.get(FIRE_DOOR_COLOR_CODE);
					setReportOutputDetails(pl, scrutinyDetail, "NA", "Fire Doorways Count", floor.getNumber(),
							fireDoorwaysCountRequired + "", fireDoorways.size() + "",
							fireDoorways.size() >= fireDoorwaysCountRequired ? Result.Accepted.getResultVal()
									: Result.Not_Accepted.getResultVal());

					if (fireDoorways.size() > 0) {
						BigDecimal requiredWidth = new BigDecimal("1.2");
						BigDecimal providedWidth = fireDoorways.stream().reduce(BigDecimal::min).get();
						setReportOutputDetails(pl, scrutinyDetail, "ODA Annexure VIII(6-ii)", "Fire Doorways Width", floor.getNumber(),
								requiredWidth.toString(), providedWidth.toString(),
								providedWidth.compareTo(requiredWidth) >= 0 ? Result.Accepted.getResultVal()
										: Result.Not_Accepted.getResultVal());

					}
				}

				// SpecialDoorways
				boolean isDADoorwaysRequired = false;
				int daDoorwaysCountRequired = 0;
				if (floor.getSpecialWaterClosets() != null && floor.getSpecialWaterClosets().size() > 0) {
					isDADoorwaysRequired = true;
					daDoorwaysCountRequired = floor.getSpecialWaterClosets().size();
				}

				if (isDADoorwaysRequired) {
					List<BigDecimal> daDoorways = doorWays.get(DA_DOOR_COLOR_CODE);
					setReportOutputDetails(pl, scrutinyDetail, "NA", "DA Doorways Count", floor.getNumber(),
							daDoorwaysCountRequired + "", daDoorways.size() + "",
							daDoorways.size() >= daDoorwaysCountRequired ? Result.Accepted.getResultVal()
									: Result.Not_Accepted.getResultVal());

					if (daDoorways.size() > 0) {
						BigDecimal requiredWidth = new BigDecimal("0.9");
						BigDecimal providedWidth = daDoorways.stream().reduce(BigDecimal::min).get();
						setReportOutputDetails(pl, scrutinyDetail, "ODA Annexure VIII(4-i)", "DA Doorways Width", floor.getNumber(),
								requiredWidth.toString(), providedWidth.toString(),
								providedWidth.compareTo(requiredWidth) >= 0 ? Result.Accepted.getResultVal()
										: Result.Not_Accepted.getResultVal());

					}
				}

			}
			pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		}
		return pl;
	}

	private Map<Integer, List<BigDecimal>> getDoors(List<Door> doors) {
		List<BigDecimal> general = new ArrayList<>();
		List<BigDecimal> bathroomAndWc = new ArrayList<>();
		List<BigDecimal> fire = new ArrayList<>();
		List<BigDecimal> da = new ArrayList<>();

		for (Door door : doors) {
			switch (door.getColorCode()) {
			case GENERAL_DOOR_COLOR_CODE:
				general.addAll(door.getWidths());
				break;
			case BATHROOMS_WATER_CLOSET_AND_STORES_DOOR_COLOR_CODE:
				bathroomAndWc.addAll(door.getWidths());
				break;
			case FIRE_DOOR_COLOR_CODE:
				fire.addAll(door.getWidths());
				break;
			case DA_DOOR_COLOR_CODE:
				da.addAll(door.getWidths());
				break;

			}
		}

		Map<Integer, List<BigDecimal>> map = new HashMap<>();
		map.put(GENERAL_DOOR_COLOR_CODE, general);
		map.put(BATHROOMS_WATER_CLOSET_AND_STORES_DOOR_COLOR_CODE, bathroomAndWc);
		map.put(FIRE_DOOR_COLOR_CODE, fire);
		map.put(DA_DOOR_COLOR_CODE, da);

		return map;
	}

	private int isFireDoorwaysRequiredCount(Plan pl, Floor floor) {
		int count = 0;
		Set<String> allowedRooms = new HashSet();
		allowedRooms.add(DxfFileConstants.COLOR_LIFT_LOBBY);
		List<Room> spcRoom = OdishaUtill.getRegularRoom(pl, floor.getRegularRooms(), allowedRooms);
		count = spcRoom.size();
		return count;
	}

	private int bathRoomAndStoreCount(Plan pl, Floor floor) {
		int count = 0;
		Set<String> allowedRooms = new HashSet();
		allowedRooms.add(DxfFileConstants.COLOR_STORE_ROOM);
		List<Room> spcRoom = OdishaUtill.getRegularRoom(pl, floor.getRegularRooms(), allowedRooms);
		count = spcRoom.size();

		// getBathRoomWaterClosets
		int totalwcBath = 0;
		if (floor.getBathRoomWaterClosets() != null) {
			totalwcBath = floor.getBathRoomWaterClosets().getHeights().size();
		}
		count = count + totalwcBath;
		return count;
	}

	private void setReportOutputDetails(Plan pl, ScrutinyDetail scrutinyDetail, String ruleNo, String ruleDesc,
			int floor, String expected, String actual, String status) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
		details.put(FLOOR, floor + "");
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status);
		scrutinyDetail.getDetail().add(details);
	}

	public void addTypicalFloor(Plan pl) {
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				for (TypicalFloor tp : block.getTypicalFloor()) {
					if (tp.getRepetitiveFloorNos().contains(floor.getNumber()))
						for (Floor allFloors : block.getBuilding().getFloors())
							if (allFloors.getNumber().equals(tp.getModelFloorNo()) && !allFloors.getDoors().isEmpty()) {
								floor.setDoors(allFloors.getDoors());
								break;
							}
				}
			}
		}
	}
	
	private boolean isGeneralDoorwaysRequired(Plan pl,Floor floor,String serviceType) {
		boolean isGeneralDoorwaysRequired = true;
		if (DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType)) {
			isGeneralDoorwaysRequired = false;
			BigDecimal totalBuildUpArea = floor.getOccupancies().stream().map( oc -> oc.getBuiltUpArea()).reduce(BigDecimal::add).orElse(BigDecimal.ZERO); 
			BigDecimal totalExistingArea = floor.getOccupancies().stream().map( oc -> oc.getExistingBuiltUpArea()).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
			BigDecimal totalPropusedArea= totalBuildUpArea.subtract(totalExistingArea).setScale(2,BigDecimal.ROUND_HALF_UP);
			if(totalPropusedArea.compareTo(BigDecimal.ZERO)>0)
				isGeneralDoorwaysRequired = true;
		}
		return isGeneralDoorwaysRequired;
	}
}
