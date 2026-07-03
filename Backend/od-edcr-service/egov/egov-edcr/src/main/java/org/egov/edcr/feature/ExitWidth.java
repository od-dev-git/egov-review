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

import static org.egov.edcr.constants.DxfFileConstants.A_R;
import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.egov.common.entity.dcr.helper.OccupancyHelperDetail;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.service.ProcessHelper;
import org.egov.edcr.utility.DcrConstants;
import org.egov.infra.utils.StringUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ExitWidth extends FeatureProcess {

	private static final String EXIT_WIDTH_DESC = "Exit Width";
	// private static final String SUB_RULE_DESCRIPTION = "Minimum exit width";
	public static final BigDecimal VAL_0_75 = BigDecimal.valueOf(0.75);
	public static final BigDecimal VAL_1_2 = BigDecimal.valueOf(1.2);
	private static final String SUBRULE_42_3 = "ODA Rule 72 (9)";
	// private static final String SUB_RULE_OCCUPANTS_DESCRIPTION = "Maximum number
	// of occupants that can be allowed through";
	private static final String OCCUPANCY = "Occupancy";
	private static final String EXIT_WIDTH = "Exit Width";
	private static final String FLOOR = "Floor";
	private static final BigDecimal FIFTEEN = new BigDecimal("15");

	private Plan validateExitWidth(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {

		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		for (Block block : pl.getBlocks()) {

			ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
			scrutinyDetail.addColumnHeading(1, RULE_NO);
			scrutinyDetail.addColumnHeading(2, DESCRIPTION);
			scrutinyDetail.addColumnHeading(3, FLOOR);
			scrutinyDetail.addColumnHeading(4, REQUIRED);
			scrutinyDetail.addColumnHeading(5, PROVIDED);
			scrutinyDetail.addColumnHeading(6, STATUS);
			scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "Exit Width Staircase");

			BigDecimal noOfFloorsAboveGround = block.getBuilding().getFloorsAboveGround();
			BigDecimal numberOfOccupantsOrUsersOrBedBlk = block.getNumberOfOccupantsOrUsersOrBedBlk();
			if (numberOfOccupantsOrUsersOrBedBlk != null) {
				BigDecimal noOfUserPerFloor = BigDecimal.ZERO;
				if (numberOfOccupantsOrUsersOrBedBlk != null
						&& numberOfOccupantsOrUsersOrBedBlk.compareTo(BigDecimal.ZERO) > 0)
					noOfUserPerFloor = numberOfOccupantsOrUsersOrBedBlk.divide(noOfFloorsAboveGround, 2,
							BigDecimal.ROUND_HALF_UP);
				for (Floor floor : block.getBuilding().getFloors()) {
					if (floor.getNumber() >= 0) {

						List<BigDecimal> exitWidthStair = floor.getExitWidthStair();
						// validate count
						boolean isExistCountAccepted = false;
						int countExcepted = 0;
						if (FIFTEEN.compareTo(block.getBuilding().getBuildingHeight()) <= 0) {
							countExcepted = 2;
						}
						if (exitWidthStair.size() >= countExcepted)
							isExistCountAccepted = true;
						
						setReportOutputDetailsWithoutOccupancy(pl, SUBRULE_42_3, "Staricase exit width count", floor,
								countExcepted == 0 ? DxfFileConstants.NA : countExcepted + "", exitWidthStair.size() + "",
								isExistCountAccepted, scrutinyDetail);

						if (exitWidthStair.size() > 0) {

							// Unit Exit Width Of Staircase
							BigDecimal providedMinWidth = exitWidthStair.stream().reduce(BigDecimal::min).get();
							BigDecimal requiredMinWidth = new BigDecimal("1");
							boolean isExistWidthUnitAccepted = false;

							if (providedMinWidth.compareTo(requiredMinWidth) >= 0)
								isExistWidthUnitAccepted = true;

							setReportOutputDetailsWithoutOccupancy(pl, SUBRULE_42_3, "Unit Exit Width Of Staircase",
									floor, requiredMinWidth.toString(), providedMinWidth.toString(),
									isExistWidthUnitAccepted, scrutinyDetail);

							// Total Exit Width Of Staircase
							BigDecimal providedExitWidth = exitWidthStair.stream().reduce(BigDecimal::add).get();
							BigDecimal requiredExitWidth = BigDecimal.ZERO;
							boolean isExitWidthAccepted = false;

							BigDecimal divisor = BigDecimal.ZERO;
							if (DxfFileConstants.OC_RESIDENTIAL.equals(occupancyTypeHelper.getType().getCode())) {
								divisor = new BigDecimal("25");// 0.5
							} else {
								divisor = new BigDecimal("50");// 0.5
							}
							int multiplicand = 0;
							if (noOfUserPerFloor.remainder(divisor).compareTo(BigDecimal.ZERO) == 0)
								multiplicand = noOfUserPerFloor.divide(divisor).intValue();
							else {
								multiplicand = noOfUserPerFloor.divide(divisor).intValue();
								multiplicand++;
							}

							requiredExitWidth = new BigDecimal(multiplicand * 0.5);
							requiredExitWidth = requiredExitWidth.setScale(DECIMALDIGITS_MEASUREMENTS,
									ROUNDMODE_MEASUREMENTS);

							if (providedExitWidth.compareTo(requiredExitWidth) >= 0)
								isExitWidthAccepted = true;

							setReportOutputDetailsWithoutOccupancy(pl, SUBRULE_42_3, "Total Exit Width Of Staircase",
									floor, requiredExitWidth.toString(), providedExitWidth.toString(),
									isExitWidthAccepted, scrutinyDetail);
						}

					}
				}
				pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
			}

		}
		return pl;
	}

	private BigDecimal getOccupantLoadOfAFloor(Occupancy occupancy, BigDecimal occupantLoadDivisonFactor) {
		return BigDecimal.valueOf(Math.ceil(occupancy.getBuiltUpArea()
				.divide(occupantLoadDivisonFactor, DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).doubleValue()));
	}

	private void setReportOutputDetailsWithoutOccupancy(Plan pl, String ruleNo, String description, Floor floor,
			String expected, String actual, boolean status, ScrutinyDetail scrutinyDetail2) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, description);
		details.put(FLOOR, floor.getNumber() + "");
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());
		scrutinyDetail2.getDetail().add(details);
	}

	private String removeDuplicates(SortedSet<String> uniqueData) {
		StringBuilder str = new StringBuilder();
		List<String> unqList = new ArrayList<>(uniqueData);
		for (String unique : unqList) {
			str.append(unique);
			if (!unique.equals(unqList.get(unqList.size() - 1))) {
				str.append(" , ");
			}
		}
		return str.toString();
	}

	@Override
	public Plan validate(Plan pl) {
		validateExitWidth(pl);
		return pl;
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}