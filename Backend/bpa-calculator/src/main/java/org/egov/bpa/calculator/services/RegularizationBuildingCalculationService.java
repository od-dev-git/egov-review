package org.egov.bpa.calculator.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.egov.bpa.calculator.utils.BPACalculatorConstants;
import org.egov.bpa.calculator.utils.RegularizationConstants;
import org.egov.bpa.calculator.utils.RegularizationUtils;
import org.egov.bpa.calculator.web.models.demand.Category;
import org.egov.bpa.calculator.web.models.demand.TaxHeadEstimate;
import org.egov.bpa.calculator.web.models.regularization.BlockFloor;
import org.egov.bpa.calculator.web.models.regularization.BuildingBlock;
import org.egov.bpa.calculator.web.models.regularization.BuildingOtherDetails;
import org.egov.bpa.calculator.web.models.regularization.BuildingRegularizationInfo;
import org.egov.bpa.calculator.web.models.regularization.CalculateFeeInfo;
import org.egov.bpa.calculator.web.models.regularization.LandRegularizationInfo;
import org.egov.bpa.calculator.web.models.regularization.Regularization;
import org.egov.bpa.calculator.web.models.regularization.RegularizationCalculationCriteria;
import org.egov.bpa.calculator.web.models.regularization.SubOcuupancy;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RegularizationBuildingCalculationService {
	
	@Autowired
	private RegularizationUtils utils;
	
	@Autowired
	private RegularizationService regularizationService;
	
	@Autowired
	private RegularizationLandCalculationService landCalculationService;
	
	
	
	/**
	 * Calculate fee and Estimates here for Building Regularization
	 * 
	 * @param requestInfo
	 * @param criteria
	 * @param estimates
	 * @param paramMap 
	 */
	public void calculateTotalFeeForBuilding(RequestInfo requestInfo, RegularizationCalculationCriteria criteria,
			List<TaxHeadEstimate> estimates, Map<String, Object> paramMap) {
		if (StringUtils.hasText(criteria.getFeeType()) && criteria.getFeeType().equalsIgnoreCase(RegularizationConstants.APPLICATION_FEE)) {
			BigDecimal buildingAppFee = calculateTotalBuildingScrutinyFee(requestInfo, criteria, estimates, paramMap);
			log.info("Total App FEE For:::" + criteria.getApplicationNo() + " is " + buildingAppFee);
		
		} else if (StringUtils.hasText(criteria.getFeeType()) && criteria.getFeeType().equalsIgnoreCase(RegularizationConstants.SANCTION_FEE)) {
			
			//For calculation of Permit Fee  refer from line 443
			BigDecimal compoundingFee = calculateTotalBuildingPermitFee(requestInfo, criteria, estimates, paramMap);
			log.info("Total Sanc FEE For:::" + criteria.getApplicationNo() + " is " + compoundingFee);
		}
		
	}


	//////////////////////////////////////////Application Fee Calculation Started //////////////////////////////////////

	/**
	 * Calculate BuildingAppFee For Type-A & Type-B
	 * 
	 * For Entire Building area is Unauthorized (Type-A)
	 * && Building area has Approved & Unauthorized area (Type-B)
	 * @param requestInfo 
	 * 
	 * @param criteria
	 * @param estimates
	 * @param paramMap 
	 * @return calculatedTotalAppFee
	 */
	public BigDecimal calculateTotalBuildingScrutinyFee(RequestInfo requestInfo, RegularizationCalculationCriteria criteria, List<TaxHeadEstimate> estimates, Map<String, Object> paramMap) {
		BigDecimal calculatedTotalAppFee = BigDecimal.ZERO;
		
		BigDecimal feeForDevelopmentOfLand = landCalculationService.calculateLandDevelopmentFee(criteria, estimates, paramMap);
		BigDecimal feeForBuildingOperation = calculateBuildingOperationFee(requestInfo, criteria, estimates, paramMap);
		
		calculatedTotalAppFee = calculatedTotalAppFee.add(feeForDevelopmentOfLand)
													 .add(feeForBuildingOperation)
													 .setScale(2, BigDecimal.ROUND_UP);
		
		log.info(" Total App fee for Appl :" + criteria.getApplicationNo() + " is " + calculatedTotalAppFee);

		return calculatedTotalAppFee;
	}



	/**
	 * Calculate BuildingOperationFee 
	 * @param requestInfo 
	 * 
	 * @param criteria
	 * @param estimates
	 * @param paramMap 
	 * @return feeForBuildingOperation
	 */
	private BigDecimal calculateBuildingOperationFee(RequestInfo requestInfo, RegularizationCalculationCriteria criteria,
			List<TaxHeadEstimate> estimates, Map<String, Object> paramMap) {
		
		BigDecimal feesForBuildingOperation = BigDecimal.ZERO;
		BigDecimal buildingOperationFeePaid = BigDecimal.ZERO;
		Boolean isReworkAppliation = false;
		
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.BUILDING_OPR_FEE_PAID_KEY))) {
			buildingOperationFeePaid = (BigDecimal)paramMap.get(RegularizationConstants.BUILDING_OPR_FEE_PAID_KEY);
		}
		
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.IS_REWORK_APP_KEY))) {
			isReworkAppliation = (Boolean) paramMap.get(RegularizationConstants.IS_REWORK_APP_KEY);
		}
		
		if(buildingOperationFeePaid.compareTo(BigDecimal.ZERO) > 0 && !isReworkAppliation) {
			feesForBuildingOperation = buildingOperationFeePaid;
		} else {

			List<SubOcuupancy> subOcuupancies = getAllSubOcuupancies(criteria.getRegularization().getBuildingRegularizationInfo());
			
			for(SubOcuupancy subOcuupancy : subOcuupancies) {
				BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
				Double totalBuiltUpArea = subOcuupancy.getTotalBuiltUpArea();
				String subOccupancyType = subOcuupancy.getValue();
				log.info("occupancy inside: " + subOccupancyType +" - "+subOcuupancy.getLabel());
				
				if (!ObjectUtils.isEmpty(totalBuiltUpArea)) {
					
					if ((subOccupancyType.startsWith(BPACalculatorConstants.A))) {
						feeForBuildingOperation = calculateBuildingOperationFeeForResidentialOccupancy(totalBuiltUpArea, paramMap);
					}
					if ((subOccupancyType.startsWith(BPACalculatorConstants.B))) {
						feeForBuildingOperation = calculateBuildingOperationFeeForCommercialOccupancy(totalBuiltUpArea, paramMap);
					}
					if ((subOccupancyType.startsWith(BPACalculatorConstants.C))) {
						feeForBuildingOperation = calculateBuildingOperationFeeForPublicSemiPublicInstitutionalOccupancy(totalBuiltUpArea, subOccupancyType, paramMap);
					}
					if ((subOccupancyType.startsWith(BPACalculatorConstants.D))) {
						feeForBuildingOperation = calculateBuildingOperationFeeForPublicUtilityOccupancy(totalBuiltUpArea, paramMap);
					}
					if ((subOccupancyType.startsWith(BPACalculatorConstants.E))) {
						feeForBuildingOperation = calculateBuildingOperationFeeForIndustrialZoneOccupancy(totalBuiltUpArea, paramMap);
					}
					if ((subOccupancyType.startsWith(BPACalculatorConstants.F))) {
						feeForBuildingOperation = calculateBuildingOperationFeeForEducationOccupancy(totalBuiltUpArea, paramMap);
					}
					if ((subOccupancyType.startsWith(BPACalculatorConstants.G))) {
						feeForBuildingOperation = calculateBuildingOperationFeeForTransportationOccupancy(totalBuiltUpArea, paramMap);
					}
					if ((subOccupancyType.startsWith(BPACalculatorConstants.H))) {
						feeForBuildingOperation = calculateBuildingOperationFeeForAgricultureOccupancy(totalBuiltUpArea, paramMap);
					}
				}
					
				log.info("FeeFrBuildingOperation:::::::::::" + feeForBuildingOperation);
				//Summation of fees
				feesForBuildingOperation = feesForBuildingOperation.add(feeForBuildingOperation);
				log.info("FeeFromBuildingOperation:::::::::::" + feesForBuildingOperation);
			}
		}
		utils.generateTaxHeadEstimate(estimates, feesForBuildingOperation, RegularizationConstants.TAXHEAD_REG_BUILDING_OPPERATION_FEE, Category.FEE);
		log.info("FeeForBuildingOperation:::::::::::" + feesForBuildingOperation);
		return feesForBuildingOperation;
	}



	/**
	 * Get All floors from All building blocks 
	 * 
	 * @param buildingRegularizationInfo
	 * @return List of floors details
	 */
	private List<SubOcuupancy> getAllSubOcuupancies(BuildingRegularizationInfo buildingRegularizationInfo) {
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			Map<String, SubOcuupancy> map = new HashMap<>();
			String jsonString = mapper.writeValueAsString(buildingRegularizationInfo.getBuildingBlocks());
			List<BuildingBlock> buildingBlocks = mapper.readValue(jsonString, new TypeReference<List<BuildingBlock>>(){});

			List<BlockFloor> allFloorsList = buildingBlocks.stream()
					.filter(buildingBlock -> ObjectUtils.isNotEmpty(buildingBlock))
					.map(buildingBlock -> buildingBlock.getFloors())
					.flatMap(List::stream)
					.collect(Collectors.toList());
			
			allFloorsList.forEach(floor -> {
				String key = floor.getSubOcuupancy().getValue();

				double asBuiltBUA = utils.getDoubleValue(floor.getAsBuiltBUA());
				double approvedBUA = utils.getDoubleValue(floor.getApprovedBUA());
				double carpetArea = utils.getDoubleValue(floor.getAsBuiltCarpetArea());

				double diff = asBuiltBUA - approvedBUA;
				double totalBuiltUpArea = diff >= 0 ? diff : 0; // if negative, use 0

				if (map.containsKey(key)) {
					SubOcuupancy subOcuupancy = map.get(key);

					// Accumulate values
					subOcuupancy.setTotalAsBuiltBUA(subOcuupancy.getTotalAsBuiltBUA() + asBuiltBUA);
					subOcuupancy.setTotalApprovedBUA(subOcuupancy.getTotalApprovedBUA() + approvedBUA);
					subOcuupancy.setTotalAsBuiltCarpetArea(subOcuupancy.getTotalAsBuiltCarpetArea() + carpetArea);

					// Floor-wise total built-up area difference
					subOcuupancy.setTotalBuiltUpArea(subOcuupancy.getTotalBuiltUpArea() + totalBuiltUpArea);

					log.info("SubOcuupancy (existing): AsBuiltBUA=" + asBuiltBUA + ", ApprovedBUA=" + approvedBUA
							+ ", Diff=" + diff + ", Set TotalBuiltUpArea=" + totalBuiltUpArea);

					map.put(key, subOcuupancy);
				} else {
					SubOcuupancy subOcuupancy = floor.getSubOcuupancy();

					// Initialize values
					subOcuupancy.setTotalAsBuiltBUA(asBuiltBUA);
					subOcuupancy.setTotalApprovedBUA(approvedBUA);
					subOcuupancy.setTotalAsBuiltCarpetArea(carpetArea);
					subOcuupancy.setTotalBuiltUpArea(totalBuiltUpArea);

					log.info("SubOcuupancy (new): AsBuiltBUA=" + asBuiltBUA + ", ApprovedBUA=" + approvedBUA + ", Diff="
							+ diff + ", Set TotalBuiltUpArea=" + totalBuiltUpArea);

					map.put(key, subOcuupancy);
				}
			});

			
			List<SubOcuupancy> subOcuupancies = new ArrayList<>(map.values());
			log.info("subOcuupancies >> " + subOcuupancies);
			log.info("subOcuupancies size >> " + subOcuupancies.size());
			log.info("Total Buildup Area >> " + subOcuupancies.stream().filter(Objects::nonNull).mapToDouble(SubOcuupancy::getTotalBuiltUpArea).sum());
			return subOcuupancies;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}



	/**
	 * Calculate BuildingOperationFee For Residential Occupancy
	 * i.e. For Occupancy Type A
	 * 
	 * @param totalBuiltUpArea
	 * @param paramMap 
	 * @return Fee for Residential Occupancy
	 */
	private BigDecimal calculateBuildingOperationFeeForResidentialOccupancy(Double totalBuiltUpArea, Map<String, Object> paramMap) {
		
		Boolean isSparit = null;
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.IS_SPARIT_KEY))) {
			isSparit = (Boolean) paramMap.get(RegularizationConstants.IS_SPARIT_KEY);
		}
		
		if(isSparit) {
			return calculateVariableFeeForSparitUlbs1(totalBuiltUpArea);
			
		} else {
			CalculateFeeInfo feeInfo = CalculateFeeInfo.builder()
					.totalBuiltUpArea(totalBuiltUpArea)
					.minCalculationPoint(RegularizationConstants.INT_HUNDRED)
					.maxCalculationPoint(RegularizationConstants.INT_THREE_HUNDRED)
					.minimumFee(RegularizationConstants.TWO_HUNDRED_FIFTY)
					.inBetweenFeePerUnit(RegularizationConstants.FIFTEEN)
					.maximumFeePerUnit(RegularizationConstants.TEN)
					.build();
			
			return utils.calculateVariableFee(feeInfo);
		}

	}

	

	/**
	 * Calculate BuildingOperationFee For Commercial Occupancy
	 * i.e. For Occupancy Type B
	 * 
	 * @param totalBuiltUpArea
	 * @param paramMap 
	 * @return Fee for Commercial Occupancy
	 */
	private BigDecimal calculateBuildingOperationFeeForCommercialOccupancy(Double totalBuiltUpArea, Map<String, Object> paramMap) {
		
		Boolean isSparit = null;
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.IS_SPARIT_KEY))) {
			isSparit = (Boolean) paramMap.get(RegularizationConstants.IS_SPARIT_KEY);
		}
		
		if(isSparit) {
			return calculateVariableFeeForSparitUlbs2(totalBuiltUpArea);
		} else {
			CalculateFeeInfo feeInfo = CalculateFeeInfo.builder()
					.totalBuiltUpArea(totalBuiltUpArea)
					.minCalculationPoint(RegularizationConstants.INT_TWENTY)
					.maxCalculationPoint(RegularizationConstants.INT_FIFTY)
					.minimumFee(RegularizationConstants.FIVE_HUNDRED)
					.inBetweenFeePerUnit(RegularizationConstants.FIFTY)
					.maximumFeePerUnit(RegularizationConstants.TWENTY)
					.build();
			
			return utils.calculateVariableFee(feeInfo);
		}
	}


	
	/**
	 * Calculate BuildingOperationFee For PublicSemiPublicInstitutional Occupancy
	 * i.e. For Occupancy Type C
	 * 
	 * @param totalBuiltUpArea , 
	 * @param paramMap 
	 * @return Fee for PublicSemiPublicInstitutional Occupancy
	 */
	private BigDecimal calculateBuildingOperationFeeForPublicSemiPublicInstitutionalOccupancy(Double totalBuiltUpArea , String subOccupancyType, Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		Boolean isSparit = null;
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.IS_SPARIT_KEY))) {
			isSparit = (Boolean) paramMap.get(RegularizationConstants.IS_SPARIT_KEY);
		}
		
		if (StringUtils.hasText(subOccupancyType)) {
			
			if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_A))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_B))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CL))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MP))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CH))) {
				if(isSparit) 
					feeForBuildingOperation = calculateVariableFeeForSparitUlbs2(totalBuiltUpArea);
				else 
					feeForBuildingOperation = calculateVariableFee(totalBuiltUpArea);
			} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_O))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_OAH))) {
				if(isSparit) 
					feeForBuildingOperation = calculateConstantSparitFee(totalBuiltUpArea);
				else 
					feeForBuildingOperation = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.COMMON_CONSTANT_PRICE_PER_UNIT);
				
			} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SC))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C1H))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C2H))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SCC))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CC))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_EC))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_G))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MH))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_ML))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_M))) {
				if(isSparit) 
					feeForBuildingOperation = calculateVariableFeeForSparitUlbs2(totalBuiltUpArea);
				else 
					feeForBuildingOperation = calculateVariableFee(totalBuiltUpArea);

			} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PW))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PL))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_REB))) {
				if(isSparit) 
					feeForBuildingOperation = calculateConstantSparitFee(totalBuiltUpArea);
				else
					feeForBuildingOperation = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.COMMON_CONSTANT_PRICE_PER_UNIT);

			} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SPC))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_S))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_T))) {
				if(isSparit)
					feeForBuildingOperation = calculateVariableFeeForSparitUlbs2(totalBuiltUpArea);	
				else
					feeForBuildingOperation = calculateVariableFee(totalBuiltUpArea);

			} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_AB))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GO))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_LSGO))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_P))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RB))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SWC))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CI))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_D))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_YC))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_DC))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GSGH))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RT))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_HC))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_H))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_L))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MTH))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MB))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_NH))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PLY))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RC))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_VHAB))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RTI))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PS))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_FS))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_J))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PO))) {
				if(isSparit) 
					feeForBuildingOperation = calculateConstantSparitFee(totalBuiltUpArea);
				else
					feeForBuildingOperation = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.COMMON_CONSTANT_PRICE_PER_UNIT);
			}
		}

		return feeForBuildingOperation;
	}
	
	
	
	/**
	 * @param totalBuiltUpArea
	 * @return calculated Fee
	 */
	private BigDecimal calculateVariableFee(Double totalBuiltUpArea) {
		CalculateFeeInfo feeInfo = CalculateFeeInfo.builder()
				.totalBuiltUpArea(totalBuiltUpArea)
				.minCalculationPoint(RegularizationConstants.INT_TWENTY)
				.maxCalculationPoint(RegularizationConstants.INT_FIFTY)
				.minimumFee(RegularizationConstants.FIVE_HUNDRED)
				.inBetweenFeePerUnit(RegularizationConstants.FIFTY)
				.maximumFeePerUnit(RegularizationConstants.TWENTY)
				.build();
		
		return utils.calculateVariableFee(feeInfo);
	}



	/**
	 * Calculate BuildingOperationFee For PublicUtility Occupancy
	 * i.e. For Occupancy Type D
	 * 
	 * @param totalBuiltUpArea
	 * @param paramMap 
	 * @return Fee for PublicUtility Occupancy
	 */
	private BigDecimal calculateBuildingOperationFeeForPublicUtilityOccupancy(Double totalBuiltUpArea, Map<String, Object> paramMap) {
		Boolean isSparit = null;
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.IS_SPARIT_KEY))) {
			isSparit = (Boolean) paramMap.get(RegularizationConstants.IS_SPARIT_KEY);
		}
		
		if(isSparit) {
			return calculateConstantSparitFee(totalBuiltUpArea);
		} else {
			return utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.COMMON_CONSTANT_PRICE_PER_UNIT);
		}
	}
	
	
	
	/**
	 * Calculate BuildingOperationFee For IndustrialZone Occupancy
	 * i.e. For Occupancy Type E
	 * 
	 * @param totalBuiltUpArea
	 * @param paramMap 
	 * @return Fee for IndustrialZone Occupancy
	 */
	private BigDecimal calculateBuildingOperationFeeForIndustrialZoneOccupancy(Double totalBuiltUpArea, Map<String, Object> paramMap) {
		Boolean isSparit = null;
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.IS_SPARIT_KEY))) {
			isSparit = (Boolean) paramMap.get(RegularizationConstants.IS_SPARIT_KEY);
		}
		
		if(isSparit) {
			CalculateFeeInfo feeInfo = CalculateFeeInfo.builder()
					.totalBuiltUpArea(totalBuiltUpArea)
					.minCalculationPoint(RegularizationConstants.INT_HUNDRED)
					.maxCalculationPoint(RegularizationConstants.INT_THREE_HUNDRED)
					.minimumFee(RegularizationConstants.SEVEN_HUNDRED_FIFTY)
					.inBetweenFeePerUnit(RegularizationConstants.TWELVE_POINT_FIVE)
					.maximumFeePerUnit(RegularizationConstants.SEVEN_POINT_FIVE)
					.build();
			return utils.calculateVariableFee(feeInfo);
			
		} else {
			CalculateFeeInfo feeInfo = CalculateFeeInfo.builder()
					.totalBuiltUpArea(totalBuiltUpArea)
					.minCalculationPoint(RegularizationConstants.INT_HUNDRED)
					.maxCalculationPoint(RegularizationConstants.INT_THREE_HUNDRED)
					.minimumFee(RegularizationConstants.FIFTEEN_HUNDRED)
					.inBetweenFeePerUnit(RegularizationConstants.TWENTY_FIVE)
					.maximumFeePerUnit(RegularizationConstants.FIFTEEN)
					.build();

			return utils.calculateVariableFee(feeInfo);
		}
	}

	
	
	/**
	 * Calculate BuildingOperationFee For Education Occupancy
	 * i.e. For Occupancy Type F
	 * 
	 * @param totalBuiltUpArea
	 * @param paramMap 
	 * @return Fee for Education Occupancy
	 */
	private BigDecimal calculateBuildingOperationFeeForEducationOccupancy(Double totalBuiltUpArea, Map<String, Object> paramMap) {
		Boolean isSparit = null;
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.IS_SPARIT_KEY))) {
			isSparit = (Boolean) paramMap.get(RegularizationConstants.IS_SPARIT_KEY);
		}
		
		if(isSparit) {
			return calculateConstantSparitFee(totalBuiltUpArea);
		} else {
			return utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.COMMON_CONSTANT_PRICE_PER_UNIT);
		}
		
	}



	
	/**
	 * Calculate BuildingOperationFee For Transportation Occupancy
	 * i.e. For Occupancy Type G
	 * 
	 * @param totalBuiltUpArea
	 * @param paramMap 
	 * @return Fee for Transportation Occupancy
	 */
	private BigDecimal calculateBuildingOperationFeeForTransportationOccupancy(Double totalBuiltUpArea, Map<String, Object> paramMap) {
		Boolean isSparit = null;
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.IS_SPARIT_KEY))) {
			isSparit = (Boolean) paramMap.get(RegularizationConstants.IS_SPARIT_KEY);
		}
		if(isSparit) {
			return calculateConstantSparitFee(totalBuiltUpArea);	
		} else {
			return utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.COMMON_CONSTANT_PRICE_PER_UNIT);
		}
		
	}


	
	/**
	 * Calculate BuildingOperationFee For Agriculture Occupancy
	 * i.e. For Occupancy Type H
	 * 
	 * @param totalBuiltUpArea
	 * @param paramMap 
	 * @return Fee for Agriculture Occupancy
	 */
	private BigDecimal calculateBuildingOperationFeeForAgricultureOccupancy(Double totalBuiltUpArea, Map<String, Object> paramMap) {
		Boolean isSparit = null;
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.IS_SPARIT_KEY))) {
			isSparit = (Boolean) paramMap.get(RegularizationConstants.IS_SPARIT_KEY);
		}
		if(isSparit) {
			return calculateVariableFeeForSparitUlbs1(totalBuiltUpArea);
		} else {
			CalculateFeeInfo feeInfo = CalculateFeeInfo.builder()
					.totalBuiltUpArea(totalBuiltUpArea)
					.minCalculationPoint(RegularizationConstants.INT_HUNDRED)
					.maxCalculationPoint(RegularizationConstants.INT_THREE_HUNDRED)
					.minimumFee(RegularizationConstants.TWO_HUNDRED_FIFTY)
					.inBetweenFeePerUnit(RegularizationConstants.FIFTEEN)
					.maximumFeePerUnit(RegularizationConstants.TEN)
					.build();
			
			return utils.calculateVariableFee(feeInfo);
		}
	}
	//////////////////////////////////////////Application Fee Calculation Ended /////////////////////////////////////////

	
	
	
	
	//////////////////////////////////////////Total Permit Fee Calculation Started //////////////////////////////////////

	/**
	 * Calculate Building Sanction Fee 
	 * @param requestInfo 
	 * 
	 * @param criteria
	 * @param estimates
	 * @param paramMap 
	 * @return calculated totalBuildingSanctionFee
	 */
	public BigDecimal calculateTotalBuildingPermitFee(RequestInfo requestInfo, RegularizationCalculationCriteria criteria,
			List<TaxHeadEstimate> estimates, Map<String, Object> paramMap) {

		BigDecimal calculatedTotalPermitFee = BigDecimal.ZERO;
		
		// Calculate Building Permit Fee
		BigDecimal sanctionFee = calculateSanctionFee(criteria, estimates, paramMap);
		BigDecimal constructionWorkerWelfareCess = calculateConstructionWorkerWelfareCess(criteria, estimates, paramMap);
		BigDecimal shelterFee = calculateShelterFee(criteria, estimates, paramMap);
		BigDecimal temporaryRetentionFee = calculateTemporaryRetentionFee(requestInfo, criteria, estimates);
		BigDecimal securityDeposit = calculateSecurityDeposit(criteria, estimates);
		BigDecimal purchasableFAR = calculatePurchasableFAR(criteria, estimates);
		BigDecimal eidpFee = calculateEIDPFee(criteria, estimates);
		
		//Compound fee calculation based on available criteria
		BigDecimal compoundingFarFee = calculateCompoundingFARFee(criteria, estimates);
		BigDecimal compoundingSetbackFee = calculateCompoundingSetbackFee(criteria, estimates);
		
		calculatedTotalPermitFee = (calculatedTotalPermitFee.add(sanctionFee).add(constructionWorkerWelfareCess)
				.add(shelterFee).add(temporaryRetentionFee).add(securityDeposit).add(purchasableFAR)
				.add(eidpFee).add(compoundingFarFee).add(compoundingSetbackFee))
				.setScale(2, BigDecimal.ROUND_UP);
		
		log.info(" Total Permit fee for Appl :" + criteria.getApplicationNo() + " is " + calculatedTotalPermitFee);
		return calculatedTotalPermitFee;
	}

	
	
	//////////////////////////////////////////Sanction Fee Calculation Started //////////////////////////////////////

	/**
	 * Calculate SanctionFee
	 * @param criteria
	 * @param estimates
	 * @param paramMap 
	 * @return sanctionFees
	 */
	private BigDecimal calculateSanctionFee(RegularizationCalculationCriteria criteria,
			List<TaxHeadEstimate> estimates, Map<String, Object> paramMap) {
		BigDecimal sanctionFees = BigDecimal.ZERO;
		Boolean isSparit = null;
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.IS_SPARIT_KEY))) {
			isSparit = (Boolean) paramMap.get(RegularizationConstants.IS_SPARIT_KEY);
		}
		
		List<SubOcuupancy> subOccupancies = getAllSubOcuupancies(criteria.getRegularization().getBuildingRegularizationInfo());
		
		for(SubOcuupancy subOcuupancy : subOccupancies) {
			BigDecimal sanctionFee = BigDecimal.ZERO;
			Double totalBuiltUpArea = subOcuupancy.getTotalBuiltUpArea();
			String subOccupancyType = subOcuupancy.getValue();
			log.info("occupancy inside: " + subOccupancyType);
			
			if (ObjectUtils.isNotEmpty(totalBuiltUpArea) && StringUtils.hasText(subOccupancyType)) {
				
				//Calculate Building SanctionFee For ResidentialOccupancy 
				if ((subOccupancyType.startsWith(BPACalculatorConstants.A))) {
					if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_P))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_S))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_R))) {
						if(isSparit)
							sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.SEVEN_POINT_FIVE);
						else
							sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.FIFTEEN);
					} else {
						if(isSparit)
							sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.TWENTY_FIVE);
						else
							sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.FIFTY);
					}
				}
				
				//Calculate Building SanctionFee For CommercialOccupancy 
				if ((subOccupancyType.startsWith(BPACalculatorConstants.B))) {
					if(isSparit)
						sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.THIRTY);
					else
						sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.SIXTY);
				}
				
				//Calculate Building SanctionFee For Public-SemiPublic-Institutional Occupancy 
				if ((subOccupancyType.startsWith(BPACalculatorConstants.C))) {
					if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_A))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_B))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CL))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MP))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CH))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_O))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_OAH))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SC))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C1H))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C2H))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SCC))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CC))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_EC))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_G))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MH))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_ML))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_M))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PW))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PL))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_REB))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SPC))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_S))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_T))) {

						if(isSparit)
							sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.FIFTEEN);
						else
							sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.THIRTY);

					} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_AB))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GO))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_LSGO))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_P))) {
						if(isSparit)
							sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.FIVE);
						else
							sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.TEN);
						
					} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SWC))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CI))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_D))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_YC))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_DC))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GSGH))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RT))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_HC))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_H))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_L))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MTH))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MB))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_NH))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PLY))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RC))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_VHAB))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_RTI))) {

						if(isSparit)
							sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.FIFTEEN);
						else
							sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.THIRTY);
						
					} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PS))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_FS))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_J))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PO))) {

						if(isSparit)
							sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.FIVE);
						else
							sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.TEN);
					}
				}
				
				//Calculate Building SanctionFee For PublicUtility Occupancy 
				if ((subOccupancyType.startsWith(BPACalculatorConstants.D))) {
					if(isSparit)
						sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.TEN);
					else
						sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.FIVE);
				}
				
				//Calculate Building SanctionFee For IndustrialZone Occupancy 
				if ((subOccupancyType.startsWith(BPACalculatorConstants.E))) {
					if(isSparit)
						sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.THIRTY);
					else
						sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.SIXTY);
				}
				
				//Calculate Building SanctionFee For Education Occupancy 
				if ((subOccupancyType.startsWith(BPACalculatorConstants.F))) {
					if(isSparit)
						sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.FIFTEEN);	
					else
						sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.THIRTY);
				}
				
				//Calculate Building SanctionFee For Transportation Occupancy 
				if ((subOccupancyType.startsWith(BPACalculatorConstants.G))) {
					if(isSparit)
						sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.FIVE);	
					else
						sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.TEN);
				}
				
				//Calculate Building SanctionFee For Agriculture Occupancy 
				if ((subOccupancyType.startsWith(BPACalculatorConstants.H))) {
					if(isSparit)
						sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.FIFTEEN);
					else
						sanctionFee = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.THIRTY);
				}
			}
			
			log.info("FeeFrBuildingOperation:::::::::::" + sanctionFee);
			//Summation of fees
			sanctionFees = sanctionFees.add(sanctionFee);
			log.info("FeeFromBuildingOperation:::::::::::" + sanctionFees);
		}
		
		utils.generateTaxHeadEstimate(estimates, sanctionFees, RegularizationConstants.TAXHEAD_REG_BUILDING_SANCTION_FEE, Category.FEE);
		log.info("FeeForBuildingOperation:::::::::::" + sanctionFees);
		return sanctionFees;
	}

	///////////////////////////////////  Sanction Fee Calculation Ended //////////////////////////////////////
	
	
	

	///////////////////////////////////  Construction Worker WelfareCess Fee Calculation Started //////////////////////////////////////
	
	/**
	 * Calculate Construction Worker WelfareCess fee
	 * @param criteria 
	 * @param estimates
	 * @param paramMap
	 * @return calculated Worker WelfareCess Fee
	 */
	private BigDecimal calculateConstructionWorkerWelfareCess(RegularizationCalculationCriteria criteria, List<TaxHeadEstimate> estimates,
			Map<String, Object> paramMap) {
		//new code
		boolean isCWWCFeeApplicable = criteria.getRegularization()
		        .getBuildingRegularizationInfo().getOtherDetails().isCWWCFeeApplicable();
		if (isCWWCFeeApplicable) {
		    log.info("CWWC Fee set to ZERO - already paid offline for: " + criteria.getApplicationNo());
		    utils.generateTaxHeadEstimate(estimates, BigDecimal.ZERO,
		            RegularizationConstants.TAXHEAD_REG_CONSTRUCTION_WORKER_WELFARE_CESS, Category.FEE);
		    return BigDecimal.ZERO;
		}
		//old code
		BigDecimal totalWelfareCess = BigDecimal.ZERO;
		BigDecimal welfareCessRate = BigDecimal.ZERO;
		BigDecimal costofConstruction = BigDecimal.ZERO;
		BigDecimal totalDeviatedBuiltUpArea = BigDecimal.ZERO;
		BigDecimal totalProvidedBuiltUpArea = utils.convertToBigDecimal(criteria.getRegularization().getBuildingRegularizationInfo().getBuaDetails().getTotalProvidedBUA());
		log.info("TotalProvidedBuiltUpArea : " + totalProvidedBuiltUpArea);
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.WALFARE_CESS_RATE))) {
			welfareCessRate = (BigDecimal) paramMap.get(RegularizationConstants.WALFARE_CESS_RATE);
		}
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.CONSTRUCTION_COST))) {
			costofConstruction = (BigDecimal) paramMap.get(RegularizationConstants.CONSTRUCTION_COST);
		}
		
		List<SubOcuupancy> subOcuupancies = getAllSubOcuupancies(criteria.getRegularization().getBuildingRegularizationInfo());
		
		for(SubOcuupancy subOcuupancy : subOcuupancies) {
			log.info("occupancy inside: " + subOcuupancy.getValue());
			log.info("TotalAsBuiltBUA inside: " + BigDecimal.valueOf(subOcuupancy.getTotalAsBuiltBUA()));
			log.info("TotalBuiltUpArea inside: " + BigDecimal.valueOf(subOcuupancy.getTotalBuiltUpArea()));
			//Total deviated built up area
			totalDeviatedBuiltUpArea = totalDeviatedBuiltUpArea.add(BigDecimal.valueOf(subOcuupancy.getTotalBuiltUpArea()));
			log.info("totalDeviatedBuiltUpArea inside: " + totalDeviatedBuiltUpArea);
		}
		
		//WelfareCess Fee Calculated For Residential Occupancy only
		if (ObjectUtils.isNotEmpty(totalProvidedBuiltUpArea) && ObjectUtils.isNotEmpty(totalDeviatedBuiltUpArea)) {
			BigDecimal totalCostOfConstruction = (costofConstruction
					.multiply(totalProvidedBuiltUpArea)
					.multiply(RegularizationConstants.SQMT_SQFT_MULTIPLIER))
					.setScale(2, BigDecimal.ROUND_UP);
			log.info("Total Cost Of Construction: " + totalCostOfConstruction);
			if (totalCostOfConstruction.compareTo(RegularizationConstants.TEN_LAC) > 0) {
				totalWelfareCess = (welfareCessRate
						.multiply(totalDeviatedBuiltUpArea)
						.multiply(RegularizationConstants.SQMT_SQFT_MULTIPLIER))
						.setScale(2, BigDecimal.ROUND_UP);
			}
		}
		utils.generateTaxHeadEstimate(estimates, totalWelfareCess, RegularizationConstants.TAXHEAD_REG_CONSTRUCTION_WORKER_WELFARE_CESS, Category.FEE);
		log.info("Total WelfareCess::::::::::::::" + totalWelfareCess);
		return totalWelfareCess;
	}

	
	
	///////////////////////////////////  Construction Worker WelfareCess Fee Calculation Ended //////////////////////////////////////

	
	
	
	
	///////////////////////////////////  Shelter Fee Calculation Started ////////////////////////////////////////////////
	
	/**
	 * Calculate Shelter fee
	 * @param criteria
	 * @param estimates
	 * @param paramMap 
	 * @return calculated Shelter Fee
	 */
	private BigDecimal calculateShelterFee(RegularizationCalculationCriteria criteria, List<TaxHeadEstimate> estimates, Map<String, Object> paramMap) {
		
		BigDecimal shelterFees = BigDecimal.ZERO;
		List<SubOcuupancy> subOcuupancies = getAllSubOcuupancies(criteria.getRegularization().getBuildingRegularizationInfo());
		
		for(SubOcuupancy subOcuupancy : subOcuupancies) {
			BigDecimal shelterFee = BigDecimal.ZERO;
			String subOccupancyType = subOcuupancy.getValue();
			log.info("occupancy inside: " + subOccupancyType);
			
			//shelter Fee Calculated For Residential Occupancy only
			if (StringUtils.hasText(subOccupancyType)) {
				if ((subOccupancyType.startsWith(BPACalculatorConstants.A))) {
					shelterFee = calculateShelterFeeForResidentialOccupancy(subOccupancyType, criteria, paramMap);
				}
				
				shelterFees=shelterFees.add(shelterFee);
				
				if(shelterFees.compareTo(BigDecimal.ZERO) > 0) {
					break;
				}
			}
		}
		utils.generateTaxHeadEstimate(estimates, shelterFees, RegularizationConstants.TAXHEAD_REG_SHELTER_FEE, Category.FEE);
		log.info("ShelterFee::::::::::::::::" + shelterFees);
		return shelterFees;
	}

	
	
	/**
	 * Calculate ShelterFee For ResidentialOccupancy
	 * 
	 * @param subOccupancyType
	 * @param criteria 
	 * @param paramMap 
	 * @return Calculated ShelterFee
	 */
	private BigDecimal calculateShelterFeeForResidentialOccupancy(String subOccupancyType, RegularizationCalculationCriteria criteria, Map<String, Object> paramMap) {
		BigDecimal shelterFee = BigDecimal.ZERO;
		BigDecimal costOfConstruction = BigDecimal.ZERO;
		
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.CONSTRUCTION_COST))) {
			costOfConstruction = (BigDecimal) paramMap.get(RegularizationConstants.CONSTRUCTION_COST);
		}
		BuildingRegularizationInfo buildingInfo = criteria.getRegularization().getBuildingRegularizationInfo();
		BigDecimal totalEffectiveEWSArea = utils.convertToBigDecimal(buildingInfo.getOtherDetails().getEffectiveEWSArea());
		boolean isShelterFeeApplicable = buildingInfo.getOtherDetails().isShelterFeeApplicable();
		int totalNoOfDwellingUnits = utils.convertToInteger(buildingInfo.getOtherDetails().getTotalNoOfDwellingUnits());
		
		if (totalNoOfDwellingUnits > 8 && isShelterFeeApplicable) {
			if (StringUtils.hasText(subOccupancyType)) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_P))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_R))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_HP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_WCR))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SA))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_MIH))) {
                 
                 BigDecimal totalEWSAreaForShelterFee = totalEffectiveEWSArea.multiply(RegularizationConstants.ONE_FORTY_PERCENT);
				 
                 shelterFee = (totalEWSAreaForShelterFee
						 .multiply(RegularizationConstants.SQMT_SQFT_MULTIPLIER)
						 .multiply(costOfConstruction)
						 .multiply(RegularizationConstants.ZERO_TWO_FIVE))
						 .setScale(2, BigDecimal.ROUND_UP);
				}
			}
		}
		return shelterFee;
	}

	///////////////////////////////////  Shelter Fee Calculation Ended ////////////////////////////////////////////////
	


	/**
	 * Calculate Temporary Retention Fee
	 * @param requestInfo 
	 * @param criteria
	 * @param estimates
	 * @return Calculated Retention Fee
	 */
	private BigDecimal calculateTemporaryRetentionFee(RequestInfo requestInfo, RegularizationCalculationCriteria criteria,
			List<TaxHeadEstimate> estimates) {
		BigDecimal retentionFee = BigDecimal.ZERO;
		BigDecimal numberOfTemporaryStructures = BigDecimal.ZERO;
		
		if(ObjectUtils.isNotEmpty(criteria.getRegularization().getBuildingRegularizationInfo().getOtherDetails().getNumberOfTemporaryStructures())) {
			numberOfTemporaryStructures =  utils.convertToBigDecimal(criteria.getRegularization().getBuildingRegularizationInfo().getOtherDetails().getNumberOfTemporaryStructures());
		}
		
		if (numberOfTemporaryStructures.compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal retentionFeeForTenant = regularizationService.getRetentionFeeForTenant(requestInfo , criteria);
			
			retentionFee = retentionFeeForTenant
					.multiply(numberOfTemporaryStructures)
					.setScale(2, BigDecimal.ROUND_UP);
		}
		
		utils.generateTaxHeadEstimate(estimates, retentionFee, RegularizationConstants.TAXHEAD_REG_TEMPORARY_RETENTION_FEE, Category.FEE);
		log.info("RetentionFee:::::::::::" + retentionFee);
		return retentionFee;
	}

	
	
	
	///////////////////////////////////  Security Deposit Calculation Started ////////////////////////////////////////////////
	/**
	 * Calculate Security Deposit Fee 
	 * 
	 * @param criteria
	 * @param estimates
	 * @return Calculated Fee
	 */
	private BigDecimal calculateSecurityDeposit(RegularizationCalculationCriteria criteria, List<TaxHeadEstimate> estimates) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		BigDecimal securityDeposits = BigDecimal.ZERO;
		BuildingRegularizationInfo buildingInfo = criteria.getRegularization().getBuildingRegularizationInfo();
		boolean isSecurityDepositRequired = buildingInfo.getOtherDetails().isSecurityDepositRequired();
		
		List<SubOcuupancy> subOccupancies = getAllSubOcuupancies(buildingInfo);
		
		for(SubOcuupancy subOccupancy : subOccupancies) {
			Double totalBuiltUpArea = new Double(subOccupancy.getTotalAsBuiltCarpetArea());
			log.info("total Carpet Area inside: " + totalBuiltUpArea);
			
			String subOccupancyType = subOccupancy.getValue();;
			log.info("occupancy inside: " + subOccupancyType);

			if (ObjectUtils.isNotEmpty(totalBuiltUpArea) && isSecurityDepositRequired) {
				if ((subOccupancyType.startsWith(BPACalculatorConstants.A))) {
					securityDeposit = calculateSecurityDepositForResidentialOccupancy(subOccupancyType, totalBuiltUpArea);
				}
				if ((subOccupancyType.startsWith(BPACalculatorConstants.B))) {
					securityDeposit = calculateSecurityDepositForCommercialOccupancy(totalBuiltUpArea);
				}
				if ((subOccupancyType.startsWith(BPACalculatorConstants.C))) {
					securityDeposit = calculateSecurityDepositForPublicSemiPublicInstitutionalOccupancy(subOccupancyType, totalBuiltUpArea);
				}
				if ((subOccupancyType.startsWith(BPACalculatorConstants.F))) {
					securityDeposit = calculateSecurityDepositForEducationOccupancy(totalBuiltUpArea);
				}
			}
			securityDeposits=securityDeposits.add(securityDeposit); 
		}
		
		utils.generateTaxHeadEstimate(estimates, securityDeposits, RegularizationConstants.TAXHEAD_REG_SECURITY_DEPOSIT, Category.FEE);
		log.info("SecurityDeposit::::::::::::::" + securityDeposits);
		return securityDeposit;
	}
	


	/**
	 * Calculate Security Deposit For Residential Occupancy
	 * @param subOccupancyType
	 * @param totalBuiltUpArea
	 * @return Calculated fee
	 */
	private BigDecimal calculateSecurityDepositForResidentialOccupancy(String subOccupancyType, Double totalBuiltUpArea) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		if (StringUtils.hasText(subOccupancyType)) {
			if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_AB))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_HP))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_WCR))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SA))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_E))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_LIH))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_MIH))
					|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SQ))) {

				securityDeposit = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.HUNDRED);
			}
		}
		return securityDeposit;
	}

	
	

	/**
	 * Calculate Security Deposit For Commercial Occupancy
	 * @param subOccupancyType
	 * @param totalBuiltUpArea
	 * @return Calculated fee
	 */
	private BigDecimal calculateSecurityDepositForCommercialOccupancy(Double totalBuiltUpArea) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		if (ObjectUtils.isNotEmpty(totalBuiltUpArea) && totalBuiltUpArea >= 200) {
			securityDeposit = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.HUNDRED);
		}
		return securityDeposit;
	}
	
	
	
	
	/**
	 * Calculate Security Deposit For PublicSemiPublicInstitutional Occupancy
	 * @param subOccupancyType 
	 * @param subOccupancyType
	 * @param totalBuiltUpArea
	 * @return Calculated fee
	 */
	private BigDecimal calculateSecurityDepositForPublicSemiPublicInstitutionalOccupancy(String subOccupancyType, Double totalBuiltUpArea) {
		BigDecimal securityDeposit = BigDecimal.ZERO;

		if (StringUtils.hasText(subOccupancyType) && subOccupancyType.startsWith(BPACalculatorConstants.C)) {
			if (ObjectUtils.isNotEmpty(totalBuiltUpArea) && totalBuiltUpArea >= 200) {
				securityDeposit = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.HUNDRED);
			}
		}
		return securityDeposit;
	}


	
	
	/**
	 * Calculate Security Deposit For Education Occupancy
	 * @param subOccupancyType
	 * @param totalBuiltUpArea
	 * @return Calculated fee
	 */
	private BigDecimal calculateSecurityDepositForEducationOccupancy(Double totalBuiltUpArea) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		if (ObjectUtils.isNotEmpty(totalBuiltUpArea) && totalBuiltUpArea >= 200) {
			securityDeposit = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.HUNDRED);
		}
		return securityDeposit;
	}


	///////////////////////////////////  Security Deposit Calculation Ended ////////////////////////////////////////////////


	
	
	
	///////////////////////////////////  Calculate Purchasable FAR Started ////////////////////////////////////////////////


	/**
	 * Calculate Purchasable FAR
	 * @param criteria
	 * @param estimates
	 * @return purchasableFARFee
	 */
	private BigDecimal calculatePurchasableFAR(RegularizationCalculationCriteria criteria, List<TaxHeadEstimate> estimates) {
		BigDecimal purchasableFARFee = BigDecimal.ZERO;
		BigDecimal tdrFarRelaxation = BigDecimal.ZERO;
		Regularization regularization = criteria.getRegularization();
		BuildingRegularizationInfo buildingInfo = regularization.getBuildingRegularizationInfo();
		BigDecimal baseFar = utils.convertToBigDecimal(buildingInfo.getFarDetails().getBaseFar());
		BigDecimal approvedFar = utils.convertToBigDecimal(buildingInfo.getFarDetails().getApprovedFar());
		BigDecimal providedFar = utils.convertToBigDecimal(buildingInfo.getFarDetails().getAsBuiltFar());
		BigDecimal plotArea = utils.getNetPlotArea(regularization.getLandRegularizationInfo().getPlotInfo());
		BigDecimal maxPermissibleFar = utils.convertToBigDecimal(buildingInfo.getFarDetails().getMaxPermissibleFar());
		log.info("Total Net Plot Area Excluding Gift Area : " + plotArea);
		
		Boolean isLayoutApproved = Boolean.FALSE;
		String riskType = null;
		
		Map<String, String> additionalDetails = regularization.getAdditionalDetails() != null ? (Map) regularization.getAdditionalDetails()
				: new HashMap<String, String>();
		
		if (!Objects.isNull(additionalDetails.get("isLayoutApprovedUnderSectionSixteenOfOdaAct"))) {
			isLayoutApproved = ((String) additionalDetails.get("isLayoutApprovedUnderSectionSixteenOfOdaAct"))
					.equalsIgnoreCase("YES") ? true : false;
		}
		
		if (!Objects.isNull(additionalDetails.get("riskType"))) {
			riskType = ((String) additionalDetails.get("riskType"));
		}
		
		if("LOW".equalsIgnoreCase(riskType)) {
			log.info("Risk Type is low, no pur far fee applicable..");
			return purchasableFARFee;
		}
		
		if(ObjectUtils.isNotEmpty(buildingInfo.getOtherDetails().getTdrFarRelaxation())) {
			tdrFarRelaxation = utils.convertToBigDecimal(buildingInfo.getOtherDetails().getTdrFarRelaxation());
		}
		BigDecimal averageBenchmarkValuePerSQM = getAverageBenchmarkValuePerSQM(regularization.getLandRegularizationInfo());

		if (ObjectUtils.isNotEmpty(providedFar) && ObjectUtils.isNotEmpty(baseFar) 
				&& !(providedFar.compareTo(BigDecimal.ZERO) == 0)  && !(baseFar.compareTo(BigDecimal.ZERO) == 0)
				&& providedFar.compareTo(baseFar) > 0 && ObjectUtils.isNotEmpty(plotArea)) {

			BigDecimal purchasableFARRate = (averageBenchmarkValuePerSQM.multiply(RegularizationConstants.ZERO_TWO_FIVE));
			
			BigDecimal deltaFAR = BigDecimal.ZERO;
			if(!(approvedFar.compareTo(BigDecimal.ZERO) == 0) && approvedFar.compareTo(baseFar) > 0) {
				deltaFAR = providedFar.subtract(approvedFar);
			} else {
				deltaFAR = providedFar.subtract(baseFar);
			}
				
			//tdr relaxation- decrease deltaFar based on tdrFarRelaxation-
			if(ObjectUtils.isNotEmpty(tdrFarRelaxation) && !(tdrFarRelaxation.compareTo(BigDecimal.ZERO) == 0)) {
				deltaFAR=deltaFAR.subtract(tdrFarRelaxation).setScale(8, BigDecimal.ROUND_UP);
			}
			
			if (deltaFAR.compareTo(BigDecimal.ZERO) < 0) {
				deltaFAR = BigDecimal.ZERO;
			}
			
			log.info("Is Layout Approved.."+isLayoutApproved);
			if(isLayoutApproved && maxPermissibleFar.compareTo(new BigDecimal("3.00")) >= 0) {
				log.info("Inside Layout Approved.. delta far before "+deltaFAR);
				deltaFAR = applyLayoutApprovedDiscountIfApplicable(baseFar,approvedFar,providedFar, deltaFAR);
				log.info("Inside Layout Approved.. delta far."+deltaFAR);
			} 
			else {
				log.info("Delta Far without Layout Approved>>>>>"+deltaFAR);
				deltaFAR = applyMIGDiscountIfApplicable(buildingInfo, deltaFAR, baseFar, approvedFar);
			}
			
//			deltaFAR = applyMIGDiscountIfApplicable(buildingInfo, deltaFAR);
     log.info("Fee>>>"+purchasableFARFee+" ; Far Rate>>>"+purchasableFARRate+" ; delta Far>>"+deltaFAR+" ; plotArea>>"+plotArea);
			purchasableFARFee = (purchasableFARRate
					.multiply(deltaFAR)
					.multiply(plotArea))
					.setScale(2, BigDecimal.ROUND_UP);

		}

		utils.generateTaxHeadEstimate(estimates, purchasableFARFee, RegularizationConstants.TAXHEAD_REG_PURCHASABLE_FAR, Category.FEE);
		log.info("PurchasableFARFee:::::::::::::::::" + purchasableFARFee);
		return purchasableFARFee;
	}
	
	private BigDecimal applyLayoutApprovedDiscountIfApplicable(BigDecimal baseFAR,BigDecimal approvedFAR, BigDecimal providedFAR,
			BigDecimal deltaFAR) {
//		BigDecimal applicableDiscountFar = new BigDecimal("1.00");
//		deltaFAR = deltaFAR.subtract(applicableDiscountFar).max(BigDecimal.ZERO);
//		log.info("Updated deltaFAR after applying discount: {}", deltaFAR);		
		BigDecimal applyLayoutDiscountFar = BigDecimal.ZERO;
	    BigDecimal incentiveFar = new BigDecimal("1.00");

	    // BASE + INCENTIVE
	    BigDecimal basePlusIncentive = baseFAR.add(incentiveFar);

	    if (basePlusIncentive.compareTo(approvedFAR) < 0) {
	        // Deduction on APPROVED FAR
	    	applyLayoutDiscountFar = providedFAR.subtract(approvedFAR);
	        log.info("Case: BASE+INCENTIVE < APPROVED_FAR. Purchasable FAR = Proposed - Approved>>>"+applyLayoutDiscountFar);
	    } else {
	        // Deduction on BASE+INCENTIVE FAR
	    	applyLayoutDiscountFar = providedFAR.subtract(basePlusIncentive);
	        log.info("Case: BASE+INCENTIVE >= APPROVED_FAR. Purchasable FAR = Proposed - (Base+Incentive)>>>"+applyLayoutDiscountFar);
	    }

	    // Ensure non-negative
	  //  deltaFAR = deltaFAR.max(BigDecimal.ZERO);
	    log.info("Upadted ApplyLayout discount>>"+applyLayoutDiscountFar);
	    deltaFAR = deltaFAR.subtract(applyLayoutDiscountFar).max(BigDecimal.ZERO);

	    log.info("Updated Delta FAR after applying Layout discount: {}", deltaFAR);
	   
		return deltaFAR;
	}


	private BigDecimal applyMIGDiscountIfApplicable(BuildingRegularizationInfo buildingInfo, BigDecimal deltaFAR, BigDecimal  baseFar, BigDecimal approvedFar) {
		BigDecimal applicableDiscountFar = BigDecimal.ZERO;

		List<SubOcuupancy> subOcuupancies = getAllSubOcuupancies(buildingInfo);
		Map<String, Integer> subOccupancyCounter = new HashMap<>();

		if (subOcuupancies == null || subOcuupancies.isEmpty()) {
		    log.warn("No sub-occupancies found for processing.");
		}else {
			for (SubOcuupancy subOcuupancy : subOcuupancies) {
				String subOccupancyType = subOcuupancy.getValue();
				log.info("occupancy inside: " + subOccupancyType + " - " + subOcuupancy.getLabel());

				if (subOccupancyType != null) {
					subOccupancyCounter.put(subOccupancyType,
							subOccupancyCounter.getOrDefault(subOccupancyType, 0) + 1);
				}

			}
			System.out.println("SubOccupancy Counter: " + subOccupancyCounter);

			boolean containsMIGSubOccupancy = subOccupancyCounter.containsKey(BPACalculatorConstants.A_MIH);

			if (!containsMIGSubOccupancy) {
			    log.info("The sub-occupancy map does not contain the target: {}", BPACalculatorConstants.A_MIH);
			}else {
				log.info("The sub-occupancy map contains the target: {}", BPACalculatorConstants.A_MIH);
				// Parse total dwelling units with error handling
				String totalDwellingUnitsStr = Optional.ofNullable(buildingInfo.getOtherDetails())
				        .map(BuildingOtherDetails::getTotalNoOfDwellingUnits)
				        .orElse("");
				
				int totalDwellingUnits = 0; // Default to 0 if parsing fails
				try {
				    totalDwellingUnits = Integer.parseInt(totalDwellingUnitsStr);
				    log.info("Number of dwelling units: {}", totalDwellingUnits);
				} catch (NumberFormatException e) {
				    log.error("Error parsing total dwelling units '{}'. Defaulting to 0.", totalDwellingUnitsStr, e);
				}
				
				// Apply discount if conditions are met
				if (totalDwellingUnits >= 8) {
				    applicableDiscountFar = new BigDecimal("0.25");
				}
				
				if (applicableDiscountFar.compareTo(BigDecimal.ZERO) > 0) {
					BigDecimal baseFARwithRelection = BigDecimal.ZERO;
					baseFARwithRelection = baseFar.add(applicableDiscountFar);
					log.info("Bas Far with Relection>>>>"+baseFARwithRelection);
					if (approvedFar.compareTo(baseFARwithRelection) >= 0) {
						applicableDiscountFar = BigDecimal.ZERO;
					}else {
						log.info("inside relection");
						applicableDiscountFar = baseFARwithRelection.subtract(approvedFar);
					}
				}
				log.info("Applicable Dis>>>"+applicableDiscountFar);
				
				deltaFAR = deltaFAR.subtract(applicableDiscountFar).max(BigDecimal.ZERO);
				log.info("Updated deltaFAR after applying discount: {}", deltaFAR);
			}
			
		}
		return deltaFAR;
	}

	
	/**
	 * Calculate Average BenchmarkValue Per SQM
	 * @param landRegularizationInfo
	 * @return AverageBenchmarkValuePerSQM
	 */
	private BigDecimal getAverageBenchmarkValuePerSQM(LandRegularizationInfo landRegularizationInfo) {
		
		Double averageBenchmarkValuePerAcre = landRegularizationInfo.getPlotInfo().stream()
						.mapToDouble(plot -> new Double(plot.getBmvValue()))
						.average()
						.orElse(0.0);
		
		BigDecimal averageBenchmarkValuePerSQM = BigDecimal.valueOf(averageBenchmarkValuePerAcre)
				.divide(RegularizationConstants.ACRE_SQMT_MULTIPLIER, 8, BigDecimal.ROUND_UP);
		
		
		return averageBenchmarkValuePerSQM;
	}

	
	///////////////////////////////////  Calculate Purchasable FAR Started ////////////////////////////////////////////////


	/**
	 * Calculate EIDP Fee
	 * @param criteria
	 * @param estimates
	 * @return EIDP Fee
	 */
	private BigDecimal calculateEIDPFee(RegularizationCalculationCriteria criteria, List<TaxHeadEstimate> estimates) {
		
		boolean isEIDPFeeApplicable = criteria.getRegularization()
		        .getBuildingRegularizationInfo().getOtherDetails().isEIDPFeeApplicable();
		if (isEIDPFeeApplicable) {
		    log.info("EIDP Fee set to ZERO - already paid offline for: " + criteria.getApplicationNo());
		    utils.generateTaxHeadEstimate(estimates, BigDecimal.ZERO,
		            RegularizationConstants.TAXHEAD_REG_EIDP_FEE, Category.FEE);
		    return BigDecimal.ZERO;
		}
		
		BigDecimal eidpFee = BigDecimal.ZERO;
		String projectValue = criteria.getRegularization().getBuildingRegularizationInfo().getOtherDetails().getProjectValueIfEIDPFeeApplicableForProject();

		if (ObjectUtils.isNotEmpty(projectValue) || !(projectValue.equals("0.00"))){
			eidpFee = utils.convertToBigDecimal(projectValue).divide(RegularizationConstants.HUNDRED);
		}
		
		utils.generateTaxHeadEstimate(estimates, eidpFee, RegularizationConstants.TAXHEAD_REG_EIDP_FEE, Category.FEE);
		log.info("EIDP Fee:::::::::::::::::" + eidpFee);
		return eidpFee;
	}

	
	

	
	
	///////////////////////////////////  Calculate Compounding Fee Started ////////////////////////////////////////////////

	
	/**
	 * Calculate Compounding FAR Fee
	 * 
	 * @param criteria
	 * @param estimates
	 * @return CompoundingFARFee
	 */
	private BigDecimal calculateCompoundingFARFee(RegularizationCalculationCriteria criteria,
			List<TaxHeadEstimate> estimates) {
		BigDecimal compoundingFARFee  = BigDecimal.ZERO;
		BigDecimal totalUnauthorizedBUA = utils.convertToBigDecimal(criteria.getRegularization().getBuildingRegularizationInfo().getBuaDetails().getTotalUnauthorizedBUA());
		BigDecimal farFeeRate = utils.convertToBigDecimal(criteria.getRegularization().getBuildingRegularizationInfo().getOtherDetails().getFarFeeRate());
		
		if (totalUnauthorizedBUA.compareTo(BigDecimal.ZERO) > 0) {
			compoundingFARFee = utils.multiplyWithRoundUp(totalUnauthorizedBUA, farFeeRate);
		}
		utils.generateTaxHeadEstimate(estimates, compoundingFARFee, RegularizationConstants.TAXHEAD_REG_COMPOUNDING_FAR_FEE, Category.FEE);
		log.info("Compounding FAR Fee:::::::::::::::::" + compoundingFARFee);
		return compoundingFARFee;
	}


	
	/**
	 * Calculate Compounding Setback Fee
	 * @param criteria
	 * @param estimates
	 * @return CompoundingSetbackFee
	 */
	private BigDecimal calculateCompoundingSetbackFee(RegularizationCalculationCriteria criteria,
			List<TaxHeadEstimate> estimates) {
		BigDecimal compoundingSetbackFee  = BigDecimal.ZERO;
		
		BigDecimal totalUnauthAreaonSBWithinNorms = utils.convertToBigDecimal(criteria.getRegularization().getBuildingRegularizationInfo().getBuaDetails().getTotalUnauthAreaonSBWithinNorms());
		BigDecimal totalUnauthorizedSBBNUnder5 = utils.convertToBigDecimal(criteria.getRegularization().getBuildingRegularizationInfo().getBuaDetails().getTotalUnauthAreaonSBBeyondNormsButUnder5());
		BigDecimal totalUnauthorizedSBBNUnder10 = utils.convertToBigDecimal(criteria.getRegularization().getBuildingRegularizationInfo().getBuaDetails().getTotalUnauthAreaonSBBeyondNormsButUnder10());
		
		BigDecimal unauthAreaonSBWithinNormsRate = utils.convertToBigDecimal(criteria.getRegularization().getBuildingRegularizationInfo().getOtherDetails().getUnauthorizedSBWithinNormsRate());
		BigDecimal unauthorizedSBBNUnder5Rate = utils.convertToBigDecimal(criteria.getRegularization().getBuildingRegularizationInfo().getOtherDetails().getUnauthorizedSBBNUnder5Rate());
		BigDecimal unauthorizedSBBNUnder10Rate = utils.convertToBigDecimal(criteria.getRegularization().getBuildingRegularizationInfo().getOtherDetails().getUnauthorizedSBBNUnder10Rate());
		
		if (ObjectUtils.isNotEmpty(totalUnauthAreaonSBWithinNorms) && totalUnauthAreaonSBWithinNorms.compareTo(BigDecimal.ZERO) > 0 
				&& ObjectUtils.isNotEmpty(unauthAreaonSBWithinNormsRate)) {
			compoundingSetbackFee = utils.multiplyWithRoundUp(totalUnauthAreaonSBWithinNorms, unauthAreaonSBWithinNormsRate);
		}
		
		if (ObjectUtils.isNotEmpty(totalUnauthorizedSBBNUnder5) && totalUnauthorizedSBBNUnder5.compareTo(BigDecimal.ZERO) > 0 
				&& ObjectUtils.isNotEmpty(unauthorizedSBBNUnder5Rate)) {
			compoundingSetbackFee = compoundingSetbackFee
					.add(utils.multiplyWithRoundUp(totalUnauthorizedSBBNUnder5, unauthorizedSBBNUnder5Rate))
					.setScale(2, BigDecimal.ROUND_UP);
		}
		
		
		if (ObjectUtils.isNotEmpty(totalUnauthorizedSBBNUnder10) && totalUnauthorizedSBBNUnder10.compareTo(BigDecimal.ZERO) > 0 
				&& ObjectUtils.isNotEmpty(unauthorizedSBBNUnder10Rate)) {
			compoundingSetbackFee = compoundingSetbackFee
					.add(utils.multiplyWithRoundUp(totalUnauthorizedSBBNUnder10, unauthorizedSBBNUnder10Rate))
					.setScale(2, BigDecimal.ROUND_UP);
		}
		
		utils.generateTaxHeadEstimate(estimates, compoundingSetbackFee, RegularizationConstants.TAXHEAD_REG_COMPOUNDING_SETBACK_FEE, Category.FEE);
		log.info("Compounding FAR Fee:::::::::::::::::" + compoundingSetbackFee);
		return compoundingSetbackFee;
	}
	
	
	///////////////////////////////////  Calculate Compounding Fee Ended ////////////////////////////////////////////////

	
	
	///////////////////////////////////  Calculate Variable Fee For Sparit Ulbs Started////////////////////////////////////////////////
	
	private BigDecimal calculateVariableFeeForSparitUlbs1(Double totalBuiltUpArea) {
		BigDecimal amount = BigDecimal.ZERO;

		if (ObjectUtils.isNotEmpty(totalBuiltUpArea)) {
			if (totalBuiltUpArea <= RegularizationConstants.INT_HUNDRED) {
				amount = RegularizationConstants.ONE_HUNDRED_FIFTY; // 150
			} else if (totalBuiltUpArea <= RegularizationConstants.INT_ONE_HUNDRED_FIFTY) { // next is >100-150
				amount = RegularizationConstants.THREE_HUNDRED_SEVENTY_FIVE; // (225+150)
			} else if (totalBuiltUpArea <= RegularizationConstants.INT_THREE_HUNDRED) { // (150-300) //300
				amount = RegularizationConstants.SIX_HUNDRED_SEVENTY_FIVE; // (300+225+150)
			} else if (totalBuiltUpArea > RegularizationConstants.INT_THREE_HUNDRED) { // additional 300 for every 50
				Double builtUpAreaAbove300 = (totalBuiltUpArea + RegularizationConstants.FOURTY_NINE) - RegularizationConstants.INT_THREE_HUNDRED;
				//System.out.println("builtUpAreaAbove300 :" + builtUpAreaAbove300);
				int additionalFifties = (int) (builtUpAreaAbove300 / RegularizationConstants.INT_FIFTY);
				//System.out.println("additionalFifties :" + additionalFifties);
				amount = RegularizationConstants.SIX_HUNDRED_SEVENTY_FIVE.add(new BigDecimal(additionalFifties).multiply(RegularizationConstants.THREE_HUNDRED));
			}
		}
		
		log.info("Total Built up area for Sparit: " + totalBuiltUpArea);
		log.info("Building Operation Fee for Sparit: " + amount);
		return amount;
	}
	
	
	
	private BigDecimal calculateVariableFeeForSparitUlbs2(Double totalBuiltUpArea) {
		// totalBuitUpArea =122.00;
		BigDecimal amount = BigDecimal.ZERO;
		if (ObjectUtils.isNotEmpty(totalBuiltUpArea)) {
			if (totalBuiltUpArea <= RegularizationConstants.INT_TWENTY) {
				amount = RegularizationConstants.TWO_HUNDRED_FIFTY;
			} else if (totalBuiltUpArea <= RegularizationConstants.INT_FIFTY) {
				amount = RegularizationConstants.SIX_HUNDRED_TWENTY_FIVE; // (375+250)
			} else if (totalBuiltUpArea > RegularizationConstants.INT_FIFTY) {// for every additional 50 //500
				Double builtUpAreaAbove50 = (totalBuiltUpArea + RegularizationConstants.FOURTY_NINE) - RegularizationConstants.INT_FIFTY;
				int additionalFifties = (int) (builtUpAreaAbove50 / RegularizationConstants.INT_FIFTY);
				amount = RegularizationConstants.SIX_HUNDRED_TWENTY_FIVE.add(new BigDecimal(additionalFifties).multiply(RegularizationConstants.FIVE_HUNDRED));
			}

		}
		
		log.info("Total Built up area for Sparit: " + totalBuiltUpArea);
		log.info("Building Operation Fee for Sparit: " + amount);
		return amount;

	}
	
	
	private BigDecimal calculateConstantSparitFee(Double totalBuiltUpArea) {
		//0.5 is applicable here
		BigDecimal amount = utils.calculateConstantFee(totalBuiltUpArea, RegularizationConstants.ZERO_POINT_FIVE);
		return amount;

	}
	
	///////////////////////////////////  Calculate Variable Fee For Sparit Ulbs Started////////////////////////////////////////////////
	
}
