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

import static org.egov.edcr.constants.DxfFileConstants.MINIMUM_GENERATION_CAPACITY_OF_THE_ROOFTOP_SOLAR_PV_SYSTEM_IN_W;
import static org.egov.edcr.utility.DcrConstants.OBJECTDEFINED_DESC;
import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED;
import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED_DESC;
import static org.egov.edcr.utility.DcrConstants.RULE109;
import static org.egov.edcr.utility.DcrConstants.SOLAR_SYSTEM;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.OccupancyType;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.egov.edcr.service.PlanService;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class Solar extends FeatureProcess {
	private static final String SUB_RULE_109_C_DESCRIPTION = "Solar Assisted Water Heating / Lighting system ";
	private static final String SUB_RULE_109_C = "109-C";
	private static final BigDecimal FOURHUNDRED = BigDecimal.valueOf(400);
	private static final Logger LOG = Logger.getLogger(FeatureProcess.class);

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		scrutinyDetail.setKey("Common_Solar PV System");
		String rule = RULE109;
		String subRule = SUB_RULE_109_C;
		String subRuleDesc = SUB_RULE_109_C_DESCRIPTION;
		String desc1 = "Total connected load of the proposed project in W";
		String desc3 = "Solar PV System";
		String desc2 = "Minimum generation capacity of the Rooftop Solar PV system in W";

		OccupancyTypeHelper typeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		BigDecimal plotArea = pl.getPlot().getArea();
		boolean isApplicable = isRequired(typeHelper, plotArea);

		BigDecimal totalRoofTopArea = OdishaUtill.getTotalRoofArea(pl);
		BigDecimal totalConnectedLoadOfTheProposedProjectInW = pl.getPlanInformation()
				.getTotalConnectedLoadOfTheProposedProjectInW();
		BigDecimal required = BigDecimal.ZERO;
		try {
			BigDecimal FivePOfTCLOTPP = totalConnectedLoadOfTheProposedProjectInW.multiply(new BigDecimal("0.05"))
					.setScale(2, BigDecimal.ROUND_HALF_UP);
			BigDecimal totalRequiredPerRoofTopAreaInFeet = totalRoofTopArea.multiply(new BigDecimal("10.764"))
					.multiply(new BigDecimal("20")).setScale(2, BigDecimal.ROUND_HALF_UP);

			if (FivePOfTCLOTPP.compareTo(totalRequiredPerRoofTopAreaInFeet) <= 0)
				required = FivePOfTCLOTPP;
			else
				required = totalRequiredPerRoofTopAreaInFeet;

			if (DxfFileConstants.OC_RESIDENTIAL.equals(typeHelper.getType().getCode())) {
				if (pl.getVirtualBuilding().getTotalCoverageArea().compareTo(new BigDecimal("300")) >= 0)
					if (required.compareTo(new BigDecimal("500")) < 0)
						required = new BigDecimal("500");
			} else if (DxfFileConstants.FIVE_STAR_HOTEL.equals(typeHelper.getSubtype().getCode())) {
				if (required.compareTo(new BigDecimal("5000")) < 0)
					required = new BigDecimal("5000");
			} else if (DxfFileConstants.HOTEL.equals(typeHelper.getSubtype().getCode())) {
				if (required.compareTo(new BigDecimal("2000")) < 0)
					required = new BigDecimal("2000");
			} else if (DxfFileConstants.OC_COMMERCIAL.equals(typeHelper.getType().getCode())) {
				if (pl.getVirtualBuilding().getTotalCoverageArea().compareTo(new BigDecimal("500")) >= 0)
					if (required.compareTo(new BigDecimal("2000")) < 0)
						required = new BigDecimal("2000");
			}

		} catch (Exception e) {
			LOG.error(e);
		}

		// Total connected load of the proposed project in W
		setReportOutputDetailsWithoutOccupancy(pl, "ODA Rule 48 (1,2,3)", desc1, DxfFileConstants.NA,
				totalConnectedLoadOfTheProposedProjectInW.toString(), Result.Accepted.getResultVal());

		// Solar PV System
		if (isApplicable) {
			BigDecimal minimumGenerationCapacityOfTheRooftopSolarPvSystemInW = BigDecimal.ZERO;
			try {
				
				if(pl.getPlanInfoProperties()
				.get(MINIMUM_GENERATION_CAPACITY_OF_THE_ROOFTOP_SOLAR_PV_SYSTEM_IN_W).equalsIgnoreCase(DxfFileConstants.NA)) {
					pl.addError("minimumGenerationCapacityOfTheRooftopSolarPvSystemInW",
							"Value for 'MINIMUM_GENERATION_CAPACITY_OF_THE_ROOFTOP_SOLAR_PV_SYSTEM_IN_W' cannot be NA.");
				} else {
					minimumGenerationCapacityOfTheRooftopSolarPvSystemInW = pl.getPlanInformation()
							.getMinimumGenerationCapacityOfTheRooftopSolarPvSystemInW();
				}
	
			} catch (Exception e) {
				pl.addError("MINIMUM_GENERATION_CAPACITY_OF_THE_ROOFTOP_SOLAR_PV_SYSTEM_IN_W",
						"MINIMUM_GENERATION_CAPACITY_OF_THE_ROOFTOP_SOLAR_PV_SYSTEM_IN_W is required.");
			}
			boolean flage = false;
			if (minimumGenerationCapacityOfTheRooftopSolarPvSystemInW.compareTo(required) >= 0)
				flage = true;

			setReportOutputDetailsWithoutOccupancy(pl, "ODA Rule 48 (1,2,3)", desc2, required.toString(),
					minimumGenerationCapacityOfTheRooftopSolarPvSystemInW.toString(),
					flage ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());

			processSolar(pl, rule, subRule, desc3);

		}
		return pl;
	}

	private void processSolar(Plan pl, String rule, String subRule, String subRuleDesc) {
		if (!pl.getUtility().getSolar().isEmpty()) {
			setReportOutputDetailsWithoutOccupancy(pl, "ODA Rule 48 (1,2,3), 51(1)", subRuleDesc, DxfFileConstants.MANDATORY,
					OBJECTDEFINED_DESC, Result.Accepted.getResultVal());
			return;
		} else {
			setReportOutputDetailsWithoutOccupancy(pl, "ODA Rule 48 (1,2,3), 51(1)", subRuleDesc, DxfFileConstants.MANDATORY,
					OBJECTNOTDEFINED_DESC, Result.Not_Accepted.getResultVal());
			return;
		}
	}

	private boolean isRequired(OccupancyTypeHelper typeHelper, BigDecimal plotarea) {
		boolean flage = false;

		if (DxfFileConstants.OC_RESIDENTIAL.equals(typeHelper.getType().getCode())
				&& plotarea.compareTo(new BigDecimal("300")) >= 0) {
			flage = true;
		} else if (plotarea.compareTo(new BigDecimal("500")) >= 0) {
			flage = true;
		}

		return flage;
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
