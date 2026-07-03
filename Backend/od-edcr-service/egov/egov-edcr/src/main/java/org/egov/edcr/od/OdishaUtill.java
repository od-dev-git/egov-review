package org.egov.edcr.od;

import static org.egov.edcr.constants.DxfFileConstants.PROJECT_VALUE_IN_INR_IF_EIDP_FEE_IS_APPLICABLE_FOR_PROJECT;
import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.egov.common.entity.dcr.helper.OccupancyHelperDetail;
import org.egov.common.entity.edcr.AccessoryBlock;
import org.egov.common.entity.edcr.Ammenity;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.FloorUnit;
import org.egov.common.entity.edcr.Lift;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.MeasurementWithHeight;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyPercentage;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.ParkingDetails;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Room;
import org.egov.common.entity.edcr.RoomHeight;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.ServiceFloor;
import org.egov.common.entity.edcr.StiltFloor;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.OdishaUlbs;
import org.egov.edcr.feature.Parking;
import org.egov.edcr.feature.PublicWashroomService;
import org.egov.edcr.utility.Util;

import com.jayway.jsonpath.JsonPath;

import net.minidev.json.JSONArray;

public class OdishaUtill {

	private static final BigDecimal MINIMUM_NUMBER_OF_OCCUPANTS_OR_USERS_FOR_ASSEMBLY_BUILDING = new BigDecimal("50");
	private static final BigDecimal SQMT_SQFT_MULTIPLIER = new BigDecimal("10.764");
	private static final int COLOR_EWS = 1;
	private static final int COLOR_LIG = 2;
	private static final int COLOR_MIG1 = 3;
	private static final int COLOR_MIG2 = 4;
	private static final int COLOR_OTHER = 5;
	private static final int COLOR_ROOM = 6;
	private static final int COLOR_OWNERS_SOCIETY_OFFICE = 8;

	private static final Logger LOG = Logger.getLogger(OdishaUtill.class);
	
	public static boolean isAssemblyBuildingCriteria(Plan pl) {
		boolean isAssemblyBuilding = false;
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		if (DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.OC_COMMERCIAL.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.OC_TRANSPORTATION.equals(occupancyTypeHelper.getType().getCode())) {

			// BigDecimal providedNumberOfOccupantsOrUser =
			// pl.getPlanInformation().getNumberOfOccupantsOrUsers();
			BigDecimal providedNumberOfOccupantsOrUser = BigDecimal.ZERO;
			for (Block block : pl.getBlocks()) {
				if (block.getNumberOfOccupantsOrUsersOrBedBlk() != null) {
					isAssemblyBuildingCriteria(pl, block);
					providedNumberOfOccupantsOrUser = providedNumberOfOccupantsOrUser
							.add(block.getNumberOfOccupantsOrUsersOrBedBlk());

				}
			}

			if (providedNumberOfOccupantsOrUser
					.compareTo(MINIMUM_NUMBER_OF_OCCUPANTS_OR_USERS_FOR_ASSEMBLY_BUILDING) >= 0) {

				if (DxfFileConstants.AUDITORIUM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.BANQUET_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CINEMA.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CLUB.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MUSIC_PAVILIONS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.COMMUNITY_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SCIENCE_CENTRE_OR_MUSEUM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CONFERNCE_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CONVENTION_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SCULPTURE_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CULTURAL_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.EXHIBITION_CENTER.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.GYMNASIA.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MULTIPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MUSUEM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.PLACE_OF_WORKSHIP.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.PUBLIC_LIBRARIES.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RECREATION_BLDG.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SPORTS_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.STADIUM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.THEATRE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RELIGIOUS_BUILDING.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RESTAURANT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SHOPPING_MALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SHOWROOM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SUPERMARKETS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.FOOD_COURTS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.AIRPORT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.METRO_STATION.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.BUS_TERMINAL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.ISBT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RAILWAY_STATION.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.TRUCK_TERMINAL.equals(occupancyTypeHelper.getSubtype().getCode())) {
					isAssemblyBuilding = true;
				}
			}
		}
		pl.getPlanInformation().setAssemblyBuilding(isAssemblyBuilding);
		return isAssemblyBuilding;
	}

	public static boolean isAssemblyBuildingCriteria(Plan pl, Block block) {
		boolean isAssemblyBuilding = false;
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		if (DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.OC_COMMERCIAL.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.OC_TRANSPORTATION.equals(occupancyTypeHelper.getType().getCode())) {

			// BigDecimal providedNumberOfOccupantsOrUser =
			// pl.getPlanInformation().getNumberOfOccupantsOrUsers();
			BigDecimal providedNumberOfOccupantsOrUser = BigDecimal.ZERO;
			if (block.getNumberOfOccupantsOrUsersOrBedBlk() != null)
				providedNumberOfOccupantsOrUser = providedNumberOfOccupantsOrUser
						.add(block.getNumberOfOccupantsOrUsersOrBedBlk());

			if (providedNumberOfOccupantsOrUser
					.compareTo(MINIMUM_NUMBER_OF_OCCUPANTS_OR_USERS_FOR_ASSEMBLY_BUILDING) >= 0) {

				if (DxfFileConstants.AUDITORIUM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.BANQUET_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CINEMA.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CLUB.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MUSIC_PAVILIONS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.COMMUNITY_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SCIENCE_CENTRE_OR_MUSEUM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CONFERNCE_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CONVENTION_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SCULPTURE_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CULTURAL_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.EXHIBITION_CENTER.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.GYMNASIA.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MULTIPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MUSUEM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.PLACE_OF_WORKSHIP.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.PUBLIC_LIBRARIES.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RECREATION_BLDG.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SPORTS_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.STADIUM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.THEATRE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RELIGIOUS_BUILDING.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RESTAURANT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SHOPPING_MALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SHOWROOM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SUPERMARKETS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.FOOD_COURTS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.AIRPORT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.METRO_STATION.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.BUS_TERMINAL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.ISBT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RAILWAY_STATION.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.TRUCK_TERMINAL.equals(occupancyTypeHelper.getSubtype().getCode())) {
					isAssemblyBuilding = true;
				}
			}
		}
		block.setAssemblyBuilding(isAssemblyBuilding);
		return isAssemblyBuilding;
	}

