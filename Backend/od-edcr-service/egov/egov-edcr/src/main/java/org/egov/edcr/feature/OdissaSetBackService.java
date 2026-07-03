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
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.SetBack;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.NOCConstants;
import org.egov.edcr.od.NocAndDocumentsUtill;
import org.egov.edcr.od.OdishaUtill;
import org.springframework.stereotype.Service;

@Service
public class OdissaSetBackService extends FeatureProcess {
	private static final String MIN_FRONT_DESC = "Min Front setback";
	private static final String MIN_REAR_DESC = "Min Rear setback";
	private static final String MIN_SIDE1_DESC = "Min Side1 setback";
	private static final String MIN_SIDE2_DESC = "Min Side2 setback";
	
	private static final String FRONT_OVRD_DESC = "Front setback override";
	private static final String REAR_OVRD_DESC = "Rear setback override";
	private static final String SIDE1_OVRD_DESC = "Left setback override";
	private static final String SIDE2_OVRD_DESC = "Right setback override";
	
	private static final String SETBACK_POLYGON = "All setback polygons are drawn correctly (Each setback Polygon shall be touching the corners of Building Footprint and Plot Area)";
	
	private static final String TOTAL_CUMULATIVA_FRONT_AND_REAR_DESC = "Total cumulative Front and rear Setback";
	private static final String TOTAL_CUMULATIVA_SIDE_DESC = "Total cumulative side Setback";
	private static final String RULE_37_TWO_B = "37-2-B";

