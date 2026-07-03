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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.Room;
import org.egov.common.entity.edcr.RoomHeight;
import org.egov.common.entity.edcr.SanityHelper;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PublicWashroomService extends FeatureProcess {
	private static final Logger LOG = Logger.getLogger(PublicWashroomService.class);
	@Autowired
	private Sanitation sanitation;
	
	@Override
	public Plan validate(Plan pl) {

		List<Block> publicwashrooms = pl.getPublicWashroom();
		//LOG.info(publicwashrooms);
		if (isOCApplicable(pl) && pl.getPlot().getArea().compareTo(new BigDecimal("4046.86")) >= 0 && publicwashrooms.size() ==0) {
			pl.addError("publicwashroomNotAllowed", "publicwashroom is mandatory");
			return pl;
		}
		
		for (Block block : publicwashrooms) {
			if (block.getBuilding().getFloors().size() > 1) {
				pl.addError("publicwashroomFloor", "More than 1 floor is not allowed in publicwashroom");
				return pl;
			}
			for (Floor floor : block.getBuilding().getFloors()) {
				for (Occupancy occupancy : floor.getOccupancies()) {
					if (occupancy.getTypeHelper() != null && occupancy.getTypeHelper().getSubtype() != null
							&& !DxfFileConstants.PUBLIC_WASHROOMS.equals(occupancy.getTypeHelper().getSubtype().getCode())) {
						pl.addError("OtherOcInPublicWashroom", "No other Sub Occupancy allowed in publicwashroom block");
					}
				}
			}
		}

		return pl;
	}

	@Override
	public Plan process(Plan pl) {
//		System.out.println("file changed for public washroom");
//		boolean flage = true;
//		if(flage)
//			return pl;
		validate(pl);
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Public Washroom complex in front setback area");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		BigDecimal height = BigDecimal.ZERO;
		boolean lightAndVentilationPersent=false;
		List<Block> publicwashrooms = pl.getPublicWashroom();
		
		if(publicwashrooms!=null && publicwashrooms.size()>0) {
			List<Room> list=new ArrayList<>();
			List<BigDecimal> parapets=new ArrayList<>();
			BigDecimal parapetsHeight=BigDecimal.ZERO;
			for(Block block:publicwashrooms) {
				for(Floor floor:block.getBuilding().getFloors()) {
					list.addAll(floor.getRegularRooms());
				}
				if(block.getGenralParapets()!=null)
					parapets.addAll(block.getGenralParapets());
			}
				
			
			List<Room> rooms=getRegularRoom(pl, list);
			List<BigDecimal> list2=new ArrayList<>();
			for(Room r:rooms) {
				for(RoomHeight rh:r.getHeights()) {
					list2.add(rh.getHeight());
				}
				if(r.getLightAndVentilation().getMeasurements().size()>0)
					lightAndVentilationPersent=true;
			}
			
			try {
				height=list2.stream().reduce(BigDecimal::max).get();
				parapetsHeight=parapets.stream().reduce(BigDecimal::max).get();
			}catch (Exception e) {
				// TODO: handle exception
			}
			
		
			BigDecimal requiredHeight=new BigDecimal("2.8");
			BigDecimal requiredParapetHeight=new BigDecimal("1");
			
			if(publicwashrooms.size()>0) {
				//Height
				addDetails(scrutinyDetail, "ODA Rule 50(1,6)", "Max Room Clear Height", requiredHeight.toString(),
						height.toString(), height.compareTo(requiredHeight)<=0?Result.Accepted.getResultVal():Result.Not_Accepted.getResultVal());
			
			}
			
			//checking lightAndVentilation
			if(!lightAndVentilationPersent) {
				pl.addError("lightAndVentilation-publicwashroom", "lightAndVentilation is mandatory in public washroom");
			}
			
			//parapet max 1min
			if(parapets.size()>0)
			addDetails(scrutinyDetail, "ODA Rule 50(3)", "Parapet height", parapetsHeight.toString(),
					height.toString(), height.compareTo(requiredParapetHeight)<=0?Result.Accepted.getResultVal():Result.Not_Accepted.getResultVal());
		
			
			//validate Sanitation
			SanityHelper helper = new SanityHelper();
			helper.commonWash=1d;
			helper.urinal=2d;
			helper.maleWc=1d;
			helper.femaleWc=1d;
			helper.ruleNo.add(RULE_NO);
			
			for(Block block:pl.getPublicWashroom()) {
				sanitation.processSanity(pl, block, helper, scrutinyDetail);
			}
			
			pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		}
		
		return pl;
	}

	private List<Room> getRegularRoom(Plan pl, List<Room> rooms) {
		Set<String> allowedRooms=new HashSet();
		allowedRooms.add(DxfFileConstants.COLOR_PUBLIC_WASHROOM);
		List<Room> spcRoom=OdishaUtill.getRegularRoom(pl, rooms, allowedRooms);
		return spcRoom;
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
	
	private boolean isOCApplicable(Plan pl) {
		OccupancyTypeHelper occupancyTypeHelper=pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		boolean flage=true;
		if(DxfFileConstants.OC_RESIDENTIAL.equals(occupancyTypeHelper.getType().getCode())
			|| DxfFileConstants.OC_PUBLIC_UTILITY.equals(occupancyTypeHelper.getType().getCode())
			|| DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(occupancyTypeHelper.getType().getCode())
			|| DxfFileConstants.OC_AGRICULTURE.equals(occupancyTypeHelper.getType().getCode())
				)
			flage=false;
		return flage;
	}
//test comment
}
