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

import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.FloorUnit;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.utility.DcrConstants;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class SolarWaterHeating extends FeatureProcess {

	private static final Logger LOG = Logger.getLogger(SolarWaterHeating.class);

	private static final String RULE_51 = "51";
	private static final String RULE_51_DESCRIPTION = "Solar Water Heating";
	public static final BigDecimal MINIMUM_AREA_200 = BigDecimal.valueOf(200);

	@Override
	public Plan validate(Plan pl) {
//		HashMap<String, String> errors = new HashMap<>();
//		if (pl != null && pl.getUtility() != null) { // Solar Water Heating system defined or not
//			String subOccupancyCode = pl.getVirtualBuilding().getMostRestrictiveFarHelper().getSubtype().getCode();
//			if (checkOccupancyTypeForSolarWaterHeating(subOccupancyCode)
//					&& pl.getUtility().getSolarWaterHeatingSystems().isEmpty()) {
//				errors.put(RULE_51_DESCRIPTION, edcrMessageSource.getMessage(OBJECTNOTDEFINED,
//						new String[] { RULE_51_DESCRIPTION }, LocaleContextHolder.getLocale()));
//				pl.addErrors(errors);
//			}
//
//		}

		return pl;

	}

	public boolean isRequired(OccupancyTypeHelper occupancyTypeHelper, BigDecimal covrage) {
		boolean isApplicable = false;

		if (covrage.compareTo(new BigDecimal("200")) >= 0) {
			if (DxfFileConstants.OC_RESIDENTIAL.equals(occupancyTypeHelper.getType().getCode())
					|| DxfFileConstants.HOTEL.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.FIVE_STAR_HOTEL.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.GUEST_HOUSES.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.FOOD_COURTS.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.BANQUET_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP
							.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.LOCAL_AND_SEMI_GOVERNMENT_OFFICES
							.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.POLICE_OR_ARMY_OR_BARRACK.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.HEALTH_CENTRE.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.HOSPITAL.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.LAB.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.OC_EDUCATION.equals(occupancyTypeHelper.getType().getCode())) {
				isApplicable = true;
			}
		}

		return isApplicable;
	}

	@Override
	public Plan process(Plan pl) {
		validate(pl);
		scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		scrutinyDetail.setKey("Common_Solar Water Heating");
		String subRule = "";
		String subRuleDesc = RULE_51_DESCRIPTION;

		BigDecimal actualTankCapacity = pl.getPlanInformation().getCapacityOfSolarWaterHeatingSystemInLpd();

		OccupancyTypeHelper mostRestrictiveFarHelper = pl.getVirtualBuilding() != null
				? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
				: null;
		if (isRequired(mostRestrictiveFarHelper, pl.getVirtualBuilding().getTotalCoverageArea())) {
			BigDecimal expectedTankCapacity = BigDecimal.ZERO;

			long totalDU = pl.getPlanInformation().getTotalNoOfDwellingUnits()>0?pl.getPlanInformation().getTotalNoOfDwellingUnits():1;
			BigDecimal totalUserInPlan = pl.getPlanInformation().getNumberOfOccupantsOrUsers();

			if (DxfFileConstants.DORMITORY.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.HOSTEL.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {
				expectedTankCapacity = totalUserInPlan.multiply(new BigDecimal("10"));
			} else if (DxfFileConstants.OC_RESIDENTIAL.equals(mostRestrictiveFarHelper.getType().getCode())) {
				expectedTankCapacity = new BigDecimal(totalDU * 100);
			} else if (DxfFileConstants.FIVE_STAR_HOTEL.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {
				expectedTankCapacity = new BigDecimal(totalDU * 15);
			} else if (DxfFileConstants.GUEST_HOUSES.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.FOOD_COURTS.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.BANQUET_HALL.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP
							.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.LOCAL_AND_SEMI_GOVERNMENT_OFFICES
							.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.POLICE_OR_ARMY_OR_BARRACK
							.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {
				expectedTankCapacity = new BigDecimal("200");
			} else if (DxfFileConstants.LAB.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.RESEARCH_AND_TRAINING_CENTER
							.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {
				expectedTankCapacity = new BigDecimal("100");
			} else if (DxfFileConstants.OC_EDUCATION.equals(mostRestrictiveFarHelper.getType().getCode())) {
				expectedTankCapacity = totalUserInPlan.multiply(new BigDecimal("10"));
			} else if (DxfFileConstants.HEALTH_CENTRE.equals(mostRestrictiveFarHelper.getType().getCode())
					|| DxfFileConstants.HOSPITAL.equals(mostRestrictiveFarHelper.getType().getCode())
					|| DxfFileConstants.HOTEL.equals(mostRestrictiveFarHelper.getType().getCode())) {
				expectedTankCapacity = totalUserInPlan.multiply(new BigDecimal("10"));
			}

			if (!pl.getUtility().getSolarWaterHeatingSystems().isEmpty()) {
				setReportOutputDetails(pl, subRuleDesc, "Solar Water Heating System", DxfFileConstants.MANDATORY,
						DcrConstants.OBJECTDEFINED_DESC, Result.Accepted.getResultVal());
			} else {
				setReportOutputDetails(pl, subRuleDesc, "Solar Water Heating System", DxfFileConstants.MANDATORY,
						DcrConstants.OBJECTNOTDEFINED_DESC, Result.Not_Accepted.getResultVal());
			}

			if (actualTankCapacity.compareTo(expectedTankCapacity) >= 0) {
				setReportOutputDetails(pl, subRuleDesc, "Capacity of Solar Water heating system in LPD",
						expectedTankCapacity.toString(), actualTankCapacity.toString(), Result.Accepted.getResultVal());
			} else {
				setReportOutputDetails(pl, subRuleDesc, "Capacity of Solar Water heating system in LPD",
						expectedTankCapacity.toString(), actualTankCapacity.toString(),
						Result.Not_Accepted.getResultVal());
			}

		}

		return pl;
	}

	private void setReportOutputDetails(Plan pl, String ruleNo, String ruleDesc, String expected, String actual,
			String status) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, "ODA Rule 48(2,3)");
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