	private class SetBackData {
		String rule;
		String desc;
		Integer level;
		BigDecimal minFrontProvided = BigDecimal.ZERO;
		BigDecimal minFrontExpected = BigDecimal.ZERO;
		BigDecimal minRearProvided = BigDecimal.ZERO;
		BigDecimal minRearExpected = BigDecimal.ZERO;
		BigDecimal minSide1Provided = BigDecimal.ZERO;
		BigDecimal minSide1Expected = BigDecimal.ZERO;
		BigDecimal minSide2Provided = BigDecimal.ZERO;
		BigDecimal minSide2Expected = BigDecimal.ZERO;
		BigDecimal totalCumulativeFrontAndRearProvided = BigDecimal.ZERO;
		BigDecimal totalCumulativeFrontAndRearExpected = BigDecimal.ZERO;
		BigDecimal totalCumulativeSideProvided = BigDecimal.ZERO;
		BigDecimal totalCumulativeSideExpected = BigDecimal.ZERO;
	}

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {

		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		String serviceType = pl.getPlanInformation().getServiceType();
		String scrutinyDetailKey = "Setback";
		if(DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType)) {
			scrutinyDetailKey = "Setback - Proposed";
			setbackExisting(pl);
		}
		for (Block block : pl.getBlocks()) {
			if (!block.getCompletelyExisting()) {
				ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
				scrutinyDetail.addColumnHeading(1, RULE_NO);
				scrutinyDetail.addColumnHeading(2, DESCRIPTION);
				// scrutinyDetail.addColumnHeading(3, LEVEL);
				scrutinyDetail.addColumnHeading(3, PERMISSIBLE);
				scrutinyDetail.addColumnHeading(4, PROVIDED);
				scrutinyDetail.addColumnHeading(5, STATUS);
				scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + scrutinyDetailKey);

				if (!DxfFileConstants.OC_MIXED_USE.equals(occupancyTypeHelper.getType().getCode())) {
					occupancyTypeHelper = OdishaUtill.getMostRestrictiveBlockMaxBuildupArea(block);
				}

				for (SetBack setBack : block.getSetBacks()) {
					SetBackData data = prepareSetback(pl, block, setBack);

					if (block.isAssemblyBuilding()) {
						BigDecimal front = BigDecimal.ZERO;
						if (block.getBuilding().getTotalBuitUpArea().compareTo(new BigDecimal("1000")) <= 0)
							front = new BigDecimal("6");
						else
							front = new BigDecimal("12");

						if (front.compareTo(data.minFrontExpected) > 0)
							data.minFrontExpected = front;
						if (data.minRearExpected.compareTo(new BigDecimal("6")) < 0)
							data.minRearExpected = new BigDecimal("6");
						if (data.minSide1Expected.compareTo(new BigDecimal("6")) < 0)
							data.minSide1Expected = new BigDecimal("6");
						if (data.minSide2Expected.compareTo(new BigDecimal("6")) < 0)
							data.minSide2Expected = new BigDecimal("6");

						validateMinFrontSetBack(data, scrutinyDetail, serviceType);
						validateMinRearSetBack(data, scrutinyDetail, serviceType);
						validateMinSide1SetBack(data, scrutinyDetail, serviceType);
						validateMinSide2SetBack(data, scrutinyDetail, serviceType);

					} else if (DxfFileConstants.YES
							.equals(pl.getPlanInformation().getBuildingUnderHazardousOccupancyCategory())) {
						if (data.minFrontExpected.compareTo(new BigDecimal("6")) < 0)
							data.minFrontExpected = new BigDecimal("6");
						if (data.minRearExpected.compareTo(new BigDecimal("6")) < 0)
							data.minRearExpected = new BigDecimal("6");
						if (data.minSide1Expected.compareTo(new BigDecimal("6")) < 0)
							data.minSide1Expected = new BigDecimal("6");
						if (data.minSide2Expected.compareTo(new BigDecimal("6")) < 0)
							data.minSide2Expected = new BigDecimal("6");

						validateMinFrontSetBack(data, scrutinyDetail, serviceType);
						validateMinRearSetBack(data, scrutinyDetail, serviceType);
						validateMinSide1SetBack(data, scrutinyDetail, serviceType);
						validateMinSide2SetBack(data, scrutinyDetail, serviceType);

					} else {
						if (DxfFileConstants.OC_RESIDENTIAL.equals(occupancyTypeHelper.getType().getCode())
								|| DxfFileConstants.OC_PUBLIC_UTILITY.equals(occupancyTypeHelper.getType().getCode())
								|| DxfFileConstants.OC_EDUCATION.equals(occupancyTypeHelper.getType().getCode())
								|| DxfFileConstants.OC_TRANSPORTATION.equals(occupancyTypeHelper.getType().getCode())
								|| DxfFileConstants.OC_MIXED_USE.equals(occupancyTypeHelper.getType().getCode())
								|| DxfFileConstants.HOTEL.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.FIVE_STAR_HOTEL.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.MOTELS.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.SERVICES_FOR_HOUSEHOLDS
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.BANK.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.RESORTS.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.LAGOONS_AND_LAGOON_RESORT
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.AMUSEMENT_BUILDING_OR_PARK_AND_WATER_SPORTS
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.FINANCIAL_SERVICES_AND_STOCK_EXCHANGES
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.COLD_STORAGE_AND_ICE_FACTORY
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.COMMERCIAL_AND_BUSINESS_OFFICES_OR_COMPLEX
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.CONVENIENCE_AND_NEIGHBORHOOD_SHOPPING
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.PROFESSIONAL_OFFICES
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.DEPARTMENTAL_STORE
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.GAS_GODOWN.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.GUEST_HOUSES.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.HOLIDAY_RESORT.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.BOARDING_AND_LODGING_HOUSES
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.CNG_MOTHER_STATION
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.RESTAURANT.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.LOCAL_RETAIL_SHOPPING
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.SHOPPING_CENTER.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.SHOPPING_MALL.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.SHOWROOM.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.SUPERMARKETS.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.WHOLESALE_MARKET.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.MEDIA_CENTRES.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.FOOD_COURTS.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.WEIGH_BRIDGES.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.MERCENTILE.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.AGRICULTURE_FARM.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.AGRO_GODOWN.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.AGRO_RESEARCH_FARM
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.NURSERY_AND_GREEN_HOUSES
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.POLUTRY_DIARY_AND_SWINE_OR_GOAT_OR_HORSE
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.HORTICULTURE.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.SERI_CULTURE.equals(occupancyTypeHelper.getSubtype().getCode())) {
							applyGeneralCriterias(pl, data, scrutinyDetail, serviceType);
						} else if (DxfFileConstants.GODOWNS.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.GOOD_STORAGE.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.STORAGE_OR_HANGERS_OR_TERMINAL_DEPOT
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.WARE_HOUSE.equals(occupancyTypeHelper.getSubtype().getCode())) {
							List<String> nocList = NocAndDocumentsUtill.updateNoc(pl);
							if (nocList.contains(NOCConstants.FIRE_SERVICE_NOC)) {
								applyGeneralCriterias(pl, data, scrutinyDetail, serviceType);
							} else {
								BigDecimal allSetbackvalue = BigDecimal.ZERO;
								if (pl.getPlot().getArea().compareTo(new BigDecimal("500")) <= 0) {
									allSetbackvalue = new BigDecimal("3");
								} else {
									allSetbackvalue = new BigDecimal("4.5");
								}

								if (allSetbackvalue.compareTo(data.minFrontExpected) > 0)
									data.minFrontExpected = allSetbackvalue;

								if (allSetbackvalue.compareTo(data.minRearExpected) > 0)
									data.minRearExpected = allSetbackvalue;

								if (allSetbackvalue.compareTo(data.minSide1Expected) > 0)
									data.minSide1Expected = allSetbackvalue;

								if (allSetbackvalue.compareTo(data.minSide2Expected) > 0)
									data.minSide2Expected = allSetbackvalue;

								validateMinFrontSetBack(data, scrutinyDetail, serviceType);
								validateMinRearSetBack(data, scrutinyDetail, serviceType);
								validateMinSide1SetBack(data, scrutinyDetail, serviceType);
								validateMinSide2SetBack(data, scrutinyDetail, serviceType);

							}

						} else if (DxfFileConstants.PETROL_PUMP_FILLING_STATION_AND_SERVICE_STATION
								.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.PETROL_PUMP_ONLY_FILLING_STATION
										.equals(occupancyTypeHelper.getSubtype().getCode())) {
							data.minFrontExpected = new BigDecimal("6");
							data.minRearExpected = BigDecimal.ZERO;
							data.minSide1Expected = BigDecimal.ZERO;
							data.minSide2Expected = BigDecimal.ZERO;
							validateMinFrontSetBack(data, scrutinyDetail, serviceType);
							validateMinRearSetBack(data, scrutinyDetail, serviceType);
							validateMinSide1SetBack(data, scrutinyDetail, serviceType);
							validateMinSide2SetBack(data, scrutinyDetail, serviceType);
						} else if (DxfFileConstants.CINEMA.equals(occupancyTypeHelper.getSubtype().getCode())) {
							data.minFrontExpected = new BigDecimal("12");
							data.minRearExpected = new BigDecimal("6");
							data.minSide1Expected = new BigDecimal("6");
							data.minSide2Expected = new BigDecimal("6");
							validateMinFrontSetBack(data, scrutinyDetail, serviceType);
							validateMinRearSetBack(data, scrutinyDetail, serviceType);
							validateMinSide1SetBack(data, scrutinyDetail, serviceType);
							validateMinSide2SetBack(data, scrutinyDetail, serviceType);
						} else if (DxfFileConstants.MULTIPLEX.equals(occupancyTypeHelper.getSubtype().getCode())) {

							BigDecimal noOfSeat = block.getNumberOfOccupantsOrUsersOrBedBlk();

							if (noOfSeat.compareTo(new BigDecimal("400")) <= 0) {
								data.minFrontExpected = new BigDecimal("7.5");
								data.minRearExpected = new BigDecimal("6");
								data.minSide1Expected = new BigDecimal("6");
								data.minSide2Expected = new BigDecimal("6");
							} else {
								data.minFrontExpected = new BigDecimal("9");
								data.minRearExpected = new BigDecimal("6");
								data.minSide1Expected = new BigDecimal("6");
								data.minSide2Expected = new BigDecimal("6");
							}
							validateMinFrontSetBack(data, scrutinyDetail, serviceType);
							validateMinRearSetBack(data, scrutinyDetail, serviceType);
							validateMinSide1SetBack(data, scrutinyDetail, serviceType);
							validateMinSide2SetBack(data, scrutinyDetail, serviceType);

						} else if (DxfFileConstants.AUDITORIUM.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.BANQUET_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.CLUB.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.MUSIC_PAVILIONS.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.COMMUNITY_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.ORPHANAGE.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.OLD_AGE_HOME.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.SCIENCE_CENTRE_OR_MUSEUM
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.CONFERNCE_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.CONVENTION_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.SCULPTURE_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.CULTURAL_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.EXHIBITION_CENTER.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.GYMNASIA.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL
										.equals(occupancyTypeHelper.getType().getCode())) {
							BigDecimal buildingHeight = block.getBuilding().getBuildingHeight();
							BigDecimal tempSetBack = BigDecimal.ZERO;
							if (buildingHeight.compareTo(new BigDecimal("12")) <= 0) {
								tempSetBack = new BigDecimal("3");
								data.minFrontExpected = tempSetBack;
								data.minRearExpected = tempSetBack;
								data.minSide1Expected = tempSetBack;
								data.minSide2Expected = tempSetBack;
								validateMinFrontSetBack(data, scrutinyDetail, serviceType);
								validateMinRearSetBack(data, scrutinyDetail, serviceType);
								validateMinSide1SetBack(data, scrutinyDetail, serviceType);
								validateMinSide2SetBack(data, scrutinyDetail, serviceType);
							} else {
								generalCriterias(pl, block, data);
								validateMinFrontSetBack(data, scrutinyDetail, serviceType);
								validateMinRearSetBack(data, scrutinyDetail, serviceType);
								validateMinSide1SetBack(data, scrutinyDetail, serviceType);
								validateMinSide2SetBack(data, scrutinyDetail, serviceType);
							}
						} else if (DxfFileConstants.FARM_HOUSE.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.COUNTRY_HOMES.equals(occupancyTypeHelper.getSubtype().getCode())) {
							data.minFrontExpected = new BigDecimal("15");
							data.minRearExpected = new BigDecimal("9");
							data.minSide1Expected = new BigDecimal("9");
							data.minSide2Expected = new BigDecimal("9");

							validateMinFrontSetBack(data, scrutinyDetail, serviceType);
							validateMinRearSetBack(data, scrutinyDetail, serviceType);
							validateMinSide1SetBack(data, scrutinyDetail, serviceType);
							validateMinSide2SetBack(data, scrutinyDetail, serviceType);
						} else if (DxfFileConstants.INDUSTRIAL_BUILDINGS_FACTORIES_WORKSHOPS_ETC
								.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.NON_POLLUTING_INDUSTRIAL
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.SEZ_INDUSTRIAL.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.LOADING_OR_UNLOADING_SPACES
										.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.SMALL_FACTORIES_AND_ETC_FALLS_IN_INDUSTRIAL
										.equals(occupancyTypeHelper.getSubtype().getCode())) {
							List<String> nocList = NocAndDocumentsUtill.updateNoc(pl);
							if (!nocList.contains(NOCConstants.FIRE_SERVICE_NOC)) {
								generalCriterias(pl, block, data);
								validateMinFrontSetBack(data, scrutinyDetail, serviceType);
								validateMinRearSetBack(data, scrutinyDetail, serviceType);
								validateMinSide1SetBack(data, scrutinyDetail, serviceType);
								validateMinSide2SetBack(data, scrutinyDetail, serviceType);
							} else {
								BigDecimal buildingHeight = block.getBuilding().getBuildingHeight();
								BigDecimal tempSetback = BigDecimal.ZERO;
								if (buildingHeight.compareTo(new BigDecimal("15")) <= 0) {
									tempSetback = new BigDecimal("4.5");
								} else {
									// each 1m -> 0.25
									BigDecimal perOnemeterMul = new BigDecimal("0.25");
									BigDecimal additionalheightOtherThen15min = buildingHeight
											.subtract(new BigDecimal("15")).setScale(1, BigDecimal.ROUND_HALF_UP);
									int multvalue = additionalheightOtherThen15min.intValue();
									BigDecimal additionalValue = perOnemeterMul.multiply(new BigDecimal(multvalue + ""))
											.setScale(2, BigDecimal.ROUND_HALF_UP);
									tempSetback = tempSetback.add(additionalValue);
								}
								data.minFrontExpected = tempSetback;
								data.minRearExpected = tempSetback;
								data.minSide1Expected = tempSetback;
								data.minSide2Expected = tempSetback;

								validateMinFrontSetBack(data, scrutinyDetail, serviceType);
								validateMinRearSetBack(data, scrutinyDetail, serviceType);
								validateMinSide1SetBack(data, scrutinyDetail, serviceType);
								validateMinSide2SetBack(data, scrutinyDetail, serviceType);
							}
						} else if (DxfFileConstants.IT_ITES_BUILDINGS.equals(occupancyTypeHelper.getSubtype().getCode())
								|| DxfFileConstants.FLATTED_FACTORY
										.equals(occupancyTypeHelper.getSubtype().getCode())) {
							BigDecimal buildingHeight = block.getBuilding().getBuildingHeight();
							BigDecimal tempSetback = BigDecimal.ZERO;
							if (buildingHeight.compareTo(new BigDecimal("15")) <= 0) {
								tempSetback = new BigDecimal("4.5");
								data.minFrontExpected = tempSetback;
								data.minRearExpected = tempSetback;
								data.minSide1Expected = tempSetback;
								data.minSide2Expected = tempSetback;

								validateMinFrontSetBack(data, scrutinyDetail, serviceType);
								validateMinRearSetBack(data, scrutinyDetail, serviceType);
								validateMinSide1SetBack(data, scrutinyDetail, serviceType);
								validateMinSide2SetBack(data, scrutinyDetail, serviceType);
							} else {
								generalCriterias(pl, block, data);
								validateMinFrontSetBack(data, scrutinyDetail, serviceType);
								validateMinRearSetBack(data, scrutinyDetail, serviceType);
								validateMinSide1SetBack(data, scrutinyDetail, serviceType);
								validateMinSide2SetBack(data, scrutinyDetail, serviceType);
							}

						}

					}

				}

				pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
			}
		}
		
		for (Block block : pl.getBlocks()) {
			ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
			scrutinyDetail.addColumnHeading(1, RULE_NO);
			scrutinyDetail.addColumnHeading(2, DESCRIPTION);
			// scrutinyDetail.addColumnHeading(3, LEVEL);
			//scrutinyDetail.addColumnHeading(3, PERMISSIBLE);
//			scrutinyDetail.addColumnHeading(4, PROVIDED);
			scrutinyDetail.addColumnHeading(5, STATUS);
			scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "Setback polygon");
			
			Map<String, String> details = new HashMap<>();
			details.put(RULE_NO, "NA");
			details.put(DESCRIPTION, SETBACK_POLYGON);
			details.put(STATUS, Result.Verify.getResultVal());
			scrutinyDetail.getDetail().add(details);
			
			pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		}
		
		for (Block block : pl.getBlocks()) {
			ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
			scrutinyDetail.addColumnHeading(1, RULE_NO);
			scrutinyDetail.addColumnHeading(2, DESCRIPTION);
			// scrutinyDetail.addColumnHeading(3, LEVEL);
			//scrutinyDetail.addColumnHeading(3, PERMISSIBLE);
			scrutinyDetail.addColumnHeading(4, PROVIDED);
			scrutinyDetail.addColumnHeading(5, STATUS);
			scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "Setback Override Details");
			
			for (SetBack setBack : block.getSetBacks()) {
				
				SetBackData data = prepareOverrideSetback(pl, block, setBack);
				
				overrideReport(data.minFrontProvided, FRONT_OVRD_DESC, Result.Verify, scrutinyDetail);
				overrideReport(data.minRearProvided, REAR_OVRD_DESC, Result.Verify, scrutinyDetail);
				overrideReport(data.minSide1Provided, SIDE1_OVRD_DESC, Result.Verify, scrutinyDetail);
				overrideReport(data.minSide2Provided, SIDE2_OVRD_DESC, Result.Verify, scrutinyDetail);

			}
			
			pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		}
		
		return pl;
	}

	public void applyGeneralCriterias(Plan pl, SetBackData data, ScrutinyDetail scrutinyDetail,String serviceType) {
		if (pl.getPlanInformation().isLowRiskBuilding()) {
			validateMinFrontSetBack(data, scrutinyDetail,serviceType);
			validateTotalCumulativeFrontAndRearSetBack(data, scrutinyDetail,serviceType);
			validateTotalCumulativeSideSetBack(data, scrutinyDetail,serviceType);
		} else {
			validateMinFrontSetBack(data, scrutinyDetail,serviceType);
			validateMinRearSetBack(data, scrutinyDetail,serviceType);
			validateMinSide1SetBack(data, scrutinyDetail,serviceType);
			validateMinSide2SetBack(data, scrutinyDetail,serviceType);
		}
	}

	private SetBackData prepareSetback(Plan pl, Block block, SetBack setBack) {
		SetBackData setBackData = new SetBackData();

		setBackData.level = setBack.getLevel();
		if (setBack.getFrontYard() != null)
			setBackData.minFrontProvided = setBack.getFrontYard().getMinimumDistance();
		if (setBack.getRearYard() != null)
			setBackData.minRearProvided = setBack.getRearYard().getMinimumDistance();
		if (setBack.getSideYard1() != null)
			setBackData.minSide1Provided = setBack.getSideYard1().getMinimumDistance();
		if (setBack.getSideYard2() != null)
			setBackData.minSide2Provided = setBack.getSideYard2().getMinimumDistance();
		setBackData.totalCumulativeFrontAndRearProvided = setBackData.minFrontProvided.add(setBackData.minRearProvided);
		setBackData.totalCumulativeSideProvided = setBackData.minSide1Provided.add(setBackData.minSide2Provided);

		generalCriterias(pl, block, setBackData);

		return setBackData;
	}
	
	private SetBackData prepareOverrideSetback(Plan pl, Block block, SetBack setBack) {
		SetBackData setBackData = new SetBackData();

		setBackData.level = setBack.getLevel();
		if (setBack.getFrontYardOverride() != null)
			setBackData.minFrontProvided = setBack.getFrontYardOverride().getMinimumDistance();
		if (setBack.getRearYardOverride() != null)
			setBackData.minRearProvided = setBack.getRearYardOverride().getMinimumDistance();
		if (setBack.getSideYard1Override() != null)
			setBackData.minSide1Provided = setBack.getSideYard1Override().getMinimumDistance();
		if (setBack.getSideYard2Override() != null)
			setBackData.minSide2Provided = setBack.getSideYard2Override().getMinimumDistance();
		return setBackData;
	}

	private void generalCriterias(Plan pl, Block block, SetBackData setBackData) {
		boolean shopCumResidential=false;
		if (pl.getPlanInformation().getIsProjectComingUnderShopCumResidential() != null
				&& pl.getPlanInformation().getIsProjectComingUnderShopCumResidential().equalsIgnoreCase("YES")
				&& pl.getPlanInformation().getAreThePlotsAllotedInRow() != null
				&& pl.getPlanInformation().getAreThePlotsAllotedInRow().equalsIgnoreCase("YES")) {
			
			if(block.getDistFromExtWall()!=null 
					&& !block.getDistFromExtWall().isEmpty()
					&& block.getDistFromExtWall().get(0).compareTo(BigDecimal.TEN) == 0) {
				shopCumResidential=true;
			}
			
		}
		
		if (pl.getPlanInformation().isLowRiskBuilding()) {
			if (pl.getPlot().getArea().compareTo(new BigDecimal("115")) <= 0) {
				setBackData.minFrontExpected = new BigDecimal("1");
				setBackData.totalCumulativeFrontAndRearExpected = new BigDecimal("0");
				setBackData.totalCumulativeSideExpected = new BigDecimal("0");
			} else if (pl.getPlot().getArea().compareTo(new BigDecimal("115")) > 0
					&& pl.getPlot().getArea().compareTo(new BigDecimal("170")) <= 0) {
				setBackData.minFrontExpected = new BigDecimal("1");
				setBackData.totalCumulativeFrontAndRearExpected = new BigDecimal("2");
				setBackData.totalCumulativeSideExpected = new BigDecimal("0");
			} else if (pl.getPlot().getArea().compareTo(new BigDecimal("170")) > 0
					&& pl.getPlot().getArea().compareTo(new BigDecimal("225")) <= 0) {
				setBackData.minFrontExpected = new BigDecimal("1");
				setBackData.totalCumulativeFrontAndRearExpected = new BigDecimal("2");
				if(shopCumResidential)
					setBackData.totalCumulativeSideExpected = new BigDecimal("0");
				else	
					setBackData.totalCumulativeSideExpected = new BigDecimal("1.5");
			} else if (pl.getPlot().getArea().compareTo(new BigDecimal("225")) > 0
					&& pl.getPlot().getArea().compareTo(new BigDecimal("300")) <= 0) {
				setBackData.minFrontExpected = new BigDecimal("1.5");
				setBackData.totalCumulativeFrontAndRearExpected = new BigDecimal("2.5");
				if(shopCumResidential)
					setBackData.totalCumulativeSideExpected = new BigDecimal("0");
				else	
					setBackData.totalCumulativeSideExpected = new BigDecimal("2");				
			} else if (pl.getPlot().getArea().compareTo(new BigDecimal("300")) > 0
					&& pl.getPlot().getArea().compareTo(new BigDecimal("500")) <= 0) {
				setBackData.minFrontExpected = new BigDecimal("1.5");
				setBackData.totalCumulativeFrontAndRearExpected = new BigDecimal("3");
				if(shopCumResidential)
					setBackData.totalCumulativeSideExpected = new BigDecimal("0");
				else	
					setBackData.totalCumulativeSideExpected = new BigDecimal("3");	
			}
		} else {
			BigDecimal buildingHeight = BigDecimal.ZERO;
			buildingHeight = block.getBuilding().getBuildingHeight();

			if (buildingHeight.compareTo(new BigDecimal("12")) <= 0) {
				setBackData.minFrontExpected = new BigDecimal("2");
				setBackData.minRearExpected = new BigDecimal("2");
				if(shopCumResidential) {
					setBackData.minSide1Expected = new BigDecimal("0");
					setBackData.minSide2Expected = new BigDecimal("0");
				} else {
					setBackData.minSide1Expected = new BigDecimal("2");
					setBackData.minSide2Expected = new BigDecimal("2");
				}
				
			} else if (buildingHeight.compareTo(new BigDecimal("12")) > 0
					&& buildingHeight.compareTo(new BigDecimal("15")) < 0) {
				setBackData.minFrontExpected = new BigDecimal("3");
				setBackData.minRearExpected = new BigDecimal("3");
				if(shopCumResidential) {
					setBackData.minSide1Expected = new BigDecimal("0");
					setBackData.minSide2Expected = new BigDecimal("0");
				} else {
					setBackData.minSide1Expected = new BigDecimal("3");
					setBackData.minSide2Expected = new BigDecimal("3");
				}
			} else if (buildingHeight.compareTo(new BigDecimal("15")) >= 0
					&& buildingHeight.compareTo(new BigDecimal("18")) <= 0) {
				setBackData.minFrontExpected = new BigDecimal("4.5");
				setBackData.minRearExpected = new BigDecimal("4.5");
				if(shopCumResidential) {
					setBackData.minSide1Expected = new BigDecimal("0");
					setBackData.minSide2Expected = new BigDecimal("0");
				} else {
					setBackData.minSide1Expected = new BigDecimal("4.5");
					setBackData.minSide2Expected = new BigDecimal("4.5");
				}
			} else if (buildingHeight.compareTo(new BigDecimal("18")) > 0
					&& buildingHeight.compareTo(new BigDecimal("40")) <= 0) {
				setBackData.minFrontExpected = new BigDecimal("6");
				setBackData.minRearExpected = new BigDecimal("6");
				if(shopCumResidential) {
					setBackData.minSide1Expected = new BigDecimal("0");
					setBackData.minSide2Expected = new BigDecimal("0");
				} else {
					setBackData.minSide1Expected = new BigDecimal("6");
					setBackData.minSide2Expected = new BigDecimal("6");
				}
			} else {
				setBackData.minFrontExpected = new BigDecimal("9");
				setBackData.minRearExpected = new BigDecimal("9");
				if(shopCumResidential) {
					setBackData.minSide1Expected = new BigDecimal("0");
					setBackData.minSide2Expected = new BigDecimal("0");
				} else {
					setBackData.minSide1Expected = new BigDecimal("9");
					setBackData.minSide2Expected = new BigDecimal("9");
				}
			}

		}
	}

	private void validateMinFrontSetBack(SetBackData data, ScrutinyDetail scrutinyDetail, String serviceType) {
		if (DxfFileConstants.NEW_CONSTRUCTION.equals(serviceType)) {
			if (data.minFrontProvided.compareTo(data.minFrontExpected) >= 0)
				setReport("ODA Rule 32(1), 33(2), 60(3,5), 63(10), 75(4)", MIN_FRONT_DESC, data.level.toString(), data.minFrontExpected.toString(),
						data.minFrontProvided.toString(), Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 32(1), 33(2), 60(3,5), 63(10), 75(4)", MIN_FRONT_DESC, data.level.toString(), data.minFrontExpected.toString(),
						data.minFrontProvided.toString(), Result.Not_Accepted, scrutinyDetail);
		} else if (DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType)) {
			if (data.minFrontProvided.compareTo(data.minFrontExpected) >= 0)
				setReport("ODA Rule 32(1), 33(2), 60(3,5), 63(10), 75(4)", MIN_FRONT_DESC, data.level.toString(), data.minFrontExpected.toString(),
						data.minFrontProvided.toString(), Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 32(1), 33(2), 60(3,5), 63(10), 75(4)", MIN_FRONT_DESC, data.level.toString(), data.minFrontExpected.toString(),
						data.minFrontProvided.toString(), Result.Not_Accepted, scrutinyDetail);
		}

	}

	private void validateMinRearSetBack(SetBackData data, ScrutinyDetail scrutinyDetail, String serviceType) {
		String expected = DxfFileConstants.NA;
		if (data.minRearExpected.compareTo(BigDecimal.ZERO) > 0)
			expected = data.minRearExpected.toString();
		if (DxfFileConstants.NEW_CONSTRUCTION.equals(serviceType)) {
			if (data.minRearProvided.compareTo(data.minRearExpected) >= 0)
				setReport("ODA Rule 32(1), 33(2), 60(3,5), 63(10), 75(4)", MIN_REAR_DESC, data.level.toString(), expected,
						data.minRearProvided.toString(), Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 32(1), 33(2), 60(3,5), 63(10), 75(4)", MIN_REAR_DESC, data.level.toString(), expected,
						data.minRearProvided.toString(), Result.Not_Accepted, scrutinyDetail);
		} else if (DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType)) {
			if (data.minRearProvided.compareTo(data.minRearExpected) >= 0)
				setReport("ODA Rule 32(1), 33(2), 60(3,5), 63(10), 75(4)", MIN_REAR_DESC, data.level.toString(), expected,
						data.minRearProvided.toString(), Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 32(1), 33(2), 60(3,5), 63(10), 75(4)", MIN_REAR_DESC, data.level.toString(), expected,
						data.minRearProvided.toString(), Result.Not_Accepted, scrutinyDetail);
		}
	}

	private void validateMinSide1SetBack(SetBackData data, ScrutinyDetail scrutinyDetail, String serviceType) {
		String expected = DxfFileConstants.NA;
		if (data.minSide1Expected.compareTo(BigDecimal.ZERO) > 0)
			expected = data.minSide1Expected.toString();
		if (DxfFileConstants.NEW_CONSTRUCTION.equals(serviceType)) {
			if (data.minSide1Provided.compareTo(data.minSide1Expected) >= 0)
				setReport("ODA Rule 33(1,2), 60(2,4,5,6), 63(10)", MIN_SIDE1_DESC, data.level.toString(), expected,
						data.minSide1Provided.toString(), Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 33(1,2), 60(2,4,5,6), 63(10)", MIN_SIDE1_DESC, data.level.toString(), expected,
						data.minSide1Provided.toString(), Result.Not_Accepted, scrutinyDetail);
		} else if (DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType)) {
			if (data.minSide1Provided.compareTo(data.minSide1Expected) >= 0)
				setReport("ODA Rule 33(1,2), 60(2,4,5,6), 63(10)", MIN_SIDE1_DESC, data.level.toString(), expected,
						data.minSide1Provided.toString(), Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 33(1,2), 60(2,4,5,6), 63(10)", MIN_SIDE1_DESC, data.level.toString(), expected,
						data.minSide1Provided.toString(), Result.Not_Accepted, scrutinyDetail);
		}
	}

	private void validateMinSide2SetBack(SetBackData data, ScrutinyDetail scrutinyDetail, String serviceType) {
		String expected = DxfFileConstants.NA;
		if (data.minSide2Expected.compareTo(BigDecimal.ZERO) > 0)
			expected = data.minSide2Expected.toString();
		if (DxfFileConstants.NEW_CONSTRUCTION.equals(serviceType)) {
			if (data.minSide2Provided.compareTo(data.minSide2Expected) >= 0)
				setReport("ODA Rule 33(1,2), 60(2,4,5,6), 63(10)", MIN_SIDE2_DESC, data.level.toString(), expected,
						data.minSide2Provided.toString(), Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 33(1,2), 60(2,4,5,6), 63(10)", MIN_SIDE2_DESC, data.level.toString(), expected,
						data.minSide2Provided.toString(), Result.Not_Accepted, scrutinyDetail);
		} else if (DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType)) {
			if (data.minSide2Provided.compareTo(data.minSide2Expected) >= 0)
				setReport("ODA Rule 33(1,2), 60(2,4,5,6), 63(10)", MIN_SIDE2_DESC, data.level.toString(), expected,
						data.minSide2Provided.toString(), Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 33(1,2), 60(2,4,5,6), 63(10)", MIN_SIDE2_DESC, data.level.toString(), expected,
						data.minSide2Provided.toString(), Result.Not_Accepted, scrutinyDetail);
		}
	}

	private void validateTotalCumulativeFrontAndRearSetBack(SetBackData data, ScrutinyDetail scrutinyDetail,
			String serviceType) {
		String expected = DxfFileConstants.NA;
		if (data.totalCumulativeFrontAndRearExpected.compareTo(BigDecimal.ZERO) > 0)
			expected = data.totalCumulativeFrontAndRearExpected.toString();
		if (DxfFileConstants.NEW_CONSTRUCTION.equals(serviceType)) {
			if (data.totalCumulativeFrontAndRearProvided.compareTo(data.totalCumulativeFrontAndRearExpected) >= 0)
				setReport("ODA Rule 32 (1)", TOTAL_CUMULATIVA_FRONT_AND_REAR_DESC, data.level.toString(), expected,
						data.totalCumulativeFrontAndRearProvided.toString(), Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 32 (1)", TOTAL_CUMULATIVA_FRONT_AND_REAR_DESC, data.level.toString(), expected,
						data.totalCumulativeFrontAndRearProvided.toString(), Result.Not_Accepted, scrutinyDetail);
		} else if (DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType)) {
			if (data.totalCumulativeFrontAndRearProvided.compareTo(data.totalCumulativeFrontAndRearExpected) >= 0)
				setReport("ODA Rule 32 (1)", TOTAL_CUMULATIVA_FRONT_AND_REAR_DESC, data.level.toString(), expected,
						data.totalCumulativeFrontAndRearProvided.toString(), Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 32 (1)", TOTAL_CUMULATIVA_FRONT_AND_REAR_DESC, data.level.toString(), expected,
						data.totalCumulativeFrontAndRearProvided.toString(), Result.Not_Accepted, scrutinyDetail);
		}

	}

	private void validateTotalCumulativeSideSetBack(SetBackData data, ScrutinyDetail scrutinyDetail,
			String serviceType) {
		String expected = DxfFileConstants.NA;
		if (data.totalCumulativeSideExpected.compareTo(BigDecimal.ZERO) > 0)
			expected = data.totalCumulativeSideExpected.toString();
		
		if (DxfFileConstants.NEW_CONSTRUCTION.equals(serviceType)) {
			if (data.totalCumulativeSideProvided.compareTo(data.totalCumulativeSideExpected) >= 0)
				setReport("ODA Rule 32 (1)", TOTAL_CUMULATIVA_SIDE_DESC, data.level.toString(), expected,
						data.totalCumulativeSideProvided.toString(), Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 32 (1)", TOTAL_CUMULATIVA_SIDE_DESC, data.level.toString(), expected,
						data.totalCumulativeSideProvided.toString(), Result.Not_Accepted, scrutinyDetail);
		} else if (DxfFileConstants.ADDITION_AND_ALTERATION.equals(serviceType)) {
			if (data.totalCumulativeSideProvided.compareTo(data.totalCumulativeSideExpected) >= 0)
				setReport("ODA Rule 32 (1)", TOTAL_CUMULATIVA_SIDE_DESC, data.level.toString(), expected,
						data.totalCumulativeSideProvided.toString(), Result.Accepted, scrutinyDetail);
			else
				setReport("ODA Rule 32 (1)", TOTAL_CUMULATIVA_SIDE_DESC, data.level.toString(), expected,
						data.totalCumulativeSideProvided.toString(), Result.Not_Accepted, scrutinyDetail);
		}
	}

	private void setReport(String ruleNo, String description, String level, String expected, String provided,
			Result result, ScrutinyDetail scrutinyDetail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, description);
		// details.put(LEVEL, level);
		details.put(PERMISSIBLE, expected);
		details.put(PROVIDED, provided);
		details.put(STATUS, result.getResultVal());
		scrutinyDetail.getDetail().add(details);
	}
	
	private void setReportOVRD(String ruleNo, String description, String provided,
			Result result, ScrutinyDetail scrutinyDetail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, description);
		// details.put(LEVEL, level);
		//details.put(PERMISSIBLE, expected);
		details.put(PROVIDED, provided);
		details.put(STATUS, result.getResultVal());
		scrutinyDetail.getDetail().add(details);
	}
	
	private void setbackExisting(Plan pl) {
		String scrutinyDetailKey = "Setback - Existing";
		ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		for(Block block:pl.getBlocks()) {
			String blkNumber = block.getNumber();
			scrutinyDetail.setKey("Block_" + blkNumber + "_" + scrutinyDetailKey);
			
			//Front
			String frontSetBack = getPlanInfoKey(pl, String.format(DxfFileConstants.SETBACK_FRONT_EXISTING, blkNumber));
			Map<String, String> frontDeatils = new HashMap<>();
			frontDeatils.put(RULE_NO,"ODA rule 4(3)");
			frontDeatils.put(DESCRIPTION, "Front setback");
			frontDeatils.put(PROVIDED, frontSetBack);
			frontDeatils.put(STATUS, Result.Verify.getResultVal());
			scrutinyDetail.getDetail().add(frontDeatils);
			
			//Rear
			String rearSetBack = getPlanInfoKey(pl, String.format(DxfFileConstants.SETBACK_REAR_EXISTING, blkNumber));
			Map<String, String> rearDeatils = new HashMap<>();
			rearDeatils.put(RULE_NO,"ODA rule 4(3)");
			rearDeatils.put(DESCRIPTION, "Rear setback");
			rearDeatils.put(PROVIDED, rearSetBack);
			rearDeatils.put(STATUS, Result.Verify.getResultVal());
			scrutinyDetail.getDetail().add(rearDeatils);
			
			//Left
			String leftSetBack = getPlanInfoKey(pl, String.format(DxfFileConstants.SETBACK_LEFT_EXISTING, blkNumber));
			Map<String, String> leftDeatils = new HashMap<>();
			leftDeatils.put(RULE_NO,"ODA rule 4(3)");
			leftDeatils.put(DESCRIPTION, "Left setback");
			leftDeatils.put(PROVIDED, leftSetBack);
			leftDeatils.put(STATUS, Result.Verify.getResultVal());
			scrutinyDetail.getDetail().add(leftDeatils);
			
			
			//Right
			String rightSetBack = getPlanInfoKey(pl, String.format(DxfFileConstants.SETBACK_RIGHT_EXISTING, blkNumber));
			Map<String, String> rightDeatils = new HashMap<>();
			rightDeatils.put(RULE_NO,"ODA rule 4(3)");
			rightDeatils.put(DESCRIPTION, "Right setback");
			rightDeatils.put(PROVIDED, rightSetBack);
			rightDeatils.put(STATUS, Result.Verify.getResultVal());
			scrutinyDetail.getDetail().add(rightDeatils);
			
		}
		
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}
	
	private String getPlanInfoKey(Plan pl,String key) {
		String value = DxfFileConstants.NA;
		String v = pl.getPlanInfoProperties().get(key);
		if(!DxfFileConstants.NA.equalsIgnoreCase(v)) {
			try {
				value = (new BigDecimal(v)).toString();
				value = v;
			}catch (Exception e) {
				pl.addError(key, key+" is invalid in planinfo layer.");
			}
		}
		return value;
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
	
	private void overrideReport(BigDecimal value, String description, Result result,
			ScrutinyDetail scrutinyDetail) {
		if (value != null && value.compareTo(BigDecimal.ZERO) > 0) {
	        setReportOVRD(RULE_37_TWO_B, description, value.toString(), result, scrutinyDetail);
	    }
	}
}
