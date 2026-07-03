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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.egov.common.entity.dcr.helper.OccupancyHelperDetail;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Lift;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.SanityDetails;
import org.egov.common.entity.edcr.SanityHelper;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.od.OdishaUtill;
import org.egov.edcr.utility.DcrConstants;
import org.springframework.stereotype.Service;

/**
 * @author mani
 */

@Service
public class Sanitation extends FeatureProcess {
	public static final String MSG_ERROR_MANDATORY = "msg.error.mandatory.object.not.defined";
	public static final String FEMALE = "Female ";
	public static final String MALE = "Male ";
	public static final String BLOCK = "Block ";
	public static final String SANITY_RULE_DESC = "Sanity facility for Occupancy ";
	public static final String NEWLINE = "\n";
	public static final String SANITATION = "Sanitation";
	public static final String BLOCK_U_S = "Block_";
	private static final String WITH = " with ";
	private static final String BLDG_PART_WATER_CLOSET = "Water Closet";
	private static final String BLDG_PART_SPECIAL_WATER_CLOSET = "Special Water Closet";
	private static final String BLDG_PART_URINAL = "Urinal";
	private static final String BLDG_DRINKING_WATER = "Driniking Water";
	private static final String BLDG_PART_BATHROOM = "Bath Room";
	private static final String MALE_BATH_WITH_WC = BLDG_PART_BATHROOM + WITH + BLDG_PART_WATER_CLOSET;
	private static final String BLDG_PART_WASHBASIN = "Wash Basin";
	private static final String MINIMUM_SIDE_DIMENSION_VIOLATED = "Minimum Side Dimension of {0} M violated";
	private static final String MINIMUM_AREA_DIMENSION_VIOLATED = "Minimum Area of {0} M violated";
	private static final String DIMESION_DESC_KEY = "msg.sanity.dimension.desc";
	private static final Logger LOG = Logger.getLogger(Sanitation.class);
	private static final String FEATURE_NAME = "Sanitary Detail";
	private static final String RULE_38_1 = "ODA Annexure VIII(4-v)";
	private static final String NOOFBEDS = "No Of Beds";
	public static final String RULE_55_12 = "55-12";
	public static final String RULE_40_A_4 = "40A-4";
	public static final String RULE_54_6 = "54-6";
	public static final BigDecimal MINAREAOFSPWC = BigDecimal.valueOf(2.625);
	public static final BigDecimal MINDIMENSIONOFSPWC = BigDecimal.valueOf(1.5);
	public static final String MINIMUM_AREA_SPWC = "2.625";
	public static final String MINIMUM_DIMENSION_SPWC = "1.5";

	@Override
	public Plan validate(Plan pl) {

		for (Block b : pl.getBlocks()) {
			if (!b.getCompletelyExisting()) {

				int totalSpecialWC = 0;
				int totalWashBasins = 0;
				for (Floor f : b.getBuilding().getFloors()) {
					totalSpecialWC += f.getSpecialWaterClosets().size();
					totalWashBasins += f.getWashBasins().size();
				}
				b.getSanityDetails().setTotalSPWC(totalSpecialWC);
				b.getSanityDetails().setTotalwashBasins(totalWashBasins);
				List<Occupancy> occupancies = b.getBuilding().getTotalArea();
				SanityDetails sanityDetails = b.getSanityDetails();
				validateDimensions(pl, b, sanityDetails);
				for (Occupancy occupancy : occupancies) {
					OccupancyHelperDetail o = occupancy.getTypeHelper().getSubtype() != null
							? occupancy.getTypeHelper().getSubtype()
							: occupancy.getTypeHelper().getType();
					if (o != null) {
						switch (o.getCode()) {
						case DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL:
						case DxfFileConstants.OC_EDUCATION:
						case DxfFileConstants.OC_TRANSPORTATION:
							List<Integer> colorCodes=new ArrayList<>();
							colorCodes.add(LiftService.COLOR_GENERAL_LEFT);
							colorCodes.add(LiftService.COLOR_SPECIAL_LEFT);
							boolean isLiftPersent=OdishaUtill.isLiftPersent(b, colorCodes);
							
							for(Floor floor:b.getBuilding().getFloors()) {
								if(floor.getNumber()>=0) {
									if(floor.getNumber()==0 && (floor.getSpecialWaterClosets()==null || floor.getSpecialWaterClosets().size()==0))
										pl.addError(BLDG_PART_SPECIAL_WATER_CLOSET, getLocaleMessage(MSG_ERROR_MANDATORY,
												FEATURE_NAME, BLDG_PART_SPECIAL_WATER_CLOSET, b.getNumber(),floor.getNumber()+""));
									else if(isLiftPersent && (floor.getSpecialWaterClosets()==null || floor.getSpecialWaterClosets().size()==0))
										pl.addError(BLDG_PART_SPECIAL_WATER_CLOSET, getLocaleMessage(MSG_ERROR_MANDATORY,
												FEATURE_NAME, BLDG_PART_SPECIAL_WATER_CLOSET, b.getNumber(),floor.getNumber()+""));
									if(!isLiftPersent)
										break;
								}
							}
						}
					}
				}
			}
		}
		return pl;
	}

	private void validateBathRoom(Plan pl, Block b, SanityDetails sanityDetails) {
		if (sanityDetails.getMaleBathRooms().isEmpty() && sanityDetails.getFemaleBathRooms().isEmpty()
				&& sanityDetails.getMaleRoomsWithWaterCloset().isEmpty()
				&& sanityDetails.getFemaleRoomsWithWaterCloset().isEmpty()) {
			pl.addError(BLDG_PART_BATHROOM,
					getLocaleMessage(MSG_ERROR_MANDATORY, FEATURE_NAME, BLDG_PART_BATHROOM, b.getNumber()));
		}
	}

	private void commonSanitationValidations(Plan pl, Block b, SanityDetails sanityDetails,
			OccupancyHelperDetail type) {

		if (sanityDetails.getMaleWaterClosets().isEmpty() && sanityDetails.getFemaleWaterClosets().isEmpty()
				&& sanityDetails.getMaleRoomsWithWaterCloset().isEmpty()
				&& sanityDetails.getFemaleRoomsWithWaterCloset().isEmpty()) {
			pl.addError(BLDG_PART_WATER_CLOSET,
					getLocaleMessage(MSG_ERROR_MANDATORY, FEATURE_NAME, BLDG_PART_WATER_CLOSET, b.getNumber()));
		}

		if (sanityDetails.getUrinals().isEmpty()) {
			pl.addError(BLDG_PART_URINAL,
					getLocaleMessage(MSG_ERROR_MANDATORY, FEATURE_NAME, BLDG_PART_URINAL, b.getNumber()));
		}

//		if (!DxfFileConstants.F.equals(type.getCode()) && !DxfFileConstants.F_K.equals(type.getCode())
//				&& !DxfFileConstants.E.equals(type.getCode()) && sanityDetails.getTotalwashBasins() == 0) {
//			pl.addError(BLDG_PART_WASHBASIN,
//					getLocaleMessage(MSG_ERROR_MANDATORY, FEATURE_NAME, BLDG_PART_WASHBASIN, b.getNumber()));
//		}

		if (sanityDetails.getTotalSPWC() == 0)
			pl.addError(BLDG_PART_SPECIAL_WATER_CLOSET,
					getLocaleMessage(MSG_ERROR_MANDATORY, FEATURE_NAME, BLDG_PART_SPECIAL_WATER_CLOSET, b.getNumber()));

	}

