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
import org.egov.common.entity.edcr.FarDetails;
import org.egov.common.entity.edcr.OccupancyPercentage;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaMixedUseUtill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AdditionalMixedUseFeature extends FeatureProcess {
	private static final Logger LOG = Logger.getLogger(AdditionalMixedUseFeature.class);
	
	@Autowired
	private OdishaMixedUseUtill odishaMixedUseUtill;

	@Override
	public Plan validate(Plan pl) {
		String principalUseColorCodeStr = pl.getPlanInfoProperties()
				.get(DxfFileConstants.COLOUR_CODE_OF_PRINCIPAL_USE_OF_THE_BUILDING_IN_CASE_OF_MIXED_USE_PROJECTS);
		int principalUseColorCode = -1;

		try {
			principalUseColorCode = Integer.valueOf(principalUseColorCodeStr);
		} catch (Exception e) {
			LOG.error(e);
		}
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		if (occupancyTypeHelper.getSubtype().getCode().equals(DxfFileConstants.MIXED_USE)
				&& principalUseColorCode == -1) {
			pl.addError("COLOUR_CODE_OF_PRINCIPAL_USE_OF_THE_BUILDING_IN_CASE_OF_MIXED_USE_PROJECTS",
					"COLOUR_CODE_OF_PRINCIPAL_USE_OF_THE_BUILDING_IN_CASE_OF_MIXED_USE_PROJECTS is not defined in plan info.");
			return pl;
		}

		List<Integer> validColorCode = getValidColorCode(pl);
		if (validColorCode.contains(principalUseColorCode)) {
			OccupancyTypeHelper principalOccupancyTypeHelper = odishaMixedUseUtill
					.getOccupancyTypeHelper(principalUseColorCode, pl);
			pl.getVirtualBuilding().setPrincipalOccupancyTypeHelper(principalOccupancyTypeHelper);
		} else {
			pl.addError("COLOUR_CODE_OF_PRINCIPAL_USE_OF_THE_BUILDING_IN_CASE_OF_MIXED_USE_PROJECTS",
					"Invalid colour code identified in Plan info. Kindly provide the correct colour code in COLOUR_CODE_OF_PRINCIPAL_USE_OF_THE_BUILDING_IN_CASE_OF_MIXED_USE_PROJECTS.");
			return pl;
		}
		
		if(pl.getPlot().getArea().compareTo(new BigDecimal("500"))<0) {
			pl.addError("Mixed use", "Mixed use not allowed in Plot area less than 500 sqm");
		}

		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		if (!occupancyTypeHelper.getSubtype().getCode().equals(DxfFileConstants.MIXED_USE)) {
			return pl;
		}
		validate(pl);
		OccupancyTypeHelper principalOccupancyTypeHelper = pl.getVirtualBuilding().getPrincipalOccupancyTypeHelper();
		if (principalOccupancyTypeHelper == null || principalOccupancyTypeHelper.getSubtype() == null) {
			return pl;
		}
		processPrincipalOccupancyPercentage(pl);
		return pl;
	}

	private void processPrincipalOccupancyPercentage(Plan pl) {
		OccupancyTypeHelper principalOccupancyTypeHelper = pl.getVirtualBuilding().getPrincipalOccupancyTypeHelper();
		Map<String, OccupancyPercentage> occupancyPercentages = pl.getPlanInformation().getOccupancyPercentages();

		OccupancyPercentage occupancyPercentage = occupancyPercentages
				.get(principalOccupancyTypeHelper.getSubtype().getName());
		
		BigDecimal BaseFarArea = pl.getPlot().getPlotBndryArea().multiply(BigDecimal.valueOf(pl.getFarDetails().getBaseFar()));
		BigDecimal totalFloorAreaProvided = pl.getVirtualBuilding().getTotalFloorArea();
		
		BigDecimal totalFloorAreaTillBaseFar = totalFloorAreaProvided.compareTo(BaseFarArea)>0?BaseFarArea:totalFloorAreaProvided;
		BigDecimal totalFloorAreaOverBaseFar = totalFloorAreaProvided.compareTo(BaseFarArea)>0?totalFloorAreaProvided.subtract(BaseFarArea):BigDecimal.ZERO;
		
				
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, PERMISSIBLE);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		scrutinyDetail.setKey("Common_Principal use of the building");
		
		BigDecimal requiredTillBaseFar = totalFloorAreaTillBaseFar.multiply(new BigDecimal("0.6666"));
		requiredTillBaseFar = requiredTillBaseFar.setScale(2,BigDecimal.ROUND_HALF_UP);
		
		BigDecimal principalUseOccupancyFloorArea = getTotalFloorAreaForOccupancy(occupancyPercentages, occupancyPercentage);
		
		if(totalFloorAreaTillBaseFar.compareTo(BigDecimal.ZERO) > 0) {
			
			String permissibleStr = "Minimum 2/3rd (66.66%) of base far area ("+requiredTillBaseFar+").";
			
//			boolean isAccepted = requiredTillBaseFar.compareTo(occupancyPercentage.getTotalFloorArea()) <= 0 ? true : false;
			boolean isAccepted = requiredTillBaseFar.compareTo(principalUseOccupancyFloorArea) <= 0 ? true : false;

			Map<String, String> details = new HashMap<>();
			details.put(RULE_NO, "ODA Rule 75(4-iii), 76(4-ii)");
			details.put(DESCRIPTION, occupancyPercentage.getOccupancy()+" - Principal use of the building within Base FAR");
			details.put(PERMISSIBLE, permissibleStr);
//			details.put(PROVIDED, occupancyPercentage.getTotalFloorArea().toString());
			details.put(PROVIDED, principalUseOccupancyFloorArea.toString());
			details.put(STATUS, isAccepted ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());
			scrutinyDetail.getDetail().add(details);
		}
		
		if(totalFloorAreaOverBaseFar.compareTo(BigDecimal.ZERO) > 0) {
			
			BigDecimal  requiredOverBasefar =totalFloorAreaOverBaseFar.multiply(new BigDecimal("0.3333"));
			requiredOverBasefar = requiredOverBasefar.setScale(2,BigDecimal.ROUND_HALF_UP);
			String permissibleStr = "Minimum 1/3rd (33.33%) of over base far ("+requiredOverBasefar+").";
			
//			BigDecimal provided = occupancyPercentage.getTotalFloorArea().subtract(requiredTillBaseFar).setScale(2,BigDecimal.ROUND_HALF_UP);
			BigDecimal provided = principalUseOccupancyFloorArea.subtract(requiredTillBaseFar).setScale(2,BigDecimal.ROUND_HALF_UP);
			
			boolean isAccepted = requiredOverBasefar.compareTo(provided) <= 0 ? true : false;

			Map<String, String> details = new HashMap<>();
			details.put(RULE_NO, "ODA Rule 75(4-iii), 76(4-ii)");
			details.put(DESCRIPTION, occupancyPercentage.getOccupancy()+" - Principal use of the building over Base FAR");
			details.put(PERMISSIBLE, permissibleStr);
			details.put(PROVIDED, provided.toString());
			details.put(STATUS, isAccepted ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());
			scrutinyDetail.getDetail().add(details);
		}

		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);

	}
	
	private BigDecimal getTotalFloorAreaForOccupancy(Map<String, OccupancyPercentage> occupancyPercentages,OccupancyPercentage occupancyPercentage) {
		BigDecimal totalArea = BigDecimal.ZERO;
		
		for(OccupancyPercentage percentage:occupancyPercentages.values()) {
			if(occupancyPercentage.getOccupancyCode().equals(percentage.getOccupancyCode()))
				totalArea = totalArea.add(percentage.getTotalFloorArea());
		}
		
		totalArea = totalArea.setScale(2,BigDecimal.ROUND_HALF_UP);
		return totalArea;
	}

	private List<Integer> getValidColorCode(Plan pl) {
		List<Integer> list = new ArrayList<>();
		for (OccupancyTypeHelper occupancyTypeHelper : pl.getVirtualBuilding().getOccupancyTypes()) {
			if (occupancyTypeHelper.getSubtype().getColor() != null) {
				list.add(occupancyTypeHelper.getSubtype().getColor());
			}
		}
		return list;
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

	private ScrutinyDetail getNewScrutinyDetailRoadArea(String key) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, DxfFileConstants.AREA_TYPE);
		scrutinyDetail.addColumnHeading(4, DxfFileConstants.ROAD_WIDTH);
		scrutinyDetail.addColumnHeading(5, PERMISSIBLE);
		scrutinyDetail.addColumnHeading(6, PROVIDED);
		scrutinyDetail.addColumnHeading(7, STATUS);
		scrutinyDetail.setKey(key);
		return scrutinyDetail;
	}

	private ScrutinyDetail getNewScrutinyDetail(String key) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, PERMISSIBLE);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		scrutinyDetail.setKey(key);
		return scrutinyDetail;
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

}
