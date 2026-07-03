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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.FloorUnit;
import org.egov.common.entity.edcr.Lift;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.ParkingDetails;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class LiftService extends FeatureProcess {

	private static final String SUBRULE_118 = "ODA Rule 42 (3), Annexure VIII (4-iv)";
	public static final int COLOR_GENERAL_LEFT = 1;
	public static final int COLOR_SPECIAL_LEFT = 2;
	public static final int COLOR_CAR_LEFT = 3;

	@Override
	public Plan validate(Plan plan) {
//		for (Block block : plan.getBlocks()) {
//			if (block.getBuilding() != null && !block.getBuilding().getFloors().isEmpty()) {
//				for (Floor floor : block.getBuilding().getFloors()) {
//					List<Lift> lifts = floor.getLifts();
//					if (lifts != null && !lifts.isEmpty()) {
//						for (Lift lift : lifts) {
//							List<Measurement> liftPolyLines = lift.getLifts();
//							if (liftPolyLines != null && !liftPolyLines.isEmpty()) {
//								validateDimensions(plan, block.getNumber(), floor.getNumber(),
//										lift.getNumber().toString(), liftPolyLines);
//							}
//						}
//					}
//				}
//			}
//		}
		return plan;
	}

	@Override
	public Plan process(Plan pl) {
		// validate(plan);
		if (pl != null && !pl.getBlocks().isEmpty()) {
			for (Block block : pl.getBlocks()) {
				ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
				scrutinyDetail.addColumnHeading(1, RULE_NO);
				scrutinyDetail.addColumnHeading(2, DESCRIPTION);
				scrutinyDetail.addColumnHeading(3, FLOOR);
				scrutinyDetail.addColumnHeading(4, REQUIRED);
				scrutinyDetail.addColumnHeading(5, PROVIDED);
				scrutinyDetail.addColumnHeading(6, STATUS);
				scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "General Lift");

				ScrutinyDetail scrutinyDetail1 = new ScrutinyDetail();
				scrutinyDetail1.addColumnHeading(1, RULE_NO);
				scrutinyDetail1.addColumnHeading(2, DESCRIPTION);
				scrutinyDetail1.addColumnHeading(3, FLOOR);
				scrutinyDetail1.addColumnHeading(4, REQUIRED);
				scrutinyDetail1.addColumnHeading(5, PROVIDED);
				scrutinyDetail1.addColumnHeading(6, STATUS);
				scrutinyDetail1.setKey("Block_" + block.getNumber() + "_" + "Special Lift");

				ScrutinyDetail scrutinyDetail2 = new ScrutinyDetail();
				scrutinyDetail2.addColumnHeading(1, RULE_NO);
				scrutinyDetail2.addColumnHeading(2, DESCRIPTION);
				scrutinyDetail2.addColumnHeading(3, FLOOR);
				scrutinyDetail2.addColumnHeading(4, REQUIRED);
				scrutinyDetail2.addColumnHeading(5, PROVIDED);
				scrutinyDetail2.addColumnHeading(6, STATUS);
				scrutinyDetail2.setKey("Block_" + block.getNumber() + "_" + "Car Lift");

				validateGeneral(pl, block, scrutinyDetail);
				validateSpecial(pl, block, scrutinyDetail1);
				validateCar(pl, block, scrutinyDetail2);
			}

		}
		return pl;
	}

	private void validateGeneral(Plan pl, Block block, ScrutinyDetail scrutinyDetail) {

		OccupancyTypeHelper typeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		int requiredcount = 0;
		BigDecimal buildingHeight = block.getBuilding().getBuildingHeight();
		if (DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING
				.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.SEMI_DETACHED.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.ROW_HOUSING.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.DHARMASALA.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.DORMITORY.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.HOSTEL.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.SHELTER_HOUSE.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.STAFF_QAURTER.equals(typeHelper.getSubtype().getCode())) {
			
			if (buildingHeight.compareTo(new BigDecimal("21")) >= 0) {
				requiredcount = 2;
			}
			int count = getTotalDUAbove2Floor(block);
			int requiredCountWithRespectToDU = 0;
			if (count > 0) {
				requiredCountWithRespectToDU = count / 20;
				if ((count % 20) != 0)
					requiredCountWithRespectToDU++;
			}

			if (requiredCountWithRespectToDU > requiredcount)
				requiredcount = requiredCountWithRespectToDU;
		} else if (DxfFileConstants.APARTMENT_BUILDING.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.HOUSING_PROJECT.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.STUDIO_APARTMENTS.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(typeHelper.getSubtype().getCode())) {

			if (buildingHeight.compareTo(new BigDecimal("10")) > 0) {
				requiredcount = 1;
			}
			if (buildingHeight.compareTo(new BigDecimal("21")) >= 0) {
				requiredcount = 2;
			}
			int totalDUAbove2Floor = getTotalDUAbove2Floor(block);
			int requiredCountWithRespectToDU = 0;
			if (totalDUAbove2Floor > 0) {
				requiredCountWithRespectToDU = totalDUAbove2Floor / 20;
				if ((totalDUAbove2Floor % 20) != 0)
					requiredCountWithRespectToDU++;
			}
			if (requiredCountWithRespectToDU > requiredcount)
				requiredcount = requiredCountWithRespectToDU;

			if (totalDUAbove2Floor == getTotalEWSAndLIGDUAbove2Floor(block)) {
				if (buildingHeight.compareTo(new BigDecimal("15")) <= 0) {
					requiredcount = 0;
				} else {
					requiredcount = 1;
				}
				if (buildingHeight.compareTo(new BigDecimal("21")) >= 0 && requiredCountWithRespectToDU <= 2) {
					requiredcount = 2;
				}
			}

		} else if (DxfFileConstants.EWS.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.LOW_INCOME_HOUSING.equals(typeHelper.getSubtype().getCode())) {
			if (buildingHeight.compareTo(new BigDecimal("15")) <= 0) {
				requiredcount = 0;
				// nothing else like dwellingunit to be considered if building height less than 15
			} else {
				requiredcount = 1;
				if (buildingHeight.compareTo(new BigDecimal("21")) >= 0) {
					requiredcount = 2;
				}
				int totalDUAbove2Floor = getTotalDUAbove2Floor(block);
				int requiredCountWithRespectToDU = 0;
				if (totalDUAbove2Floor > 0) {
					requiredCountWithRespectToDU = totalDUAbove2Floor / 20;
					if ((totalDUAbove2Floor % 20) != 0)
						requiredcount++;
				}
				if (requiredCountWithRespectToDU > requiredcount)
					requiredcount = requiredCountWithRespectToDU;
			}
			
		} else if (!DxfFileConstants.OC_RESIDENTIAL.equals(typeHelper.getType().getCode())) {
			if (buildingHeight.compareTo(new BigDecimal("10")) <= 0) {
				requiredcount = 0;
			} else {
				requiredcount = 1;
			}
			if (buildingHeight.compareTo(new BigDecimal("21")) >= 0) {
				requiredcount = 2;
			}

			int requiredCoutAsPerBuildUp = getGenralLiftCountAsPerBuildupAreaAbove2Floor(block);

			if (requiredcount < requiredCoutAsPerBuildUp) {
				requiredcount = requiredCoutAsPerBuildUp;
			}
		}
		
		for (Floor floor : block.getBuilding().getFloors()) {
			String isAccepted = "";
			String desc = "Minimum number of lifts";
			int providedCount = getLifts(floor, COLOR_GENERAL_LEFT).size() + getLifts(floor, COLOR_SPECIAL_LEFT).size();
			
			if(pl.getPlanInformation().getLiftCountRelaxation()!=null && 
					(pl.getPlanInformation().getLiftCountRelaxation().equalsIgnoreCase("NO")
					|| pl.getPlanInformation().getLiftCountRelaxation().equalsIgnoreCase("NA"))) {
				
				if(providedCount >= requiredcount) {
					isAccepted = Result.Accepted.getResultVal();
				}else {
					isAccepted = Result.Not_Accepted.getResultVal();
				}
				
				
			} else if(pl.getPlanInformation().getLiftCountRelaxation()!=null && 
					pl.getPlanInformation().getLiftCountRelaxation().equalsIgnoreCase("YES")) {
				isAccepted = Result.Verify.getResultVal();
				
			}
			
			if (block.getBuilding().getFloors().size() >= 2) {
				Floor lastFloor = block.getBuilding().getFloors().get(block.getBuilding().getFloors().size() - 1);
				Floor secondLastFloor = block.getBuilding().getFloors().get(block.getBuilding().getFloors().size() - 2);

				BigDecimal lastFloorBuiltUpArea = lastFloor.getOccupancies().stream().map(Occupancy::getBuiltUpArea)
						.reduce(BigDecimal.ZERO, BigDecimal::add);

				BigDecimal secondLastFloorBuiltUpArea = secondLastFloor.getOccupancies().stream()
						.map(Occupancy::getBuiltUpArea).reduce(BigDecimal.ZERO, BigDecimal::add);

				if (lastFloorBuiltUpArea.compareTo(secondLastFloorBuiltUpArea) < 0) {
					setReportOutputDetails3("ODA Rule 42 (1,2,4)", desc, floor.getNumber().intValue(), "NA",
							providedCount + "", Result.Accepted.getResultVal(), scrutinyDetail, pl);
				} else {
					setReportOutputDetails3("ODA Rule 42 (1,2,4)", desc, floor.getNumber().intValue(), requiredcount + "",
							providedCount + "", isAccepted, scrutinyDetail, pl);
				}

			} else {
				setReportOutputDetails3("ODA Rule 42 (1,2,4)", desc, floor.getNumber().intValue(), requiredcount + "",
						providedCount + "", isAccepted, scrutinyDetail, pl);
			}			

		}
		String ans = pl.getPlanInformation().getLiftCountRelaxation();
		setReportOutputDetails2("Do you want to avail lift count relaxation as per section 42.2.2 of ODA rules?", ans , scrutinyDetail);

		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	private int getGenralLiftCountAsPerBuildupAreaAbove2Floor(Block block) {
		int count = 0;
		for (Floor floor : block.getBuilding().getFloors()) {
			if (floor.getNumber() > 2) {
				double buildUpArea = floor.getArea().doubleValue();

				int countPerFloor = 0;
				if (buildUpArea != 0) {
					countPerFloor = (int) (buildUpArea / 1000);
				}
				if ((buildUpArea % 1000) != 0)
					countPerFloor++;

				if (count < countPerFloor)
					count = countPerFloor;
			}
		}
		return count;
	}

	private int getTotalDUAbove2Floor(Block block) {
		int count = 0;
		for (Floor floor : block.getBuilding().getFloors()) {
			if (floor.getNumber() > 2) {
				count = count + floor.getEwsUnit().size() + floor.getLigUnit().size() + floor.getMig1Unit().size()
						+ floor.getMig2Unit().size() + floor.getOthersUnit().size() + floor.getRoomUnit().size();
			}
		}
		return count;
	}

	private int getTotalEWSAndLIGDUAbove2Floor(Block block) {
		int count = 0;
		for (Floor floor : block.getBuilding().getFloors()) {
			if (floor.getNumber() > 2)
				count = count + floor.getEwsUnit().size() + floor.getLigUnit().size();
		}
		return count;
	}

	private int getTotalEWSAndLIGDU(Block block) {
		int count = 0;
		for (Floor floor : block.getBuilding().getFloors()) {
				count = count + floor.getEwsUnit().size() + floor.getLigUnit().size();
		}
		return count;
	}

	private void validateSpecial(Plan pl, Block block, ScrutinyDetail scrutinyDetail) {
		boolean isMandatory = false;
		OccupancyTypeHelper typeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		BigDecimal plotArea=pl.getPlot().getArea();
		if(plotArea.compareTo(new BigDecimal("2000"))<=0)
			return;
		if (DxfFileConstants.OC_RESIDENTIAL.equals(typeHelper.getType().getCode())) {
			int totalDu = getTotalDU(block);
			int ewsAndLIGDu = getTotalEWSAndLIGDU(block);
			BigDecimal buildingHeight = block.getBuilding().getBuildingHeight();

			if (DxfFileConstants.EWS.equals(typeHelper.getSubtype().getCode())
					|| DxfFileConstants.LOW_INCOME_HOUSING.equals(typeHelper.getSubtype().getCode()) || totalDu == ewsAndLIGDu) {
				if (buildingHeight.compareTo(new BigDecimal("15")) >= 0)
					isMandatory = true;
			} else {
				if (getTotalDU(block) > 8 && buildingHeight.compareTo(new BigDecimal("10")) >= 0)
					isMandatory = true;
			}
		} else if (DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL.equals(typeHelper.getType().getCode())
				|| DxfFileConstants.OC_EDUCATION.equals(typeHelper.getType().getCode())
				|| (DxfFileConstants.OC_TRANSPORTATION.equals(typeHelper.getType().getCode()))
				&& !(DxfFileConstants.AUTO_STAND.equals(typeHelper.getSubtype().getCode()) 
						|| DxfFileConstants.BUS_STAND.equals(typeHelper.getSubtype().getCode())
						|| DxfFileConstants.TAXI_STAND.equals(typeHelper.getSubtype().getCode())
						|| DxfFileConstants.TOLL_PLAZA.equals(typeHelper.getSubtype().getCode())
						|| DxfFileConstants.PUBLIC_PARKING.equals(typeHelper.getSubtype().getCode()))) {
			isMandatory = true;
		}

		if (isMandatory) {
			int noOFFloors = block.getBuilding().getFloors().size();
			for (Floor floor : block.getBuilding().getFloors()) {
				List<Lift> lifts = getLifts(floor, COLOR_SPECIAL_LEFT);
				// checkCount
				int countRequired = noOFFloors > 1 ? 1 : 0;
				
				
				String desc = "Minimum number of lifts";
				int providedCount = lifts.size();
				boolean isCountAccepted = providedCount >= countRequired;
				
				if (block.getBuilding().getFloors().size() >= 2) {
					Floor lastFloor = block.getBuilding().getFloors().get(block.getBuilding().getFloors().size() - 1);
					Floor secondLastFloor = block.getBuilding().getFloors().get(block.getBuilding().getFloors().size() - 2);
					
					if(floor.getNumber()==lastFloor.getNumber()) {
						BigDecimal lastFloorBuiltUpArea = lastFloor.getOccupancies().stream().map(Occupancy::getBuiltUpArea)
								.reduce(BigDecimal.ZERO, BigDecimal::add);

						BigDecimal secondLastFloorBuiltUpArea = secondLastFloor.getOccupancies().stream()
								.map(Occupancy::getBuiltUpArea).reduce(BigDecimal.ZERO, BigDecimal::add);

						if (lastFloorBuiltUpArea.compareTo(secondLastFloorBuiltUpArea) < 0) {
							setReportOutputDetails("ODA Rule 42 (3), Annexure VIII (4-iv)", desc, floor.getNumber().intValue(), "NA",
									providedCount + "", true, scrutinyDetail);
						} else {
							setReportOutputDetails("ODA Rule 42 (3), Annexure VIII (4-iv)", desc, floor.getNumber().intValue(), countRequired + "",
									providedCount + "", isCountAccepted, scrutinyDetail);
						}
					} else {
						setReportOutputDetails("ODA Rule 42 (3), Annexure VIII (4-iv)", desc, floor.getNumber().intValue(), countRequired + "",
								providedCount + "", isCountAccepted, scrutinyDetail);
					}

				} else {
					setReportOutputDetails("ODA Rule 42 (3), Annexure VIII (4-iv)", desc, floor.getNumber().intValue(), countRequired + "",
							providedCount + "", isCountAccepted, scrutinyDetail);
				}	
				
				int count = 1;
				for (Lift lift : lifts) {
					Measurement measurement = lift.getLifts().get(0);

					// width validation
					BigDecimal width = measurement.getHeight().setScale(2, BigDecimal.ROUND_HALF_UP);
					BigDecimal requiredWidth = new BigDecimal("2");
					boolean isWidthAccepted = width.compareTo(requiredWidth) >= 0;
					setReportOutputDetails(SUBRULE_118, "Minimum width of Special lift " + count,
							floor.getNumber().intValue(), requiredWidth + "", width + "", isWidthAccepted,
							scrutinyDetail);

					// depth validation
					BigDecimal depth = measurement.getWidth().setScale(2, BigDecimal.ROUND_HALF_UP);
					BigDecimal requiredDepth = new BigDecimal("1.1");
					boolean isDepthAccepted = depth.compareTo(requiredDepth) >= 0;
					setReportOutputDetails(SUBRULE_118, "Minimum depth of Special lift " + count,
							floor.getNumber().intValue(), requiredDepth + "", depth + "", isDepthAccepted,
							scrutinyDetail);

					count++;
				}

			}
		}

		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	private int getTotalDU(Block block) {
		int count = 0;
		for (Floor floor : block.getBuilding().getFloors()) {
			count = count + floor.getEwsUnit().size() + floor.getLigUnit().size() + floor.getMig1Unit().size()
					+ floor.getMig2Unit().size() + floor.getOthersUnit().size() + floor.getRoomUnit().size();
		}
		return count;
	}

	private void validateCar(Plan pl, Block block, ScrutinyDetail scrutinyDetail) {
		int countRequired = getRequiredCount(pl, block);
		if (countRequired > 0) {
			for (Floor floor : block.getBuilding().getFloors()) {
				List<Lift> lifts = getLifts(floor, COLOR_CAR_LEFT);
				// checkCount
				String desc = "Minimum number of lifts";
				int providedCount = lifts.size();
				boolean isCountAccepted = providedCount >= countRequired;
				if(!isGenralStairAvailableAfterCurrentFloor(floor.getNumber(), block) || isCountAccepted)
					setReportOutputDetails("ODA Rule 42 (6)", desc, floor.getNumber().intValue(), countRequired + "",
						providedCount + "", isCountAccepted, scrutinyDetail);
			}
		}

		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}
	
	private boolean isGenralStairAvailableAfterCurrentFloor(int currentFloor,Block block) {
		boolean flage=true;
			
		for(int i=currentFloor;i<block.getBuilding().getFloors().size();i++) {
			Floor floor=block.getBuilding().getFloorNumber(currentFloor);
			if(floor.getGeneralStairs()==null || floor.getGeneralStairs().size()==0) {
				flage=false;
			}
		}
		
		return flage;
	}

	private int getRequiredCount(Plan pl, Block block) {
		int count = 0;
		boolean isPersent = false;
		for (Floor floor : block.getBuilding().getFloors()) {
			List<Lift> lifts = getLifts(floor, COLOR_CAR_LEFT);
			if (lifts.size() > 0 && count < lifts.size()) {
				count = lifts.size();
				isPersent = true;
			}
		}

		if (isPersent) {
			double totalParking = getRoofTopParking(pl).doubleValue();
			int countPerParking = 0;
			if (totalParking < 2000) {
				countPerParking = 2;
			}
			double addArea = totalParking - 2000;
			if (addArea > 0) {
				if (addArea != 0) {
					countPerParking = countPerParking + (int) (addArea / 1000);
				}
				if ((addArea % 1000) != 0)
					countPerParking++;

			}

			if (count < countPerParking)
				count = countPerParking;

		}

		return count;
	}

//	private BigDecimal getRoofTopParking(Plan pl) {
//		ParkingDetails details = pl.getParkingDetails();
//		BigDecimal totalParking = BigDecimal.ZERO;
//		if (details.getSpecial() != null && !details.getSpecial().isEmpty()) {
//			for (Measurement measurement : details.getSpecial()) {
//				switch (measurement.getColorCode()) {
//				case Parking.COLOR_LAYER_SPECIAL_PARKING_ROOF_TOP_PARKING:
//					totalParking = totalParking.add(measurement.getArea()).setScale(2, BigDecimal.ROUND_HALF_UP);
//					break;
//				}
//			}
//		}
//		return totalParking;
//	}

	private BigDecimal getRoofTopParking(Plan pl) {
		return OdishaUtill.getRoofTopParking(pl);
	}

	private void setReportOutputDetails(String ruleNo, String ruleDesc, int floor, String expected, String actual,
			boolean isAccepted, ScrutinyDetail scrutinyDetail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
		details.put(FLOOR, floor + "");
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, isAccepted ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());
		scrutinyDetail.getDetail().add(details);
	}
	
	private void setReportOutputDetails3(String ruleNo, String ruleDesc, int floor, String expected, String actual,
			String isAccepted, ScrutinyDetail scrutinyDetail, Plan pl) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
		details.put(FLOOR, floor + "");
		if (pl.getPlanInformation().getLiftCountRelaxation() != null
				&& pl.getPlanInformation().getLiftCountRelaxation().equalsIgnoreCase("YES")) {
			details.put(REQUIRED, "NA");
		}else {
			details.put(REQUIRED, expected);
		}
		details.put(PROVIDED, actual);
		details.put(STATUS, isAccepted);
		scrutinyDetail.getDetail().add(details);
	}
	

	private void setReportOutputDetails2(String Desc, String ans, ScrutinyDetail scrutinyDetail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, "ODA rule 42 (2-ii)");
		details.put(DESCRIPTION, Desc);
		details.put(STATUS, ans==null ? "Not Declared":ans);
		scrutinyDetail.getDetail().add(details);
	}
	

	public static List<Lift> getLifts(Floor floor, int colorCode) {
		List<Lift> lifts = new ArrayList<>();

		for (Lift lift : floor.getLifts()) {
			Measurement measurement = lift.getLifts().get(0);
			if (measurement.getColorCode() == colorCode) {
				lifts.add(lift);
			}
		}

		return lifts;
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

}
