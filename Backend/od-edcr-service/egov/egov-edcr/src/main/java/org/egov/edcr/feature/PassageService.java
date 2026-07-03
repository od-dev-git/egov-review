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

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.stereotype.Service;


@Service
public class PassageService extends FeatureProcess {
		private static final String RULE41 = "41";
		private static final String RULE39_6 = "39(6)";
		private static final String PASSAGE_STAIR_MINIMUM_WIDTH = "1.2";
		private static final String RULE39_6_DESCRIPTION = "The minimum width of Passage";
		private static final String RULE_41_DESCRIPTION = "The minimum width of Passage";
		private static final String RULE_42_DESCRIPTION = "The minimum height of Passage";
		
	@Override
	public Plan validate(Plan plan) {
		return plan;
	}

	@Override
	public Plan process(Plan plan) {
		OccupancyTypeHelper typeHelper=plan.getVirtualBuilding().getMostRestrictiveFarHelper();
		for (Block block : plan.getBlocks()) {
			if (block.getBuilding() != null) {

				ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
				scrutinyDetail.addColumnHeading(1, RULE_NO);
				scrutinyDetail.addColumnHeading(2, REQUIRED);
				scrutinyDetail.addColumnHeading(3, PROVIDED);
				scrutinyDetail.addColumnHeading(4, STATUS);
				scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "Minimum Passage Width");

				ScrutinyDetail scrutinyDetail1 = new ScrutinyDetail();
				scrutinyDetail1.addColumnHeading(1, RULE_NO);
				scrutinyDetail1.addColumnHeading(2, REQUIRED);
				scrutinyDetail1.addColumnHeading(3, PROVIDED);
				scrutinyDetail1.addColumnHeading(4, STATUS);
				scrutinyDetail1.setKey("Block_" + block.getNumber() + "_" + "Minimum Passage Width (Double Loaded)");
				
				
				ScrutinyDetail scrutinyDetail2 = new ScrutinyDetail();
				scrutinyDetail2.addColumnHeading(1, RULE_NO);
				scrutinyDetail2.addColumnHeading(2, REQUIRED);
				scrutinyDetail2.addColumnHeading(3, PROVIDED);
				scrutinyDetail2.addColumnHeading(4, STATUS);
				scrutinyDetail2.setKey("Block_" + block.getNumber() + "_" + "Minimum Passage Height");

				org.egov.common.entity.edcr.Passage passage = block.getBuilding().getPassage();

				if (passage != null) {

					
					List<BigDecimal> passageWidths=passage.getPassageDimensions();

					if (passageWidths != null && passageWidths.size() > 0) {

						BigDecimal minPassagePolyLine = 
								passageWidths.stream().reduce(BigDecimal::min).get();

						BigDecimal minWidth =minPassagePolyLine;
						BigDecimal expectedWidth=BigDecimal.ZERO;
						if(DxfFileConstants.OC_RESIDENTIAL.equals(typeHelper.getType().getCode()))
							expectedWidth=new BigDecimal("1");
						else
							expectedWidth=new BigDecimal("1.5");
						
						if (minWidth.compareTo(expectedWidth) >= 0) {
							setReportOutputDetails(plan, "ODA Annexure IX (5-i)", RULE_41_DESCRIPTION,
									expectedWidth.toString(), minWidth.toString(), Result.Accepted.getResultVal(),
									scrutinyDetail);
						} else {
							setReportOutputDetails(plan, "ODA Annexure IX (5-i)", RULE_41_DESCRIPTION,
									expectedWidth.toString(), minWidth.toString(), Result.Not_Accepted.getResultVal(),
									scrutinyDetail);
						}
					}
					
					List<BigDecimal> passageDoubleLoadedWidth=passage.getPassageStairDimensions();

					if (passageDoubleLoadedWidth != null && passageDoubleLoadedWidth.size() > 0) {

						BigDecimal minPassageStairPolyLine = passageDoubleLoadedWidth.stream().reduce(BigDecimal::min).get();

						BigDecimal minWidth =minPassageStairPolyLine;
						BigDecimal expectedMinWidth=new BigDecimal("1.8");
						
						if (minWidth.compareTo(expectedMinWidth) >= 0) {
							setReportOutputDetails(plan, "ODA Annexure IX (5-i)", RULE39_6_DESCRIPTION,
									expectedMinWidth.toString(), minWidth.toString(), Result.Accepted.getResultVal(),
									scrutinyDetail1);
						} else {
							setReportOutputDetails(plan, "ODA Annexure IX (5-i)", RULE39_6_DESCRIPTION,
									expectedMinWidth.toString(), minWidth.toString(), Result.Not_Accepted.getResultVal(),
									scrutinyDetail1);
						}
					}
					
					List<BigDecimal> passageHeights=passage.getPassageHeight();
					if (passageHeights != null && passageHeights.size() > 0) {

						BigDecimal minPassageHegightPolyLine = passageHeights.stream().reduce(BigDecimal::min).get();

						BigDecimal minHeight =minPassageHegightPolyLine;
						BigDecimal expectedMinHeight=new BigDecimal("2.4");
						
						
						if (minHeight.compareTo(expectedMinHeight) >= 0) {
							setReportOutputDetails(plan, "ODA Annexure IX (5-iv)", RULE_42_DESCRIPTION,
									expectedMinHeight.toString(), minHeight.toString(), Result.Accepted.getResultVal(),
									scrutinyDetail2);
						} else {
							setReportOutputDetails(plan, "ODA Annexure IX (5-iv)", RULE_42_DESCRIPTION,
									expectedMinHeight.toString(), minHeight.toString(), Result.Not_Accepted.getResultVal(),
									scrutinyDetail2);
						}
					}

				}
			}
		}
		return plan;
	}

	private void setReportOutputDetails(Plan pl, String ruleNo, String ruleDesc, String expected, String actual,
			String status, ScrutinyDetail scrutinyDetail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
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