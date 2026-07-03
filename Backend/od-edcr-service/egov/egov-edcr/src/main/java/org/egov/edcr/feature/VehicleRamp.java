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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.runtime.parser.VelocityCharStream;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.python.antlr.PythonParser.break_stmt_return;
import org.springframework.stereotype.Service;

@Service
public class VehicleRamp extends FeatureProcess {

	private static final String SUBRULE_40_8 = "ODA Rule 37(10), Annexure IX(4,7-vi)";
	private static final String DESCRIPTION = "Vehicle Ramp";
	private static final String FLOOR = "Floor";
	private static final String LMV = "LMV";
	private static final String LCV = "LCV";
	private static final String HMV = "HMV";
//	private static final String FIRE_TENDER = "FIRE_TENDER";
//	private static final String ONE_WAY_RAMP = "ONE_WAY_RAMP";
//	private static final String TWO_WAY_RAMP = "TWO_WAY_RAMP";
	
	private static final String FIRE_TENDER = "Fire Tender";
	private static final String ONE_WAY_RAMP = "One way ramp";
	private static final String TWO_WAY_RAMP = "Two way ramp";

	private static final Integer[] COLOR_LMV = { 1, 2 };
	private static final Integer[] COLOR_LCV = { 3, 4 };
	private static final Integer[] COLOR_HMV = { 5, 6 };
	private static final Integer[] COLOR_FIRE_TENDER = { 8 };
	private static final Integer[] COLOR_ONE_WAY_RAMP = { 1, 3, 5 };
	private static final Integer[] COLOR_TWO_WAY_RAMP = { 2, 4, 6 };

	private class VehicleRampData {
		String typeOfVehicle;
		String typeOfRamp;
		BigDecimal slope;
		BigDecimal width;
		BigDecimal length;
		Measurement measurements;
	}

