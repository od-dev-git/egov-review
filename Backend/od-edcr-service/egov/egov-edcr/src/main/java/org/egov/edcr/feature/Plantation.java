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

import static org.egov.edcr.constants.DxfFileConstants.A_SA;
import static org.egov.edcr.constants.DxfFileConstants.B;
import static org.egov.edcr.constants.DxfFileConstants.D;
import static org.egov.edcr.constants.DxfFileConstants.G;
import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.stereotype.Service;

@Service
public class Plantation extends FeatureProcess {

	// color code for arch
	private static final int COLOR_TREE_CUT = 1;
	private static final int COLOR_TREE_EXISTING = 2;
	private static final int COLOR_TREE_PLANTED = 3;

	private static final Logger LOGGER = Logger.getLogger(Plantation.class);
	private static final String RULE_32 = "32";
	public static final String PLANTATION_TREECOVER_DESCRIPTION1 = "No of tree as per plot";
	public static final String PLANTATION_TREECOVER_DESCRIPTION2 = "No of tree as per tree cut";

	@Override
	public Plan validate(Plan pl) {
		return null;
	}

	@Override
	public Plan process(Plan pl) {
		updatePlantationDetails(pl);
		scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Plantation Tree Cover");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);

		int totalRequiredCountAsPerArea = 0;
		int totalRequiredCountAsPerCut = 0;
		int totalTreeOnSite = 0;

		int cutTreeCount = 0;
		int existingTreeCount = 0;
		int plantedTreeCount = 0;

		if (pl.getPlot().getArea().compareTo(new BigDecimal("115")) > 0) {
			totalRequiredCountAsPerArea = pl.getPlot().getArea()
					.divide(new BigDecimal("80"), DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).intValue();

		}

		if (pl.getPlantation().getCutTreeCount() > 0) {
			totalRequiredCountAsPerCut = pl.getPlantation().getCutTreeCount() * 3;
		}

		if (pl.getPlantation() != null) {
			cutTreeCount = pl.getPlantation().getCutTreeCount();
			existingTreeCount = pl.getPlantation().getExistingTreeCount();
			plantedTreeCount = pl.getPlantation().getPlantedTreeCount();
		}

		totalTreeOnSite = existingTreeCount + plantedTreeCount;

		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, "ODA Rule 30(1)");
		details.put(DESCRIPTION, PLANTATION_TREECOVER_DESCRIPTION1);
		details.put(REQUIRED, "" + totalRequiredCountAsPerArea);
		details.put(PROVIDED, "" + totalTreeOnSite);
		details.put(STATUS, totalTreeOnSite >= totalRequiredCountAsPerArea ? Result.Accepted.getResultVal()
				: Result.Not_Accepted.getResultVal());
		scrutinyDetail.getDetail().add(details);
		
		
		Map<String, String> details1 = new HashMap<>();
		details1.put(RULE_NO, "ODA Rule 30(2)");
		details1.put(DESCRIPTION, PLANTATION_TREECOVER_DESCRIPTION2+" ( "+cutTreeCount+" )");
		details1.put(REQUIRED, "" + totalRequiredCountAsPerCut);
		details1.put(PROVIDED, "" + plantedTreeCount);
		details1.put(STATUS, plantedTreeCount >= totalRequiredCountAsPerCut ? Result.Accepted.getResultVal()
				: Result.Not_Accepted.getResultVal());
		scrutinyDetail.getDetail().add(details1);
		
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		return pl;
	}

	private void updatePlantationDetails(Plan pl) {
		int cutTreeCount = 0;
		int existingTreeCount = 0;
		int plantedTreeCount = 0;

		if (pl.getPlantation() != null) {
			for (Measurement measurement : pl.getPlantation().getPlantations()) {
				switch (measurement.getColorCode()) {
				case COLOR_TREE_CUT:
					cutTreeCount++;
					break;
				case COLOR_TREE_EXISTING:
					existingTreeCount++;
					break;
				case COLOR_TREE_PLANTED:
					plantedTreeCount++;
					break;
				}
			}
			pl.getPlantation().setCutTreeCount(cutTreeCount);
			pl.getPlantation().setExistingTreeCount(existingTreeCount);
			pl.getPlantation().setPlantedTreeCount(plantedTreeCount);
		}

	}

	public static void main(String[] args) {
		BigDecimal t = new BigDecimal("123");
		//System.out.println(t.divide(new BigDecimal("129"), DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS));
		//System.out.println(
				//t.divide(new BigDecimal("129"), DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS).intValue());
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}
