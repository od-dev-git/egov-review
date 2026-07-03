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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyPercentage;
import org.egov.common.entity.edcr.OccupancyType;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.OdishaUlbs;
import org.egov.edcr.od.OdishaUtill;
import org.egov.edcr.service.ProcessHelper;
import org.egov.edcr.utility.DcrConstants;
import org.kabeja.dxf.DXFConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class MeanOfAccess extends FeatureProcess {
	private static final Logger LOG = Logger.getLogger(MeanOfAccess.class);
	
	@Autowired
	private ProvisionService provisionService;

	public static final BigDecimal VAL_4000 = BigDecimal.valueOf(4000);
	private static final String ACCESS_WIDTH = "Access Width";
	private static final String SUBRULE_57_5 = "57-5";
	private static final String SUBRULE_58_3b = "58-3-b";
	private static final String SUBRULE_59_4 = "59-4";
	private static final String SUB_RULE_DESCRIPTION = "Minimum access width";
	public static final String OCCUPANCY = "occupancy";
	private static final String SUBRULE_33_1 = "ODA Rule 31(2,3,4), 63(2), 74(4-iv)";
	public static final BigDecimal VAL_300 = BigDecimal.valueOf(300);
	public static final BigDecimal VAL_600 = BigDecimal.valueOf(600);
	public static final BigDecimal VAL_1000 = BigDecimal.valueOf(1000);
	public static final BigDecimal VAL_8000 = BigDecimal.valueOf(8000);
	public static final BigDecimal VAL_18000 = BigDecimal.valueOf(18000);
	public static final BigDecimal VAL_24000 = BigDecimal.valueOf(24000);
	public static final BigDecimal VAL_1500 = BigDecimal.valueOf(1500);
	public static final BigDecimal VAL_6000 = BigDecimal.valueOf(6000);
	public static final BigDecimal VAL_12000 = BigDecimal.valueOf(12000);
	private static final String SUBRULE_116 = "116";
	private static final String SUB_RULE_DES = "Minimum access width";
	private static final String OCCPNCYCONDITION = "Occupancy/Condition";
	private static final String REMARKS = "Remarks";
	
	Set<String> allowedSubOccs = new HashSet<>(Arrays.asList(
		    DxfFileConstants.APARTMENT_BUILDING,
		    DxfFileConstants.HOUSING_PROJECT,
		    DxfFileConstants.STUDIO_APARTMENTS,
		    DxfFileConstants.LOW_INCOME_HOUSING,
		    DxfFileConstants.MEDIUM_INCOME_HOUSING,
		    DxfFileConstants.SHELTER_HOUSE,
		    DxfFileConstants.EWS
		));

	private BigDecimal expectedValue;

	
	
	@Override
	public Plan process(Plan pl) {
		scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Means of access");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);

		BigDecimal expectedValue = BigDecimal.ZERO;

		boolean isAssemblyBuilding = OdishaUtill.isAssemblyBuildingCriteria(pl);
		BigDecimal provided = pl.getPlanInformation().getTotalRoadWidth();
		
		boolean provisionFlag = false;
		
		Map<String, BigDecimal> map = new HashMap<>();
		
		OccupancyTypeHelper occupancyTypeHelperFromFar = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		OdishaUlbs ulb = OdishaUlbs.getUlb(pl.getThirdPartyUserTenantld());
		BigDecimal buildingHeight = OdishaUtill.getMaxBuildingHeight(pl);
		Set<BigDecimal> expectedValueSet = new HashSet<>();
		if(DxfFileConstants.OC_MIXED_USE.equals(occupancyTypeHelperFromFar.getType().getCode())) {
			if (ulb.isSparitFlag() && buildingHeight.compareTo(new BigDecimal("15")) < 0) {
				expectedValue = new BigDecimal("9");

			} else
				expectedValue = new BigDecimal("12");
			expectedValueSet.add(expectedValue);
		}else {			
			for (OccupancyTypeHelper occupancyTypeHelper : pl.getVirtualBuilding().getOccupancyTypes()) {
				if (DxfFileConstants.PUBLIC_WASHROOMS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.INFO_COMMS_SYSTEM.equals(occupancyTypeHelper.getSubtype().getCode())) {
					continue;
				}					
				if (DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING
						.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SEMI_DETACHED.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.ROW_HOUSING.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.DHARMASALA.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.DORMITORY.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.HOSTEL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SHELTER_HOUSE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.STAFF_QAURTER.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MOTELS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SERVICES_FOR_HOUSEHOLDS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.BANK.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RESORTS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.LAGOONS_AND_LAGOON_RESORT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.FINANCIAL_SERVICES_AND_STOCK_EXCHANGES
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.COLD_STORAGE_AND_ICE_FACTORY.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.PROFESSIONAL_OFFICES.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.DEPARTMENTAL_STORE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.GODOWNS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.GOOD_STORAGE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.GUEST_HOUSES.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.HOLIDAY_RESORT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.BOARDING_AND_LODGING_HOUSES.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.LOCAL_RETAIL_SHOPPING.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SHOWROOM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SUPERMARKETS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.WARE_HOUSE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.WHOLESALE_MARKET.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MEDIA_CENTRES.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.WEIGH_BRIDGES.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MERCENTILE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.FARM_HOUSE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.COUNTRY_HOMES.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.LOW_INCOME_HOUSING.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.EWS.equals(occupancyTypeHelper.getSubtype().getCode())) {

					expectedValue = new BigDecimal("6");

					int totalDuCount = getTotalDU(pl);
					if (!(DxfFileConstants.LOW_INCOME_HOUSING.equals(occupancyTypeHelper.getSubtype().getCode())
							|| DxfFileConstants.EWS.equals(occupancyTypeHelper.getSubtype().getCode()))
							&& totalDuCount > 8) {
						expectedValue = new BigDecimal("9");
					}
					map.put(occupancyTypeHelper.getSubtype().getCode(), expectedValue);
				} else if (DxfFileConstants.APARTMENT_BUILDING.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.HOUSING_PROJECT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.STUDIO_APARTMENTS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.PRIMARY_SCHOOL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.NURSERY_SCHOOL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.PLAY_SCHOOL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CRECHE.equals(occupancyTypeHelper.getSubtype().getCode())) {

					expectedValue = new BigDecimal("9");
					map.put(occupancyTypeHelper.getSubtype().getCode(), expectedValue);
				} else if (DxfFileConstants.HOTEL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.FIVE_STAR_HOTEL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.GAS_GODOWN.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CNG_MOTHER_STATION.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RESTAURANT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SHOPPING_CENTER.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SHOPPING_MALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.FOOD_COURTS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.AUDITORIUM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.BANQUET_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CLUB.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MUSIC_PAVILIONS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.COMMUNITY_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.ORPHANAGE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.OLD_AGE_HOME.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SCIENCE_CENTRE_OR_MUSEUM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CONFERNCE_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SCULPTURE_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CULTURAL_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.EXHIBITION_CENTER.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.GYMNASIA.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MUSUEM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.PLACE_OF_WORKSHIP.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.PUBLIC_LIBRARIES.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RECREATION_BLDG.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SPORTS_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.THEATRE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.ADMINISTRATIVE_BUILDINGS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.GOVERNMENT_OFFICES.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.LOCAL_AND_SEMI_GOVERNMENT_OFFICES
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.POLICE_OR_ARMY_OR_BARRACK.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RELIGIOUS_BUILDING.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SOCIAL_AND_WELFARE_CENTRES.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CLINIC.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.DISPENSARY.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.YOGA_CENTER.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.DIAGNOSTIC_CENTRE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.GOVT_SEMI_GOVT_HOSPITAL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.REGISTERED_TRUST.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.HEALTH_CENTRE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.HOSPITAL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.LAB.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MATERNITY_HOME.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MEDICAL_BUILDING.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.NURSING_HOME.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.POLYCLINIC.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.REHABILITAION_CENTER.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.VETERINARY_HOSPITAL_FOR_PET_ANIMALS_AND_BIRDS
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RESEARCH_AND_TRAINING_INSTITUTE
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.POLICE_STATION.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.FIRE_STATION.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.JAIL_OR_PRISON.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.POST_OFFICE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.OC_PUBLIC_UTILITY.equals(occupancyTypeHelper.getType().getCode())
						|| DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(occupancyTypeHelper.getType().getCode())
						|| DxfFileConstants.OC_EDUCATION.equals(occupancyTypeHelper.getType().getCode())
						|| DxfFileConstants.OC_TRANSPORTATION.equals(occupancyTypeHelper.getType().getCode())
						|| DxfFileConstants.OC_AGRICULTURE.equals(occupancyTypeHelper.getType().getCode())
						|| DxfFileConstants.OC_MIXED_USE.equals(occupancyTypeHelper.getType().getCode())) {

					if (ulb.isSparitFlag() && buildingHeight.compareTo(new BigDecimal("15")) < 0) {
						// System.out.println("Sparit implementation3:");
						expectedValue = new BigDecimal("9");

					} else
						expectedValue = new BigDecimal("12");
					map.put(occupancyTypeHelper.getSubtype().getCode(), expectedValue);
					
				} else if (DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION
						.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION
								.equals(occupancyTypeHelper.getSubtype().getCode())) {

					expectedValue = new BigDecimal("30");
					map.put(occupancyTypeHelper.getSubtype().getCode(), expectedValue);
				} else {
					if (isAssemblyBuilding) {
						expectedValue = new BigDecimal("18");

					} else {

						expectedValue = new BigDecimal("12");
					}
					
				}
				
				if (buildingHeight.compareTo(new BigDecimal("15")) >= 0) {
					expectedValue = new BigDecimal("12");
					
				}
				
				expectedValueSet.add(expectedValue);
			}
		}
		
		if (!DxfFileConstants.OC_MIXED_USE.equals(occupancyTypeHelperFromFar.getType().getCode())) {
			if (pl.getVirtualBuilding().getOccupancyTypes().stream().map(occ -> occ.getSubtype().getCode())
					.anyMatch(allowedSubOccs::contains)) {
				if (pl.getVirtualBuilding().getOccupancyTypes().stream().map(occ -> occ.getSubtype().getCode())
						.anyMatch(provisionService.getCommercialSubOccupancies()::contains)) {
					provisionFlag = true;
				}
			}
		}
		
		
		OccupancyPercentage occupancyPercentage = getMostOccupancyPercentage(
				pl.getPlanInformation().getOccupancyPercentages());
		
		if(!provisionFlag) {
			expectedValue = expectedValueSet.stream().reduce(BigDecimal::max).get();
		} else {
			expectedValue = map.get(occupancyPercentage.getSubOccupancyCode());
		}
		
		

		if (expectedValue.compareTo(new BigDecimal("6")) == 0) {// No need to validate

			Map<String, String> details = new HashMap<>();
			details.put(RULE_NO, SUBRULE_33_1);
			details.put(DESCRIPTION, SUB_RULE_DESCRIPTION);
			details.put(REQUIRED, expectedValue.toString());
			details.put(PROVIDED, provided != null ? provided.toString() : "");
			details.put(STATUS, Result.Verify.getResultVal());
			scrutinyDetail.getDetail().add(details);
			pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		} else {
			boolean status = false;
			if (provided != null)
				status = provided.compareTo(expectedValue) >= 0 ? true : false;

			Map<String, String> details = new HashMap<>();
			details.put(RULE_NO, SUBRULE_33_1);
			details.put(DESCRIPTION, SUB_RULE_DESCRIPTION);
			details.put(REQUIRED, expectedValue.toString());
			details.put(PROVIDED, provided != null ? provided.toString() : "");
			details.put(STATUS, status ? Result.Accepted.getResultVal() : Result.Not_Accepted.getResultVal());
			scrutinyDetail.getDetail().add(details);
			pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		}

		return pl;

	}

	public Plan validateAccessWidth(Plan pl) {
		HashMap<String, String> errors = new HashMap<>();
		if (pl.getPlanInformation() != null) {
			if (pl.getPlanInformation().getAccessWidth() == null) {
				errors.put(ACCESS_WIDTH, edcrMessageSource.getMessage(DcrConstants.OBJECTNOTDEFINED,
						new String[] { ACCESS_WIDTH }, LocaleContextHolder.getLocale()));
				pl.addErrors(errors);
			}
		}
		return pl;
	}

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

	private int getTotalDU(Plan pl) {
		int count = 0;
		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				if (floor.getNumber() > 2) {
					count = count + floor.getEwsUnit().size() + floor.getLigUnit().size() + floor.getMig1Unit().size()
							+ floor.getMig2Unit().size() + floor.getOthersUnit().size() + floor.getRoomUnit().size();
				}
			}
		}
		return count;
	}
	
	private OccupancyPercentage getMostOccupancyPercentage(
			Map<String, OccupancyPercentage> occupancyPercentages) {
		OccupancyPercentage occupancyPercentage = new OccupancyPercentage();
		for (Map.Entry<String, OccupancyPercentage> e : occupancyPercentages.entrySet()) {
			if (occupancyPercentage.getPercentage() == null
					|| occupancyPercentage.getPercentage().compareTo(e.getValue().getPercentage()) < 0)
				occupancyPercentage = e.getValue();
		}
		return occupancyPercentage;
	}
}