	private VehicleRampData getVehicleRampDataFromFloor(org.egov.common.entity.edcr.VehicleRamp vehicleRamp) {
		VehicleRampData rampData = new VehicleRampData();
		rampData.slope = vehicleRamp.getSlope();

		for (Measurement measurement : vehicleRamp.getRamps()) {
			if (Arrays.asList(COLOR_LMV).contains(measurement.getColorCode())) {
				rampData.typeOfVehicle = LMV;
			}
			if (Arrays.asList(COLOR_LCV).contains(measurement.getColorCode())) {
				rampData.typeOfVehicle = LCV;
			}
			if (Arrays.asList(COLOR_HMV).contains(measurement.getColorCode())) {
				rampData.typeOfVehicle = HMV;
			}
			if (Arrays.asList(COLOR_FIRE_TENDER).contains(measurement.getColorCode())) {
				rampData.typeOfVehicle = FIRE_TENDER;
			}
			if (Arrays.asList(COLOR_ONE_WAY_RAMP).contains(measurement.getColorCode())) {
				rampData.typeOfRamp = ONE_WAY_RAMP;
			}
			if (Arrays.asList(COLOR_TWO_WAY_RAMP).contains(measurement.getColorCode())) {
				rampData.typeOfRamp = TWO_WAY_RAMP;
			}
		}
		rampData.measurements = vehicleRamp.getRamps() != null && vehicleRamp.getRamps().size() > 0
				? vehicleRamp.getRamps().get(0)
				: null;

//		rampData.length = vehicleRamp.getRamps().stream().map(Measurement::getHeight).reduce(BigDecimal::max).get()
//				.setScale(2, BigDecimal.ROUND_HALF_UP);
//
//		rampData.width = vehicleRamp.getRamps().stream().map(Measurement::getWidth).reduce(BigDecimal::min).get()
//				.setScale(2, BigDecimal.ROUND_HALF_UP);

		rampData.length = rampData.measurements.getLength().setScale(2, BigDecimal.ROUND_HALF_UP);

		rampData.width = rampData.measurements.getWidth().setScale(2, BigDecimal.ROUND_HALF_UP);

		return rampData;
	}

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		validate(pl);
		if (pl != null && !pl.getBlocks().isEmpty()) {
			for (Block block : pl.getBlocks()) {
				ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
				scrutinyDetail.addColumnHeading(1, RULE_NO);
				scrutinyDetail.addColumnHeading(2, DESCRIPTION);
				scrutinyDetail.addColumnHeading(3, TYPE);
				scrutinyDetail.addColumnHeading(4, FLOOR);
				scrutinyDetail.addColumnHeading(5, REQUIRED);
				scrutinyDetail.addColumnHeading(6, PROVIDED);
				scrutinyDetail.addColumnHeading(7, STATUS);
				scrutinyDetail.setKey("Vehicle Ramp");
				scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "Vehicle Ramp");

				if (block.getBuilding() != null && !block.getBuilding().getFloors().isEmpty()) {
					for (Floor floor : block.getBuilding().getFloors()) {
						List<VehicleRampData> vehicleRampOnFloor = new ArrayList<>();
						for (org.egov.common.entity.edcr.VehicleRamp ramp : floor.getVehicleRamps()) {
							VehicleRampData vehicleRampData = getVehicleRampDataFromFloor(ramp);
							//System.out.println("floor " + floor.getNumber() + " ramp" + floor.getVehicleRamps());
							// validateRampCount(vehicleRampData, floor, scrutinyDetail);
//							validateRampSlop(vehicleRampData, floor, scrutinyDetail);
//							validateRampWidth(vehicleRampData, floor, scrutinyDetail);
//							validateRampLength(vehicleRampData, floor, scrutinyDetail);
							vehicleRampOnFloor.add(vehicleRampData);
						}
						validateRampCount(vehicleRampOnFloor, floor, scrutinyDetail);
						validateRampSlop(vehicleRampOnFloor, floor, scrutinyDetail);
						validateRampWidth(vehicleRampOnFloor, floor, scrutinyDetail);
						validateRampLength(vehicleRampOnFloor, floor, scrutinyDetail);
					}
				}
				pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
			}

		}

		return pl;
	}

	private static final String RAMP_ONE_WAY_COUNT_DESC = "Count of One way Ramp";
	private static final String RAMP_TWO_WAY_COUNT_DESC = "Count of Two way Ramp";

	private void validateRampCount(List<VehicleRampData> vehicleRampOnFloor, Floor floor,
			ScrutinyDetail scrutinyDetail) {
		List<Measurement> oneway = new ArrayList<>();
		List<Measurement> twoway = new ArrayList<>();

		for (VehicleRampData vehicleRampData1 : vehicleRampOnFloor) {
			if (ONE_WAY_RAMP.equals(vehicleRampData1.typeOfRamp))
				oneway.add(vehicleRampData1.measurements);
			if (TWO_WAY_RAMP.equals(vehicleRampData1.typeOfRamp))
				twoway.add(vehicleRampData1.measurements);
		}

		if (oneway.size() > 0) {
			int requiredCount = 2;
			int provided = 0;
			if (oneway != null)
				provided = oneway.size();
			String requiredStr = DxfFileConstants.NA;
			if (requiredCount > 0)
				requiredStr = requiredCount + "";
			if (provided >= requiredCount)
				setReport(SUBRULE_40_8, RAMP_ONE_WAY_COUNT_DESC, ONE_WAY_RAMP, floor.getNumber() + "", "2",
						provided + "", Result.Accepted, scrutinyDetail);
			else
				setReport(SUBRULE_40_8, RAMP_ONE_WAY_COUNT_DESC, ONE_WAY_RAMP, floor.getNumber() + "", "2",
						provided + "", Result.Not_Accepted, scrutinyDetail);
		}

		if (twoway.size() > 0) {
			int requiredCount = 1;
			int provided = 0;
			if (twoway != null)
				provided = twoway.size();
			String requiredStr = DxfFileConstants.NA;
			if (requiredCount > 0)
				requiredStr = requiredCount + "";
			if (provided >= requiredCount)
				setReport(SUBRULE_40_8, RAMP_TWO_WAY_COUNT_DESC, TWO_WAY_RAMP, floor.getNumber() + "", "1",
						provided + "", Result.Accepted, scrutinyDetail);
			else
				setReport(SUBRULE_40_8, RAMP_TWO_WAY_COUNT_DESC, TWO_WAY_RAMP, floor.getNumber() + "", "1",
						provided + "", Result.Not_Accepted, scrutinyDetail);
		}
	}

	private static final String FIRE_TENDER_SLOP_DESC = "Fire tender Slope";
	private static final String RAMP_SLOP_DESC = "Vehicle Ramp Slope";

	private void validateRampSlop(List<VehicleRampData> vehicleRampOnFloor, Floor floor,
			ScrutinyDetail scrutinyDetail) {
		int count = 1;
		for (VehicleRampData vehicleRampData : vehicleRampOnFloor) {
			BigDecimal expectedSlope = BigDecimal.ZERO;
			String desc = "";
			String requireddesc = "";
			if (FIRE_TENDER.equals(vehicleRampData.typeOfVehicle)) {
				desc = "Fire tender " + count + " Slope";
				requireddesc = "1/10";
				expectedSlope = BigDecimal.valueOf(1).divide(BigDecimal.valueOf(10), 2, RoundingMode.HALF_UP);
			} else {
				desc = "Vehicle Ramp " + count + " Slope";
				requireddesc = "1/8";
				expectedSlope = BigDecimal.valueOf(1).divide(BigDecimal.valueOf(8), 2, RoundingMode.HALF_UP);
			}
			String type=getType(vehicleRampData);
			if (vehicleRampData.slope.compareTo(expectedSlope) <= 0) {
				setReport(SUBRULE_40_8, desc, type, floor.getNumber() + "", requireddesc,
						vehicleRampData.slope + "", Result.Accepted, scrutinyDetail);
			} else {
				setReport(SUBRULE_40_8, desc, type, floor.getNumber() + "", requireddesc,
						vehicleRampData.slope + "", Result.Not_Accepted, scrutinyDetail);
			}
			count++;
		}

	}

	private static final String RAMP_WIDTH_DESC = "Ramp width";

	public void validateRampWidth(List<VehicleRampData> vehicleRampOnFloor, Floor floor,
			ScrutinyDetail scrutinyDetail) {
		int count = 1;
		for (VehicleRampData vehicleRampData : vehicleRampOnFloor) {
			BigDecimal expectedWidth = BigDecimal.ZERO;
			String desc = "Ramp " + count + " width";

			if (LMV.equals(vehicleRampData.typeOfVehicle) && ONE_WAY_RAMP.equals(vehicleRampData.typeOfRamp)) {
				expectedWidth = new BigDecimal("3");
			} else if (LMV.equals(vehicleRampData.typeOfVehicle) && TWO_WAY_RAMP.equals(vehicleRampData.typeOfRamp)) {
				expectedWidth = new BigDecimal("6");
			} else if (LCV.equals(vehicleRampData.typeOfVehicle) && ONE_WAY_RAMP.equals(vehicleRampData.typeOfRamp)) {
				expectedWidth = new BigDecimal("4.5");
			} else if (LCV.equals(vehicleRampData.typeOfVehicle) && TWO_WAY_RAMP.equals(vehicleRampData.typeOfRamp)) {
				expectedWidth = new BigDecimal("9");
			} else if (HMV.equals(vehicleRampData.typeOfVehicle) && ONE_WAY_RAMP.equals(vehicleRampData.typeOfRamp)) {
				expectedWidth = new BigDecimal("6");
			} else if (HMV.equals(vehicleRampData.typeOfVehicle) && TWO_WAY_RAMP.equals(vehicleRampData.typeOfRamp)) {
				expectedWidth = new BigDecimal("12");
			} else if (FIRE_TENDER.equals(vehicleRampData.typeOfVehicle)) {
				expectedWidth = new BigDecimal("7.5");
			}
			String type=getType(vehicleRampData);
			if (vehicleRampData.width.compareTo(expectedWidth) >= 0) {
				setReport(SUBRULE_40_8, desc, type, floor.getNumber() + "",
						"Min " + expectedWidth.toString(), vehicleRampData.width + "", Result.Accepted, scrutinyDetail);
			} else {
				setReport(SUBRULE_40_8, desc, type, floor.getNumber() + "",
						"Min " + expectedWidth.toString(), vehicleRampData.width + "", Result.Not_Accepted,
						scrutinyDetail);
			}
			count++;
		}

	}

	private static final String RAMP_LENGTH_DESC = "";

	public void validateRampLength(List<VehicleRampData> vehicleRampOnFloor, Floor floor,
			ScrutinyDetail scrutinyDetail) {
		int count = 1;
		for (VehicleRampData vehicleRampData : vehicleRampOnFloor) {
			BigDecimal expectedLength = new BigDecimal("40");
			String desc = "Ramp " + count + " length";
			String type=getType(vehicleRampData);
			BigDecimal height=BigDecimal.ZERO;
			if(vehicleRampData.measurements!=null)
				height=vehicleRampData.measurements.getHeight();
			height=height.setScale(2, BigDecimal.ROUND_HALF_UP);
			if (height.compareTo(expectedLength) <= 0) {
				setReport(SUBRULE_40_8, desc, type, floor.getNumber() + "",
						"Max " + expectedLength.toString(),height + "", Result.Accepted,
						scrutinyDetail);
			} else {
				setReport(SUBRULE_40_8, desc, type, floor.getNumber() + "",
						"Max " + expectedLength.toString(), height+ "", Result.Not_Accepted,
						scrutinyDetail);
			}
			count++;
		}

	}

	private void setReport(String ruleNo, String description, String type, String floor, String required,
			String provided, Result result, ScrutinyDetail scrutinyDetail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, description);
		details.put(TYPE, type);
		details.put(FLOOR, floor);
		details.put(REQUIRED, required);
		details.put(PROVIDED, provided);
		details.put(STATUS, result.getResultVal());
		scrutinyDetail.getDetail().add(details);
	}

	private boolean isRequired(Plan pl, Block b, Floor floor) {
		boolean flage = false;

		if (floor.getNumber() < 0) {
			BigDecimal coverParkingArea = BigDecimal.ZERO;
			BigDecimal basementParkingArea = BigDecimal.ZERO;
			coverParkingArea = coverParkingArea.add(floor.getParking().getCoverCars().stream().map(Measurement::getArea)
					.reduce(BigDecimal.ZERO, BigDecimal::add));
			basementParkingArea = basementParkingArea.add(floor.getParking().getBasementCars().stream()
					.map(Measurement::getArea).reduce(BigDecimal.ZERO, BigDecimal::add));
			if (basementParkingArea.compareTo(BigDecimal.ZERO) > 0)
				flage = true;
		} else {
			flage = isAboveFloorStilt(pl, b, floor);
		}

		return flage;
	}

	private boolean isAboveFloorStilt(Plan pl, Block b, Floor floor) {
		boolean flage = false;
		for (Floor f : b.getBuilding().getFloors()) {
			if (f.getNumber() > floor.getNumber() && f.getIsStiltFloor()) {
				flage = true;
				break;
			}
		}
		return flage;
	}

	private void validateDimensions(Plan plan, String blockNo, int floorNo, String rampNo,
			List<Measurement> rampPolylines) {
		int count = 0;
		for (Measurement m : rampPolylines) {
			if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0) {
				count++;
			}
		}
		if (count > 0) {
			plan.addError(String.format(DxfFileConstants.LAYER_VEHICLE_RAMP_WITH_NO, blockNo, floorNo, rampNo),
					count + " number of vehicle ramp polyline not having only 4 points in layer "
							+ String.format(DxfFileConstants.LAYER_VEHICLE_RAMP_WITH_NO, blockNo, floorNo, rampNo));

		}
	}

	private String getType(VehicleRampData vehicleRampData) {
		String type=vehicleRampData.typeOfVehicle;
		if(type!=null && !type.trim().isEmpty() && vehicleRampData.typeOfRamp!=null && !vehicleRampData.typeOfRamp.trim().isEmpty()) {
			type=type+" , ";
		}
		if(vehicleRampData.typeOfRamp!=null && !vehicleRampData.typeOfRamp.trim().isEmpty())
		type=type+vehicleRampData.typeOfRamp;
		return type;
	}
	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}