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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
public class OutHouseService extends FeatureProcess {
	@Override
	public Plan validate(Plan pl) {

		List<Block> outhouses = pl.getOuthouse();

		if (pl.getPlot().getArea().compareTo(new BigDecimal("150")) < 0 && outhouses.size() > 1) {
			pl.addError("outhouseNotAllowed", "Outhouse not allowed in less then 150 sqm plot.");
			return pl;
		}

		for (Block block : outhouses) {
			if (block.getBuilding().getFloors().size() > 1) {
				pl.addError("outhouseFloor", "More than 1 floor is not allowed in Outhouse");
				return pl;
			}
			for (Floor floor : block.getBuilding().getFloors()) {
				for (Occupancy occupancy : floor.getOccupancies()) {
					if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
							&& !DxfFileConstants.OUTHOUSE.equals(occupancy.getTypeHelper().getSubtype().getCode())) {
						pl.addError("OtherOcInothouse", "No other Sub Occupancy allowed in Outhouse block");
					}
				}
			}
		}

		return pl;
	}

//	private List<Block> getOutHouseBlock(Plan pl) {
//		List<Block> outhouses = new ArrayList<>();
//
//		for (Block block : pl.getBlocks()) {
//			boolean flage = false;
//			for (Floor floor : block.getBuilding().getFloors()) {
//				for (Occupancy occupancy : floor.getOccupancies()) {
//					if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
//							&& DxfFileConstants.OUTHOUSE.equals(occupancy.getTypeHelper().getSubtype().getCode())) {
//						outhouses.add(block);
//						flage = true;
//					}
//				}
//				if (flage) {
//					break;
//				}
//			}
//			if (flage) {
//				block.setOutHouse(flage);
//				break;
//			}
//		}
//		return outhouses;
//	}

	@Override
	public Plan process(Plan pl) {
		validate(pl);
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Outhouse");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		BigDecimal coverage = BigDecimal.ZERO;
		BigDecimal height = BigDecimal.ZERO;
		List<Block> outhouses = pl.getOuthouse();
		try {
			height = outhouses.stream().map(block -> block.getBuilding().getBuildingHeight()).reduce(BigDecimal::max).get().setScale(2, BigDecimal.ROUND_HALF_UP);
			coverage=outhouses.stream().map(block -> block.getBuilding().getCoverageArea()).reduce(BigDecimal::add).get().setScale(2, BigDecimal.ROUND_HALF_UP);
		}catch (Exception e) {
			// TODO: handle exception
		}
		
		BigDecimal requiredCovrage=new BigDecimal("30");
		BigDecimal requiredHeight=new BigDecimal("3");
		
		if(outhouses.size()>0) {
			//coverage
			addDetails(scrutinyDetail, "ODA Rule 59 (i)", "Max coverage", requiredCovrage.toString(),
					coverage.toString(), coverage.compareTo(requiredCovrage)<=0?Result.Accepted.getResultVal():Result.Not_Accepted.getResultVal());
			
			//Height
			addDetails(scrutinyDetail, "ODA Rule 59 (i)", "Max height", requiredHeight.toString(),
					height.toString(), height.compareTo(requiredHeight)<=0?Result.Accepted.getResultVal():Result.Not_Accepted.getResultVal());
		
		}
		

		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		return pl;
	}

	private void addDetails(ScrutinyDetail scrutinyDetail, String rule, String description, String required,
			String provided, String status) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, rule);
		details.put(DESCRIPTION, description);
		details.put(REQUIRED, required);
		details.put(PROVIDED, provided);
		details.put(STATUS, status);
		scrutinyDetail.getDetail().add(details);
	}
	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

}
