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
import java.util.Map;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.stereotype.Service;

@Service
public class ApprovedConstruction extends FeatureProcess {
	private static final String APPROVED_ERROR_MSG = "Approved Area is not equal to existing area in plan. Unauthorised area found in drawing, kindly regularise the area before applying for building permit - addition and alteration service.";
	private static final String SUBRULE_88_1 = "88-1";
	private static final String DEMOLITION_ERROR_MSG = "Demolition area in plan info should be equal to total demolition polygon area.";
	private static final String TOTAL_BUILD_UP_AREA = "Total Built Up Area";
	private static final String DEMOLITION_AREA = "Demolition Area";
	private static final String BUILT_UP_AREA_AFTER_DEMOLITION = "Built Up Area After Demolition";

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	private BigDecimal getexistingBuiltUpArea(Floor f) {
		BigDecimal existingBuiltUpArea = BigDecimal.ZERO;

		for (Occupancy oc : f.getOccupancies()) {
			existingBuiltUpArea = existingBuiltUpArea.add(oc.getExistingBuiltUpArea());
		}
		existingBuiltUpArea = existingBuiltUpArea.setScale(2, BigDecimal.ROUND_HALF_UP);
		return existingBuiltUpArea;
	}

	@Override
	public Plan process(Plan pl) {
		processDemolition(pl);

		String serviceType = pl.getPlanInformation().getServiceType();
		if (!DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType))
			return pl;

		ScrutinyDetail scrutinyDetail3 = new ScrutinyDetail();
		scrutinyDetail3.setKey("Common_Approved Area Detail");
		scrutinyDetail3.addColumnHeading(1, RULE_NO);
		scrutinyDetail3.addColumnHeading(2, BLOCK);
		scrutinyDetail3.addColumnHeading(3, FLOOR);
		scrutinyDetail3.addColumnHeading(4, EXISTING_AREA);
		scrutinyDetail3.addColumnHeading(5, APPROVED_AREA);
		scrutinyDetail3.addColumnHeading(6, STATUS);
		String errorMsg = null;
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				BigDecimal existingBuiltUpArea = getexistingBuiltUpArea(floor);
				BigDecimal approvedConstructionArea = floor.getApprovedConstruction().stream().map(app -> app.getArea())
						.reduce(BigDecimal::add).orElse(BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
				if (existingBuiltUpArea.compareTo(BigDecimal.ZERO) > 0
						|| approvedConstructionArea.compareTo(BigDecimal.ZERO) > 0) {
					boolean isAccepted = existingBuiltUpArea.compareTo(approvedConstructionArea) <= 0 ? true : false;
					setReportOutputDetails("ODA Rule 4 (3)", block.getName(), floor.getNumber().toString(),
							existingBuiltUpArea.toString(), approvedConstructionArea.toString(), isAccepted,
							scrutinyDetail3);
					if (!isAccepted) {
						errorMsg = APPROVED_ERROR_MSG;
					}
				}
			}
		}
		if (errorMsg != null) {
			pl.addError("ApprovedRejected", errorMsg);
		}
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail3);
		return pl;
	}

	private Plan processDemolition(Plan pl) {
		String serviceType = pl.getPlanInformation().getServiceType();
		if (!DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType))
			return pl;
		ScrutinyDetail scrutinyDetail3 = new ScrutinyDetail();
		scrutinyDetail3.setKey("Common_Demolition Area Detail");
		scrutinyDetail3.addColumnHeading(1, RULE_NO);
		scrutinyDetail3.addColumnHeading(2, BLOCK);
		scrutinyDetail3.addColumnHeading(3, FLOOR);
		scrutinyDetail3.addColumnHeading(4, TOTAL_BUILD_UP_AREA);
		scrutinyDetail3.addColumnHeading(5, DEMOLITION_AREA);
		scrutinyDetail3.addColumnHeading(6, BUILT_UP_AREA_AFTER_DEMOLITION);

		BigDecimal totalDemolitionArea = BigDecimal.ZERO;
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {

				BigDecimal existingBuiltUpArea = floor.getOccupancies().stream()
						.map(occ -> occ.getExistingBuiltUpArea()).reduce(BigDecimal::add).orElse(BigDecimal.ZERO)
						.setScale(2, BigDecimal.ROUND_HALF_UP);
				BigDecimal demolitionArea = floor.getDemolitionArea().stream().map(m -> m.getArea())
						.reduce(BigDecimal::add).orElse(BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
				totalDemolitionArea = totalDemolitionArea.add(demolitionArea);

				BigDecimal totalBuildUpArea = existingBuiltUpArea.add(demolitionArea).setScale(2,
						BigDecimal.ROUND_HALF_UP);

				if (demolitionArea.compareTo(BigDecimal.ZERO) > 0) {
					setReportOutputDetails("ODA Rule 22", block.getName(), floor.getNumber()+"", totalBuildUpArea.toString(), demolitionArea.toString(), existingBuiltUpArea.toString(), scrutinyDetail3);
				}

			}
		}
		
		if(totalDemolitionArea.compareTo(pl.getPlanInformation().getDemolitionArea()) != 0) {
			pl.addError("totalDemolitionAreaError", DEMOLITION_ERROR_MSG);
		}
		
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail3);
		return pl;
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

	private void setReportOutputDetails(String ruleNo, String block, String floor, String existingArea,
			String approvedArea, boolean isAccepted, ScrutinyDetail scrutinyDetail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(BLOCK, block);
		details.put(FLOOR, floor);
		details.put(EXISTING_AREA, existingArea);
		details.put(APPROVED_AREA, approvedArea);
		details.put(STATUS, isAccepted ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());
		scrutinyDetail.getDetail().add(details);
	}

	private void setReportOutputDetails(String ruleNo, String block, String floor, String totalBuildUpArea,
			String demolitionArea, String buildUpAreaAfterDemolition, ScrutinyDetail scrutinyDetail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(BLOCK, block);
		details.put(FLOOR, floor);
		details.put(TOTAL_BUILD_UP_AREA, totalBuildUpArea);
		details.put(DEMOLITION_AREA, demolitionArea);
		details.put(BUILT_UP_AREA_AFTER_DEMOLITION, buildUpAreaAfterDemolition);
		scrutinyDetail.getDetail().add(details);
	}
}
