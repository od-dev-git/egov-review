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

import static org.egov.edcr.utility.DcrConstants.OBJECTDEFINED_DESC;
import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED;
import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED_DESC;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.common.entity.edcr.LiquidWasteTreatementPlant;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.SupplyLine;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class WaterTreatmentPlant extends FeatureProcess {
	private static final String SUB_RULE_53_5_DESCRIPTION = "Water Treatment Plant";
	private static final String SUB_RULE_53_5 = "ODA Rule 49, 51(1)";
	private static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10000);
	private static final int COLOR_WATER_TREATMENT_PLANT=1;
	private static final int COLOR_WASTE_WATER_RECYCLING_AND_REUSE=2;

	@Override
	public Plan validate(Plan pl) {
		
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
	
//		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
//		scrutinyDetail.addColumnHeading(1, RULE_NO);
//		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
//		scrutinyDetail.addColumnHeading(3, REQUIRED);
//		scrutinyDetail.addColumnHeading(4, PROVIDED);
//		scrutinyDetail.addColumnHeading(5, STATUS);
//		scrutinyDetail.setKey("Common_Water Treatment Plant");
//		processWaterTreatmentPlant(pl,scrutinyDetail);
//		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		
		ScrutinyDetail scrutinyDetail1 = new ScrutinyDetail();
		scrutinyDetail1.addColumnHeading(1, RULE_NO);
		scrutinyDetail1.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail1.addColumnHeading(3, REQUIRED);
		scrutinyDetail1.addColumnHeading(4, PROVIDED);
		scrutinyDetail1.addColumnHeading(5, STATUS);
		scrutinyDetail1.setKey("Common_Waste Water Recycling And Reuse");
		processWasteWaterRecyclingAndReus(pl, scrutinyDetail1);
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail1);
		
		return pl;
	}

	private void processWaterTreatmentPlant(Plan pl,ScrutinyDetail scrutinyDetail) {
		String subRule = SUB_RULE_53_5;
		String subRuleDesc = SUB_RULE_53_5_DESCRIPTION;
		BigDecimal expectedTankCapacity = BigDecimal.ZERO;
		BigDecimal plotArea = pl.getPlot() != null ? pl.getPlot().getArea() : BigDecimal.ZERO;
		OccupancyTypeHelper mostRestrictiveFarHelper = pl.getVirtualBuilding() != null
				? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
				: null;
		boolean isWaterTreatmentPlantRequired=false;
		
		List<LiquidWasteTreatementPlant> liquidWasteTreatementPlants=getLiquidWasteTreatementPlantByColorCode(pl.getUtility().getLiquidWasteTreatementPlant(), COLOR_WATER_TREATMENT_PLANT);
		
		if (plotArea.compareTo(new BigDecimal("115")) >0 && (DxfFileConstants.OC_COMMERCIAL.equals(mostRestrictiveFarHelper.getType().getCode())
				|| DxfFileConstants.OC_PUBLIC_UTILITY.equals(mostRestrictiveFarHelper.getType().getCode())
				|| DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(mostRestrictiveFarHelper.getType().getCode())
				|| DxfFileConstants.OC_EDUCATION.equals(mostRestrictiveFarHelper.getType().getCode())
				|| DxfFileConstants.OC_TRANSPORTATION.equals(mostRestrictiveFarHelper.getType().getCode())
				|| DxfFileConstants.OC_MIXED_USE.equals(mostRestrictiveFarHelper.getType().getCode())
				)) {
			isWaterTreatmentPlantRequired=true;
		}
		
		//Rain Water Harvesting
		
		if (!isWaterTreatmentPlantRequired) {

			if (liquidWasteTreatementPlants != null && liquidWasteTreatementPlants.size()>0) {
				setReportOutputDetails(scrutinyDetail, subRule, subRuleDesc, DxfFileConstants.OPTIONAL,
						DxfFileConstants.PROVIDED,
						Result.Verify.getResultVal());
			} else {
				setReportOutputDetails(scrutinyDetail, subRule, subRuleDesc, DxfFileConstants.OPTIONAL, "Not Defined in the plan",
						Result.Verify.getResultVal());
			}

		} else  if(isWaterTreatmentPlantRequired){
			if ((liquidWasteTreatementPlants!=null)&&(liquidWasteTreatementPlants.size() > 0))
				setReportOutputDetails(scrutinyDetail, subRule, subRuleDesc, "Mandatory",
						DxfFileConstants.PROVIDED,
						Result.Verify.getResultVal());
			else
				setReportOutputDetails(scrutinyDetail, subRule, subRuleDesc, "Mandatory", "Not Defined in the plan",
						Result.Not_Accepted.getResultVal());
		}
		

	}

	private void processWasteWaterRecyclingAndReus(Plan pl,ScrutinyDetail scrutinyDetail) {
		String subRule = SUB_RULE_53_5;
		String subRuleDesc = "Waste water recycling system for horticultural purposes";
		BigDecimal plotArea = pl.getPlot() != null ? pl.getPlot().getArea() : BigDecimal.ZERO;
	
		boolean isWasteWaterRecyclingAndReusRequired=false;
		
		List<LiquidWasteTreatementPlant> liquidWasteTreatementPlants=getLiquidWasteTreatementPlantByColorCode(pl.getUtility().getLiquidWasteTreatementPlant(), COLOR_WASTE_WATER_RECYCLING_AND_REUSE);
		
		if (plotArea.compareTo(new BigDecimal("500")) >0 && DxfFileConstants.YES.equals(pl.getPlanInformation().getWasteWaterDischargePerDay())) {
			isWasteWaterRecyclingAndReusRequired=true;
		}
		
		if (!isWasteWaterRecyclingAndReusRequired) {

			if (liquidWasteTreatementPlants != null && liquidWasteTreatementPlants.size()>0) {
				setReportOutputDetails(scrutinyDetail, subRule, subRuleDesc, DxfFileConstants.OPTIONAL,
						DxfFileConstants.PROVIDED,
						Result.Accepted.getResultVal());
			} else {
				setReportOutputDetails(scrutinyDetail, subRule, subRuleDesc, DxfFileConstants.OPTIONAL, "Not Defined in the plan",
						Result.Accepted.getResultVal());
			}

		} else  if(isWasteWaterRecyclingAndReusRequired){
			if ((liquidWasteTreatementPlants!=null)&&(liquidWasteTreatementPlants.size() > 0))
				setReportOutputDetails(scrutinyDetail, subRule, subRuleDesc, DxfFileConstants.MANDATORY,
						DxfFileConstants.PROVIDED,
						Result.Accepted.getResultVal());
			else
				setReportOutputDetails(scrutinyDetail, subRule, subRuleDesc, DxfFileConstants.MANDATORY, "Not Defined in the plan",
						Result.Not_Accepted.getResultVal());
		}
		

	}
	
	private void setReportOutputDetails(ScrutinyDetail scrutinyDetail, String ruleNo, String ruleDesc, String expected, String actual,
			String status) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status);
		scrutinyDetail.getDetail().add(details);
		//pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	private List<LiquidWasteTreatementPlant> getLiquidWasteTreatementPlantByColorCode(
			List<LiquidWasteTreatementPlant> wasteTreatementPlants, int colorCode) {

		List<LiquidWasteTreatementPlant> liquidWasteTreatementPlants = wasteTreatementPlants.stream()
				.filter(measurement -> measurement.getColorCode() == colorCode).collect(Collectors.toList());

		return liquidWasteTreatementPlants;
	}

	private void setReportOutputDetailsWithoutOccupancy(Plan pl, String ruleNo, String ruleDesc, String expected,
			String actual, String status) {
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
