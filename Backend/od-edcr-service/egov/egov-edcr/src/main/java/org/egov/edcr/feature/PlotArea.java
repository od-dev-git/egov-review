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
import org.egov.common.entity.dcr.helper.OccupancyHelperDetail;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.kabeja.dxf.DXFConstants;
import org.springframework.stereotype.Service;

@Service
public class PlotArea extends FeatureProcess {

	private static final Logger LOG = Logger.getLogger(PlotArea.class);
	private static final String RULE_34 = "34-1";
	public static final String PLOTAREA_DESCRIPTION_1 = "Minimum Plot Area";
	public static final String PLOTAREA_DESCRIPTION_2 = "Minimum Plot Size";
	public static final BigDecimal THREE_ZERO = BigDecimal.valueOf(300);
	public static final BigDecimal FIVE_ZERO = BigDecimal.valueOf(500);

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {

		OccupancyTypeHelper typeHelper=pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		boolean flage=isApplicable(typeHelper);
		
		if (pl.getPlot() != null && flage) {
			BigDecimal plotArea = pl.getPlot().getArea();
			if (plotArea != null) {
				ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
				scrutinyDetail.setKey("Common_Size of plot");
				scrutinyDetail.addColumnHeading(1, RULE_NO);
				scrutinyDetail.addColumnHeading(2, DESCRIPTION);
				scrutinyDetail.addColumnHeading(3, PERMITTED);
				scrutinyDetail.addColumnHeading(4, PROVIDED);
				scrutinyDetail.addColumnHeading(5, STATUS);

				BigDecimal requiredArea=BigDecimal.ZERO;
				BigDecimal widthMinRequired=BigDecimal.ZERO;
				BigDecimal depthMinRequired=BigDecimal.ZERO;
				
				BigDecimal widthMaxRequired=BigDecimal.ZERO;
				BigDecimal depthMaxRequired=BigDecimal.ZERO;
				
				BigDecimal minimumDistanceFromTheRoadIntersectionsRequired=BigDecimal.ZERO;
				BigDecimal minimumDistanceOfPropertyLineFromCentreLineOfTheRoadRequired=BigDecimal.ZERO;
				
				BigDecimal roadWith=pl.getPlanInformation().getTotalRoadWidth();
				
				switch (typeHelper.getSubtype().getCode()) {
				case DxfFileConstants.ROW_HOUSING:
					requiredArea=new BigDecimal("30");
					break;
				case DxfFileConstants.FARM_HOUSE:
					requiredArea=new BigDecimal("10000");
					break;
				case DxfFileConstants.COUNTRY_HOMES:
					requiredArea=new BigDecimal("2000");
					break;
				case DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION:
					widthMinRequired=new BigDecimal("30");
					depthMinRequired=new BigDecimal("17");
					if(roadWith.compareTo(new BigDecimal("30"))<0) {
						minimumDistanceFromTheRoadIntersectionsRequired=new BigDecimal("50");
						minimumDistanceOfPropertyLineFromCentreLineOfTheRoadRequired=new BigDecimal("15");
					}
					else if(roadWith.compareTo(new BigDecimal("30"))>=0) {
						minimumDistanceFromTheRoadIntersectionsRequired=new BigDecimal("100");
						minimumDistanceOfPropertyLineFromCentreLineOfTheRoadRequired=roadWith.divide(new BigDecimal("2")).setScale(2, BigDecimal.ROUND_HALF_UP);
					}
					break;
				case DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION:
					widthMinRequired=new BigDecimal("30");
					depthMinRequired=new BigDecimal("36");
					if(roadWith.compareTo(new BigDecimal("30"))<0) {
						minimumDistanceFromTheRoadIntersectionsRequired=new BigDecimal("50");
						minimumDistanceOfPropertyLineFromCentreLineOfTheRoadRequired=new BigDecimal("15");
					}
					else if(roadWith.compareTo(new BigDecimal("30"))>=0) {
						minimumDistanceFromTheRoadIntersectionsRequired=new BigDecimal("100");
						minimumDistanceOfPropertyLineFromCentreLineOfTheRoadRequired=roadWith.divide(new BigDecimal("2")).setScale(2, BigDecimal.ROUND_HALF_UP);
					}
					break;
				case DxfFileConstants.CNG_MOTHER_STATION:
					widthMaxRequired=new BigDecimal("30");
					depthMaxRequired=new BigDecimal("36");
					break;
				}
				
				
				
				
				
				Map<String, String> details = new HashMap<>();
				details.put(RULE_NO, "ODA Rule 2 (xlv,lxxxiii), 74(3)");
				details.put(DESCRIPTION, PLOTAREA_DESCRIPTION_1);

				Map<String, BigDecimal> occupancyValuesMap = getOccupancyValues();
				


//				if (plotArea.compareTo(occupancyValues) >= 0) {
//					details.put(PERMITTED, String.valueOf(occupancyValues) + "m2");
//					details.put(PROVIDED, plotArea.toString() + "m2");
//					details.put(STATUS, Result.Accepted.getResultVal());
//					scrutinyDetail.getDetail().add(details);
//					pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
//				} else {
//					details.put(PERMITTED, String.valueOf(occupancyValues) + "m2");
//					details.put(PROVIDED, plotArea.toString() + "m2");
//					details.put(STATUS, Result.Not_Accepted.getResultVal());
//					scrutinyDetail.getDetail().add(details);
//					pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
//				}
			
				
			

			
			}
		}
		return pl;
	}

	public boolean isApplicable(OccupancyTypeHelper typeHelper) {
		boolean flage = false;
		if (DxfFileConstants.ROW_HOUSING.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.FARM_HOUSE.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.COUNTRY_HOMES.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION.equals(typeHelper.getSubtype().getCode())
				|| DxfFileConstants.CNG_MOTHER_STATION.equals(typeHelper.getSubtype().getCode())) {
			flage = true;
		}
		return flage;
	}

	public Map<String, BigDecimal> getOccupancyValues() {

		Map<String, BigDecimal> plotAreaValues = new HashMap<>();
//        plotAreaValues.put(F_RT, THREE_ZERO);
//        plotAreaValues.put(M_NAPI, THREE_ZERO);
//        plotAreaValues.put(F_CB, THREE_ZERO);
//        plotAreaValues.put(S_MCH, FIVE_ZERO);
//        plotAreaValues.put(E_PS, THREE_ZERO);
		return plotAreaValues;
	}
}