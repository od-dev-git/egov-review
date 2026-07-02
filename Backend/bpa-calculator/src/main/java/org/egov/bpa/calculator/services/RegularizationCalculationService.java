package org.egov.bpa.calculator.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;

import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.kafka.broker.BPACalculatorProducer;
import org.egov.bpa.calculator.utils.BPACalculatorConstants;
import org.egov.bpa.calculator.utils.RegularizationConstants;
import org.egov.bpa.calculator.utils.RegularizationUtils;
import org.egov.bpa.calculator.web.models.bpa.EstimatesAndSlabs;
import org.egov.bpa.calculator.web.models.demand.TaxHeadEstimate;
import org.egov.bpa.calculator.web.models.regularization.CalculationRequest;
import org.egov.bpa.calculator.web.models.regularization.CalculationResponse;
import org.egov.bpa.calculator.web.models.regularization.Regularization;
import org.egov.bpa.calculator.web.models.regularization.RegularizationCalculation;
import org.egov.bpa.calculator.web.models.regularization.RegularizationCalculationCriteria;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RegularizationCalculationService {
	
	
	@Autowired
	private RegularizationService regularizationService;
	
	@Autowired
	private RegularizationUtils utils;
	
	@Autowired
	private RegularizationDemandService demandService;
	
	@Autowired
	private MDMSService mdmsService;
	
	@Autowired
	private BPACalculatorConfig config;
	
	@Autowired
	private BPACalculatorProducer producer;
	
	@Autowired
	private RegularizationLandCalculationService landCalculationService;
	
	@Autowired
	private RegularizationBuildingCalculationService buildingCalculationService;
	
	@Autowired
	private RegularizationLandAndBuildingCalculationService landAndBuildingCalculationService;
	
	/**
	 * Service layer method to get fee estimates
	 * 
	 * @param calculationRequest
	 * @return
	 */
	public List<RegularizationCalculation> getEstimate(@Valid CalculationRequest calculationRequest) {

		if (calculationRequest.getCalculationCriteria().get(0).getApplicationNo() == null) {
			throw new CustomException("Application NO is mandatory", "Application Number is mandatory");
		}

		//Set Regularization details in criteria from DB
		setRegularizationFromDBForCalculation(calculationRequest);

		//Validate the owner details
		utils.validateOwnerDetails(calculationRequest);
		
		//get estimations here
		List<RegularizationCalculation> calculations = calculateRegularizationFee(calculationRequest);

		return calculations;
	}

	
	
	/**
	 * Service layer method to generate demands for App and Sanc fee
	 * 
	 * @param calculationRequest
	 * @return
	 */
	private List<RegularizationCalculation> calculateRegularizationFee(@Valid CalculationRequest calculationRequest) {

		List<RegularizationCalculation> calculations = new LinkedList<>();
		RequestInfo requestInfo = calculationRequest.getRequestInfo();
		List<RegularizationCalculationCriteria> criterias = calculationRequest.getCalculationCriteria();

		for (RegularizationCalculationCriteria criteria : criterias) {

			if (criteria.getRegularization() == null && criteria.getApplicationNo() != null) {
				Regularization regularization = regularizationService.getRegularization(requestInfo, criteria.getTenantId(),criteria.getApplicationNo());
				criteria.setRegularization(regularization);
			}

			EstimatesAndSlabs estimatesAndSlabs = getTaxHeadEstimatesV2(criteria, requestInfo);
			List<TaxHeadEstimate> taxHeadEstimates = estimatesAndSlabs.getEstimates();

			RegularizationCalculation calculation = new RegularizationCalculation();
			calculation.setApplicationNumber(criteria.getApplicationNo());
			calculation.setRegularization(criteria.getRegularization());
			calculation.setTenantId(criteria.getTenantId());
			calculation.setTaxHeadEstimates(taxHeadEstimates);
			calculation.setFeeType(criteria.getFeeType());
			calculations.add(calculation);
		}

		return calculations;

	}

	
	
	/**
	 * Get Final Fee Estimates from this method
	 * 
	 * @param criteria
	 * @param requestInfo
	 * @return
	 */
	private EstimatesAndSlabs getTaxHeadEstimatesV2(RegularizationCalculationCriteria criteria, RequestInfo requestInfo) {
		List<TaxHeadEstimate> estimates = new LinkedList<>();
		EstimatesAndSlabs estimatesAndSlabs = getBaseTaxV2(criteria, requestInfo);
		estimates.addAll(estimatesAndSlabs.getEstimates());
		estimatesAndSlabs.setEstimates(estimates);

		return estimatesAndSlabs;
	}
	
	
	

	/**
	 * Calculate fee and estimates here based on Fee Type and App Type
	 * 
	 * @param criteria
	 * @param requestInfo
	 * @return
	 */
	private EstimatesAndSlabs getBaseTaxV2(RegularizationCalculationCriteria criteria, RequestInfo requestInfo) {

		EstimatesAndSlabs estimatesAndSlabs = new EstimatesAndSlabs();
		List<TaxHeadEstimate> estimates = new ArrayList<>();
		Map<String, Object> paramMap = prepareParamMap(requestInfo, criteria);

		switch (criteria.getApplicationType()) {
		case RegularizationConstants.APP_TYPE_LAND:
			landCalculationService.calculateTotalFeeForLand(criteria, estimates, paramMap);
			break;

		case RegularizationConstants.APP_TYPE_BUILDING:
			buildingCalculationService.calculateTotalFeeForBuilding(requestInfo, criteria, estimates, paramMap);
			break;

		case RegularizationConstants.APP_TYPE_LAND_BUILDING:
			landAndBuildingCalculationService.calculateTotalFeeForLandAndBuilding(requestInfo, criteria, estimates, paramMap);
			break;

		default:
			throw new CustomException("INVALID_APP_TYPE", "Kindly check the Application Type !!!");
		}
		
		//Calculate Other Adjustment Fees 
		if (StringUtils.hasText(criteria.getFeeType()) && criteria.getFeeType().equalsIgnoreCase(RegularizationConstants.SANCTION_FEE)) {
			
			// calculate application fees again if rework history is there and compare with
			// payment done and add calculations for adjustments in separate tax heads--
			BigDecimal appFeeAfterRework = landAndBuildingCalculationService.processApplicationFeesAfterRework(requestInfo, criteria, estimates, paramMap);
			log.info("Application Rework Adjustment FEE For:::" + criteria.getApplicationNo() + " is " + appFeeAfterRework);
			
			//Calculate Other Fee if mentioned by department
			BigDecimal otherAdjustmentFee = landAndBuildingCalculationService.calculateAllOtherAdjustmentFee(criteria, estimates, paramMap);
			log.info("Sanc FEE Adjustment Amount For:::" + criteria.getApplicationNo() + " is " + otherAdjustmentFee);
		}
		
		estimatesAndSlabs.setEstimates(estimates);
		return estimatesAndSlabs;
	
	}
	
	
	
	/**
	 * Additional details needs for calculation
	 * 
	 * @param requestInfo
	 * @param criteria
	 * @return paramMap
	 */
	private Map<String, Object> prepareParamMap(RequestInfo requestInfo, RegularizationCalculationCriteria criteria) {
		Map<String, Object> paramMap = new HashMap<>();
		
		if (Objects.nonNull(criteria.getRegularization())) {
			String riskType = getRiskTypeForBuilding(criteria);
			paramMap.put(RegularizationConstants.RISK_TYPE_KEY, riskType);
		}
		
		if (Objects.nonNull(criteria.getRegularization())) {
			Boolean isSparit = mdmsService.getMdmsSparitValueforRegularization(requestInfo,criteria.getTenantId());
			paramMap.put(RegularizationConstants.IS_SPARIT_KEY, isSparit);
		}
		
		if (Objects.nonNull(criteria.getRegularization())) {
			regularizationService.getConstructionWelfareCessRate(requestInfo , criteria, paramMap);
		}
		
		if (Objects.nonNull(criteria.getRegularization())) {
			fetchPaymentDetails(requestInfo, criteria, paramMap);
		}
		
		return paramMap;
	}

	
	



	/**
	 * Get risk type from additional details
	 * 
	 * @param criteria
	 * @return risk type
	 */
	@SuppressWarnings("unchecked")
	private String getRiskTypeForBuilding(RegularizationCalculationCriteria criteria) {
		
		Map<String, Object> additionalDetails = new HashMap<>();
		if (Objects.nonNull(criteria.getRegularization().getAdditionalDetails())) {
			additionalDetails = (Map<String, Object>)criteria.getRegularization().getAdditionalDetails();
		}
		String riskType = "";
		if (Objects.nonNull(additionalDetails)
				&& Objects.nonNull(additionalDetails.get(RegularizationConstants.RISK_TYPE_KEY))
				&& !StringUtils.isEmpty(additionalDetails.get(RegularizationConstants.RISK_TYPE_KEY))) {
			riskType = String .valueOf(additionalDetails.get(RegularizationConstants.RISK_TYPE_KEY));
		}
		return riskType;
	}


	/**
	 * search the regularization from DB and set regularization object in criteria
	 * 
	 * @param calculationRequest
	 */
	private void setRegularizationFromDBForCalculation(@Valid CalculationRequest calculationRequest) {

		if (Objects.nonNull(calculationRequest.getCalculationCriteria().get(0).getRegularization())) {
			log.info("Regularization Object Already Present in Calculation Criteria....");
		} else {
			log.info("fetching regularization from db for applicationNo:" + calculationRequest.getCalculationCriteria().get(0).getApplicationNo());
			Regularization regularization = regularizationService.getRegularization(calculationRequest.getRequestInfo(),
					calculationRequest.getCalculationCriteria().get(0).getTenantId(),
					calculationRequest.getCalculationCriteria().get(0).getApplicationNo());
			calculationRequest.getCalculationCriteria().get(0).setRegularization(regularization);

		}

	}

	
	/**
	 * Fetch PaymentDetails to compare payment history with current tax head amount
	 * 
	 * @param requestInfo
	 * @param criteria
	 * @param paramMap
	 */
	@SuppressWarnings("rawtypes")
	private void fetchPaymentDetails(RequestInfo requestInfo, RegularizationCalculationCriteria criteria, Map<String, Object> paramMap) {
		// fetch payment details-
		Object paymentResponse = landAndBuildingCalculationService.fetchPaymentDetails(requestInfo, criteria);
		int paymentsLength = ((List) ((Map) paymentResponse).get("Payments")).size();
		if(paymentsLength > 0) {
		
			String paymentAmountbyTaxHeadPath = BPACalculatorConstants.PAYMENT_TAXHEAD_AMOUNT_PATH;
			
			String landDevelopmentFeePaidString = landAndBuildingCalculationService.getValue((Map) paymentResponse,
					String.format(paymentAmountbyTaxHeadPath, (paymentsLength - 1), RegularizationConstants.TAXHEAD_REG_LAND_DEVELOPMENT_FEE));
			landDevelopmentFeePaidString = landDevelopmentFeePaidString.replace("[", "").replace("]", "");
			landDevelopmentFeePaidString = landDevelopmentFeePaidString.isEmpty() ? "0" : landDevelopmentFeePaidString;
			
			
			String buildingOpernFeePaidString = landAndBuildingCalculationService.getValue((Map) paymentResponse,
					String.format(paymentAmountbyTaxHeadPath, (paymentsLength - 1), RegularizationConstants.TAXHEAD_REG_BUILDING_OPPERATION_FEE));
			buildingOpernFeePaidString = buildingOpernFeePaidString.replace("[", "").replace("]", "");
			buildingOpernFeePaidString = buildingOpernFeePaidString.isEmpty() ? "0" : buildingOpernFeePaidString;
			
			
			paramMap.put(RegularizationConstants.LAND_DEV_FEE_PAID_KEY, new BigDecimal(landDevelopmentFeePaidString));
			paramMap.put(RegularizationConstants.BUILDING_OPR_FEE_PAID_KEY, new BigDecimal(buildingOpernFeePaidString));
		}
	}
	
	
	
	/**
	 * Service layer method to generate demands for regularization application
	 * 
	 * @param calculationRequest
	 * @return
	 */
	public List<RegularizationCalculation> calculate(@Valid CalculationRequest calculationRequest) {
		
		if (calculationRequest.getCalculationCriteria().get(0).getApplicationNo() == null) {
			throw new CustomException("Application NO is mandatory", "Application Number is mandatory");
		}
		
		//Search Regularization from DB and set in criteria
		setRegularizationFromDBForCalculation(calculationRequest);

		//Validate owner details here
		utils.validateOwnerDetails(calculationRequest);
		
		//get the fee calculations here
		List<RegularizationCalculation> calculations = calculateRegularizationFee(calculationRequest);
		
		//generate the demands from Billing service here
		demandService.generateDemand(calculationRequest.getRequestInfo(), calculations);
		
		CalculationResponse calculationRes = CalculationResponse.builder().calculations(calculations).build();
		producer.push(config.getSaveTopic(), calculationRes);
		return calculations;
	}

}
