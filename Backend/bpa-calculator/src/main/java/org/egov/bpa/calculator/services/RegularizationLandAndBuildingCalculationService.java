package org.egov.bpa.calculator.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang3.ObjectUtils;
import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.repository.ServiceRequestRepository;
import org.egov.bpa.calculator.utils.RegularizationConstants;
import org.egov.bpa.calculator.utils.RegularizationUtils;
import org.egov.bpa.calculator.web.models.OtherFeeDetails;
import org.egov.bpa.calculator.web.models.demand.Category;
import org.egov.bpa.calculator.web.models.demand.TaxHeadEstimate;
import org.egov.bpa.calculator.web.models.regularization.RegularizationCalculationCriteria;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RegularizationLandAndBuildingCalculationService {
	
	@Autowired
	private RegularizationLandCalculationService landCalculationService;
	
	@Autowired
	private RegularizationBuildingCalculationService buildingCalculationService;
	
	@Autowired
	private RegularizationUtils utils;
	
	@Autowired
	private BPACalculatorConfig config;
	
	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	
	/**
	 * Calculate fee and Estimates here for Land & Building Regularization
	 * 
	 * @param requestInfo
	 * @param criteria
	 * @param estimates
	 * @param paramMap 
	 */
	public void calculateTotalFeeForLandAndBuilding(RequestInfo requestInfo, @Valid RegularizationCalculationCriteria criteria, List<TaxHeadEstimate> estimates, Map<String, Object> paramMap ) {

		if (StringUtils.hasText(criteria.getFeeType()) && criteria.getFeeType().equalsIgnoreCase(RegularizationConstants.APPLICATION_FEE)) {
			
			BigDecimal landAndBuildingAppFee = buildingCalculationService.calculateTotalBuildingScrutinyFee(requestInfo, criteria, estimates, paramMap);
			log.info("Total App FEE For:::" + criteria.getApplicationNo() + " is " + landAndBuildingAppFee);
		
		} else if (StringUtils.hasText(criteria.getFeeType()) && criteria.getFeeType().equalsIgnoreCase(RegularizationConstants.SANCTION_FEE)) {
			
			BigDecimal landAndBuildingPermitFee = calculateTotalLandAndBuildingPermitFee(requestInfo, criteria, estimates, paramMap);
			log.info("Total Sanc FEE For:::" + criteria.getApplicationNo() + " is " + landAndBuildingPermitFee);
		}

	}

	
	
	/**
	 * Calculate Total LandAndBuilding Permit Fee
	 * 
	 * @param requestInfo
	 * @param criteria
	 * @param estimates
	 * @param paramMap 
	 * @return Calculated Total Land And Building Permit Fee
	 */
	private BigDecimal calculateTotalLandAndBuildingPermitFee(RequestInfo requestInfo, RegularizationCalculationCriteria criteria,
			List<TaxHeadEstimate> estimates, Map<String, Object> paramMap) {
		BigDecimal calculatedTotalPermitFee = BigDecimal.ZERO;
		
		BigDecimal totalCompoundingFeeForLand = landCalculationService.calculateCompoundingFee(criteria, estimates);
		BigDecimal totalBuildingPermitFee = buildingCalculationService.calculateTotalBuildingPermitFee(requestInfo, criteria, estimates, paramMap);
		
		calculatedTotalPermitFee = (calculatedTotalPermitFee
				.add(totalCompoundingFeeForLand)
				.add(totalBuildingPermitFee))
				.setScale(2, BigDecimal.ROUND_UP);
		
		log.info(" Total Permit fee for Appl :" + criteria.getApplicationNo() + " is " + calculatedTotalPermitFee);
		return calculatedTotalPermitFee;
	}

	
	
	/**
	 * Calculate other Fee in sanction fee calculation
	 * 
	 * @param paramMap
	 * @param estimates
	 * @return calculateAllOtherFees
	 */
	@SuppressWarnings("rawtypes")
	public BigDecimal calculateAllOtherAdjustmentFee(RegularizationCalculationCriteria criteria,
			List<TaxHeadEstimate> estimates, Map<String, Object> paramMap) {
		BigDecimal adjustmentAmount = BigDecimal.ZERO;
		
		if (Objects.nonNull(criteria.getRegularization()) && Objects.nonNull(criteria.getRegularization().getAdditionalDetails())
				&& !StringUtils.isEmpty(((Map) criteria.getRegularization().getAdditionalDetails()).get(RegularizationConstants.REG_SANC_FEE_OTHER_FEES_KEY))) {
			List<OtherFeeDetails> allOtherFeesDetails = getAllOtherFeesDetails(((Map)criteria.getRegularization().getAdditionalDetails()).get(RegularizationConstants.REG_SANC_FEE_OTHER_FEES_KEY));
			BigDecimal otherFeeAmount = BigDecimal.ZERO;
			for(OtherFeeDetails otherFeeDetails : allOtherFeesDetails) {
				try {
					otherFeeAmount = new BigDecimal(otherFeeDetails.getAmount().toString());
				} catch(Exception e) {e.printStackTrace();}
				
				switch (otherFeeDetails.getOrder()) {
				case 1:
					
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					utils.generateTaxHeadEstimate(estimates, otherFeeAmount, RegularizationConstants.TAXHEAD_REG_ADJUSTMENT_AMOUNT_1, Category.FEE);
					break;

				case 2:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					utils.generateTaxHeadEstimate(estimates, otherFeeAmount, RegularizationConstants.TAXHEAD_REG_ADJUSTMENT_AMOUNT_2, Category.FEE);
					break;

				case 3:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					utils.generateTaxHeadEstimate(estimates, otherFeeAmount, RegularizationConstants.TAXHEAD_REG_ADJUSTMENT_AMOUNT_3, Category.FEE);
					break;
					
				case 4:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					utils.generateTaxHeadEstimate(estimates, otherFeeAmount, RegularizationConstants.TAXHEAD_REG_ADJUSTMENT_AMOUNT_4, Category.FEE);
					break;
					
				case 5:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					utils.generateTaxHeadEstimate(estimates, otherFeeAmount, RegularizationConstants.TAXHEAD_REG_ADJUSTMENT_AMOUNT_5, Category.FEE);
					break;

				default:
					throw new CustomException("INVALID_OTHER_FEE_DETAILS", "Kindly check the other Fee details !!!");
				}
			}
		}
		log.info("Adjustment Amount :::::::::::::::::" + adjustmentAmount);
		return adjustmentAmount;
	}
	
	
	
	private List<OtherFeeDetails> getAllOtherFeesDetails(Object object) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			String jsonString = mapper.writeValueAsString(object);
			List<OtherFeeDetails> allOtherFeeDetails = mapper.readValue(jsonString, new TypeReference<List<OtherFeeDetails>>(){});
			
			allOtherFeeDetails = allOtherFeeDetails.stream()
					.filter(otherFee -> !ObjectUtils.isEmpty(otherFee.getAmount()))
					.collect(Collectors.toList());
			
			return allOtherFeeDetails;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}



	///////////////////////////////////  Calculate Other Fee Ended ////////////////////////////////////////////////

	
	

	////////////////////////////////////////// Application Fee Adjustment Started //////////////////////////////////////

	
	/**
	 * Process Application Fees After Rework 
	 * and adjust extra fee in separate tax heads
	 * 
	 * @param requestInfo
	 * @param criteria
	 * @param estimates
	 * @param paramMap 
	 * @return 
	 */
	public BigDecimal processApplicationFeesAfterRework(RequestInfo requestInfo, RegularizationCalculationCriteria criteria,
			List<TaxHeadEstimate> estimates, Map<String, Object> paramMap) {
		
		BigDecimal landDevelopmentFeeReCalculated = BigDecimal.ZERO;
		BigDecimal buildingOperationFeeReCalculated = BigDecimal.ZERO;
		BigDecimal landDevelopmentFeePaid = BigDecimal.ZERO;
		BigDecimal buildingOperationFeePaid = BigDecimal.ZERO;
		BigDecimal landDevelopmentAdjustmentFee = BigDecimal.ZERO;
		BigDecimal buildingOperationAdjustmentFee = BigDecimal.ZERO;
		paramMap.put(RegularizationConstants.IS_REWORK_APP_KEY, true);
		
		ArrayList<TaxHeadEstimate> applicationFeeEstimates = new ArrayList<>();
		if(RegularizationConstants.APP_TYPE_LAND.equalsIgnoreCase(criteria.getApplicationType())) {
			BigDecimal landAppFee = landCalculationService.calculateLandDevelopmentFee(criteria, applicationFeeEstimates, paramMap);
			log.info("Total App FEE For Land Rework:::" + criteria.getApplicationNo() + " is " + landAppFee);
		} else {
			BigDecimal landAndBuildingAppFee = buildingCalculationService.calculateTotalBuildingScrutinyFee(requestInfo, criteria, applicationFeeEstimates, paramMap);
			log.info("Total App FEE For Rework:::" + criteria.getApplicationNo() + " is " + landAndBuildingAppFee);
		}

		for (TaxHeadEstimate estimate : applicationFeeEstimates) {
			if (estimate.getTaxHeadCode().equals(RegularizationConstants.TAXHEAD_REG_LAND_DEVELOPMENT_FEE))
				landDevelopmentFeeReCalculated = estimate.getEstimateAmount();
			else if (estimate.getTaxHeadCode().equals(RegularizationConstants.TAXHEAD_REG_BUILDING_OPPERATION_FEE))
				buildingOperationFeeReCalculated = estimate.getEstimateAmount();
		}

		// fetch payment details-
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.LAND_DEV_FEE_PAID_KEY))) {
			landDevelopmentFeePaid = (BigDecimal)paramMap.get(RegularizationConstants.LAND_DEV_FEE_PAID_KEY);
			if(landDevelopmentFeeReCalculated.compareTo(landDevelopmentFeePaid) > 0 ) {
				landDevelopmentAdjustmentFee = calculateLandDevelopmentFeeReWorkAdjustment(landDevelopmentFeeReCalculated, landDevelopmentFeePaid, estimates);
			}
		}
		
		if (ObjectUtils.isNotEmpty(paramMap.get(RegularizationConstants.BUILDING_OPR_FEE_PAID_KEY))) {
			buildingOperationFeePaid = (BigDecimal)paramMap.get(RegularizationConstants.BUILDING_OPR_FEE_PAID_KEY);
			if(buildingOperationFeeReCalculated.compareTo(buildingOperationFeePaid) > 0) {
				buildingOperationAdjustmentFee = calculateBuildingOperationFeeReWorkAdjustment(buildingOperationFeeReCalculated, buildingOperationFeePaid, estimates);
			}
		}
		return (landDevelopmentAdjustmentFee.add(buildingOperationAdjustmentFee)).setScale(2, BigDecimal.ROUND_UP);
	}


	
	private BigDecimal calculateLandDevelopmentFeeReWorkAdjustment(BigDecimal landDevelopmentFeeReCalculated,
			BigDecimal landDevelopmentFeePaid, List<TaxHeadEstimate> estimates) {
		BigDecimal landDevelopmentFeeReWorkAdjustmentAmount = landDevelopmentFeeReCalculated
				.compareTo(landDevelopmentFeePaid) > 0 ? landDevelopmentFeeReCalculated.subtract(landDevelopmentFeePaid) : BigDecimal.ZERO;
		utils.generateTaxHeadEstimate(estimates, landDevelopmentFeeReWorkAdjustmentAmount,
				RegularizationConstants.TAXHEAD_REG_LAND_DEVELOPMENT_FEE_REWORK_ADJUSTMENT, Category.FEE);
		return landDevelopmentFeeReWorkAdjustmentAmount;
	}

	
	private BigDecimal calculateBuildingOperationFeeReWorkAdjustment(BigDecimal buildingOperationFeeReCalculated,
			BigDecimal buildingOperationFeePaid, List<TaxHeadEstimate> estimates) {
		BigDecimal buildingOperationFeeReWorkAdjustmentAmount = buildingOperationFeeReCalculated
				.compareTo(buildingOperationFeePaid) > 0 ? buildingOperationFeeReCalculated.subtract(buildingOperationFeePaid) : BigDecimal.ZERO;
		utils.generateTaxHeadEstimate(estimates, buildingOperationFeeReWorkAdjustmentAmount,
				RegularizationConstants.TAXHEAD_REG_BUILDING_OPPERATION_FEE_REWORK_ADJUSTMENT, Category.FEE);
		return buildingOperationFeeReWorkAdjustmentAmount;
	}


	/**
	 * Fetch Payment Details
	 * @param requestInfo
	 * @param criteria
	 * @return paymentResponse
	 */
	public Object fetchPaymentDetails(RequestInfo requestInfo, RegularizationCalculationCriteria criteria) {
		StringBuilder fetchPaymentUrl = new StringBuilder(config.getCollectionServiceHost())
				.append(config.getCollectionServiceSearchRegularizationPermitFeeEndpoint())
				.append("?consumerCodes=").append(criteria.getApplicationNo())
				.append("&tenantId=").append(criteria.getTenantId());
		Map<String, Object> payload = new HashMap<>();
		payload.put("RequestInfo", requestInfo);
		Object paymentResponse = serviceRequestRepository.fetchResult(fetchPaymentUrl, payload);
		return paymentResponse;
	}


	
	@SuppressWarnings("rawtypes")
	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
	}



	
	

	////////////////////////////////////////// Application Fee Adjustment End //////////////////////////////////////

	
	
}
