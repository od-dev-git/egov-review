package org.egov.bpa.calculator.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.kafka.broker.BPACalculatorProducer;
import org.egov.bpa.calculator.utils.DemolitionConstants;
import org.egov.bpa.calculator.utils.DemolitionUtils;
import org.egov.bpa.calculator.web.models.OtherFeeDetails;
import org.egov.bpa.calculator.web.models.bpa.EstimatesAndSlabs;
import org.egov.bpa.calculator.web.models.demand.Category;
import org.egov.bpa.calculator.web.models.demand.TaxHeadEstimate;
import org.egov.bpa.calculator.web.models.demolition.Demolition;
import org.egov.bpa.calculator.web.models.demolition.DemolitionCalculation;
import org.egov.bpa.calculator.web.models.demolition.DemolitionCalculationCriteria;
import org.egov.bpa.calculator.web.models.demolition.DemolitionCalculationRequest;
import org.egov.bpa.calculator.web.models.demolition.DemolitionCalculationResponse;
import org.egov.bpa.calculator.web.models.demolition.DemolitionDemandService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DemolitionCalculationService {

	@Autowired
	private DemolitionUtils utils;

	@Autowired
	private DemolitionService demolitionService;

	@Autowired
	private MDMSService mdmsService;
	
	@Autowired
	private DemolitionDemandService demandService;
	
	@Autowired
	private BPACalculatorConfig config;
	
	@Autowired
	private BPACalculatorProducer producer;

	public List<DemolitionCalculation> getEstimate(@Valid DemolitionCalculationRequest calculationRequest) {

		if (calculationRequest.getCalculationCriteria().get(0).getApplicationNo() == null) {
			throw new CustomException("Application NO is mandatory", "Application Number is mandatory");
		}

		// Set Regularization details in criteria from DB
		setDemolitionFromDBForCalculation(calculationRequest);

		// Validate the owner details
		//utils.validateOwnerDetails(calculationRequest);

		// get estimations here
		List<DemolitionCalculation> calculations = calculateDemolitionFee(calculationRequest);

		return calculations;
	}

	private List<DemolitionCalculation> calculateDemolitionFee(@Valid DemolitionCalculationRequest calculationRequest) {

		List<DemolitionCalculation> calculations = new LinkedList<>();
		RequestInfo requestInfo = calculationRequest.getRequestInfo();
		List<DemolitionCalculationCriteria> criterias = calculationRequest.getCalculationCriteria();

		for (DemolitionCalculationCriteria criteria : criterias) {

			if (criteria.getDemolition() == null && criteria.getApplicationNo() != null) {
				Demolition demolition = demolitionService.getDemolition(requestInfo, criteria.getTenantId(),
						criteria.getApplicationNo());
				criteria.setDemolition(demolition);
			}

			EstimatesAndSlabs estimatesAndSlabs = getTaxHeadEstimatesV2(criteria, requestInfo);
			List<TaxHeadEstimate> taxHeadEstimates = estimatesAndSlabs.getEstimates();

			DemolitionCalculation calculation = new DemolitionCalculation();
			calculation.setApplicationNumber(criteria.getApplicationNo());
			calculation.setDemolition(criteria.getDemolition());
			calculation.setTenantId(criteria.getTenantId());
			calculation.setTaxHeadEstimates(taxHeadEstimates);
			calculation.setFeeType(criteria.getFeeType());
			calculations.add(calculation);
		}

		return calculations;
	}

	private EstimatesAndSlabs getTaxHeadEstimatesV2(DemolitionCalculationCriteria criteria, RequestInfo requestInfo) {
		List<TaxHeadEstimate> estimates = new LinkedList<>();
		EstimatesAndSlabs estimatesAndSlabs = getBaseTaxV2(criteria, requestInfo);
		estimates.addAll(estimatesAndSlabs.getEstimates());
		estimatesAndSlabs.setEstimates(estimates);

		return estimatesAndSlabs;
	}

	private EstimatesAndSlabs getBaseTaxV2(DemolitionCalculationCriteria criteria, RequestInfo requestInfo) {

		EstimatesAndSlabs estimatesAndSlabs = new EstimatesAndSlabs();
		List<TaxHeadEstimate> estimates = new ArrayList<>();
		Map<String, Object> paramMap = prepareParamMap(requestInfo, criteria);

		//calculation logic for Land Dev fee
		calculateLandDevFeeForDemolition(criteria, estimates, paramMap);
		
		// Other fee handling which comes from Additional Detials
		calculateAllOtherFee(criteria, estimates, paramMap);

		estimatesAndSlabs.setEstimates(estimates);
		return estimatesAndSlabs;
	}

	private void calculateAllOtherFee(DemolitionCalculationCriteria criteria, List<TaxHeadEstimate> estimates,
			Map<String, Object> paramMap) {

		BigDecimal adjustmentAmount = BigDecimal.ZERO;
		BigDecimal otherFeeAmount = BigDecimal.ZERO;

		Demolition demolition = criteria.getDemolition();

		if (Objects.nonNull(demolition) && Objects.nonNull(demolition.getAdditionalDetails()) && !StringUtils.isEmpty(
				((Map) demolition.getAdditionalDetails()).get(DemolitionConstants.DEMOLITION_OTHER_FEES_KEY))) {

			List<OtherFeeDetails> allOtherFeesDetails = getAllOtherFeesDetails(
					((Map) demolition.getAdditionalDetails()).get(DemolitionConstants.DEMOLITION_OTHER_FEES_KEY));

			for (OtherFeeDetails otherFeeDetails : allOtherFeesDetails) {
				try {
					otherFeeAmount = new BigDecimal(otherFeeDetails.getAmount().toString());
				} catch (Exception e) {
					e.printStackTrace();
				}

				switch (otherFeeDetails.getOrder()) {
				case 1:

					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					utils.generateTaxHeadEstimate(estimates, otherFeeAmount,
							DemolitionConstants.TAXHEAD_DEMOLITION_ADJUSTMENT_AMOUNT_1, Category.FEE);
					break;

				case 2:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					utils.generateTaxHeadEstimate(estimates, otherFeeAmount,
							DemolitionConstants.TAXHEAD_DEMOLITION_ADJUSTMENT_AMOUNT_2, Category.FEE);
					break;

				case 3:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					utils.generateTaxHeadEstimate(estimates, otherFeeAmount,
							DemolitionConstants.TAXHEAD_DEMOLITION_ADJUSTMENT_AMOUNT_3, Category.FEE);
					break;

				case 4:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					utils.generateTaxHeadEstimate(estimates, otherFeeAmount,
							DemolitionConstants.TAXHEAD_DEMOLITION_ADJUSTMENT_AMOUNT_5, Category.FEE);
					break;

				case 5:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					utils.generateTaxHeadEstimate(estimates, otherFeeAmount,
							DemolitionConstants.TAXHEAD_DEMOLITION_ADJUSTMENT_AMOUNT_5, Category.FEE);
					break;

				default:
					throw new CustomException("INVALID_OTHER_FEE_DETAILS", "Kindly check the other Fee details !!!");
				}
			}
		}
		log.info("Adjustment Amount Demolition :::::::::::::::::" + adjustmentAmount);
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

	private void calculateLandDevFeeForDemolition(DemolitionCalculationCriteria criteria,
			List<TaxHeadEstimate> estimates, Map<String, Object> paramMap) {
		
		BigDecimal totalLandDevelopmentFee = BigDecimal.ZERO;
		Boolean isSparit = null;
		Double plotArea = null;
		
		if (!ObjectUtils.isEmpty(paramMap.get(DemolitionConstants.IS_SPARIT_KEY))) {
			isSparit = (Boolean) paramMap.get(DemolitionConstants.IS_SPARIT_KEY);
		}
		
		if (null != paramMap.get(DemolitionConstants.TOTAL_LAND_AREA_KEY)) {
			String plot = (String) paramMap.get(DemolitionConstants.TOTAL_LAND_AREA_KEY);
			plotArea = Double.valueOf(plot);
		}
		
		log.info("total plot area :"+plotArea);
		if (null != plotArea) {
			
			if (isSparit) {
				log.info("sparit ULB, total plot area: "+plotArea);
				totalLandDevelopmentFee = (BigDecimal.valueOf(plotArea).multiply(BigDecimal.valueOf(2.5))).setScale(2,
						BigDecimal.ROUND_UP);
			} else {
				log.info("Non sparit ULB, total plot area: "+plotArea);
				totalLandDevelopmentFee = (BigDecimal.valueOf(plotArea).multiply(BigDecimal.valueOf(5))).setScale(2,
						BigDecimal.ROUND_UP);
			}
		}
		
		if (utils.anyApprovedAreaExists(criteria)) {
			log.info("project has some approved area, 50% of land dev fee is applicable...");
			totalLandDevelopmentFee = totalLandDevelopmentFee.divide(BigDecimal.valueOf(2)).setScale(2,
					BigDecimal.ROUND_UP);
		}
		
		log.info("total land dev fee applicable : "+ totalLandDevelopmentFee);
		log.info("land dev fee applicable for :" + criteria.getApplicationNo());
		utils.generateTaxHeadEstimate(estimates, totalLandDevelopmentFee,
				DemolitionConstants.TAXHEAD_LAND_DEVELOPMENT_FEE, Category.FEE);

	}

	private Map<String, Object> prepareParamMap(RequestInfo requestInfo, DemolitionCalculationCriteria criteria) {

		Map<String, Object> paramMap = new HashMap<>();

		if (Objects.nonNull(criteria.getDemolition())) {
			Boolean isSparit = mdmsService.getMdmsSparitValueforRegularization(requestInfo, criteria.getTenantId());
			paramMap.put(DemolitionConstants.IS_SPARIT_KEY, isSparit);
		}

		if (Objects.nonNull(criteria.getDemolition())) {

			String totalLandArea = criteria.getDemolition().getLandInfo().getTotalLandArea();
			paramMap.put(DemolitionConstants.TOTAL_LAND_AREA_KEY, totalLandArea);

		}

		return paramMap;
	}

	private void setDemolitionFromDBForCalculation(@Valid DemolitionCalculationRequest calculationRequest) {

		if (Objects.nonNull(calculationRequest.getCalculationCriteria().get(0).getDemolition())) {
			log.info("Demolition Object Already Present in Calculation Criteria....");
		} else {
			log.info("fetching demolition from db for applicationNo:"
					+ calculationRequest.getCalculationCriteria().get(0).getApplicationNo());
			Demolition demolition = demolitionService.getDemolition(calculationRequest.getRequestInfo(),
					calculationRequest.getCalculationCriteria().get(0).getTenantId(),
					calculationRequest.getCalculationCriteria().get(0).getApplicationNo());
			calculationRequest.getCalculationCriteria().get(0).setDemolition(demolition);

		}

	}

	public List<DemolitionCalculation> calculate(@Valid DemolitionCalculationRequest calculationRequest) {
		
		if (calculationRequest.getCalculationCriteria().get(0).getApplicationNo() == null) {
			throw new CustomException("Application NO is mandatory", "Application Number is mandatory");
		}

		// Set Regularization details in criteria from DB
		setDemolitionFromDBForCalculation(calculationRequest);

		// Validate the owner details
		//utils.validateOwnerDetails(calculationRequest);

		// get estimations here
		List<DemolitionCalculation> calculations = calculateDemolitionFee(calculationRequest);
		
		// generate the demands from Billing service here
		demandService.generateDemand(calculationRequest.getRequestInfo(), calculations);
		
		DemolitionCalculationResponse response = DemolitionCalculationResponse.builder().calculations(calculations)
				.build();
		producer.push(config.getSaveTopic(), response);
		return calculations;
	}

}