	public static BigDecimal getMaxBuildingHeight(Plan pl) {
		BigDecimal buildingHeight = BigDecimal.ZERO;
		try {
			buildingHeight = pl.getBlocks().stream().map(block -> block.getBuilding().getBuildingHeight())
					.reduce(BigDecimal::max).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return buildingHeight;
	}

	public static void validateServiceFloor(Plan pl, Block b, Floor f) {
		
		BigDecimal noOfFloorsAboveGround = BigDecimal.ZERO;
		for (Floor floor : b.getBuilding().getFloors()) {
			if (floor.getNumber() != null && floor.getNumber() >= 0) {
				noOfFloorsAboveGround = noOfFloorsAboveGround.add(BigDecimal.valueOf(1));
			}
		}

		boolean hasTerrace = b.getBuilding().getFloors().stream()
				.anyMatch(floor -> floor.getTerrace().equals(Boolean.TRUE));

		noOfFloorsAboveGround = hasTerrace ? noOfFloorsAboveGround.subtract(BigDecimal.ONE) : noOfFloorsAboveGround;
		
		boolean isServiceFloor = !f.getServiceFloors().isEmpty();


		BigDecimal totalArea = BigDecimal.ZERO;
		BigDecimal height = BigDecimal.ZERO;
		
		if (isServiceFloor) {
			for (ServiceFloor serviceFloor : f.getServiceFloors()) {
				
				if (!pl.getSubOccupanciesMaster().isEmpty()
						&& !pl.getSubOccupanciesMaster().containsKey(serviceFloor.getColorCode())) {
					pl.addError("Invalid_color_code", "Provided service floor color code is not valid.");
				}
				
				totalArea = totalArea.add(serviceFloor.getArea());
				height = height.add(serviceFloor.getHeight());
				
				if (noOfFloorsAboveGround.compareTo(new BigDecimal("4")) <= 0)
					pl.addError("SERVICE_FLOOR", "Service Floor not allowed in less then 5 story building");
				if (f.getNumber() <= 0)
					pl.addError("SERVICE_FLOOR1", "Service Floor not allowed on floor 0 or besment");
			}	
		}

		f.setIsServiceFloor(isServiceFloor);
		totalArea = roundUp(totalArea);
		f.setTotalServiceArea(totalArea);
		height = roundUp(height);
		f.setServiceFloorHeight(height);
	}
	
	public static void validateExistingServiceFloor(Plan pl, Block b, Floor f) {
		
		BigDecimal noOfFloorsAboveGround = BigDecimal.ZERO;
		for (Floor floor : b.getBuilding().getFloors()) {
			if (floor.getNumber() != null && floor.getNumber() >= 0) {
				noOfFloorsAboveGround = noOfFloorsAboveGround.add(BigDecimal.valueOf(1));
			}
		}

		boolean hasTerrace = b.getBuilding().getFloors().stream()
				.anyMatch(floor -> floor.getTerrace().equals(Boolean.TRUE));

		noOfFloorsAboveGround = hasTerrace ? noOfFloorsAboveGround.subtract(BigDecimal.ONE) : noOfFloorsAboveGround;
		
		boolean isExistingServiceFloor = !f.getExistingServiceFloors().isEmpty();


		BigDecimal totalExistingArea = BigDecimal.ZERO;
		BigDecimal existingHeight = BigDecimal.ZERO;
		
		if (isExistingServiceFloor) {
			for (ServiceFloor serviceFloor : f.getExistingServiceFloors()) {
				
				if (!pl.getSubOccupanciesMaster().isEmpty()
						&& !pl.getSubOccupanciesMaster().containsKey(serviceFloor.getColorCode())) {
					pl.addError("Invalid_color_code", "Provided service floor color code is not valid.");
				}
				
				totalExistingArea = totalExistingArea.add(serviceFloor.getArea());
				existingHeight = existingHeight.add(serviceFloor.getHeight());
				
				if (noOfFloorsAboveGround.compareTo(new BigDecimal("4")) <= 0)
					pl.addError("SERVICE_FLOOR", "Service Floor not allowed in less then 5 story building");
				if (f.getNumber() <= 0)
					pl.addError("SERVICE_FLOOR1", "Service Floor not allowed on floor 0 or besment");
			}	
		}

		f.setIsExistingServiceFloor(isExistingServiceFloor);
		totalExistingArea = roundUp(totalExistingArea);
		f.setTotalExistingServiceArea(totalExistingArea);
		existingHeight = roundUp(existingHeight);
		f.setExistingServiceFloorHeight(existingHeight);
	}

	public static void validateStilledFloor(Plan pl, Block b, Floor f) {
		
		BigDecimal totalStilledArea = BigDecimal.ZERO;
		BigDecimal flrHeight = BigDecimal.ZERO;
		
		boolean isStiltFloor = !f.getStiltFloors().isEmpty();
		
		if (isStiltFloor) {
			for (StiltFloor stiltFloor : f.getStiltFloors()) {
				
				if (!pl.getSubOccupanciesMaster().isEmpty()
						&& !pl.getSubOccupanciesMaster().containsKey(stiltFloor.getColorCode())) {
					pl.addError("Invalid_color_code", "Provided stilt floor color code is not valid.");
				}
				
				if (stiltFloor.getHeight().compareTo(BigDecimal.valueOf(2.4)) < 0) {
					pl.addError("stilt_floor_dimension", "Stilt floor cannot be less than 2.4m.");
				}
				
				totalStilledArea = totalStilledArea.add(stiltFloor.getArea());
				flrHeight = flrHeight.add(stiltFloor.getHeight());
				
				if (f.getNumber() < 0) {
					pl.addError("STILT_FLOOR", "Stilt Floor can not be in besment.");
				}

			}	
		}
		Boolean isApplicable = true;
		
		if (isStiltFloor) {
			List<BigDecimal> heights = f.getStiltFloors().stream().map(stilt -> stilt.getHeight())
					.collect(Collectors.toList());
			
			for(BigDecimal bd: heights) {
				if(!(bd.compareTo(BigDecimal.valueOf(2.4))==0)) {
					isApplicable = false;
				}
				
			}
		}
		
		if (isStiltFloor) {
			if (b.getBuilding().getDeclaredBuildingHeight().compareTo(new BigDecimal("15")) < 0) {
				if (isApplicable) {
					b.getBuilding()
							.setBuildingHeight(b.getBuilding().getHeightDeducted() ? b.getBuilding().getBuildingHeight()
									: b.getBuilding().getBuildingHeight().subtract(BigDecimal.valueOf(2.4)));
					b.getBuilding().setHeightDeducted(true);
				}

			} else {
				if (isApplicable) {
					b.getBuilding()
							.setBuildingHeight(b.getBuilding().getHeightDeducted() ? b.getBuilding().getBuildingHeight()
									: b.getBuilding().getBuildingHeight().subtract(BigDecimal.valueOf(2.4)));

					b.getBuilding().setHeightDeducted(true);
				}
			}
		}
	

		f.setIsStiltFloor(isStiltFloor);
		totalStilledArea = roundUp(totalStilledArea);
		f.setTotalStiltArea(totalStilledArea);
		flrHeight = roundUp(flrHeight);
		f.setStiltFloorHeight(flrHeight);
	}

	public static void validateExistingStilledFloor(Plan pl, Block b, Floor f) {

		BigDecimal totalExistingStilledArea = BigDecimal.ZERO;
		BigDecimal existingFlrHeight = BigDecimal.ZERO;

		boolean isExistingStiltFloor = !f.getExistingStiltFloors().isEmpty();
		
		boolean isStiltFloor = !f.getStiltFloors().isEmpty();

		if (isExistingStiltFloor) {
			for (StiltFloor stiltFloor : f.getExistingStiltFloors()) {

				if (!pl.getSubOccupanciesMaster().isEmpty()
						&& !pl.getSubOccupanciesMaster().containsKey(stiltFloor.getColorCode())) {
					pl.addError("Invalid_color_code", "Provided stilt floor color code is not valid.");
				}

				if (stiltFloor.getHeight().compareTo(BigDecimal.valueOf(2.4)) < 0) {
					pl.addError("stilt_floor_dimension", "Stilt floor cannot be less than 2.4m.");
				}

				totalExistingStilledArea = totalExistingStilledArea.add(stiltFloor.getArea());
				existingFlrHeight = existingFlrHeight.add(stiltFloor.getHeight());

				if (f.getNumber() < 0) {
					pl.addError("STILT_FLOOR", "Stilt Floor can not be in besment.");
				}

			}
		}
		Boolean isApplicable = true;

		if (isExistingStiltFloor) {
			List<BigDecimal> heights = f.getExistingStiltFloors().stream().map(stilt -> stilt.getHeight())
					.collect(Collectors.toList());

			for (BigDecimal bd : heights) {
				if (!(bd.compareTo(BigDecimal.valueOf(2.4)) == 0)) {
					isApplicable = false;
				}

			}
		}
		Boolean isStiltApplicable = true;
		
		if (isStiltFloor) {
			List<BigDecimal> heights = f.getStiltFloors().stream().map(stilt -> stilt.getHeight())
					.collect(Collectors.toList());
			
			for(BigDecimal bd: heights) {
				if(!(bd.compareTo(BigDecimal.valueOf(2.4))==0)) {
					isStiltApplicable = false;
				}
				
			}
		}
		
		if(!(isStiltFloor && isStiltApplicable)) {
			
			if (isExistingStiltFloor) {
				if (b.getBuilding().getDeclaredBuildingHeight().compareTo(new BigDecimal("15")) < 0) {
					if (isApplicable) {
						b.getBuilding()
								.setBuildingHeight(b.getBuilding().getHeightDeducted() ? b.getBuilding().getBuildingHeight()
										: b.getBuilding().getBuildingHeight().subtract(BigDecimal.valueOf(2.4)));
						b.getBuilding().setHeightDeducted(true);	
					}
				} else {
					if (isApplicable) {
						b.getBuilding()
								.setBuildingHeight(b.getBuilding().getHeightDeducted() ? b.getBuilding().getBuildingHeight()
										: b.getBuilding().getBuildingHeight().subtract(BigDecimal.valueOf(2.4)));
						b.getBuilding().setHeightDeducted(true);
					
					}	
				}
			}
		}

		f.setExistingStiltFloor(isExistingStiltFloor);
		totalExistingStilledArea = roundUp(totalExistingStilledArea);
		f.setTotalExistingStiltArea(totalExistingStilledArea);
		existingFlrHeight = roundUp(existingFlrHeight);
		f.setExistingStiltFloorHeight(existingFlrHeight);
	}
	
	public static void validateHeightOfTheCeilingOfUpperBasementDeduction(Plan pl, Block b, Floor f) {
		if (isBasementParesent(b)) {
			if (f != null && f.getNumber() == -1) {
				BigDecimal maxLength = BigDecimal.ZERO;
				try {
					maxLength = f.getHeightOfTheCeilingOfUpperBasement().stream().reduce(BigDecimal::max).get();
				} catch (Exception e) {
					// TODO: handle exception
				}
				b.getBuilding().setBuildingHeight(b.getBuilding().getBuildingHeight().subtract(maxLength));
			}
		} else {
//			if (f != null && f.getNumber() == 0) {
//				BigDecimal maxLength = BigDecimal.ZERO;
//				try {
//					maxLength = b.getPlinthHeight().stream().reduce(BigDecimal::max).get();
//				} catch (Exception e) {
//					// TODO: handle exception
//				}
//				b.getBuilding().setBuildingHeight(b.getBuilding().getBuildingHeight().subtract(maxLength));
//			}
		}
	}

	public static boolean isBasementParesent(Block blk) {
		boolean flage = false;
		if (blk.getBuilding().getFloorNumber(-1) != null)
			flage = true;
		return flage;
	}

	public static BigDecimal roundUp(BigDecimal number) {
		number = number.setScale(2, BigDecimal.ROUND_HALF_UP);
		return number;
	}

	public static void setPlanInfoBlkWise(Plan pl, String key) {
		BigDecimal totalUserInPlan = BigDecimal.ZERO;
		for (Block block : pl.getBlocks()) {
			String value = pl.getPlanInfoProperties().get(key.replace("%S", block.getNumber()));
			String glassFacadeOpening = pl.getPlanInfoProperties()
					.get(DxfFileConstants.IS_BLOCK_S_HAVING_ENTIRE_FACADE_IN_GLASS.replace("%S", block.getNumber()));
			try {
				BigDecimal numValue = new BigDecimal(value);
				block.setNumberOfOccupantsOrUsersOrBedBlk(numValue);
				totalUserInPlan.add(numValue);
				block.setGlassFacadeOpening(DxfFileConstants.YES.equals(glassFacadeOpening) ? true : false);
				if (numValue.compareTo(BigDecimal.ZERO) <= 0)
					pl.addError("NUMBER_OF_OCCUPANTS_OR_USERS_" + block.getNumber(),
							"Number Of Occupants/Users/Bed is not defined in block " + block.getNumber());
			} catch (Exception e) {
				pl.addError("NUMBER_OF_OCCUPANTS_OR_USERS_" + block.getNumber(),
						key+" is invalid in block " + block.getNumber());
			}
		}
		pl.getPlanInformation().setNumberOfOccupantsOrUsers(totalUserInPlan);
	}

	public static void updateDUnitInPlan(Plan pl) {
		long totalDU = 0;
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				List<FloorUnit> ews = new ArrayList<>();
				List<FloorUnit> lig = new ArrayList<>();
				List<FloorUnit> mig1 = new ArrayList<>();
				List<FloorUnit> mig2 = new ArrayList<>();
				List<FloorUnit> other = new ArrayList<>();
				List<FloorUnit> room = new ArrayList<>();
				List<FloorUnit> ownersSocietyOffice = new ArrayList<>();
				for (FloorUnit floorUnit : floor.getUnits()) {
					switch (floorUnit.getColorCode()) {
					case COLOR_EWS:
						ews.add(floorUnit);
						totalDU++;
						break;
					case COLOR_LIG:
						lig.add(floorUnit);
						totalDU++;
						break;
					case COLOR_MIG1:
						mig1.add(floorUnit);
						totalDU++;
						break;
					case COLOR_MIG2:
						mig2.add(floorUnit);
						totalDU++;
						break;
					case COLOR_OTHER:
						other.add(floorUnit);
						totalDU++;
						break;
					case COLOR_ROOM:
						room.add(floorUnit);
						totalDU++;
						break;
					case COLOR_OWNERS_SOCIETY_OFFICE:
						ownersSocietyOffice.add(floorUnit);
						break;
					}
				}
				floor.setEwsUnit(ews);
				floor.setLigUnit(lig);
				floor.setMig1Unit(mig1);
				floor.setMig2Unit(mig2);
				floor.setOthersUnit(other);
				floor.setRoomUnit(room);
				floor.setOwnersSocietyOffice(ownersSocietyOffice);
			}
		}
		pl.getPlanInformation().setTotalNoOfDwellingUnits(totalDU);
	}

	public static BigDecimal getTotalTopMostRoofArea(Plan pl) {
		BigDecimal totalArea = BigDecimal.ZERO;

//		for (Block block : pl.getBlocks()) {
//			for (Floor floor : block.getBuilding().getFloors()) {
//				try {
//					BigDecimal area = floor.getRoofAreas().stream().map(roofArea -> roofArea.getArea())
//							.reduce(BigDecimal::add).get();
//					totalArea = totalArea.add(area);
//				} catch (Exception exception) {
//
//				}
//			}
//		}

		for (Block block : pl.getBlocks()) {
			List<Floor> floors = block.getBuilding().getFloors();
			if (floors != null && !floors.isEmpty()) {
				Floor lastFloor = floors.get(floors.size() - 1);
				BigDecimal area = BigDecimal.ZERO;
				try {
					area = lastFloor.getRoofAreas().stream().map(roofArea -> roofArea.getArea()).reduce(BigDecimal::add)
							.get();
				} catch (Exception e) {
					// TODO: handle exception
				}
				if (area == null || area.compareTo(BigDecimal.ZERO) <= 0) {
					pl.addError("RoofArea", "RoofArea is not defined in block " + block.getNumber());
				}
				totalArea = totalArea.add(area);
			}

		}

		return totalArea;
	}

	public static BigDecimal getTotalRoofArea(Plan pl) {
		BigDecimal totalArea = BigDecimal.ZERO;

		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				try {
					BigDecimal area = floor.getRoofAreas().stream().map(roofArea -> roofArea.getArea())
							.reduce(BigDecimal::add).get();
					totalArea = totalArea.add(area);
				} catch (Exception exception) {

				}
			}
		}

		return totalArea;
	}

	public static List<Room> getRegularRoom(Plan pl, List<Room> rooms, Set<String> allowedRooms) {
		Map<String, Integer> heightOfRoomFeaturesColor = pl.getSubFeatureColorCodesMaster().get("HeightOfRoom");
		List<Integer> allowedRoomColorCode = new ArrayList<>();
		for (String allowedRoom : allowedRooms) {
			allowedRoomColorCode.add(heightOfRoomFeaturesColor.get(allowedRoom));
		}
		List<Room> spcRoom = new ArrayList<Room>();
		if (rooms != null) {
			for (Room room : rooms) {
				List<Measurement> measurements = new ArrayList<>();
				List<RoomHeight> heightOfRooms = new ArrayList<>();
				if (room.getRooms() != null && !room.getRooms().isEmpty() && room.getRooms().size() >= 1) {
					for (Measurement r : room.getRooms()) {
						if (allowedRoomColorCode.contains(r.getColorCode())) {
							measurements.add(r);
						}
					}
					for (RoomHeight roomHeight : room.getHeights()) {
						if (allowedRoomColorCode.contains(roomHeight.getColorCode())) {
							RoomHeight height = new RoomHeight();
							height.setColorCode(roomHeight.getColorCode());
							height.setHeight(roomHeight.getHeight());
							heightOfRooms.add(height);
						}
					}
					// lightAndVentilation
					if (!measurements.isEmpty()) {
						Room room2 = new Room();
						room2.setNumber(room.getNumber());
						room2.setHeights(heightOfRooms);
						room2.setClosed(room.getClosed());
						room2.setRooms(measurements);
						room2.setLightAndVentilation(room.getLightAndVentilation());
						room2.setMezzanineAreas(room.getMezzanineAreas());
						spcRoom.add(room2);
					}
				}

			}
		}
		return spcRoom;
	}

	public static Map<Integer, String> getRoomColorCodesMaster(Plan pl) {
		Map<Integer, String> result = new HashMap<>();
		Set<String> allowedRooms = new HashSet<>();
		allowedRooms.add(DxfFileConstants.COLOR_STUDY_ROOM);
		allowedRooms.add(DxfFileConstants.COLOR_LIBRARY_ROOM);
		allowedRooms.add(DxfFileConstants.COLOR_GAME_ROOM);
		allowedRooms.add(DxfFileConstants.COLOR_STORE_ROOM);
		allowedRooms.add(DxfFileConstants.COLOR_GUARD_ROOM);
		allowedRooms.add(DxfFileConstants.COLOR_ELECTRIC_CABIN_ROOM);
		allowedRooms.add(DxfFileConstants.COLOR_SUB_STATION_ROOM);
		allowedRooms.add(DxfFileConstants.COLOR_GYM_ROOM);
		allowedRooms.add(DxfFileConstants.COLOR_CCTV_ROOM);
		allowedRooms.add(DxfFileConstants.COLOR_SERVICE_ROOM);
		allowedRooms.add(DxfFileConstants.COLOR_MEP_ROOM);
		allowedRooms.add(DxfFileConstants.COLOR_LIFT_LOBBY);
		allowedRooms.add(DxfFileConstants.COLOR_STILT_FLOOR);
		allowedRooms.add(DxfFileConstants.COLOR_SERVICE_FLOOR);
		allowedRooms.add(DxfFileConstants.COLOR_LAUNDRY_ROOM);
		allowedRooms.add(DxfFileConstants.COLOR_GENERATOR_ROOM);

		allowedRooms.add(DxfFileConstants.COLOR_RESIDENTIAL_ROOM_NATURALLY_VENTILATED);
		allowedRooms.add(DxfFileConstants.COLOR_RESIDENTIAL_ROOM_MECHANICALLY_VENTILATED);
		allowedRooms.add(DxfFileConstants.COLOR_PUBLIC_WASHROOM);

		for (Map.Entry<String, Integer> entry : pl.getSubFeatureColorCodesMaster().get("HeightOfRoom").entrySet()) {
			if (allowedRooms.contains(entry.getKey())) {
				result.put(entry.getValue(), entry.getKey());
			}
		}
		return result;
	}

	public static BigDecimal getRoofTopParking(Plan pl) {
		ParkingDetails details = pl.getParkingDetails();
		BigDecimal totalParking = BigDecimal.ZERO;
		if (details.getSpecial() != null && !details.getSpecial().isEmpty()) {
			for (Measurement measurement : details.getSpecial()) {
				switch (measurement.getColorCode()) {
				case Parking.COLOR_LAYER_SPECIAL_PARKING_ROOF_TOP_PARKING:
					totalParking = totalParking.add(measurement.getArea()).setScale(2, BigDecimal.ROUND_HALF_UP);
					break;
				}
			}
		}
		return totalParking;
	}

	public static BigDecimal getOpenParking(Plan pl) {
		BigDecimal openParking = BigDecimal.ZERO;
		try {
			openParking = pl.getParkingDetails().getOpenCars().stream().map(Measurement::getArea)
					.reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, BigDecimal.ROUND_HALF_UP);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return openParking;
	}

	public static boolean isEWSOrLIGBlock(Plan pl, Block block) {
		boolean flage = false;
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		if (DxfFileConstants.EWS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.LOW_INCOME_HOUSING.equals(occupancyTypeHelper.getSubtype().getCode())) {
			return true;
		} else {
			for (Floor floor : block.getBuilding().getFloors()) {
				for (Occupancy occupancy : floor.getOccupancies()) {
					OccupancyTypeHelper helper = occupancy.getTypeHelper();
					if (helper != null && helper.getSubtype() != null
							&& (DxfFileConstants.EWS.equals(helper.getSubtype().getCode())
									|| DxfFileConstants.LOW_INCOME_HOUSING.equals(helper.getSubtype().getCode()))) {
						return true;
					}

				}
			}
		}
		return flage;
	}

	public static boolean isSpecialBuilding(Plan pl) {
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
				|| DxfFileConstants.FIVE_STAR_HOTEL.equals(occupancyTypeHelper.getSubtype().getCode()))
			isSplOccupancy = true;

		boolean isMixedOccupancies = false;// need to add condition

		if (isAssemblyBuilding || isHazardousBuildings || isBuildingCentrallyAirConditioned || isSplOccupancy
				|| isMixedOccupancies)
			specialBuilding = true;

		return specialBuilding;
	}

	public static void updateBlock(Plan pl) {
		List<Block> outhouses = new ArrayList<>();
		List<Block> pulicwashroom = new ArrayList<>();
		List<Block> ict = new ArrayList<>();
		List<Block> blocks = new ArrayList<>();

		for (Block block : pl.getBlocks()) {
			boolean outhousesFlage = false;
			boolean pulicwashroomFlage = false;
			boolean ictFlag = false;
			for (Floor floor : block.getBuilding().getFloors()) {
				for (Occupancy occupancy : floor.getOccupancies()) {
					//LOG.info("block - "+block.getNumber()+" floor - "+floor.getNumber()+" occupancy - "+occupancy.getTypeHelper()!=null && occupancy.getTypeHelper().getSubtype()!=null?occupancy.getTypeHelper().getSubtype().getCode():"");
					if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
							&& DxfFileConstants.OUTHOUSE.equals(occupancy.getTypeHelper().getSubtype().getCode())) {
						outhousesFlage = true;
					}

					if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
							&& DxfFileConstants.PUBLIC_WASHROOMS
									.equals(occupancy.getTypeHelper().getSubtype().getCode())) {
						pulicwashroomFlage = true;
					}
					
					if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
							&& DxfFileConstants.INFO_COMMS_SYSTEM
									.equals(occupancy.getTypeHelper().getSubtype().getCode())) {
						ictFlag = true;
					}
					
				}
				if (outhousesFlage && pulicwashroomFlage && ictFlag) {
					break;
				}
			}
			block.setOutHouse(outhousesFlage);
			block.setPublicWashroom(pulicwashroomFlage);
			block.setIct(ictFlag);
			
			if (outhousesFlage) {
				outhouses.add(block);
				removeSetbackError(pl, block);
			} else if (pulicwashroomFlage) {
				pulicwashroom.add(block);
				removeSetbackError(pl, block);
			} else if(ictFlag) {
				ict.add(block);
			} else {
				blocks.add(block);
			}
		}
		pl.setBlocks(blocks);
		pl.setOuthouse(outhouses);
		pl.setPublicWashroom(pulicwashroom);
		pl.setIctBlocks(ict);
	}

	private static void removeSetbackError(Plan pl, Block block) {
		Set<Map.Entry<String, String>> set = pl.getErrors().entrySet();
		Iterator<Map.Entry<String, String>> iterator = set.iterator();
		String setbackerror = "BLK_%s_LVL_0_FRONT_SETBACK".replace("%s", block.getNumber());
		while (iterator.hasNext()) {
			Map.Entry<String, String> entry = iterator.next();
			String value = entry.getValue();
			if (value.contains(setbackerror))
				iterator.remove();
		}
	}

	public static BigDecimal getNumberOfPerson(Plan pl) {
		OccupancyTypeHelper mostRestrictiveOccupancyType = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		BigDecimal numberOfPerson = BigDecimal.ZERO;
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				numberOfPerson.add(getNumberPerson(floor.getArea(), mostRestrictiveOccupancyType, floor.getNumber()));
			}
		}

		return numberOfPerson;

	}

	private static BigDecimal getNumberPerson(BigDecimal bulidUpArea, OccupancyTypeHelper mostRestrictiveOccupancyType,
			int floor) {
		BigDecimal numberOfPerson = BigDecimal.ZERO;
		BigDecimal perPersonBuildupArea = BigDecimal.ZERO;
		if (DxfFileConstants.OC_RESIDENTIAL.equals(mostRestrictiveOccupancyType.getType().getCode())
				|| DxfFileConstants.OC_AGRICULTURE.equals(mostRestrictiveOccupancyType.getType().getCode()))
			perPersonBuildupArea = BigDecimal.valueOf(12.5);
		else if (DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(mostRestrictiveOccupancyType.getType().getCode())
				|| DxfFileConstants.OC_TRANSPORTATION.equals(mostRestrictiveOccupancyType.getType().getCode())
				|| DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE
						.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE
						.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT
						.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.WARE_HOUSE.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.GOOD_STORAGE.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.GODOWNS.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.GAS_GODOWN.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.HOTEL.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.FIVE_STAR_HOTEL.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.MOTELS.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.BANK.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.RESORTS.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.LAGOONS_AND_LAGOON_RESORT
						.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS
						.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.FINANCIAL_SERVICES_AND_STOCK_EXCHANGES
						.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX
						.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.PROFESSIONAL_OFFICES.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.HOLIDAY_RESORT.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.GUEST_HOUSES.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.BOARDING_AND_LODGING_HOUSES
						.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.RESTAURANT.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION
						.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION
						.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.CNG_MOTHER_STATION.equals(mostRestrictiveOccupancyType.getSubtype().getCode())
				|| DxfFileConstants.WEIGH_BRIDGES.equals(mostRestrictiveOccupancyType.getSubtype().getCode()))
			perPersonBuildupArea = BigDecimal.valueOf(10);
		else if (DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL
				.equals(mostRestrictiveOccupancyType.getType().getCode())
				|| DxfFileConstants.OC_PUBLIC_UTILITY.equals(mostRestrictiveOccupancyType.getType().getCode()))
			perPersonBuildupArea = BigDecimal.valueOf(15);
		else if (DxfFileConstants.OC_EDUCATION.equals(mostRestrictiveOccupancyType.getType().getCode()))
			perPersonBuildupArea = BigDecimal.valueOf(4);
		else if (DxfFileConstants.OC_COMMERCIAL.equals(mostRestrictiveOccupancyType.getType().getCode())) {
			if (floor <= 0)
				perPersonBuildupArea = BigDecimal.valueOf(1);
			else
				perPersonBuildupArea = BigDecimal.valueOf(6);

		}
		
		if(perPersonBuildupArea.compareTo(BigDecimal.ZERO)>0)
			numberOfPerson = perPersonBuildupArea.divide(perPersonBuildupArea);

		return new BigDecimal(String.format("%.0f", numberOfPerson));

	}

	private static final int COLOR_AMMENITY_GUARD_ROOM = 1;
	private static final int COLOR_ELECTRIC_CABIN = 2;
	private static final int COLOR_SUB_STATION = 3;
	private static final int COLOR_AREA_FOR_GENERATOR_SET = 4;
	private static final int COLOR_ATM = 5;
	private static final int COLOR_OTHER_AMMENITY = 6;

	public static void updateAmmenity(Plan pl) {
		Ammenity ammenity = new Ammenity();
		for (AccessoryBlock accessoryBlock : pl.getAccessoryBlocks()) {
			for (Measurement measurement : accessoryBlock.getAccessoryBuilding().getUnits()) {
				switch (measurement.getColorCode()) {
				case COLOR_AMMENITY_GUARD_ROOM:
					ammenity.getGuardRooms().add(measurement);
					break;
				case COLOR_ELECTRIC_CABIN:
					ammenity.getElectricCabins().add(measurement);
					break;
				case COLOR_SUB_STATION:
					ammenity.getSubStations().add(measurement);
					break;
				case COLOR_AREA_FOR_GENERATOR_SET:
					ammenity.getAreaForGeneratorSet().add(measurement);
					break;
				case COLOR_ATM:
					ammenity.getAtms().add(measurement);
					break;
				case COLOR_OTHER_AMMENITY:
					ammenity.getOtherAmmenities().add(measurement);
					break;
				}
			}
		}

		pl.setAmmenity(ammenity);

	}

	public static boolean isLiftPersent(Block block, List<Integer> colorCodes) {
		boolean flage = false;

		for (Floor floor : block.getBuilding().getFloors()) {
			for (Lift lift : floor.getLifts()) {
				Measurement measurement = lift.getLifts().get(0);
				if (colorCodes.contains(measurement.getColorCode())) {
					flage = true;
					break;
				}
			}
			if (flage)
				break;
		}

		return flage;
	}

	public static void computeOccupancyPercentage(Plan pl) {
		Map<String, OccupancyPercentage> ocPercentage = new HashMap<>();

		for (Block bl : pl.getBlocks()) {
			for (Floor flr : bl.getBuilding().getFloors()) {
				for (Occupancy oc : flr.getOccupancies()) {
					OccupancyHelperDetail ohd = oc.getTypeHelper().getSubtype() == null ? oc.getTypeHelper().getType()
							: oc.getTypeHelper().getSubtype();
					BigDecimal existingBua = ocPercentage.get(ohd.getName()) != null
							? ocPercentage.get(ohd.getName()).getTotalBuildUpArea()
							: BigDecimal.ZERO;
					BigDecimal existingFlrArea = ocPercentage.get(ohd.getName()) != null
							? ocPercentage.get(ohd.getName()).getTotalFloorArea()
							: BigDecimal.ZERO;
					BigDecimal existingcarpetArea = ocPercentage.get(ohd.getName()) != null
							? ocPercentage.get(ohd.getName()).getTotalCarpetArea()
							: BigDecimal.ZERO;
					OccupancyPercentage ocp = new OccupancyPercentage();
					ocp.setOccupancy(oc.getTypeHelper().getType().getName());
					ocp.setSubOccupancy(ohd.getName());
					ocp.setOccupancyCode(oc.getTypeHelper().getType().getCode());
					ocp.setSubOccupancyCode(ohd.getCode());
					ocp.setTotalBuildUpArea(existingBua.add(oc.getBuiltUpArea()));
					ocp.setTotalCarpetArea(existingcarpetArea.add(oc.getCarpetArea()));
					ocp.setTotalFloorArea(existingFlrArea.add(
							oc.getFarAreaDeductExisting() != null 
							&& oc.getFarAreaDeductExisting().compareTo(BigDecimal.ZERO) != 0 ? 
									oc.getExistingFloorArea()
									: oc.getFloorArea()));
					ocPercentage.put(ohd.getName(), ocp);
				}
			}
		}

		for (String oc : ocPercentage.keySet()) {
			BigDecimal percentage = ocPercentage.get(oc).getTotalBuildUpArea().multiply(BigDecimal.valueOf(100)).divide(
					pl.getVirtualBuilding().getTotalBuitUpArea(), DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);
			ocPercentage.get(oc).setPercentage(percentage);
			// ocPercentage.put(oc, percentage);
		}

		pl.getPlanInformation().setOccupancyPercentages(ocPercentage);
	}


	public static boolean isStairRequired(Plan pl, Block block) {
		BigDecimal buildingHeight = block.getBuilding().getBuildingHeight();
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		boolean flage = false;
		if ((DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING
				.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.SEMI_DETACHED.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.ROW_HOUSING.equals(occupancyTypeHelper.getSubtype().getCode()))
				&& buildingHeight.compareTo(new BigDecimal("15")) < 0) {
			flage = true;
		}

		return flage;
	}

	public static void additionalValidation(Plan pl) {
		BigDecimal buildupArea = pl.getVirtualBuilding().getTotalBuitUpArea();

		if (buildupArea.compareTo(new BigDecimal("500")) > 0) {
			if (pl.getPlanInformation().getProjectValueForEIDP() == null
					|| pl.getPlanInformation().getProjectValueForEIDP().compareTo(BigDecimal.ZERO) <= 0) {
				pl.addError("projectValueForEIDP500",
						"Project value is mandatory for project with more than 500 BuitUpArea.");
			}
		}
		
		if (pl.getPlanInformation().getProjectValueForEIDP() != null) {
			// validate project value for EIDP fee.
			String rate = new String();
			List listNode = JsonPath.read(pl.getMdmsMasterData(), "$.ConstructionCostRate");

			String filterExp = "$.[?(@.name == '" + DxfFileConstants.PERSQFTCOST + "')]";
			List<Map<String, String>> constructionCostRateJson = JsonPath.read(listNode, filterExp);

			rate = constructionCostRateJson.get(0).get("rate");
			if (rate != null) {
				try {
					Float constructionRate = Float.parseFloat(rate);
					BigDecimal constructionRateBigDecimal = BigDecimal.valueOf(constructionRate);
					BigDecimal totalBuiltupArea = pl.getVirtualBuilding().getTotalBuitUpArea();

					if (pl.getPlanInformation().getProjectValueForEIDP().compareTo(
							constructionRateBigDecimal.multiply(totalBuiltupArea).multiply(SQMT_SQFT_MULTIPLIER)) < 0) {
						pl.addError("ConstructionRate",
								"Project value entered in the plan info cannot be less than the construction cost of the project.");
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}
		
		// TODO: 11 City level customization for Puri code starts here.

		OdishaUlbs ulb = OdishaUlbs.getUlb(pl.getThirdPartyUserTenantld());

		/*
		 * Point 3. In Konark NAC, Puri Municipality and PKDA if Coastal regulation
		 * zone-3 rule is applicable, building permission will be given only up to
		 * overall 9 meter height (maximum 2 floors) and maximum 33% ground coverage is
		 * allowed for all development.
		 */
		if (ulb.getUlbCode().equals("od.puri") || ulb.getUlbCode().equals("od.purikonarkdevelopmentauthority")
				|| ulb.getUlbCode().equals("od.konark") || ulb.getUlbCode().equals("od.nimapara")) {

			// If CRZ zone is 1, scrutiny to be rejected with error "Construction not
			// allowed in CRZ 1 area."
			if (pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea() != null) {
				try {
					if (DxfFileConstants.NA
							.equals(pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea())) {

					} else if (Integer
							.parseInt(pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea()) == 1) {
						pl.addError("CRZ_Zone_1", "Construction is not allowed in CRZ 1 area.");
					} else if (Integer
							.parseInt(pl.getPlanInformation().getCrzNumberForProjectsFallingUnderCrzArea()) == 3) {

						if (pl.getThirdPartyUserTenantld().equals("od.puri")
								|| pl.getThirdPartyUserTenantld().equals("od.purikonarkdevelopmentauthority")
								|| pl.getThirdPartyUserTenantld().equals("od.konark")) {

							if (getMaxNumberOfFloor(pl) > 2) {
								pl.addError("Max_Floor_exceeded", "Maximum 2 floors are allowed in CRZ 3 area.");
							}

						}

					}

				} catch (Exception e) {
					e.printStackTrace();
					pl.addError("Invalid_CRZ_Zone", "Invalid CRZ number declared.");
				}
			}
		}
		// City level customization for Puri code ends here.
		
		for (Block b : pl.getBlocks()) {
			if(b.isPublicWashroom()) 
				continue;
			
			if(b.getBuilding().getBuildingHeight().compareTo(BigDecimal.ZERO) <= 0) {
				pl.addError("BUILDING_HEIGHT", "Building height must be greater than 0");
			}
		}
		
		if (pl.getPlanInformation().getIsProjectComingUnderShopCumResidential() != null
				&& pl.getPlanInformation().getIsProjectComingUnderShopCumResidential().equalsIgnoreCase("YES")
				&& pl.getPlanInformation().getAreThePlotsAllotedInRow() != null
				&& pl.getPlanInformation().getAreThePlotsAllotedInRow().equalsIgnoreCase("YES")) {

			for (Block b : pl.getBlocks()) {
				if (b.getBuilding().getBuildingHeight().compareTo(BigDecimal.valueOf(12.0)) > 0) {
					pl.addError("BUILDING_HEIGHT", "Building height cannot be more than 12m in shop cum residential.");
				}
			}
		}
		

	}

	public static void validateRestricatedOccupancies(Plan pl) {
		// SPARIT Industry Check
//		OdishaUlbs ulb = OdishaUlbs.getUlb(pl.getThirdPartyUserTenantld());
//		if (ulb.isSparitFlag()) {
//			OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
//			System.out.println("occupancy:" + occupancyTypeHelper.getType().getCode());
//			if (DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(occupancyTypeHelper.getType().getCode())) {
//				pl.addError("occupancyError", "Industry is not allowed in this area");
//			}
//
//		}
		
		//this restriction is removed as of now: prod issue date 12/01/2024
	}

	public static BigDecimal getStiltArea(Plan plan) {
		BigDecimal area = BigDecimal.ZERO;
		for (Block block : plan.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				if (floor.getIsStiltFloor())
					area = area.add(floor.getArea());
			}
		}
		area = area.setScale(2, BigDecimal.ROUND_HALF_UP);
		return area;
	}

	public static List<ScrutinyDetail> getScrutinyDetailsFromPlan(Plan pl, String Key) {
		List<ScrutinyDetail> details = null;
		details = pl.getReportOutput().getScrutinyDetails().stream()
				.filter(s -> (s.getKey() != null && s.getKey().endsWith(Key))).collect(Collectors.toList());
		return details;

	}
	
	//TODO: 11 City level customization for Puri code starts here.
	public static int getMaxNumberOfFloor(Plan pl) {
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
	
	
	public static boolean isOccupancyPersent(Plan pl,String code) {
		Set<String> codes = new HashSet<>();
		for(Block b:pl.getBlocks()) {
			for(Floor f:b.getBuilding().getFloors()) {
				for(Occupancy o:f.getOccupancies()) {
					if(o.getTypeHelper()!=null && o.getTypeHelper().getType()!=null && code.equals(o.getTypeHelper().getType().getCode()))
						codes.add(o.getTypeHelper().getType().getCode());
					if(o.getTypeHelper()!=null && o.getTypeHelper().getSubtype()!=null && code.equals(o.getTypeHelper().getSubtype().getCode()))
						codes.add(o.getTypeHelper().getSubtype().getCode());
				}
			}
		}
		return codes.contains(code);
	}
	
	public static OccupancyTypeHelper getMostRestrictiveBlockMaxBuildupArea(Block block) {
		OccupancyTypeHelper occupancyTypeHelper = null;

		Occupancy maxOccupancyArea = null;

		for (Occupancy occupancy : block.getBuilding().getOccupancies()) {
			if (maxOccupancyArea == null || maxOccupancyArea.getBuiltUpArea() == null
					|| maxOccupancyArea.getBuiltUpArea().compareTo(occupancy.getBuiltUpArea()) < 0) {
				if (occupancy.getTypeHelper() != null)
					maxOccupancyArea = occupancy;
			}

		}
		
		if(maxOccupancyArea != null) {
			return maxOccupancyArea.getTypeHelper();
		}

		return occupancyTypeHelper;
	}
}
