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

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.stereotype.Service;

@Service
public class Verandah extends FeatureProcess {

	private static final Logger LOG = Logger.getLogger(Verandah.class);
	private static final String RULE_43A = "ODA Rule 2(xxiv,cliii), 29";
	private static final String RULE_43 = "ODA Rule 2(xxiv,cliii), 29";

	public static final String VERANDAH_DESCRIPTION = "Verandah";

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		for (Block b : pl.getBlocks()) {
			ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
			scrutinyDetail.setKey("Common_Verandah");
			scrutinyDetail.addColumnHeading(1, RULE_NO);
			scrutinyDetail.addColumnHeading(2, DESCRIPTION);
			scrutinyDetail.addColumnHeading(3, REQUIRED);
			scrutinyDetail.addColumnHeading(4, PROVIDED);
			scrutinyDetail.addColumnHeading(5, STATUS);

			if (b.getBuilding() != null && b.getBuilding().getFloors() != null
					&& !b.getBuilding().getFloors().isEmpty()) {

				for (Floor f : b.getBuilding().getFloors()) {

					if (f.getVerandah() != null && f.getVerandah().getMeasurements() != null
							&& !f.getVerandah().getMeasurements().isEmpty()) {

						BigDecimal minVerandaWidth=BigDecimal.ZERO;
						BigDecimal minVerandDepth =BigDecimal.ZERO;
						
						try {
							minVerandaWidth = f.getVerandah().getMeasurements().stream()
									.map(Measurement::getWidth).reduce(BigDecimal::min).get();
							minVerandDepth = f.getVerandah().getHeightOrDepth().stream().reduce(BigDecimal::min)
									.get();
							minVerandaWidth=minVerandaWidth.setScale(2,BigDecimal.ROUND_HALF_UP);
							minVerandDepth=minVerandDepth.setScale(2,BigDecimal.ROUND_HALF_UP);
						}catch (Exception e) {
							// TODO: handle exception
						}
						
						if (minVerandaWidth.compareTo(BigDecimal.ZERO) > 0) {
							Map<String, String> details = new HashMap<>();
							details.put(RULE_NO, RULE_43);
							details.put(DESCRIPTION, "Dept");

							details.put(REQUIRED, DxfFileConstants.NA);
							details.put(PROVIDED, minVerandaWidth + " at floor " + f.getNumber());
							details.put(STATUS, Result.Accepted.getResultVal());
							scrutinyDetail.getDetail().add(details);
							pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
							
//							if (minVerandaWidth.compareTo(BigDecimal.ZERO) >= 0) {
//								details.put(REQUIRED, DxfFileConstants.NA);
//								details.put(PROVIDED, "Width area " + minVerandaWidth + " at floor " + f.getNumber());
//								details.put(STATUS, Result.Accepted.getResultVal());
//								scrutinyDetail.getDetail().add(details);
//								pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
//
//							} else {
//								details.put(REQUIRED, DxfFileConstants.NA);
//								details.put(PROVIDED, "Width area " + minVerandaWidth + " at floor " + f.getNumber());
//								details.put(STATUS, Result.Not_Accepted.getResultVal());
//								scrutinyDetail.getDetail().add(details);
//								pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
//							}
						}
						if (minVerandDepth.compareTo(BigDecimal.ZERO) > 0) {
							Map<String, String> details = new HashMap<>();
							details.put(RULE_NO, RULE_43A);
							details.put(DESCRIPTION, "Width");
							
							details.put(REQUIRED, DxfFileConstants.NA);
							details.put(PROVIDED, minVerandDepth + " at floor " + f.getNumber());
							details.put(STATUS, Result.Accepted.getResultVal());
							scrutinyDetail.getDetail().add(details);
							pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
							
//							if (minVerandDepth.compareTo(BigDecimal.valueOf(3.66)) <= 0) {
//								details.put(REQUIRED, "Minimum depth not more than 3.66 m ");
//								details.put(PROVIDED, " Depth area  " + minVerandDepth + " at floor " + f.getNumber());
//								details.put(STATUS, Result.Accepted.getResultVal());
//								scrutinyDetail.getDetail().add(details);
//								pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
//
//							} else {
//								details.put(REQUIRED, "Minimum depth not more than 3.66 m ");
//								details.put(PROVIDED, " Depth area  " + minVerandDepth + " at floor " + f.getNumber());
//								details.put(STATUS, Result.Not_Accepted.getResultVal());
//								scrutinyDetail.getDetail().add(details);
//								pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
//							}
						}
					}

				}
			}

		}
		return pl;
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

}
