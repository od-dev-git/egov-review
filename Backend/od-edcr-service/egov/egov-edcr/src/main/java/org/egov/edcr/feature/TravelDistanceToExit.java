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

import static org.egov.edcr.constants.DxfFileConstants.A;
import static org.egov.edcr.constants.DxfFileConstants.B;
import static org.egov.edcr.constants.DxfFileConstants.D;
import static org.egov.edcr.constants.DxfFileConstants.F;
import static org.egov.edcr.constants.DxfFileConstants.G;
import static org.egov.edcr.constants.DxfFileConstants.H;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.egov.edcr.service.ProcessHelper;
import org.egov.edcr.utility.DcrConstants;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class TravelDistanceToExit extends FeatureProcess {

	private static final String SUBRULE_42_2 = "ODA Rule 72 (4,5,9)";
	private static final String SUBRULE_42_2_DESC = "Maximum travel distance to emergency exit";
	public static final BigDecimal VAL_30 = BigDecimal.valueOf(30);
	public static final BigDecimal VAL_20 = BigDecimal.valueOf(20);

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		BigDecimal requiredMaxTravelDistance = getRequiredTravelDistance(pl);

		if (requiredMaxTravelDistance.compareTo(BigDecimal.ZERO) > 0) {
			HashMap<String, String> errors = new HashMap<>();
			if (pl != null) {
				if (pl.getTravelDistancesToExit().isEmpty()) {
					errors.put(DcrConstants.TRAVEL_DIST_EXIT,
							edcrMessageSource.getMessage(DcrConstants.OBJECTNOTDEFINED,
									new String[] { DcrConstants.TRAVEL_DIST_EXIT }, LocaleContextHolder.getLocale()));
					pl.addErrors(errors);
					return pl;
				}
			}
			String subRule = SUBRULE_42_2;
			String subRuleDesc = SUBRULE_42_2_DESC;

			BigDecimal totalProvidedTravelDistance = pl.getTravelDistancesToExit().stream().reduce(BigDecimal.ZERO,
					BigDecimal::add);
			boolean valid = false;
			if (totalProvidedTravelDistance.compareTo(requiredMaxTravelDistance) <= 0) {
				valid = true;
			}

			if (valid) {
				setReportOutputDetails(pl, subRule, requiredMaxTravelDistance.toString(),
						totalProvidedTravelDistance.toString(), Result.Accepted.getResultVal());
			} else {
				setReportOutputDetails(pl, subRule, requiredMaxTravelDistance.toString(),
						totalProvidedTravelDistance.toString(), Result.Not_Accepted.getResultVal());
			}

		}

		return pl;
	}

	private BigDecimal getRequiredTravelDistance(Plan pl) {
		BigDecimal requiredTravelDistance = BigDecimal.ZERO;
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		if (DxfFileConstants.YES.equalsIgnoreCase(pl.getPlanInformation().getBuildingUnderHazardousOccupancyCategory()))
			requiredTravelDistance = new BigDecimal("20");
		else if (OdishaUtill.isAssemblyBuildingCriteria(pl))
			requiredTravelDistance = new BigDecimal("30");
		else {
			BigDecimal buildingHeight = BigDecimal.ZERO;
			buildingHeight = pl.getBlocks().stream().map(block -> block.getBuilding().getBuildingHeight())
					.reduce(BigDecimal::max).get();

			if (buildingHeight.compareTo(new BigDecimal("15")) >= 0) {
				if (DxfFileConstants.OC_RESIDENTIAL.equals(occupancyTypeHelper.getType().getCode()))
					requiredTravelDistance = new BigDecimal("20");
				else if (DxfFileConstants.OC_COMMERCIAL.equals(occupancyTypeHelper.getType().getCode()))
					requiredTravelDistance = new BigDecimal("30");
				else if (DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL
						.equals(occupancyTypeHelper.getType().getCode()))
					requiredTravelDistance = new BigDecimal("20");
				else if (DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(occupancyTypeHelper.getType().getCode()))
					requiredTravelDistance = new BigDecimal("30");
				else if (DxfFileConstants.OC_EDUCATION.equals(occupancyTypeHelper.getType().getCode()))
					requiredTravelDistance = new BigDecimal("20");
			}
		}

		return requiredTravelDistance;
	}

	private void setReportOutputDetails(Plan pl, String ruleNo, String expected, String actual, String status) {
		scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Travel Distance To Emergency Exits");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, REQUIRED);
		scrutinyDetail.addColumnHeading(3, PROVIDED);
		scrutinyDetail.addColumnHeading(4, STATUS);
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
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

	public Map<String, BigDecimal> getOccupancyValues() {

		Map<String, BigDecimal> roadWidthValues = new HashMap<>();

		roadWidthValues.put(D, VAL_30);
		roadWidthValues.put(G, VAL_30);
		roadWidthValues.put(F, VAL_30);
		roadWidthValues.put(H, VAL_30);

		roadWidthValues.put(A, VAL_20);
		// roadWidthValues.put(I, VAL_20);
		roadWidthValues.put(B, VAL_20);

		return roadWidthValues;
	}
}