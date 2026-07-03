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

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.stereotype.Service;

@Service
public class GlassFacadeOpening extends FeatureProcess {
	private static final Logger LOG = Logger.getLogger(GlassFacadeOpening.class);
	@Override
	public Plan validate(Plan plan) {
		return plan;
	}

	@Override
	public Plan process(Plan pl) {
		String serviceType = pl.getPlanInformation().getServiceType();
		for (Block b : pl.getBlocks()) {
			if (b.isGlassFacadeOpening()) {
				ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
				scrutinyDetail.setKey("Block_" + b.getNumber() + "_" + "Glass Facade Opening");
				scrutinyDetail.addColumnHeading(1, RULE_NO);
				scrutinyDetail.addColumnHeading(2, DESCRIPTION);
				scrutinyDetail.addColumnHeading(3, FLOOR);
				scrutinyDetail.addColumnHeading(4, REQUIRED);
				scrutinyDetail.addColumnHeading(5, PROVIDED);
				scrutinyDetail.addColumnHeading(6, STATUS);

				for (Floor floor : b.getBuilding().getFloors()) {
					boolean isPerposedAreaPersent = isPerposedAreaPersent(floor, serviceType);
					if (floor.getNumber() < 0 || !isPerposedAreaPersent)
						continue;

					// Height
					BigDecimal minHeight = new BigDecimal("1.5");
					BigDecimal minWidth = new BigDecimal("1.5");
					BigDecimal RequiredfloorToGlassOpeningHeights = new BigDecimal("1.2");// ==

					List<org.egov.common.entity.edcr.GlassFacadeOpening> facadeOpenings = floor
							.getGlassFacadeOpenings();
					
					if(facadeOpenings==null || facadeOpenings.size()<2) {
						pl.addError("GlassFacadeOpenings", "Minimum two GlassFacade required.");
					}

					BigDecimal providedMinHeight=BigDecimal.ZERO;
					BigDecimal providedWidth=BigDecimal.ZERO;
					BigDecimal providedFloorToGlassOpeningHeightMin=BigDecimal.ZERO;
					BigDecimal providedFloorToGlassOpeningHeightMax=BigDecimal.ZERO;
					
					//
					int minCount = 2;
					int providedCount=floor.getGlassFacadeOpenings()!=null?floor.getGlassFacadeOpenings().size():0;
					
					List<BigDecimal> widths=new ArrayList<>();
					List<BigDecimal> heights=new ArrayList<>();
					List<BigDecimal> floorToGlassOpeningHeights=new ArrayList<>();
					for(org.egov.common.entity.edcr.GlassFacadeOpening glassFacadeOpening:floor.getGlassFacadeOpenings()) {
						widths.addAll(glassFacadeOpening.getWidths());
						heights.addAll(glassFacadeOpening.getHeights());
						floorToGlassOpeningHeights.addAll(glassFacadeOpening.getFloorToGlassOpeningHeights());
					}
					
					try {
						providedWidth=widths.stream().reduce(BigDecimal::min).get();
						providedMinHeight=heights.stream().reduce(BigDecimal::min).get();
						providedFloorToGlassOpeningHeightMin=floorToGlassOpeningHeights.stream().reduce(BigDecimal::min).get();
						providedFloorToGlassOpeningHeightMax=floorToGlassOpeningHeights.stream().reduce(BigDecimal::max).get();
					}catch (Exception e) {
						LOG.error("Parssing error", e);
					}
					//count
					setReportOutputDetails(pl, floor.getNumber()+"", "NA", "Count", "2", providedCount+"", providedCount>=2?Result.Accepted.getResultVal():Result.Not_Accepted.getResultVal(), scrutinyDetail);
					
					//width
					setReportOutputDetails(pl, floor.getNumber()+"", "ODA Annexure IX (6-i)", "Width", minWidth.toString(), providedWidth.toString(), providedWidth.compareTo(minWidth)>=0?Result.Accepted.getResultVal():Result.Not_Accepted.getResultVal(), scrutinyDetail);
					
					//height
					setReportOutputDetails(pl, floor.getNumber()+"", "ODA Annexure IX (6-i)", "Height", minHeight.toString(), providedMinHeight.toString(), providedMinHeight.compareTo(minHeight)>=0?Result.Accepted.getResultVal():Result.Not_Accepted.getResultVal(), scrutinyDetail);
					
					boolean providedFloorToGlassOpeningHeightStatus=false;
					if(providedFloorToGlassOpeningHeightMax.compareTo(providedFloorToGlassOpeningHeightMin)==0 && providedFloorToGlassOpeningHeightMin.compareTo(RequiredfloorToGlassOpeningHeights)==0) {
						providedFloorToGlassOpeningHeightStatus=true;
					}
					//providedFloorToGlassOpeningHeightMax
					setReportOutputDetails(pl, floor.getNumber()+"", "ODA Annexure IX (6-i)", "Height from floor To glassOpening", RequiredfloorToGlassOpeningHeights.toString(),providedFloorToGlassOpeningHeightMax.compareTo(providedFloorToGlassOpeningHeightMin)>0?providedFloorToGlassOpeningHeightMax.toString():providedFloorToGlassOpeningHeightMin.toString(), providedFloorToGlassOpeningHeightStatus?Result.Accepted.getResultVal():Result.Not_Accepted.getResultVal(), scrutinyDetail);
					
				}
				pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
			}
		}
		return pl;
	}

	private void setReportOutputDetails(Plan pl, String floor, String ruleNo, String ruleDesc, String expected,
			String actual, String status, ScrutinyDetail scrutinyDetail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(FLOOR, floor);
		details.put(DESCRIPTION, ruleDesc);
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status);
		scrutinyDetail.getDetail().add(details);
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
	
	private boolean isPerposedAreaPersent(Floor floor, String serviceType) {
		boolean isPerposedAreaPersent = true;
		if (DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType)) {
			BigDecimal totalBuildUpArea = floor.getOccupancies().stream().map(oc -> oc.getBuiltUpArea())
					.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
			BigDecimal totalExistingArea = floor.getOccupancies().stream().map(oc -> oc.getExistingBuiltUpArea())
					.reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
			BigDecimal totalPropusedArea = totalBuildUpArea.subtract(totalExistingArea).setScale(2,
					BigDecimal.ROUND_HALF_UP);
			if (totalPropusedArea.compareTo(BigDecimal.ZERO) > 0)
				isPerposedAreaPersent = true;
			else
				isPerposedAreaPersent = false;
		}
		return isPerposedAreaPersent;
	}
}
