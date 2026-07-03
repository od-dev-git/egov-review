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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.egov.edcr.utility.DcrConstants;
import org.python.antlr.base.boolop;
import org.springframework.stereotype.Service;

@Service
public class FireTenderMovement extends FeatureProcess {
	private static final Logger LOG = Logger.getLogger(FireTenderMovement.class);
	private static final BigDecimal FIFTEEN = BigDecimal.valueOf(15);
	private static final BigDecimal THREE_POINTSIXSIX = BigDecimal.valueOf(3.66);
	private static final String RULE_36_3 = "ODA Rule 37 (8)";

	@Override
	public Plan validate(Plan plan) {
		return plan;
	}

	@Override
	public Plan process(Plan plan) {
		HashMap<String, String> errors = new HashMap<>();
		boolean isMandatory = isVehicularAccessMandatory(plan);
		String serviceType = plan.getPlanInformation().getServiceType();
		for (Block block : plan.getBlocks()) {

			ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
			scrutinyDetail.addColumnHeading(1, RULE_NO);
			scrutinyDetail.addColumnHeading(2, DESCRIPTION);
			scrutinyDetail.addColumnHeading(3, PERMISSIBLE);
			scrutinyDetail.addColumnHeading(4, PROVIDED);
			scrutinyDetail.addColumnHeading(5, STATUS);
			scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "Vehicular access within Site");

			if (block.getBuilding() != null) {
				org.egov.common.entity.edcr.FireTenderMovement fireTenderMovement = block.getFireTenderMovement();
				if (fireTenderMovement != null) {
					List<BigDecimal> widths = fireTenderMovement.getFireTenderMovements().stream()
							.map(fireTenderMovmnt -> fireTenderMovmnt.getWidth()).collect(Collectors.toList());
					BigDecimal minWidth = widths.stream().reduce(BigDecimal::min).get();
					BigDecimal providedWidth = minWidth.setScale(DcrConstants.DECIMALDIGITS_MEASUREMENTS,
							DcrConstants.ROUNDMODE_MEASUREMENTS);
					BigDecimal requiredWidth = getRequiredWidth(plan, block);
					Boolean isAccepted = providedWidth.compareTo(requiredWidth) >= 0;

					Map<String, String> details = new HashMap<>();
					details.put(RULE_NO, RULE_36_3);
					details.put(DESCRIPTION, "Width of vehicular access within Site");
					details.put(PERMISSIBLE, ">= " + requiredWidth.toString());
					details.put(PROVIDED, providedWidth.toString());
					String status = Result.Not_Accepted.getResultVal();
					if(DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType)) {
						status = isAccepted ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal();
					}else {
						status = isAccepted ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal();
					}
					details.put(STATUS,
							status);
					scrutinyDetail.getDetail().add(details);
					plan.getReportOutput().getScrutinyDetails().add(scrutinyDetail);

//                    if (!fireTenderMovement.getErrors().isEmpty()) {
//                        StringBuffer yardNames = new StringBuffer();
//
//                        for (String yardName : fireTenderMovement.getErrors()) {
//                            yardNames = yardNames.append(yardName).append(", ");
//                        }
//                        errors.put("FTM_SETBACK", "Fire tender movement for block " + block.getNumber() + " is not inside "
//                                + yardNames.toString().substring(0, yardNames.length() - 2) + ".");
//                        plan.addErrors(errors);
//                    }
				} else if (isMandatory) {
					errors.put("BLK_FTM_" + block.getNumber(),
							"Vehicular access within Site not defined for Block " + block.getNumber());
					plan.addErrors(errors);
				}
			}
		}

		return plan;
	}

	private boolean isVehicularAccessMandatory(Plan pl) {
		long blockCount = pl.getBlocks().stream().filter(block -> !block.isOutHouse())
				.filter(block -> !block.isPublicWashroom()).count();
		if (blockCount > 1)
			return true;
		else
			return false;
	}

	private boolean isMandatory(Plan pl, Block block) {
		boolean flage = false;
		BigDecimal openParking = OdishaUtill.getOpenParking(pl);
		if (openParking.compareTo(BigDecimal.ZERO) > 0)
			flage = true;

		if (DxfFileConstants.YES.equals(pl.getPlanInfoProperties().get(
				DxfFileConstants.IS_DRIVEWAY_PROVIDING_ACCESS_TO_REAR_SIDE_OR_ANY_OTHER_SIDE_OTHER_THAN_FRONT_OF_THE_BUILDING)))
			flage = true;

		return flage;
	}

	private static final int ONE_SIDE_PARKING_COLOR = 1;
	private static final int TWO_SIDE_PARKING_COLOR = 2;

	private BigDecimal getRequiredWidth(Plan pl, Block block) {
		BigDecimal required = BigDecimal.ZERO;
		if (OdishaUtill.isEWSOrLIGBlock(pl, block)) {
			org.egov.common.entity.edcr.FireTenderMovement fireTenderMovement = block.getFireTenderMovement();
			for (Measurement measurement : fireTenderMovement.getFireTenderMovements()) {
				if (measurement.getColorCode() == ONE_SIDE_PARKING_COLOR)
					required = new BigDecimal("1.2");
				if (measurement.getColorCode() == TWO_SIDE_PARKING_COLOR)
					required = new BigDecimal("1.5");
			}
		} else {
			BigDecimal buildingHeight = BigDecimal.ZERO;
			buildingHeight = block.getBuilding().getBuildingHeight();
			if (buildingHeight.compareTo(new BigDecimal("15")) < 0) {
				required = new BigDecimal("6");
			} else if (buildingHeight.compareTo(new BigDecimal("15")) >= 0
					&& buildingHeight.compareTo(new BigDecimal("18")) <= 0) {
				required = new BigDecimal("4.5");
			} else if (buildingHeight.compareTo(new BigDecimal("18")) > 0
					&& buildingHeight.compareTo(new BigDecimal("40")) <= 0) {
				required = new BigDecimal("6");
			} else {
				required = new BigDecimal("7.5");
			}
		}
		return required;
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

}