	private void validateDimensions(Plan pl, Block b, SanityDetails sanityDetails) {
		if (!sanityDetails.getUrinals().isEmpty()) {
			int count = 0;
			for (Measurement m : sanityDetails.getUrinals()) {
				if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0) {
					count++;
				}
			}
			if (count > 0) {
				pl.addError("Sanity_" + BLDG_PART_URINAL + b.getNumber(), count + " number of " + BLDG_PART_URINAL
						+ " polyline not having only 4 points in block" + b.getNumber());

			}
		}
		List<Measurement> wcList = new ArrayList<>();
		wcList.addAll(sanityDetails.getMaleWaterClosets());
		wcList.addAll(sanityDetails.getFemaleWaterClosets());
		wcList.addAll(sanityDetails.getCommonWaterClosets());
		if (!wcList.isEmpty()) {
			int count = 0;
			for (Measurement m : wcList) {
				if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0) {
					count++;
				}
			}
			if (count > 0) {
				pl.addError("Sanity_" + BLDG_PART_WATER_CLOSET + b.getNumber(), count + " number of "
						+ BLDG_PART_WATER_CLOSET + " polyline not having only 4 points in block" + b.getNumber());

			}
		}

		List<Measurement> bath = new ArrayList<>();
		bath.addAll(sanityDetails.getMaleBathRooms());
		bath.addAll(sanityDetails.getFemaleBathRooms());
		bath.addAll(sanityDetails.getCommonBathRooms());

		List<Measurement> wcrList = new ArrayList<>();
		wcrList.addAll(sanityDetails.getMaleRoomsWithWaterCloset());
		wcrList.addAll(sanityDetails.getFemaleRoomsWithWaterCloset());
		wcrList.addAll(sanityDetails.getCommonRoomsWithWaterCloset());

		if (!bath.isEmpty()) {
			int count = 0;
			for (Measurement m : bath) {
				if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0) {
					count++;
				}
			}
			if (count > 0) {
				pl.addError("Sanity_" + BLDG_PART_BATHROOM + b.getNumber(), count + " number of " + BLDG_PART_BATHROOM
						+ " polyline not having only 4 points in block" + b.getNumber());

			}
		}

		if (!wcrList.isEmpty()) {
			int count = 0;
			for (Measurement m : wcrList) {
				if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0) {
					count++;
				}
			}

			if (count > 0) {
				pl.addError("Sanity_" + MALE_BATH_WITH_WC + b.getNumber(), count + " number of " + MALE_BATH_WITH_WC
						+ " polyline not having only 4 points in block" + b.getNumber());
			}
		}
	}

	@Override
	public Plan process(Plan pl) {

		OccupancyTypeHelper mostRestrictiveFarHelper = pl.getVirtualBuilding() != null
				? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
				: null;

		verifyDimesions(pl);
		checkCount(pl);
		return pl;
	}

	private Plan verifyDimesions(Plan pl) {
		return pl;
	}

	private ScrutinyDetail getNewScrutinyDetail(String key) {
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		scrutinyDetail.setKey(key);
		return scrutinyDetail;
	}
	
	private double getTotalDuInBlcok(Block block) {
		double noOfDu=0;
		for(Floor floor:block.getBuilding().getFloors()) {
			noOfDu=noOfDu+floor.getEwsUnit().size()+floor.getLigUnit().size()+floor.getMig1Unit().size()+floor.getMig2Unit().size()+floor.getOthersUnit().size();
		}
		return noOfDu;
	}

	private void checkCount(Plan pl) {

		// BigDecimal two=BigDecimal.valueOf(val)
		Boolean allStatus = true;
		Boolean accepted = true;
		for (Block b : pl.getBlocks()) {
			if (!b.getCompletelyExisting()) {

				LOG.debug("Starting  Sanitation of ....." + b.getNumber());

				ScrutinyDetail scrutinyDetail = getNewScrutinyDetail(BLOCK_U_S + b.getNumber() + "_" + SANITATION);
				SanityHelper helper = new SanityHelper();
				Map<Integer, Integer> requiredSpWcMap = new ConcurrentHashMap<>();
				Map<Integer, Integer> providedSpWcMap = new ConcurrentHashMap<>();
				Map<Integer, Integer> failedAreaSpWcMap = new ConcurrentHashMap<>();
				Map<Integer, Integer> failedDimensionSpWcMap = new ConcurrentHashMap<>();
				Double noOfPerson = OdishaUtill.getNumberOfPerson(pl).doubleValue();
				for (Occupancy type : b.getBuilding().getTotalArea()) {

					OccupancyHelperDetail o = type.getTypeHelper().getSubtype() != null
							? type.getTypeHelper().getSubtype()
							: type.getTypeHelper().getType();

					switch (o.getCode()) {

					case DxfFileConstants.APARTMENT_BUILDING:
					case DxfFileConstants.HOUSING_PROJECT:
					case DxfFileConstants.STUDIO_APARTMENTS:
					case DxfFileConstants.EWS:
					case DxfFileConstants.LOW_INCOME_HOUSING:
					case DxfFileConstants.MEDIUM_INCOME_HOUSING:
						double noOfDU=getTotalDuInBlcok(b);
						//need to write
						helper.commonWc=noOfDU;
						helper.commonBath=noOfDU;
						helper.commonBath=noOfDU;
						helper.ruleNo.add(RULE_38_1);
						break;
					case DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING:
					case DxfFileConstants.SEMI_DETACHED:
					case DxfFileConstants.ROW_HOUSING:
						float noOfOwner=0.0f;
						 try {
							 noOfOwner = pl.getPlanInfoProperties().get(DxfFileConstants.NO_OF_OWNER_FOR_RESIDENTIAL_BUILDING) != null
										? Float.parseFloat(
												pl.getPlanInfoProperties().get(DxfFileConstants.NO_OF_OWNER_FOR_RESIDENTIAL_BUILDING))
										: 1;
						 }catch (NumberFormatException e) {
							pl.addError("RESIDENTIAL_NO_OWNER", "RESIDENTIAL_NO_OWNER not defined in plan info layer");
							return;
						}
						double requiredWc = 0;
						if (noOfOwner == 1)
							requiredWc = 1;
						else {
							requiredWc = Math.ceil(noOfOwner / 2);
						}
						helper.commonWc = requiredWc;
						helper.commonBath = requiredWc;

						helper.ruleNo.add(RULE_38_1);
						break;
					case DxfFileConstants.SERVICES_FOR_HOUSEHOLDS:
					case DxfFileConstants.FINANCIAL_SERVICES_AND_STOCK_EXCHANGES:
					case DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX:
					case DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING:
					case DxfFileConstants.PROFESSIONAL_OFFICES:
					case DxfFileConstants.DEPARTMENTAL_STORE:
					case DxfFileConstants.LOCAL_RETAIL_SHOPPING:
					case DxfFileConstants.SHOWROOM:
					case DxfFileConstants.SUPERMARKETS:
					case DxfFileConstants.WHOLESALE_MARKET:
					case DxfFileConstants.MEDIA_CENTRES:
					case DxfFileConstants.WEIGH_BRIDGES:
					case DxfFileConstants.MERCENTILE:
						helper.maleWc = (1 / 25) * noOfPerson;
						helper.femaleWc = (1 / 15) * noOfPerson;
						// helper.commonWash = (1 / 25) * noOfPerson;

						double urinal = 0;
						if (noOfPerson >= 7 && noOfPerson <= 20)
							urinal = 1;
						else if (noOfPerson >= 21 && noOfPerson <= 45)
							urinal = 2;
						else if (noOfPerson >= 46 && noOfPerson <= 70)
							urinal = 3;
						else if (noOfPerson >= 71 && noOfPerson <= 100)
							urinal = 4;
						else if (noOfPerson >= 101 && noOfPerson <= 200)
							urinal = noOfPerson * 0.03;
						else
							urinal = noOfPerson * 0.025;
						helper.urinal = urinal;

						helper.drinkingWaterEachFloor = true;
						helper.drinkingWater = noOfPerson * 0.01;

						helper.ruleNo.add(RULE_38_1);
						break;
					case DxfFileConstants.HOTEL:
					case DxfFileConstants.FIVE_STAR_HOTEL:
					case DxfFileConstants.MOTELS:
					case DxfFileConstants.RESORTS:
					case DxfFileConstants.LAGOONS_AND_LAGOON_RESORT:
					case DxfFileConstants.GUEST_HOUSES:
					case DxfFileConstants.HOLIDAY_RESORT:
					case DxfFileConstants.BOARDING_AND_LODGING_HOUSES:
						helper.commonWc = noOfPerson * 0.125;
						helper.commonWash = noOfPerson * 0.1;
						helper.commonBath = noOfPerson * 0.1;
						helper.ruleNo.add(RULE_38_1);
						break;
					case DxfFileConstants.SHOPPING_CENTER:
					case DxfFileConstants.SHOPPING_MALL:
					case DxfFileConstants.FOOD_COURTS:
					case DxfFileConstants.AUDITORIUM:
					case DxfFileConstants.CINEMA:
					case DxfFileConstants.CLUB:
					case DxfFileConstants.MUSIC_PAVILIONS:
					case DxfFileConstants.COMMUNITY_HALL:
					case DxfFileConstants.SCIENCE_CENTRE_OR_MUSEUM:
					case DxfFileConstants.CONFERNCE_HALL:
					case DxfFileConstants.CONVENTION_HALL:
					case DxfFileConstants.SCULPTURE_COMPLEX:
					case DxfFileConstants.CULTURAL_COMPLEX:
					case DxfFileConstants.EXHIBITION_CENTER:
					case DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP:
					case DxfFileConstants.MULTIPLEX:
					case DxfFileConstants.MUSUEM:
					case DxfFileConstants.PUBLIC_LIBRARIES:
					case DxfFileConstants.RECREATION_BLDG:
					case DxfFileConstants.SPORTS_COMPLEX:
					case DxfFileConstants.STADIUM:
					case DxfFileConstants.THEATRE:

						if (noOfPerson > 400)
							helper.maleWc = noOfPerson * 0.01 + (noOfPerson > 400 ? ((noOfPerson - 400) * 0.04) : 0);
						else
							helper.maleWc = noOfPerson * 0.01;

						if (noOfPerson > 200)
							helper.femaleWc = noOfPerson * 0.04 + (noOfPerson > 200 ? ((noOfPerson - 200) * 0.02) : 0);
						else
							helper.femaleWc = noOfPerson * 0.04;

						helper.drinkingWater = noOfPerson * 0.01 * 2;
						helper.maleWash = noOfPerson * 0.005;
						helper.femaleWash = noOfPerson * 0.005;

						helper.urinal = noOfPerson * 0.02;

						helper.ruleNo.add(RULE_38_1);
						break;
					case DxfFileConstants.RESTAURANT:
					case DxfFileConstants.BANQUET_HALL:

						if (noOfPerson > 200)
							helper.maleWc = noOfPerson * 0.02 + (noOfPerson > 200 ? ((noOfPerson - 200) * 0.01) : 0);
						else
							helper.maleWc = noOfPerson * 0.02;

						if (noOfPerson > 200)
							helper.femaleWc = noOfPerson * 0.04 + (noOfPerson > 200 ? ((noOfPerson - 200) * 0.02) : 0);
						else
							helper.femaleWc = noOfPerson * 0.04;

						helper.maleWash = noOfPerson * 0.005;
						helper.femaleWash = noOfPerson * 0.005;
						helper.urinal = noOfPerson * 0.02;
						helper.ruleNo.add(RULE_38_1);
						break;

					case DxfFileConstants.BANK:
					case DxfFileConstants.FIRE_STATION:
					case DxfFileConstants.JAIL_OR_PRISON:
					case DxfFileConstants.POST_OFFICE:
					case DxfFileConstants.POLICE_STATION:
					case DxfFileConstants.BILL_COLLECTION_CENTER:
					case DxfFileConstants.ADMINISTRATIVE_BUILDINGS:
					case DxfFileConstants.GOVERNMENT_OFFICES:
					case DxfFileConstants.LOCAL_AND_SEMI_GOVERNMENT_OFFICES:
					case DxfFileConstants.POLICE_OR_ARMY_OR_BARRACK:
						
						helper.maleWc = noOfPerson * 0.04;
						helper.femaleWc = noOfPerson * 0.133;
						helper.drinkingWater = noOfPerson * 0.01 * 2;
						helper.drinkingWaterEachFloor = true;
						helper.maleWash = noOfPerson * 0.04;
						helper.femaleWash = noOfPerson * 0.04;

						if (noOfPerson >= 7 && noOfPerson <= 20)
							helper.urinal = 1d;
						else if (noOfPerson >= 21 && noOfPerson <= 45)
							helper.urinal = 2d;
						else if (noOfPerson >= 46 && noOfPerson <= 70)
							helper.urinal = 3d;
						else if (noOfPerson >= 71 && noOfPerson <= 100)
							helper.urinal = 4d;
						else if (noOfPerson >= 101 && noOfPerson <= 200)
							helper.urinal = noOfPerson * 0.03;
						else
							urinal = noOfPerson * 0.025;
						helper.ruleNo.add(RULE_38_1);
						helper.commonBath = 1d;
						processSpecialWaterCloset(b, requiredSpWcMap, providedSpWcMap, failedAreaSpWcMap,
								failedDimensionSpWcMap);
						break;
					case DxfFileConstants.CLINIC:
					case DxfFileConstants.DISPENSARY:
					case DxfFileConstants.YOGA_CENTER:
					case DxfFileConstants.DIAGNOSTIC_CENTRE:
					case DxfFileConstants.GOVT_SEMI_GOVT_HOSPITAL:
					case DxfFileConstants.HEALTH_CENTRE:
					case DxfFileConstants.HOSPITAL:
					case DxfFileConstants.LAB:
					case DxfFileConstants.MATERNITY_HOME:
					case DxfFileConstants.MEDICAL_BUILDING:
					case DxfFileConstants.NURSING_HOME:
					case DxfFileConstants.POLYCLINIC:
					case DxfFileConstants.REHABILITAION_CENTER:
					case DxfFileConstants.VETERINARY_HOSPITAL_FOR_PET_ANIMALS_AND_BIRDS:
						double noOfBed = Double
								.parseDouble(pl.getPlanInfoProperties().get(DxfFileConstants.NUMBER_OF_OCCUPANTS_OR_USERS_OR_BED_BLK.replace("%S", b.getNumber())));
						helper.commonWc = noOfBed * 0.125;
						helper.maleWc = noOfPerson * 0.01;
						helper.femaleWc = noOfPerson * 0.04;

						helper.commonWash = Math.ceil(2 + (noOfBed > 30 ? ((noOfBed - 30) * 0.033) : 0));
						helper.maleWash = noOfPerson * 0.01;
						helper.femaleWash = noOfPerson * 0.04;

						helper.urinal = noOfBed * 0.033;
						helper.urinal = helper.urinal + (noOfPerson * 0.02);

						processSpecialWaterCloset(b, requiredSpWcMap, providedSpWcMap, failedAreaSpWcMap,
								failedDimensionSpWcMap);
						helper.ruleNo.add(RULE_38_1);
						break;
					case DxfFileConstants.AUTO_STAND:
					case DxfFileConstants.METRO_STATION:
					case DxfFileConstants.BUS_STAND:
					case DxfFileConstants.BUS_TERMINAL:
					case DxfFileConstants.TAXI_STAND:
					case DxfFileConstants.MULTI_LEVEL_CAR_PARKING:
					case DxfFileConstants.PUBLIC_PARKING:
					case DxfFileConstants.TOLL_PLAZA:
					case DxfFileConstants.TRUCK_TERMINAL:
					case DxfFileConstants.ISBT:
					case DxfFileConstants.BROADCASTING_TRANSMISSION_CENTRE:
					case DxfFileConstants.BURIAL_AND_CREMATION_GROUNDS:
					case DxfFileConstants.PUBLIC_DISTRIBUTION_SYSTEM_SHOP:
					case DxfFileConstants.PUBLIC_TOILETS_IN_PUBLIC_AREA:
					case DxfFileConstants.PUBLIC_UTILITY_BLDG:
					case DxfFileConstants.SUB_STATION:
					case DxfFileConstants.TELECOMMUNICATION:
					case DxfFileConstants.WATER_PUMPING_STATIONS:
					case DxfFileConstants.SERVICE_AND_STORAGE_YARDS:
					case DxfFileConstants.ELECTRICAL_DISTRIBUTION_DEPOTS:
					case DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION:
					case DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION:
					case DxfFileConstants.CNG_MOTHER_STATION:
					case DxfFileConstants.PLACE_OF_WORKSHIP:
					case DxfFileConstants.RELIGIOUS_BUILDING:
					case DxfFileConstants.SOCIAL_AND_WELFARE_CENTRES:

						if (noOfPerson > 400)
							helper.maleWc = noOfPerson * 0.01 + (noOfPerson > 400 ? ((noOfPerson - 400) * 0.004) : 0);
						else
							helper.maleWc = noOfPerson * 0.001;

						if (noOfPerson > 200)
							helper.femaleWc = noOfPerson * 0.02 + (noOfPerson > 200 ? ((noOfPerson - 200) * 0.01) : 0);
						else
							helper.femaleWc = noOfPerson * 0.02;

						helper.maleWash = helper.maleWc;
						helper.femaleWash = helper.femaleWc;
						helper.urinal = noOfPerson * 0.02;
						helper.maleBath = noOfPerson * 0.1;
						helper.femaleBath = noOfPerson * 0.1;

						processSpecialWaterCloset(b, requiredSpWcMap, providedSpWcMap, failedAreaSpWcMap,
								failedDimensionSpWcMap);
						helper.ruleNo.add(RULE_38_1);
						break;

					case DxfFileConstants.PLAY_SCHOOL:
					case DxfFileConstants.CRECHE:
					case DxfFileConstants.NURSERY_SCHOOL:

						helper.maleWc = (noOfPerson * 0.066) / 2;//
						helper.femaleWc = (noOfPerson * 0.166) / 2;//
						helper.drinkingWater = noOfPerson * 0.02;
						helper.commonWash = noOfPerson * 0.066;
						helper.urinal = (noOfPerson * 0.0833) / 2;
						helper.commonBath = noOfPerson * 0.025;

						processSpecialWaterCloset(b, requiredSpWcMap, providedSpWcMap, failedAreaSpWcMap,
								failedDimensionSpWcMap);
						helper.ruleNo.add(RULE_38_1);
						break;

					case DxfFileConstants.RESEARCH_AND_TRAINING_INSTITUTE:
					case DxfFileConstants.COACHING_CENTRE:
					case DxfFileConstants.COMMERCIAL_INSTITUTE:
					case DxfFileConstants.COLLEGE:
					case DxfFileConstants.COMPUTER_TRAINING_INSTITUTE:
					case DxfFileConstants.PRIMARY_SCHOOL:
					case DxfFileConstants.HIGH_SCHOOL:
					case DxfFileConstants.SCHOOL_FOR_MENTALLY_CHALLENGED:
					case DxfFileConstants.ART_ACADEMY:
					case DxfFileConstants.TECHNICAL_COLLEGE:
					case DxfFileConstants.SPORTS_TRAINING_CENTERS:
					case DxfFileConstants.TRAINING_INSTITUTE:
					case DxfFileConstants.VETERINARY_INSTITUTE:
					case DxfFileConstants.MEDICAL_COLLEGE:
					case DxfFileConstants.RESEARCH_AND_TRAINING_CENTER:
									
						if (DxfFileConstants.YES.equals(pl.getPlanInfoProperties().get(DxfFileConstants.IS_BOARDING_FACILITY_PRESENT))) {
							helper.maleWc = (noOfPerson * 0.125) / 2;
							helper.femaleWc = (noOfPerson * 0.166) / 2;

							helper.maleWash = noOfPerson * 0.125;
							helper.femaleWash = noOfPerson * 0.125;

							helper.urinal = noOfPerson * 0.04;
							helper.maleBath = noOfPerson * 0.125;
							helper.femaleBath = noOfPerson * 0.166;

						} else {
							helper.maleWc = (noOfPerson * 0.025) / 2;
							helper.femaleWc = (noOfPerson * 0.04) / 2;

							helper.maleWash = noOfPerson * 0.0166;
							helper.femaleWash = noOfPerson * 0.025;

							helper.urinal = noOfPerson * 0.05;
						}

						processSpecialWaterCloset(b, requiredSpWcMap, providedSpWcMap, failedAreaSpWcMap,
								failedDimensionSpWcMap);
						helper.ruleNo.add(RULE_38_1);
						break;
					case DxfFileConstants.DHARMASALA:
					case DxfFileConstants.DORMITORY:
					case DxfFileConstants.HOSTEL:
					case DxfFileConstants.SHELTER_HOUSE:
					case DxfFileConstants.STAFF_QAURTER:
					case DxfFileConstants.ORPHANAGE:
					case DxfFileConstants.OLD_AGE_HOME:
					case DxfFileConstants.HOSTEL_CAPTIVE:
						helper.maleWc = (noOfPerson * 0.125) / 2;
						helper.femaleWc = (noOfPerson * 0.166) / 2;

						helper.maleWash = noOfPerson * 0.125;
						helper.femaleWash = noOfPerson * 0.125;

						helper.urinal = noOfPerson * 0.04;
						helper.maleBath = noOfPerson * 0.125;
						helper.femaleBath = noOfPerson * 0.166;

						processSpecialWaterCloset(b, requiredSpWcMap, providedSpWcMap, failedAreaSpWcMap,
								failedDimensionSpWcMap);
						helper.ruleNo.add(RULE_38_1);
						break;

					case DxfFileConstants.COLD_STORAGE_AND_ICE_FACTORY:
					case DxfFileConstants.GAS_GODOWN:
					case DxfFileConstants.GODOWNS:
					case DxfFileConstants.GOOD_STORAGE:
					case DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE:
					case DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE:
					case DxfFileConstants.STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT:
					case DxfFileConstants.WARE_HOUSE:
					case DxfFileConstants.INDUSTRIAL_BUILDINGS_FACTORIES_WORKSHOPS_ETC:
					case DxfFileConstants.NON_POLLUTING_INDUSTRIAL:
					case DxfFileConstants.IT_ITES_BUILDINGS:
					case DxfFileConstants.SEZ_INDUSTRIAL:
					case DxfFileConstants.LOADING_OR_UNLOADING_SPACES:
					case DxfFileConstants.FLATTED_FACTORY:
					case DxfFileConstants.SMALL_FACTORIES_AND_ETC_FALLS_IN_INDUSTRIAL:

						helper.maleWc = (1 / 25) * noOfPerson;
						helper.femaleWc = (1 / 15) * noOfPerson;
						// helper.commonWash = (1 / 25) * noOfPerson;

						double urinal1 = 0;
						if (noOfPerson >= 7 && noOfPerson <= 20)
							urinal1 = 1;
						else if (noOfPerson >= 21 && noOfPerson <= 45)
							urinal1 = 2;
						else if (noOfPerson >= 46 && noOfPerson <= 70)
							urinal1 = 3;
						else if (noOfPerson >= 71 && noOfPerson <= 100)
							urinal1 = 4;
						else if (noOfPerson >= 101 && noOfPerson <= 200)
							urinal1 = noOfPerson * 0.03;
						else
							urinal1 = noOfPerson * 0.025;
						helper.urinal = urinal1;

						helper.drinkingWaterEachFloor = true;
						helper.drinkingWater = noOfPerson * 0.01;

						helper.ruleNo.add(RULE_38_1);
						break;

//					case DxfFileConstants.ITH_H:
//
//						double noOfBed1 = Double
//								.parseDouble(pl.getPlanInfoProperties().get(DxfFileConstants.NO_OF_BEDS));
//						helper.commonWc = noOfBed1 * 0.125;
//						helper.maleWc = noOfPerson * 0.01;
//						helper.femaleWc = noOfPerson * 0.04;
//
//						helper.drinkingWater = Double
//								.parseDouble(pl.getPlanInfoProperties().get(DxfFileConstants.No_Of_WARD)) * 0.01;
//
//						helper.drinkingWater = helper.drinkingWater + (noOfPerson * 0.002);// male
//						helper.drinkingWater = helper.drinkingWater + (noOfPerson * 0.002);// female
//
//						helper.commonWash = Math.ceil(2 + (noOfBed1 > 30 ? ((noOfBed1 - 30) * 0.033) : 0));
//						helper.maleWash = noOfPerson * 0.01;
//						helper.femaleWash = noOfPerson * 0.04;
//
//						helper.urinal = noOfBed1 * 0.033;
//						helper.urinal = helper.urinal + (noOfPerson * 0.02);
//
//						processSpecialWaterCloset(b, requiredSpWcMap, providedSpWcMap, failedAreaSpWcMap,
//								failedDimensionSpWcMap);
//						helper.ruleNo.add(RULE_38_1);
//						break;

//					case DxfFileConstants.ITH_C:
//
//						helper.commonWc = noOfPerson * 0.125;
//						helper.commonWash = noOfPerson * 0.1;
//						helper.commonBath = noOfPerson * 0.1;
//						helper.ruleNo.add(RULE_38_1);
//						break;

//					case DxfFileConstants.ITH_R:
//					case DxfFileConstants.ITH_GH:
//
//						float noOfOwner1 = pl.getPlanInfoProperties().get(DxfFileConstants.RESIDENTIAL_NO_OWNER) != null
//								? Float.parseFloat(
//										pl.getPlanInfoProperties().get(DxfFileConstants.RESIDENTIAL_NO_OWNER))
//								: 1;
//
//						double requiredWc1 = 0;
//						if (noOfOwner1 == 1)
//							requiredWc1 = 1;
//						else {
//							requiredWc1 = Math.ceil(noOfOwner1 / 2);
//						}
//						helper.commonWc = requiredWc1;
//						helper.commonBath = requiredWc1;
//						helper.ruleNo.add(RULE_38_1);
//						break;

					}
					if (!accepted) {
						allStatus = false;
					}

				}
				for (Map.Entry<Integer, Integer> req : requiredSpWcMap.entrySet()) {
					helper.requiredSpecialWc += req.getValue();
				}
				for (Map.Entry<Integer, Integer> pro : providedSpWcMap.entrySet()) {
					helper.providedSpecialWc += pro.getValue();
				}
				for (Map.Entry<Integer, Integer> pro : failedAreaSpWcMap.entrySet()) {
					helper.failedAreaSpecialWc += pro.getValue();
				}
				for (Map.Entry<Integer, Integer> pro : failedDimensionSpWcMap.entrySet()) {
					helper.failedDimensionSpecialWc += pro.getValue();
				}

				if (helper.requiredSpecialWc > 0) {
					Set<String> ruleNo = new HashSet<>();
					ruleNo.add(RULE_38_1);
					if (helper.providedSpecialWc < helper.requiredSpecialWc) {
						addReportDetail(ruleNo, BLDG_PART_SPECIAL_WATER_CLOSET,
								String.valueOf(helper.requiredSpecialWc.intValue()),
								String.valueOf(helper.providedSpecialWc.intValue()), Result.Not_Accepted.getResultVal(),
								scrutinyDetail);
					} else {
						addReportDetail(ruleNo, BLDG_PART_SPECIAL_WATER_CLOSET,
								String.valueOf(helper.requiredSpecialWc.intValue()),
								String.valueOf(helper.providedSpecialWc.intValue()), Result.Accepted.getResultVal(),
								scrutinyDetail);
					}
					if (helper.failedAreaSpecialWc > 0 && helper.failedAreaSpecialWc <= helper.requiredSpecialWc) {
						addReportDetail(ruleNo, BLDG_PART_SPECIAL_WATER_CLOSET + " - Minimum Area", MINIMUM_AREA_SPWC,
								String.valueOf(helper.failedAreaSpecialWc.intValue()) + " not having area 2.625",
								Result.Not_Accepted.getResultVal(), scrutinyDetail);
					} else {
						addReportDetail(ruleNo, BLDG_PART_SPECIAL_WATER_CLOSET + " - Minimum Area", MINIMUM_AREA_SPWC,
								String.valueOf(
										helper.providedSpecialWc.intValue() - helper.failedAreaSpecialWc.intValue())
										+ " having area 2.625",
								Result.Accepted.getResultVal(), scrutinyDetail);
					}

					if (helper.failedDimensionSpecialWc > 0
							&& helper.failedDimensionSpecialWc <= helper.requiredSpecialWc) {
						addReportDetail(ruleNo, BLDG_PART_SPECIAL_WATER_CLOSET + " - Minimum Dimension",
								MINIMUM_DIMENSION_SPWC, String.valueOf(helper.failedDimensionSpecialWc.intValue())
										+ " not having dimension 1.5" + DxfFileConstants.METER,
								Result.Not_Accepted.getResultVal(), scrutinyDetail);
					} else {
						addReportDetail(ruleNo, BLDG_PART_SPECIAL_WATER_CLOSET + " - Minimum Dimension",
								MINIMUM_DIMENSION_SPWC,
								String.valueOf(helper.providedSpecialWc.intValue()
										- helper.failedDimensionSpecialWc.intValue()) + " having dimension 1.5"
										+ DxfFileConstants.METER,
								Result.Accepted.getResultVal(), scrutinyDetail);
					}

				}
				accepted = processSanity(pl, b, helper, scrutinyDetail);

				pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
				LOG.debug("Keys of the Sanitation Message ....." + scrutinyDetail.getKey() + "   "
						+ scrutinyDetail.getDetail().size());
			}
		}
	}

	public Boolean processSanity(Plan pl, Block b, SanityHelper helper, ScrutinyDetail detail) {

		Boolean accepted = true;
		String description = "";
		String expected = "";
		String actual = "";
		SanityDetails sanityDetails = b.getSanityDetails();
		helper.ruleNo.add("ODA Annexure I");

		if (helper.commonWc > 0) {
			int commonWcActual = sanityDetails.getCommonWaterClosets().size()
					+ sanityDetails.getCommonRoomsWithWaterCloset().size();

			Double totalWCActual = Math.ceil(commonWcActual);
			Double totalWCExpected = Math.ceil(helper.commonWc);
			if (totalWCExpected >= 0) {
				List<Measurement> wcList = new ArrayList<>();
				wcList.addAll(sanityDetails.getMaleWaterClosets());
				wcList.addAll(sanityDetails.getFemaleWaterClosets());
				wcList.addAll(sanityDetails.getCommonWaterClosets());

				expected = "" + totalWCExpected.intValue();
				actual = "" + totalWCActual.intValue();
				description = BLDG_PART_WATER_CLOSET + " Common - Count";
				if (totalWCExpected.intValue() > totalWCActual.intValue()) {
					addReportDetail(helper.ruleNo, description, expected, actual, Result.Not_Accepted.getResultVal(),
							detail);
				} else {
					addReportDetail(helper.ruleNo, description, expected, actual, Result.Accepted.getResultVal(),
							detail);

				}
			}
		}

		if (helper.maleWc > 0) {

			int maleWcActual = sanityDetails.getMaleWaterClosets().size()
					+ sanityDetails.getMaleRoomsWithWaterCloset().size();

			Double totalWCActual = Math.ceil(maleWcActual);
			Double totalWCExpected = Math.ceil(helper.maleWc);
			if (totalWCExpected >= 0) {
				List<Measurement> wcList = new ArrayList<>();
				wcList.addAll(sanityDetails.getMaleWaterClosets());
				wcList.addAll(sanityDetails.getFemaleWaterClosets());
				wcList.addAll(sanityDetails.getCommonWaterClosets());

				expected = "" + totalWCExpected.intValue();
				actual = "" + totalWCActual.intValue();
				description = BLDG_PART_WATER_CLOSET + " Male - Count";
				if (totalWCExpected.intValue() > totalWCActual.intValue()) {
					addReportDetail(helper.ruleNo, description, expected, actual, Result.Not_Accepted.getResultVal(),
							detail);
				} else {
					addReportDetail(helper.ruleNo, description, expected, actual, Result.Accepted.getResultVal(),
							detail);

				}
			}
		}

		if (helper.femaleWc > 0) {

			int femaleWcActual = sanityDetails.getFemaleWaterClosets().size()
					+ sanityDetails.getFemaleRoomsWithWaterCloset().size();

			Double totalWCActual = Math.ceil(femaleWcActual);
			Double totalWCExpected = Math.ceil(helper.femaleWc);
			if (totalWCExpected >= 0) {
				List<Measurement> wcList = new ArrayList<>();
				wcList.addAll(sanityDetails.getMaleWaterClosets());
				wcList.addAll(sanityDetails.getFemaleWaterClosets());
				wcList.addAll(sanityDetails.getCommonWaterClosets());
//				checkDimension(totalWCExpected.intValue(), detail, wcList, 1d, 1.1d, BLDG_PART_WATER_CLOSET,
//						DIMESION_DESC_KEY, RULE_38_1);

				expected = "" + totalWCExpected.intValue();
				actual = "" + totalWCActual.intValue();
				description = BLDG_PART_WATER_CLOSET + " Female - Count";
				if (totalWCExpected.intValue() > totalWCActual.intValue()) {
					addReportDetail(helper.ruleNo, description, expected, actual, Result.Not_Accepted.getResultVal(),
							detail);
				} else {
					addReportDetail(helper.ruleNo, description, expected, actual, Result.Accepted.getResultVal(),
							detail);

				}
			}
		}

		if (helper.urinal > 0) {
			helper.urinal = Math.ceil(helper.urinal);
			description = BLDG_PART_URINAL + " - Count";
			Integer urinalActual = sanityDetails.getUrinals().size();
			expected = "" + helper.urinal.intValue();
			actual = "" + urinalActual.intValue();
			if (helper.urinal.intValue() >= 0) {

				if (helper.urinal.intValue() > urinalActual.intValue()) {
					addReportDetail(helper.ruleNo, description, expected, actual, Result.Not_Accepted.getResultVal(),
							detail);
				} else {
					addReportDetail(helper.ruleNo, description, expected, actual, Result.Accepted.getResultVal(),
							detail);
				}
			}
		}

		if (helper.drinkingWater > 0) {
			helper.drinkingWater = Math.ceil(helper.drinkingWater);
			description = BLDG_DRINKING_WATER + " - Count";
			Integer urinalActual = sanityDetails.getDrinkingWater().size();
			expected = "" + helper.drinkingWater.intValue();
			actual = "" + urinalActual.intValue();
			if (helper.drinkingWater.intValue() >= 0) {

				if (helper.drinkingWater.intValue() > urinalActual.intValue()) {
					addReportDetail(helper.ruleNo, description, expected, actual, Result.Not_Accepted.getResultVal(),
							detail);
				} else {
					addReportDetail(helper.ruleNo, description, expected, actual, Result.Accepted.getResultVal(),
							detail);
				}

				if (helper.drinkingWaterEachFloor) {
					if (!DxfFileConstants.YES
							.equals(pl.getPlanInfoProperties().get(DxfFileConstants.IS_DRINKING_WATER_ON_EACH_FLOOR)))
						pl.addError("IS_DRINKING_WATER_ON_EACH_FLOOR",
								"IS_DRINKING_WATER_ON_EACH_FLOOR No defined in plan info");
				}
			}

		}

		if (helper.maleWash > 0 || helper.femaleWash > 0 || helper.commonWash > 0) {

			int actualWash = 0;
			for (Floor f : b.getBuilding().getFloors()) {
				actualWash += f.getWashBasins().size();
			}
			helper.ruleNo.add("ODA Rule 50(4), Annexure I");
			description = BLDG_PART_WASHBASIN + " - Count";
			Double totalWashExpected = Math.ceil(helper.maleWash + helper.femaleWash + helper.commonWash);
			expected = "" + totalWashExpected.intValue();
			actual = "" + actualWash;
			if (totalWashExpected.intValue() >= 0) {
				if (totalWashExpected.intValue() > actualWash) {
					addReportDetail(helper.ruleNo, description, expected, actual, Result.Not_Accepted.getResultVal(),
							detail);
				} else {
					addReportDetail(helper.ruleNo, description, expected, actual, Result.Accepted.getResultVal(),
							detail);
				}
			}
		}

		List<Measurement> commonBaths = new ArrayList<Measurement>();
		List<Measurement> maleBaths = new ArrayList<Measurement>();

		// new for separated male and common - start
		for (Measurement Bath : sanityDetails.getMaleBathRooms()) {
			if (Bath.getColorCode() == 3) {//
				commonBaths.add(Bath);
			} else if (Bath.getColorCode() == 1) {
				maleBaths.add(Bath);
			}
		}

		if (!commonBaths.isEmpty())
			sanityDetails.setCommonBathRooms(commonBaths);
		if (!maleBaths.isEmpty())
			sanityDetails.setMaleBathRooms(maleBaths);

		// new for separated male and common -end
		
		Set<String> ruleNoSet = new HashSet<>();
		ruleNoSet.add("NBC Norms");

		if (helper.commonBath > 0) {
			description = BLDG_PART_BATHROOM + " Common - Count";

			int commomBathActual = sanityDetails.getCommonBathRooms().size();

			int totalActualBath = commomBathActual;
			Double totalBathExpected = Math.ceil(helper.commonBath);

			expected = "" + totalBathExpected.intValue();
			actual = "" + totalActualBath;
			List<Measurement> wcList = new ArrayList<>();
			wcList.addAll(sanityDetails.getMaleBathRooms());
			wcList.addAll(sanityDetails.getFemaleBathRooms());
			wcList.addAll(sanityDetails.getCommonBathRooms());

			List<Measurement> wcrList = new ArrayList<>();
			wcrList.addAll(sanityDetails.getMaleRoomsWithWaterCloset());
			wcrList.addAll(sanityDetails.getFemaleRoomsWithWaterCloset());
			wcrList.addAll(sanityDetails.getCommonRoomsWithWaterCloset());
			if (totalBathExpected.intValue() >= 0) {

				if (totalBathExpected.intValue() > totalActualBath) {
					addReportDetail(ruleNoSet, description, expected, actual, Result.Not_Accepted.getResultVal(),
							detail);
				} else {
					addReportDetail(ruleNoSet, description, expected, actual, Result.Accepted.getResultVal(),
							detail);

				}
			}
		}
		if (helper.maleBath > 0) {
			description = BLDG_PART_BATHROOM + " Male - Count";
			int maleBathActual = sanityDetails.getMaleBathRooms().size();

			int totalActualBath = maleBathActual;
			Double totalBathExpected = Math.ceil(helper.maleBath);

			expected = "" + totalBathExpected.intValue();
			actual = "" + totalActualBath;
			List<Measurement> wcList = new ArrayList<>();
			wcList.addAll(sanityDetails.getMaleBathRooms());
			wcList.addAll(sanityDetails.getFemaleBathRooms());
			wcList.addAll(sanityDetails.getCommonBathRooms());

			List<Measurement> wcrList = new ArrayList<>();
			wcrList.addAll(sanityDetails.getMaleRoomsWithWaterCloset());
			wcrList.addAll(sanityDetails.getFemaleRoomsWithWaterCloset());
			wcrList.addAll(sanityDetails.getCommonRoomsWithWaterCloset());
			if (totalBathExpected.intValue() >= 0) {

				if (totalBathExpected.intValue() > totalActualBath) {
					addReportDetail(ruleNoSet, description, expected, actual, Result.Not_Accepted.getResultVal(),
							detail);
				} else {
					addReportDetail(ruleNoSet, description, expected, actual, Result.Accepted.getResultVal(),
							detail);

				}
			}
		}
		if (helper.femaleBath > 0) {
			description = BLDG_PART_BATHROOM + " female - Count";
			int femaleBathActual = sanityDetails.getFemaleBathRooms().size();

			int totalActualBath = femaleBathActual;
			Double totalBathExpected = Math.ceil(helper.femaleBath);

			expected = "" + totalBathExpected.intValue();
			actual = "" + totalActualBath;
			List<Measurement> wcList = new ArrayList<>();
			wcList.addAll(sanityDetails.getMaleBathRooms());
			wcList.addAll(sanityDetails.getFemaleBathRooms());
			wcList.addAll(sanityDetails.getCommonBathRooms());

			List<Measurement> wcrList = new ArrayList<>();
			wcrList.addAll(sanityDetails.getMaleRoomsWithWaterCloset());
			wcrList.addAll(sanityDetails.getFemaleRoomsWithWaterCloset());
			wcrList.addAll(sanityDetails.getCommonRoomsWithWaterCloset());
			if (totalBathExpected.intValue() >= 0) {
				checkDimension(totalBathExpected.intValue(), detail, wcList, 1.1d, 1.5d, BLDG_PART_BATHROOM,
						DIMESION_DESC_KEY, RULE_38_1);

				checkDimension(totalBathExpected.intValue(), detail, wcrList, 1.1d, 2.2d, MALE_BATH_WITH_WC,
						DIMESION_DESC_KEY, RULE_38_1);

				if (totalBathExpected.intValue() > totalActualBath) {
					addReportDetail(ruleNoSet, description, expected, actual, Result.Not_Accepted.getResultVal(),
							detail);
				} else {
					addReportDetail(ruleNoSet, description, expected, actual, Result.Accepted.getResultVal(),
							detail);

				}
			}
		}

		return accepted;
	}

	private boolean checkDimension(Integer required, ScrutinyDetail scrutinyDetail, List<Measurement> list,
			double minSide, double minArea, String type, String desc, String ruleNum) {
		if (!list.isEmpty()) {
			int wcNotMeetingSide = checkDimensionSide(list, minSide);
			int wcNotMeetingArea = checkDimensionArea(list, minArea);

			int totalSize = list.size();
			desc = type + "- Minimum Dimension";

			String expectedResult = minSide + " M";
			String actualResult = "";
			Set<String> ruleNo = new HashSet<>();
			ruleNo.add(ruleNum);
			if (totalSize - wcNotMeetingSide < required && wcNotMeetingSide > 0) {
				actualResult = wcNotMeetingSide + " not having " + expectedResult;
				addReportDetail(ruleNo, desc, expectedResult, actualResult, Result.Not_Accepted.getResultVal(),
						scrutinyDetail);

			} else {
				actualResult = totalSize + " having " + expectedResult;
				addReportDetail(ruleNo, desc, expectedResult, actualResult, Result.Accepted.getResultVal(),
						scrutinyDetail);
			}

			desc = type + "- Minimum Area";
			expectedResult = minArea + " M2";
			if (totalSize - wcNotMeetingArea < required && wcNotMeetingArea > 0) {
				actualResult = wcNotMeetingArea + " not having " + expectedResult;
				addReportDetail(ruleNo, desc, expectedResult, actualResult, Result.Not_Accepted.getResultVal(),
						scrutinyDetail);

			} else {
				actualResult = totalSize + "  having " + expectedResult;
				addReportDetail(ruleNo, desc, expectedResult, actualResult, Result.Accepted.getResultVal(),
						scrutinyDetail);
			}

		}
		return true;
	}

	private void addReportDetail(Set<String> ruleNo, String ruleDesc, String expected, String actual, String status,
			ScrutinyDetail detail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo.stream().map(String::new).collect(Collectors.joining(",")));
		details.put(DESCRIPTION, ruleDesc);
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status);
		detail.getDetail().add(details);

	}

	private int checkDimensionSide(List<Measurement> measurements, Double minValue) {
		int failedCount = 0;
		for (Measurement m : measurements) {
			if (minValue == 0) {

			}
			double minSide = m.getMinimumSide()
					.setScale(DcrConstants.ONE_DECIMALDIGITS_MEASUREMENTS, DcrConstants.ROUNDMODE_MEASUREMENTS)
					.doubleValue();
			if (minSide < minValue) {
				m.setIsValid(false);
				m.appendInvalidReason(String.format(MINIMUM_SIDE_DIMENSION_VIOLATED, minValue));
				failedCount++;
			}
		}
		return failedCount;
	}

	private int checkDimensionArea(List<Measurement> measurements, Double minValue) {
		int failedCount = 0;
		for (Measurement m : measurements) {

			double area = m.getArea()
					.setScale(DcrConstants.THREE_DECIMALDIGITS_MEASUREMENTS, DcrConstants.ROUNDMODE_MEASUREMENTS)
					.doubleValue();
			if (area < minValue) {
				m.setIsValid(false);
				m.appendInvalidReason(String.format(MINIMUM_AREA_DIMENSION_VIOLATED, minValue));
				failedCount++;
				LOG.debug("Area not matching is " + m.getArea());
			}
		}
		return failedCount;
	}

	private void processSpecialWaterCloset(Block block, Map<Integer, Integer> requiredSpWcMap,
			Map<Integer, Integer> providedSpWcMap, Map<Integer, Integer> failedAreaSpWcMap,
			Map<Integer, Integer> failedDimensionSpWcMap) {

		// check required spWC

		for (Floor f : block.getBuilding().getFloors()) {
			if (f.getBathRoomWaterClosets() != null && f.getBathRoomWaterClosets().getHeights() != null
					&& !f.getBathRoomWaterClosets().getHeights().isEmpty()
					&& f.getBathRoomWaterClosets().getRooms() != null
					&& !f.getBathRoomWaterClosets().getRooms().isEmpty())
				requiredSpWcMap.put(f.getNumber(), 1);
		}

		for (Floor f : block.getBuilding().getFloors()) {

			if (f.getSpecialWaterClosets().isEmpty()) {
				// not defined
			} else {
				if (providedSpWcMap.containsKey(f.getNumber()))
					continue;
				else
					providedSpWcMap.put(f.getNumber(), 1);
			}

			validateDimensionOfSPWC(f.getSpecialWaterClosets(), f.getNumber(), failedAreaSpWcMap,
					failedDimensionSpWcMap, providedSpWcMap);

		}
	}

	private void validateDimensionOfSPWC(List<Measurement> spwcs, int flrNo, Map<Integer, Integer> failedAreaSpWcMap,
			Map<Integer, Integer> failedDimensionSpWcMap, Map<Integer, Integer> providedSpWcMap) {

		Integer failedAreaCount = 0;
		Integer failedDimensionCount = 0;
		Integer providedSpecialWc = 0;

		for (Map.Entry<Integer, Integer> pro : providedSpWcMap.entrySet()) {
			providedSpecialWc += pro.getValue();
		}

		for (Measurement spwc : spwcs) {
			BigDecimal area = spwc.getArea().setScale(DcrConstants.THREE_DECIMALDIGITS_MEASUREMENTS,
					DcrConstants.ROUNDMODE_MEASUREMENTS);
			BigDecimal width = spwc.getWidth().setScale(DcrConstants.ONE_DECIMALDIGITS_MEASUREMENTS,
					DcrConstants.ROUNDMODE_MEASUREMENTS);
			if (area.compareTo(MINAREAOFSPWC.setScale(DcrConstants.THREE_DECIMALDIGITS_MEASUREMENTS,
					DcrConstants.ROUNDMODE_MEASUREMENTS)) < 0) {
				failedAreaCount++;
			}
			if (width.compareTo(MINDIMENSIONOFSPWC.setScale(DcrConstants.ONE_DECIMALDIGITS_MEASUREMENTS,
					DcrConstants.ROUNDMODE_MEASUREMENTS)) < 0) {
				failedDimensionCount++;
			}

			if (providedSpecialWc == failedAreaCount) {
				failedAreaSpWcMap.put(flrNo, failedAreaCount);
			}

			if (providedSpecialWc == failedDimensionCount) {
				failedDimensionSpWcMap.put(flrNo, failedAreaCount);
			}

		}

	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}