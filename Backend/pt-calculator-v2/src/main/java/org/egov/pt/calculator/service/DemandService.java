package org.egov.pt.calculator.service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;
import org.egov.pt.calculator.repository.Repository;
import org.egov.pt.calculator.util.CalculatorConstants;
import org.egov.pt.calculator.util.CalculatorUtils;
import org.egov.pt.calculator.util.Configurations;
import org.egov.pt.calculator.validator.CalculationValidator;
import org.egov.pt.calculator.web.models.*;
import org.egov.pt.calculator.web.models.collections.Payment;
import org.egov.pt.calculator.web.models.demand.*;
import org.egov.pt.calculator.web.models.property.OwnerInfo;
import org.egov.pt.calculator.web.models.property.Property;
import org.egov.pt.calculator.web.models.property.PropertyCriteria;
import org.egov.pt.calculator.web.models.property.PropertyDetail;
import org.egov.pt.calculator.web.models.property.RequestInfoWrapper;
import org.egov.pt.calculator.web.models.propertyV2.PropertyV2;
import org.egov.tracer.model.CustomException;
import org.egov.tracer.model.ServiceCallException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

import static org.egov.pt.calculator.util.CalculatorConstants.*;

@Service
@Slf4j
public class DemandService {

    @Autowired
    private Configurations configurations;

	@Autowired
	private EstimationService estimationService;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private Configurations configs;

	@Autowired
	private AssessmentService assessmentService;

	@Autowired
	private CalculatorUtils utils;

	@Autowired
	private Repository repository;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private PayService payService;

	@Autowired
	private MasterDataService mstrDataService;

	@Autowired
	private CalculationValidator validator;

	@Autowired
	private MasterDataService mDataService;

	@Autowired
    private PaymentService paymentService;
	
	@Autowired
	private PropertyService propertyService; 

	/**
	 * Generates and persists the demand to billing service for the given property
	 * 
	 * if the property has been assessed already for the given financial year then
	 * 
	 * it carry forwards the old collection amount to the new demand as advance
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Calculation> generateDemands(CalculationReq request) {

		List<CalculationCriteria> criterias = request.getCalculationCriteria();
		List<Demand> demands = new ArrayList<>();
		List<String> lesserAssessments = new ArrayList<>();
		Map<String, String> consumerCodeFinYearMap = new HashMap<>();
		Map<String,Object> masterMap = mDataService.getMasterMap(request);
		//For applyTimeBasedApplicables
		RequestInfo requestInfo = request.getRequestInfo();
		Map<String, Map<String, List<Object>>> propertyBasedExemptionMasterMap = new HashMap<>();
		Map<String, JSONArray> timeBasedExmeptionMasterMap = new HashMap<>();


		Map<String, Calculation> propertyCalculationMap = estimationService.getEstimationPropertyMap(request,masterMap);
		for (CalculationCriteria criteria : criterias) {

			Property property = criteria.getProperty();

			PropertyDetail detail = property.getPropertyDetails().get(0);

			Calculation calculation = propertyCalculationMap.get(property.getPropertyDetails().get(0).getAssessmentNumber());

			String assessmentNumber = detail.getAssessmentNumber();

			// pt_tax for the new assessment
			BigDecimal newTax =  BigDecimal.ZERO;
			Optional<TaxHeadEstimate> advanceCarryforwardEstimate = propertyCalculationMap.get(assessmentNumber).getTaxHeadEstimates()
			.stream().filter(estimate -> estimate.getTaxHeadCode().equalsIgnoreCase(CalculatorConstants.PT_TAX))
				.findAny();
			if(advanceCarryforwardEstimate.isPresent())
				newTax = advanceCarryforwardEstimate.get().getEstimateAmount();

			Demand oldDemand = utils.getLatestDemandForCurrentFinancialYear(request.getRequestInfo(),criteria);

			// true represents that the demand should be updated from this call
			BigDecimal carryForwardCollectedAmount = getCarryForwardAndCancelOldDemand(newTax, criteria,
					request.getRequestInfo(),oldDemand, false);

			if (carryForwardCollectedAmount.doubleValue() >= 0.0) {

				Demand demand = prepareDemand(property, calculation ,oldDemand);

				// Add billingSLabs in demand additionalDetails as map with key calculationDescription
				demand.setAdditionalDetails(Collections.singletonMap(BILLINGSLAB_KEY, calculation.getBillingSlabIds()));

				demands.add(demand);
				consumerCodeFinYearMap.put(demand.getConsumerCode(), detail.getFinancialYear());

			}else {
				lesserAssessments.add(assessmentNumber);
			}
		}
		
		if (!CollectionUtils.isEmpty(lesserAssessments)) {
			throw new CustomException(CalculatorConstants.EG_PT_DEPRECIATING_ASSESSMENT_ERROR,
					CalculatorConstants.EG_PT_DEPRECIATING_ASSESSMENT_ERROR_MSG + lesserAssessments);
		}

		List<Demand> demandsToCreate = new ArrayList<>();
		List<Demand> demandsToUpdate = new ArrayList<>();
		
		//To update the rebate and penalty as soon as demand is updated
		for(Demand demand : demands){
			
			String tenantId = demand.getTenantId();
			mstrDataService.setPropertyMasterValues(requestInfo, tenantId,
					propertyBasedExemptionMasterMap, timeBasedExmeptionMasterMap);
			
			List<TaxPeriod> taxPeriods = mstrDataService.getTaxPeriodList(requestInfo, tenantId);
			 
			applytimeBasedApplicables(demand,new RequestInfoWrapper(requestInfo), timeBasedExmeptionMasterMap,taxPeriods); 
		
			roundOffDecimalForDemand(demand, new RequestInfoWrapper(requestInfo));
			
			if(StringUtils.hasText(demand.getId()))
				demandsToUpdate.add(demand);
			else
				demandsToCreate.add(demand);
		}
		
		if (!CollectionUtils.isEmpty(demandsToUpdate)) {
			updateDemands(demandsToUpdate, request);
		}
		if(!CollectionUtils.isEmpty(demandsToCreate)) {
			createDemands(demandsToCreate, request);
		}
	//	assessmentService.saveAssessments(res.getDemands(), consumerCodeFinYearMap, request.getRequestInfo());
		return propertyCalculationMap;
	}
	
	public void createDemands(List<Demand> demandsToCreate, CalculationReq request) {
		DemandRequest dmReq = DemandRequest.builder().demands(demandsToCreate).requestInfo(request.getRequestInfo())
				.build();

		String url = new StringBuilder().append(configs.getBillingServiceHost())
				.append(configs.getDemandCreateEndPoint()).toString();
		DemandResponse res = new DemandResponse();

		try {
			res = restTemplate.postForObject(url, dmReq, DemandResponse.class);

		} catch (HttpClientErrorException e) {
			throw new ServiceCallException(e.getResponseBodyAsString());
		}
		log.info(" The demand Response is : " + res);
	}
	
	public void updateDemands(List<Demand> demandsToUpdate, CalculationReq request) {	
		DemandRequest dmReq = DemandRequest.builder().demands(demandsToUpdate).requestInfo(request.getRequestInfo())
				.build();

		String url = new StringBuilder().append(configs.getBillingServiceHost())
				.append(configs.getDemandUpdateEndPoint()).toString();
		DemandResponse res = new DemandResponse();

		try {
			res = restTemplate.postForObject(url, dmReq, DemandResponse.class);

		} catch (HttpClientErrorException e) {
			throw new ServiceCallException(e.getResponseBodyAsString());
		}
		log.info(" The demand Response is : " + res);
	}

	/**
	 * Generates and returns bill from billing service
	 * 
	 * updates the demand with penalty and rebate if applicable before generating
	 * bill
	 * 
	 * @param getBillCriteria
	 * @param requestInfoWrapper
	 */
	public BillResponse getBill(GetBillCriteria getBillCriteria, RequestInfoWrapper requestInfoWrapper) {

		DemandResponse res = updateDemands(getBillCriteria, requestInfoWrapper);

		/**
		 * Loop through the demands and call generateBill for each demand.
		 * Group the Bills and return the bill responsew
		 */
		List<Bill> bills = new LinkedList<>();
		BillResponse billResponse;
		ResponseInfo responseInfo = null;
		StringBuilder billGenUrl;

		Set<String> consumerCodes = res.getDemands().stream().map(Demand::getConsumerCode).collect(Collectors.toSet());

		// If toDate or fromDate is not given bill is generated across all taxPeriod for the given consumerCode
		if(getBillCriteria.getToDate()==null || getBillCriteria.getFromDate()==null){
			for(String consumerCode : consumerCodes){
				billGenUrl = utils.getBillGenUrl(getBillCriteria.getTenantId(), consumerCode);
				billResponse = mapper.convertValue(repository.fetchResult(billGenUrl, requestInfoWrapper), BillResponse.class);
				responseInfo = billResponse.getResposneInfo();
				bills.addAll(billResponse.getBill());
			}
		}
		// else if toDate and fromDate is given bill is generated for the taxPeriod corresponding to given dates for the given consumerCode
		else {
			for(Demand demand : res.getDemands()){
				billGenUrl = utils.getBillGenUrl(getBillCriteria.getTenantId(),demand.getId(),demand.getConsumerCode());
				billResponse = mapper.convertValue(repository.fetchResult(billGenUrl, requestInfoWrapper), BillResponse.class);
				responseInfo = billResponse.getResposneInfo();
				bills.addAll(billResponse.getBill());
			}
		}


		return BillResponse.builder().resposneInfo(responseInfo).bill(bills).build();
	}

	/**
	 * Method updates the demands based on the getBillCriteria
	 * 
	 * The response will be the list of demands updated for the 
	 * @param getBillCriteria
	 * @param requestInfoWrapper
	 * @return
	 */
	public DemandResponse updateDemands(GetBillCriteria getBillCriteria, RequestInfoWrapper requestInfoWrapper) {
		
		if(getBillCriteria.getAmountExpected() == null) getBillCriteria.setAmountExpected(BigDecimal.ZERO);
		validator.validateGetBillCriteria(getBillCriteria);
		RequestInfo requestInfo = requestInfoWrapper.getRequestInfo();
		Map<String, Map<String, List<Object>>> propertyBasedExemptionMasterMap = new HashMap<>();
		Map<String, JSONArray> timeBasedExmeptionMasterMap = new HashMap<>();
		mstrDataService.setPropertyMasterValues(requestInfo, getBillCriteria.getTenantId(),
				propertyBasedExemptionMasterMap, timeBasedExmeptionMasterMap);

/*
		if(CollectionUtils.isEmpty(getBillCriteria.getConsumerCodes()))
			getBillCriteria.setConsumerCodes(Collections.singletonList(getBillCriteria.getPropertyId()+ CalculatorConstants.PT_CONSUMER_CODE_SEPARATOR +getBillCriteria.getAssessmentNumber()));
*/

		DemandResponse res = mapper.convertValue(
				repository.fetchResult(utils.getDemandSearchUrl(getBillCriteria), requestInfoWrapper),
				DemandResponse.class);
		if (CollectionUtils.isEmpty(res.getDemands())) {
			Map<String, String> map = new HashMap<>();
			map.put(CalculatorConstants.EMPTY_DEMAND_ERROR_CODE, CalculatorConstants.EMPTY_DEMAND_ERROR_MESSAGE);
			throw new CustomException(map);
		}


		/**
		 * Loop through the consumerCodes and re-calculate the time based applicables
		 */


		Map<String,List<Demand>> consumerCodeToDemandMap = new HashMap<>();
		res.getDemands().forEach(demand -> {
			if(consumerCodeToDemandMap.containsKey(demand.getConsumerCode()))
				consumerCodeToDemandMap.get(demand.getConsumerCode()).add(demand);
			else {
				List<Demand> demands = new LinkedList<>();
				demands.add(demand);
				consumerCodeToDemandMap.put(demand.getConsumerCode(),demands);
			}
		});
		
		if (CollectionUtils.isEmpty(consumerCodeToDemandMap))
			throw new CustomException(CalculatorConstants.EMPTY_DEMAND_ERROR_CODE,
					"No demands were found for the given consumerCodes : " + getBillCriteria.getConsumerCodes());

		List<Demand> demandsToBeUpdated = new LinkedList<>();

		String tenantId = getBillCriteria.getTenantId();

		List<TaxPeriod> taxPeriods = mstrDataService.getTaxPeriodList(requestInfoWrapper.getRequestInfo(), tenantId);

		for (String consumerCode : getBillCriteria.getConsumerCodes()) {
			List<Demand> demands = consumerCodeToDemandMap.get(consumerCode);
			if (CollectionUtils.isEmpty(demands))
				continue;

			for(Demand demand : demands){
				if (demand.getStatus() != null
						&& CalculatorConstants.DEMAND_CANCELLED_STATUS.equalsIgnoreCase(demand.getStatus().toString()))
					throw new CustomException(CalculatorConstants.EG_PT_INVALID_DEMAND_ERROR,
							CalculatorConstants.EG_PT_INVALID_DEMAND_ERROR_MSG);

				applytimeBasedApplicables(demand, requestInfoWrapper, timeBasedExmeptionMasterMap,taxPeriods);

				roundOffDecimalForDemand(demand, requestInfoWrapper);

				demandsToBeUpdated.add(demand);
			}
		}


		/**
		 * Call demand update in bulk to update the interest or penalty
		 */
		DemandRequest request = DemandRequest.builder().demands(demandsToBeUpdated).requestInfo(requestInfo).build();
		StringBuilder updateDemandUrl = utils.getUpdateDemandUrl();
		repository.fetchResult(updateDemandUrl, request);
		return res;
	}

	/**
	 * if any previous assessments and demands associated with it exists for the
	 * same financial year
	 * 
	 * Then Returns the collected amount of previous demand if the current
	 * assessment is for the current year
	 * 
	 * and cancels the previous demand by updating it's status to inactive
	 * 
	 * @param criteria
	 * @return
	 */
	protected BigDecimal getCarryForwardAndCancelOldDemand(BigDecimal newTax, CalculationCriteria criteria, RequestInfo requestInfo
			,Demand demand, boolean cancelDemand) {

		Property property = criteria.getProperty();

		BigDecimal carryForward = BigDecimal.ZERO;
		BigDecimal oldTaxAmt = BigDecimal.ZERO;

		if(null == property.getPropertyId()) return carryForward;

	//	Demand demand = getLatestDemandForCurrentFinancialYear(requestInfo, property);
		
		if(null == demand) return carryForward;

		carryForward = utils.getTotalCollectedAmountAndPreviousCarryForward(demand);
		
		for (DemandDetail detail : demand.getDemandDetails()) {
			if (detail.getTaxHeadMasterCode().equalsIgnoreCase(CalculatorConstants.PT_TAX))
				oldTaxAmt = oldTaxAmt.add(detail.getTaxAmount());
		}			

		log.debug("The old tax amount in string : " + oldTaxAmt.toPlainString());
		log.debug("The new tax amount in string : " + newTax.toPlainString());
		
		if (oldTaxAmt.compareTo(newTax) > 0) {
			boolean isDepreciationAllowed = utils.isAssessmentDepreciationAllowed(demand,new RequestInfoWrapper(requestInfo));
			if (!isDepreciationAllowed)
				carryForward = BigDecimal.valueOf(-1);
		}
		
		if(BigDecimal.ZERO.compareTo(carryForward) > 0 ) {
			log.info(" The old tax amount in carry forward is  : " +  carryForward);
			carryForward = BigDecimal.ZERO;
		}

		if (BigDecimal.ZERO.compareTo(carryForward) > 0 || !cancelDemand) return carryForward;
		
		demand.setStatus(Demand.DemandStatusEnum.CANCELLED);
		DemandRequest request = DemandRequest.builder().demands(Arrays.asList(demand)).requestInfo(requestInfo).build();
		StringBuilder updateDemandUrl = utils.getUpdateDemandUrl();
		repository.fetchResult(updateDemandUrl, request);

		return carryForward;
	}

/*	*//**
	 * @param requestInfo
	 * @param property
	 * @return
	 *//*
	@Deprecated
	public Demand getLatestDemandForCurrentFinancialYear(RequestInfo requestInfo, Property property) {
		
		Assessment assessment = Assessment.builder().propertyId(property.getPropertyId())
				.tenantId(property.getTenantId())
				.assessmentYear(property.getPropertyDetails().get(0).getFinancialYear()).build();

		List<Assessment> assessments = assessmentService.getMaxAssessment(assessment);

		if (CollectionUtils.isEmpty(assessments))
			return null;

		Assessment latestAssessment = assessments.get(0);
		log.debug(" the latest assessment : " + latestAssessment);

		DemandResponse res = mapper.convertValue(
				repository.fetchResult(utils.getDemandSearchUrl(latestAssessment), new RequestInfoWrapper(requestInfo)),
				DemandResponse.class);
		return res.getDemands().get(0);
	}*/





	/**
	 * Prepares Demand object based on the incoming calculation object and property
	 * 
	 * @param property
	 * @param calculation
	 * @return
	 */
	private Demand prepareDemand(Property property, Calculation calculation,Demand demand) {

		String tenantId = property.getTenantId();
		PropertyDetail detail = property.getPropertyDetails().get(0);
		String propertyType = detail.getPropertyType();
		String consumerCode = property.getPropertyId();
		BigDecimal minimumAmountPayable = BigDecimal.ZERO;

		OwnerInfo owner = null;

		for(OwnerInfo ownerInfo : detail.getOwners()){
			if(ownerInfo.getStatus().toString().equalsIgnoreCase(OwnerInfo.OwnerStatus.ACTIVE.toString())){
				owner = ownerInfo;
				break;
			}
		}	

		/*if (null != detail.getCitizenInfo())
			owner = detail.getCitizenInfo();
		else
			owner = detail.getOwners().iterator().next();*/
		
	//	Demand demand = getLatestDemandForCurrentFinancialYear(requestInfo, property);

		List<DemandDetail> details = new ArrayList<>();

		details = getAdjustedDemandDetails(tenantId,calculation,demand);
		
		if(configs.getPtMinAmountPayableFixed()) {
			minimumAmountPayable = BigDecimal.valueOf(configs.getPtMinAmountPayable());
		}else {
			minimumAmountPayable = calculation.getTaxAmount().multiply(configs.getPtMinAmountPayablePercentage())
					.divide(BigDecimal.valueOf(100));
		}

		if (demand != null && (demand.getTaxPeriodFrom().equals(calculation.getFromDate()))
				&& demand.getTaxPeriodTo().equals(calculation.getToDate())) {
			return Demand.builder().tenantId(tenantId).businessService(configs.getPtModuleCode()).consumerType(propertyType)
					.consumerCode(consumerCode).payer(owner.toCommonUser()).taxPeriodFrom(calculation.getFromDate())
					.taxPeriodTo(calculation.getToDate()).status(Demand.DemandStatusEnum.ACTIVE)
					.minimumAmountPayable(minimumAmountPayable).demandDetails(details).id(demand.getId())
					.build();
		} else {
			return Demand.builder().tenantId(tenantId).businessService(configs.getPtModuleCode())
					.consumerType(propertyType).consumerCode(consumerCode).payer(owner.toCommonUser())
					.taxPeriodFrom(calculation.getFromDate()).taxPeriodTo(calculation.getToDate())
					.status(Demand.DemandStatusEnum.ACTIVE).minimumAmountPayable(minimumAmountPayable)
					.demandDetails(details).build();
		}
	}

	/**
	 * Applies Penalty/Rebate/Interest to the incoming demands
	 * 
	 * If applied already then the demand details will be updated
	 * 
	 * @param demand
	 * @return
	 */
	private boolean applytimeBasedApplicables(Demand demand,RequestInfoWrapper requestInfoWrapper,
			Map<String, JSONArray> timeBasedExmeptionMasterMap,List<TaxPeriod> taxPeriods) {

		boolean isCurrentDemand = false;
		String tenantId = demand.getTenantId();
		String demandId = demand.getId();
		
		TaxPeriod taxPeriod = taxPeriods.stream()
				.filter(t -> demand.getTaxPeriodFrom().compareTo(t.getFromDate()) >= 0
				&& demand.getTaxPeriodTo().compareTo(t.getToDate()) <= 0)
		.findAny().orElse(null);
		
		//Checks if the current date id within demand from and to date 
		if((demand.getTaxPeriodFrom()<= System.currentTimeMillis() && demand.getTaxPeriodTo() >= System.currentTimeMillis()))
			isCurrentDemand = true;
		
		log.debug("isCurrentDemand ["+isCurrentDemand+"]");
		/*
		 * method to get the latest collected time from the receipt service
		 */
		List<Payment> payments = paymentService.getPaymentsFromDemand(demand,requestInfoWrapper);


		boolean isRebateUpdated = false;
		boolean isPenaltyUpdated = false;
		boolean isInterestUpdated = false;
		
		List<DemandDetail> details = demand.getDemandDetails();

		BigDecimal taxAmt = utils.getTaxAmtFromDemandForApplicablesGeneration(demand);
		BigDecimal taxAmtForPenalty = utils.getTaxAmtForDemandForPenaltyGeneration(demand);
		BigDecimal taxAmtForRebate = utils.getTaxAmtForDemandForRebateGeneration(demand);
		
		BigDecimal collectedPtTax = BigDecimal.ZERO;
		BigDecimal totalCollectedAmount = BigDecimal.ZERO;

		for (DemandDetail detail : demand.getDemandDetails()) {

			totalCollectedAmount = totalCollectedAmount.add(detail.getCollectionAmount());
			if (CalculatorConstants.TAXES_TO_BE_CONSIDERD.contains(detail.getTaxHeadMasterCode()))
				collectedPtTax = collectedPtTax.add(detail.getCollectionAmount());
		}

		BigDecimal penalty = BigDecimal.ZERO;
		BigDecimal rebate = BigDecimal.ZERO;
		BigDecimal interest = BigDecimal.ZERO;
		
		//Penalty will be applied to selected ulb's and on arrear amount
		List<String> ulbForCalculatingPenalty = Arrays.asList(configurations.getUlbForCalculatingPenalty().split(","));
		if(ulbForCalculatingPenalty.contains(demand.getTenantId())){
			penalty = payService.applyPenalty(taxAmtForPenalty,collectedPtTax,
	                taxPeriod.getFinancialYear(), timeBasedExmeptionMasterMap,payments,taxPeriod);
		}
		
		log.debug("Penalty Amount ["+penalty+"]");
		
		//Rebate is only applicable for current years demand
		if(isCurrentDemand) {
			rebate = payService.applyRebate(taxAmtForRebate, collectedPtTax, taxPeriod.getFinancialYear(),
					timeBasedExmeptionMasterMap, payments, taxPeriod);
		}
		
		
		interest = payService.applyInterest(taxAmt,collectedPtTax,
                taxPeriod.getFinancialYear(), timeBasedExmeptionMasterMap,payments,taxPeriod);
		
		
		if(Objects.isNull(penalty) && Objects.isNull(rebate) && Objects.isNull(interest)) return isCurrentDemand;
		
		DemandDetailAndCollection latestPenaltyDemandDetail, latestInterestDemandDetail;
		
		//Rebate will only be calculated/adjusted iff the demand is not paid fully
		if(!demand.getIsPaymentCompleted()) {
			
			BigDecimal oldRebate = BigDecimal.ZERO;
			BigDecimal oldCollectedRebate = BigDecimal.ZERO;
			boolean rebateTaxHeadExists = false;
			boolean isRebateUpdateRequired=true;
			if(taxAmtForRebate.compareTo(BigDecimal.ZERO) <= 0) {
				isRebateUpdateRequired=false;
			}
			
			for(DemandDetail demandDetail : details) {
				if(demandDetail.getTaxHeadMasterCode().equalsIgnoreCase(PT_TIME_REBATE)){
					oldRebate = oldRebate.add(demandDetail.getTaxAmount());
					oldCollectedRebate = oldCollectedRebate.add(demandDetail.getCollectionAmount());
					rebateTaxHeadExists = true;
				}
			}
			
			if(isRebateUpdateRequired && rebateTaxHeadExists) {
				if(oldRebate.compareTo(BigDecimal.ZERO) != 0) {
					if(oldCollectedRebate.compareTo(BigDecimal.ZERO) != 0) {
						adjustRebateAmountFromExistingTaxHeads(demand, oldCollectedRebate.negate());
					}
				}
				
				resetExistingRebateWithNewAmountTaxHeads(demand, rebate.negate());
			}else if(isRebateUpdateRequired && isCurrentDemand) {
				details.add(DemandDetail.builder().taxAmount(rebate.negate())
						.taxHeadMasterCode(PT_TIME_REBATE).demandId(demandId).tenantId(tenantId)
						.build());
			}
		}
		
		
		
		if(interest.compareTo(BigDecimal.ZERO)!=0){
			latestInterestDemandDetail = utils.getLatestDemandDetailByTaxHead(PT_TIME_INTEREST,details);
			if(latestInterestDemandDetail!=null){
				updateTaxAmount(interest,latestInterestDemandDetail);
				isInterestUpdated = true;
			}
		}

		//Penalty will applied to arrear amount and not the current years demand
		if(penalty.compareTo(BigDecimal.ZERO)!=0 && !isCurrentDemand){
			latestPenaltyDemandDetail = utils.getLatestDemandDetailByTaxHead(PT_TIME_PENALTY,details);
			if(latestPenaltyDemandDetail!=null){
				updateTaxAmount(penalty,latestPenaltyDemandDetail);
				isPenaltyUpdated = true;
			}
		}
		
		//Inserts new PT_TIME_PENALTY tax head in demand details if it does not exists
		if (!isPenaltyUpdated && penalty.compareTo(BigDecimal.ZERO) > 0 && !isCurrentDemand)
			details.add(DemandDetail.builder().taxAmount(penalty).taxHeadMasterCode(CalculatorConstants.PT_TIME_PENALTY)
					.demandId(demandId).tenantId(tenantId).build());
		
		//Inserts new PT_TIME_INTERESET tax head in demand details if it does not exists
		if (!isInterestUpdated && interest.compareTo(BigDecimal.ZERO) > 0)
			details.add(
					DemandDetail.builder().taxAmount(interest).taxHeadMasterCode(CalculatorConstants.PT_TIME_INTEREST)
							.demandId(demandId).tenantId(tenantId).build());
		
		return isCurrentDemand;
	}

	/**
	 * 
	 * Balances the decimal values in the newly updated demand by performing a roundoff
	 * 
	 * @param demand
	 * @param requestInfoWrapper
	 */
	/*public void roundOffDecimalForDemand(Demand demand, RequestInfoWrapper requestInfoWrapper) {
		
		List<DemandDetail> details = demand.getDemandDetails();
		String tenantId = demand.getTenantId();
		String demandId = demand.getId();

		BigDecimal taxAmount = BigDecimal.ZERO;
		BigDecimal collectedAmount = BigDecimal.ZERO;

		// Collecting the taxHead master codes with the isDebit field in a Map
		Map<String, Boolean> isTaxHeadDebitMap = mstrDataService.getTaxHeadMasterMap(requestInfoWrapper.getRequestInfo(), tenantId).stream()
				.collect(Collectors.toMap(TaxHeadMaster::getCode, TaxHeadMaster::getIsDebit));

		
		 * Summing the credit amount and Debit amount in to separate variables(based on the taxhead:isdebit map) to send to roundoffDecimal method
		 

		BigDecimal totalRoundOffAmount = BigDecimal.ZERO;
		BigDecimal collectedRoundOffAmount = BigDecimal.ZERO;
		for (DemandDetail detail : demand.getDemandDetails()) {

			if(!detail.getTaxHeadMasterCode().equalsIgnoreCase(PT_ROUNDOFF)){
				taxAmount = taxAmount.add(detail.getTaxAmount());
				collectedAmount = collectedAmount.add(detail.getCollectionAmount());
			}
			else{
				totalRoundOffAmount = totalRoundOffAmount.add(detail.getTaxAmount());
				collectedRoundOffAmount = collectedRoundOffAmount.add(detail.getCollectionAmount());
			}
		}

		
		 *  An estimate object will be returned incase if there is a decimal value
		 *  
		 *  If no decimal value found null object will be returned 
		 
		TaxHeadEstimate roundOffEstimate = payService.roundOffDecimals(taxAmount.subtract(collectedAmount),totalRoundOffAmount);



		BigDecimal decimalRoundOff = null != roundOffEstimate
				? roundOffEstimate.getEstimateAmount() : BigDecimal.ZERO;

		// Patch fix for roundoff amount for migrated data
		if(taxAmount.subtract(collectedAmount).compareTo(BigDecimal.ZERO) ==  0
				&& totalRoundOffAmount.subtract(collectedRoundOffAmount).compareTo(BigDecimal.ZERO) !=  0) {
			decimalRoundOff = BigDecimal.ZERO;
			for (DemandDetail detail : demand.getDemandDetails()) {
				if(PT_ROUNDOFF.equalsIgnoreCase(detail.getTaxHeadMasterCode())) {
					detail.setTaxAmount(decimalRoundOff);
				}
			};
		}
		
		if(taxAmount.subtract(collectedAmount).compareTo(BigDecimal.ZERO) != 0
				&& decimalRoundOff.compareTo(BigDecimal.ZERO)!=0){
				details.add(DemandDetail.builder().taxAmount(roundOffEstimate.getEstimateAmount())
						.taxHeadMasterCode(roundOffEstimate.getTaxHeadCode()).demandId(demandId).tenantId(tenantId).build());
		}


	}*/
	
	public void roundOffDecimalForDemand(Demand demand, RequestInfoWrapper requestInfoWrapper) {
		List<DemandDetail> demandDetails = demand.getDemandDetails();
		String tenantId = demand.getTenantId();
		
		BigDecimal totalTax = BigDecimal.ZERO;

		BigDecimal previousRoundOff = BigDecimal.ZERO;

		/*
		 * Sum all taxHeads except RoundOff as new roundOff will be calculated
		 */
		for (DemandDetail demandDetail : demandDetails) {
			if (!demandDetail.getTaxHeadMasterCode().equalsIgnoreCase(PT_ROUNDOFF))
				totalTax = totalTax.add(demandDetail.getTaxAmount().subtract(demandDetail.getCollectionAmount()));
			else
				previousRoundOff = previousRoundOff.add(demandDetail.getTaxAmount().subtract(demandDetail.getCollectionAmount()));
		}

		BigDecimal decimalValue = totalTax.remainder(BigDecimal.ONE);
		BigDecimal midVal = BigDecimal.valueOf(0.5);
		BigDecimal roundOff = BigDecimal.ZERO;

		/*
		 * If the decimal amount is greater than 0.5 we subtract it from 1 and
		 * put it as roundOff taxHead so as to nullify the decimal eg: If the
		 * tax is 12.64 we will add extra tax roundOff taxHead of 0.36 so that
		 * the total becomes 13
		 */
		if (decimalValue.compareTo(midVal) >= 0)
			roundOff = BigDecimal.ONE.subtract(decimalValue);

		/*
		 * If the decimal amount is less than 0.5 we put negative of it as
		 * roundOff taxHead so as to nullify the decimal eg: If the tax is 12.36
		 * we will add extra tax roundOff taxHead of -0.36 so that the total
		 * becomes 12
		 */
		if (decimalValue.compareTo(midVal) < 0)
			roundOff = decimalValue.negate();

		/*
		 * If roundOff already exists in previous demand create a new roundOff
		 * taxHead with roundOff amount equal to difference between them so that
		 * it will be balanced when bill is generated. eg: If the previous
		 * roundOff amount was of -0.36 and the new roundOff excluding the
		 * previous roundOff is 0.2 then the new roundOff will be created with
		 * 0.2 so that the net roundOff will be 0.2 -(-0.36)
		 */
		if (previousRoundOff.compareTo(BigDecimal.ZERO) != 0) {
			roundOff = roundOff.subtract(previousRoundOff);
		}

		if (roundOff.compareTo(BigDecimal.ZERO) != 0) {
			DemandDetail roundOffDemandDetail = DemandDetail.builder().taxAmount(roundOff)
					.taxHeadMasterCode(PT_ROUNDOFF).tenantId(tenantId)
					.collectionAmount(BigDecimal.ZERO).build();
			demandDetails.add(roundOffDemandDetail);
		}
	}

	/**
	 * Adjust positive rebate amount for partial payment into existing tax heads tax amount
	 * @param demandDetails
	 * @param additionalRebateAmount
	 * @return
	 */
	private List<DemandDetail> adjustRebateAmountFromExistingTaxHeads(Demand demand, BigDecimal additionalRebateAmount) {
		
		List<DemandDetail> demandDetails = demand.getDemandDetails();
		for(DemandDetail demandDetail : demandDetails) {
			if(!demandDetail.getTaxHeadMasterCode().equalsIgnoreCase(PT_TIME_REBATE) 
					&& !TAXES_WITH_EXCEMPTION.contains(demandDetail.getTaxHeadMasterCode()) 
					&& !PT_ADVANCE_CARRYFORWARD.contains(demandDetail.getTaxHeadMasterCode()) 
					&& demandDetail.getTaxAmount().compareTo(BigDecimal.ZERO) > 0
					&& demandDetail.getCollectionAmount().compareTo(BigDecimal.ZERO) > 0
					&& additionalRebateAmount.compareTo(BigDecimal.ZERO) > 0) {
				
				/*
				 * Adjust the old rebate amount by subtracting the same from the collection amount
				 * which was added during partial payment
				 */
				if(additionalRebateAmount.compareTo(demandDetail.getCollectionAmount()) <= 0) {
					demandDetail.setCollectionAmount(demandDetail.getCollectionAmount().subtract(additionalRebateAmount));
					break;
				}else {
					additionalRebateAmount = additionalRebateAmount.subtract(demandDetail.getCollectionAmount());
					demandDetail.setCollectionAmount(BigDecimal.ZERO);
				}
			}
		}
		
		return demandDetails;
	}
	
	/**
	 * Reset existing PT_TIME_REBATE taxhead with new amount
	 * @param demand
	 * @param newRebateAmount
	 * @return
	 */
	private List<DemandDetail> resetExistingRebateWithNewAmountTaxHeads(Demand demand, BigDecimal newRebateAmount) {
		List<DemandDetail> demandDetails = demand.getDemandDetails();
		boolean isRebateAlreadySet = false;
		for(DemandDetail demandDetail : demandDetails) {
			if(demandDetail.getTaxHeadMasterCode().equalsIgnoreCase(PT_TIME_REBATE)){
				if(!isRebateAlreadySet) {
					demandDetail.setTaxAmount(newRebateAmount);
					demandDetail.setCollectionAmount(BigDecimal.ZERO);
					isRebateAlreadySet = true;
				}else {
					demandDetail.setTaxAmount(BigDecimal.ZERO);
					demandDetail.setCollectionAmount(BigDecimal.ZERO);
				}
			}
		}
		return demandDetails;
	}

	/**
	 * Creates demandDetails for the new demand by adding all old demandDetails and then adding demandDetails
	 * using the difference between the new and old tax amounts for each taxHead
	 * @param tenantId The tenantId of the property
	 * @param calculation The calculation object for the property
	 * @param oldDemand The oldDemand against the property
	 * @return List of DemanDetails for the new demand
	 */
	private List<DemandDetail> getAdjustedDemandDetails(String tenantId,Calculation calculation,Demand oldDemand){

		List<DemandDetail> details = new ArrayList<>();

		/*Create map of taxHead to list of DemandDetail*/

		Map<String, List<DemandDetail>> taxHeadCodeDetailMap = new LinkedHashMap<>();
		if(oldDemand!=null){
			for(DemandDetail detail : oldDemand.getDemandDetails()){
				if(taxHeadCodeDetailMap.containsKey(detail.getTaxHeadMasterCode()))
					taxHeadCodeDetailMap.get(detail.getTaxHeadMasterCode()).add(detail);
				else {
					List<DemandDetail> detailList  = new LinkedList<>();
					detailList.add(detail);
					taxHeadCodeDetailMap.put(detail.getTaxHeadMasterCode(),detailList);
				}
			}
		}

		for (TaxHeadEstimate estimate : calculation.getTaxHeadEstimates()) {

			List<DemandDetail> detailList = taxHeadCodeDetailMap.get(estimate.getTaxHeadCode());
			taxHeadCodeDetailMap.remove(estimate.getTaxHeadCode());

			if (estimate.getTaxHeadCode().equalsIgnoreCase(CalculatorConstants.PT_ADVANCE_CARRYFORWARD))
				continue;

			if(!CollectionUtils.isEmpty(detailList)){
				details.addAll(detailList);
				BigDecimal amount= detailList.stream().map(DemandDetail::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

				details.add(DemandDetail.builder().taxHeadMasterCode(estimate.getTaxHeadCode())
						.taxAmount(estimate.getEstimateAmount().subtract(amount))
						.collectionAmount(BigDecimal.ZERO)
						.tenantId(tenantId).build());
			}
			else{
				details.add(DemandDetail.builder().taxHeadMasterCode(estimate.getTaxHeadCode())
						.taxAmount(estimate.getEstimateAmount())
						.collectionAmount(BigDecimal.ZERO)
						.tenantId(tenantId).build());
			}
		}

		/*
		* If some taxHeads are in old demand but not in new one a new demandetail
		*  is added for each taxhead to balance it out during apportioning
		* */

		for(Map.Entry<String, List<DemandDetail>> entry : taxHeadCodeDetailMap.entrySet()){
			List<DemandDetail> demandDetails = entry.getValue();
			BigDecimal taxAmount= demandDetails.stream().map(DemandDetail::getTaxAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal collectionAmount= demandDetails.stream().map(DemandDetail::getCollectionAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal netAmount = collectionAmount.subtract(taxAmount);
			if(CalculatorConstants.PT_TIME_REBATE.equals(entry.getKey()) && netAmount.compareTo(BigDecimal.ZERO) >= 0) {
				details.add(DemandDetail.builder().taxHeadMasterCode(entry.getKey())
						.taxAmount(netAmount.negate())
						.collectionAmount(BigDecimal.ZERO)
						.tenantId(tenantId).build());
			}else {
				details.add(DemandDetail.builder().taxHeadMasterCode(entry.getKey())
						.taxAmount(netAmount)
						.collectionAmount(BigDecimal.ZERO)
						.tenantId(tenantId).build());
			}
			
		}
		/*if (oldDemand != null) {
			if ((oldDemand.getTaxPeriodFrom().equals(calculation.getFromDate()))
					&& oldDemand.getTaxPeriodTo().equals(calculation.getToDate())) {

				String demandId = oldDemand.getId();
				for (DemandDetail demandDetail : details) {
					if (demandDetail.getDemandId() == null) {
						demandDetail.setDemandId(demandId);
					}
				}
			}
		}*/

		return details;
	}

	/**
	 * Updates the amount in the latest demandDetail by adding the diff between
	 * new and old amounts to it
	 * @param newAmount The new tax amount for the taxHead
	 * @param latestDetailInfo The latest demandDetail for the particular taxHead
	 */
	private void updateTaxAmount(BigDecimal newAmount,DemandDetailAndCollection latestDetailInfo){
		BigDecimal diff = newAmount.subtract(latestDetailInfo.getTaxAmountForTaxHead());
		BigDecimal newTaxAmountForLatestDemandDetail = latestDetailInfo.getLatestDemandDetail().getTaxAmount().add(diff);
		latestDetailInfo.getLatestDemandDetail().setTaxAmount(newTaxAmountForLatestDemandDetail);
	}

	public Map<String, Calculation> generateDemandsForMigration( MigrationAssessmentReq request) {
		Map<String, Calculation> returnMap = new HashMap<>();
		
		enrichDemand(request);
		
		DemandRequest dmReq = DemandRequest.builder().demands(request.getDemands()).requestInfo(request.getRequestInfo()).build();
		String url = new StringBuilder().append(configs.getBillingServiceHost())
				.append(configs.getDemandCreateEndPoint()).toString();
		DemandResponse res = new DemandResponse();

		try {
			res = restTemplate.postForObject(url, dmReq, DemandResponse.class);

		} catch (HttpClientErrorException e) {
			throw new ServiceCallException(e.getResponseBodyAsString());
		}
		log.info(" The demand Response is : " + res);
		return returnMap;
	}

	private void enrichDemand( MigrationAssessmentReq request) {
		for(CalculationCriteria criteria : request.getCalculationCriteria()) {
			Property property = criteria.getProperty();
			for(Demand demand : request.getDemands()) {
				updateDemandForMigration(property, demand);
			}
			
		}
		
	}
	
	private void updateDemandForMigration(Property property, Demand demand) {

		String tenantId = property.getTenantId();
		PropertyDetail detail = property.getPropertyDetails().get(0);
		String propertyType = detail.getPropertyType();
		String consumerCode = property.getPropertyId();

		OwnerInfo owner = null;

		for(OwnerInfo ownerInfo : detail.getOwners()){
			if(ownerInfo.getStatus().toString().equalsIgnoreCase(OwnerInfo.OwnerStatus.ACTIVE.toString())){
				owner = ownerInfo;
				break;
			}
		}	

		for(DemandDetail demandDetail : demand.getDemandDetails()) {
			demandDetail.setTenantId(tenantId);
		}
		
		demand.setTenantId(tenantId);
		demand.setBusinessService(configs.getPtModuleCode());
		demand.setConsumerType(propertyType);
		demand.setConsumerCode(consumerCode);
		demand.setPayer(owner.toCommonUser());
		demand.setStatus(Demand.DemandStatusEnum.ACTIVE);
		demand.setMinimumAmountPayable(BigDecimal.valueOf(configs.getPtMinAmountPayable()));

	}
	
	public List<Demand> modifyDemands(@Valid DemandRequest demandRequest) {
		log.info("modifyDemands >> ");
		List<Demand> demandsToBeUpdated = demandRequest.getDemands();
		
		List<Demand> demands = new LinkedList<>();
		
		Map<String, Property> propertyMap = null;
		validateDemandUpdateRquest(demandsToBeUpdated, demandRequest.getRequestInfo());
		
		PropertyCriteria propertyCriteria = null;
		
		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(demandRequest.getRequestInfo()).build();
		
		Map<String, Map<String, List<Object>>> propertyBasedExemptionMasterMap = new HashMap<>();
		Map<String, JSONArray> timeBasedExmeptionMasterMap = new HashMap<>();
		List<TaxPeriod> taxPeriods = null;
		
		for( Demand demand : demandsToBeUpdated ) {
			
			propertyCriteria = PropertyCriteria.builder().tenantId(demand.getTenantId())
					.propertyIds(Collections.singleton(demand.getConsumerCode())).build();
			propertyMap = propertyService.getPropertyMap(requestInfoWrapper, propertyCriteria);
			
			if(Objects.isNull(propertyMap) || propertyMap.isEmpty() || Objects.isNull(propertyMap.get(demand.getConsumerCode()))) {
				throw new CustomException("INVALID_DEMAND_UPDATE", "No demand exists for consumer code: "
						+ demand.getConsumerCode());
					
			}
			
			List<Demand> searchResult = searchDemand(demand.getTenantId(), Collections.singleton(demand.getConsumerCode()), demand.getTaxPeriodFrom(),
					demand.getTaxPeriodTo(),demand.getBusinessService(), demandRequest.getRequestInfo());
			Demand oldDemand = searchResult.get(0);
			demand.setDemandDetails(getUpdatedDemandDetails(demand.getDemandDetails(), oldDemand, demandRequest.getRequestInfo()));
			
			if (demand.getStatus() != null
					&& CalculatorConstants.DEMAND_CANCELLED_STATUS.equalsIgnoreCase(demand.getStatus().toString()))
				throw new CustomException(CalculatorConstants.EG_PT_INVALID_DEMAND_ERROR,
						CalculatorConstants.EG_PT_INVALID_DEMAND_ERROR_MSG);

			mstrDataService.setPropertyMasterValues(demandRequest.getRequestInfo(), demand.getTenantId(),
					propertyBasedExemptionMasterMap, timeBasedExmeptionMasterMap);
			
			taxPeriods = mstrDataService.getTaxPeriodList(requestInfoWrapper.getRequestInfo(), demand.getTenantId());
			
			applytimeBasedApplicablesForModifiedDemands(demand, requestInfoWrapper, timeBasedExmeptionMasterMap,taxPeriods,oldDemand);
			
			//round off demand details
			roundOffDecimalForDemand(demand, requestInfoWrapper);
			
			demands.add(demand);
			
		}
		
		return updateDemand(demandRequest.getRequestInfo(), demands);
	}
	
	/**
	 * Applies Penalty/Rebate/Interest to the incoming demands
	 * 
	 * If applied already then the demand details will be updated
	 * 
	 * @param demand
	 * @return
	 */
	private boolean applytimeBasedApplicablesForModifiedDemands(Demand demand,RequestInfoWrapper requestInfoWrapper,
			Map<String, JSONArray> timeBasedExmeptionMasterMap,List<TaxPeriod> taxPeriods, Demand oldDemand) {

		boolean isCurrentDemand = false;
		String tenantId = demand.getTenantId();
		String demandId = demand.getId();
		
		TaxPeriod taxPeriod = taxPeriods.stream()
				.filter(t -> demand.getTaxPeriodFrom().compareTo(t.getFromDate()) >= 0
				&& demand.getTaxPeriodTo().compareTo(t.getToDate()) <= 0)
		.findAny().orElse(null);
		
		//Checks if the current date id within demand from and to date 
		if((demand.getTaxPeriodFrom()<= System.currentTimeMillis() && demand.getTaxPeriodTo() >= System.currentTimeMillis()))
			isCurrentDemand = true;
		
		log.debug("isCurrentDemand ["+isCurrentDemand+"]");
		/*
		 * method to get the latest collected time from the receipt service
		 */
		List<Payment> payments = paymentService.getPaymentsFromDemand(demand,requestInfoWrapper);


		boolean isRebateUpdated = false;
		boolean isPenaltyUpdated = false;
//		boolean isInterestUpdated = false;
		
		List<DemandDetail> details = demand.getDemandDetails();

//		BigDecimal taxAmt = utils.getTaxAmtFromDemandForApplicablesGeneration(demand);
		BigDecimal taxAmtForPenalty = utils.getTaxAmtForModifiedDemandForPenaltyGeneration(demand, oldDemand);
		BigDecimal taxAmtForRebate = utils.getTaxAmtForDemandForRebateGeneration(demand);
		
		BigDecimal collectedPtTax = BigDecimal.ZERO;
		BigDecimal totalCollectedAmount = BigDecimal.ZERO;

		for (DemandDetail detail : demand.getDemandDetails()) {

			totalCollectedAmount = totalCollectedAmount.add(detail.getCollectionAmount());
			if (CalculatorConstants.TAXES_TO_BE_CONSIDERD.contains(detail.getTaxHeadMasterCode()))
				collectedPtTax = collectedPtTax.add(detail.getCollectionAmount());
		}

		BigDecimal penalty = BigDecimal.ZERO;
		BigDecimal rebate = BigDecimal.ZERO;
		//Penalty will be applied to selected ulb's and on arrear amount
		List<String> ulbForCalculatingPenalty = Arrays.asList(configurations.getUlbForCalculatingPenalty().split(","));
		if(ulbForCalculatingPenalty.contains(demand.getTenantId())){
			penalty = payService.applyPenaltyForModifiedDemand(taxAmtForPenalty,collectedPtTax,
	                taxPeriod.getFinancialYear(), timeBasedExmeptionMasterMap,payments,taxPeriod);
		}
		
		log.debug("Penalty Amount ["+penalty+"]");
		
		//Rebate is only applicable for current years demand
		if(isCurrentDemand) {
			rebate = payService.applyRebate(taxAmtForRebate, collectedPtTax, taxPeriod.getFinancialYear(),
					timeBasedExmeptionMasterMap, payments, taxPeriod);
		}
		
		
		if(Objects.isNull(penalty) && Objects.isNull(rebate)) return isCurrentDemand;
		

		
		BigDecimal oldRebate = BigDecimal.ZERO;
		BigDecimal oldCollectedRebate = BigDecimal.ZERO;
		boolean rebateTaxHeadExists = false;
		for(DemandDetail demandDetail : details) {
			if(demandDetail.getTaxHeadMasterCode().equalsIgnoreCase(PT_TIME_REBATE)){
				oldRebate = oldRebate.add(demandDetail.getTaxAmount());
				oldCollectedRebate = oldCollectedRebate.add(demandDetail.getCollectionAmount());
				rebateTaxHeadExists = true;
			}
		}
		
		if(rebateTaxHeadExists) {
			if(oldRebate.compareTo(BigDecimal.ZERO) != 0) {
				if(oldCollectedRebate.compareTo(BigDecimal.ZERO) != 0) {
					adjustRebateAmountFromExistingTaxHeads(demand, oldCollectedRebate.negate());
				}
			}
			
			resetExistingRebateWithNewAmountTaxHeads(demand, rebate.negate());
		}else if(isCurrentDemand) {
			details.add(DemandDetail.builder().taxAmount(rebate.negate())
					.taxHeadMasterCode(PT_TIME_REBATE).demandId(demandId).tenantId(tenantId)
					.build());
		}
		

		//Penalty will applied to arrear amount and not the current years demand
		if(penalty.compareTo(BigDecimal.ZERO)!=0 && !isCurrentDemand){
			isPenaltyUpdated = adjustPenaltyAmount(demand, penalty);
			
		}
		
		//Inserts new PT_TIME_PENALTY tax head in demand details if it does not exists
		if (!isPenaltyUpdated && penalty.compareTo(BigDecimal.ZERO) != 0 && !isCurrentDemand)
			details.add(DemandDetail.builder().taxAmount(penalty).taxHeadMasterCode(CalculatorConstants.PT_TIME_PENALTY)
					.demandId(demandId).tenantId(tenantId).build());
		
		
		return isCurrentDemand;
	}
	
	private boolean adjustPenaltyAmount(Demand demand, BigDecimal penalty) {
		BigDecimal totalTaxAmount = demand.getDemandDetails().stream().map(DemandDetail::getTaxAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalCollectedAmount = demand.getDemandDetails().stream().map(DemandDetail::getCollectionAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal totalDueAmount = totalTaxAmount.subtract(totalCollectedAmount);
		
		boolean isPenaltyUpdated = false;
		
		if(penalty.compareTo(BigDecimal.ZERO) < 0 && penalty.negate().compareTo(totalDueAmount) > 0 ) {
			throw new CustomException("INVALID_DEMAND_UPDATE", "Amount cannot be adjusted as penalty amount will be greater than due amount");
		}
		
		for(DemandDetail detail : demand.getDemandDetails()) {
			
			//If penalty amount is not collected then add the adjusted amount into existing penalty amount 
			if(CalculatorConstants.PT_TIME_PENALTY.equals(detail.getTaxHeadMasterCode())
        			&& detail.getTaxAmount().compareTo(BigDecimal.ZERO) > 0
        			&& detail.getCollectionAmount().compareTo(BigDecimal.ZERO) == 0) {
				detail.setTaxAmount(detail.getTaxAmount().add(penalty));
				isPenaltyUpdated = true;
        	}
		}
		return isPenaltyUpdated;
	}

	private void validateDemandUpdateRquest(List<Demand> demandsToBeUpdated,RequestInfo requestInfo) {
		Demand oldDemand = null;
		for( Demand demand : demandsToBeUpdated ) {
			Set<String> consumerCodes = Collections.singleton(demand.getConsumerCode());
			List<Demand> searchResult = searchDemand(demand.getTenantId(), Collections.singleton(demand.getConsumerCode()), demand.getTaxPeriodFrom(),
					demand.getTaxPeriodTo(),demand.getBusinessService(), requestInfo);
			if (CollectionUtils.isEmpty(searchResult))
				throw new CustomException("INVALID_DEMAND_UPDATE", "No demand exists for Number: "
						+ consumerCodes.toString());
			
			oldDemand = searchResult.get(0);
			
			//Checks if the current date id within demand from and to date 
			if((demand.getTaxPeriodFrom()<= System.currentTimeMillis() && demand.getTaxPeriodTo() >= System.currentTimeMillis())) {
				throw new CustomException("INVALID_DEMAND_UPDATE", "Demand for current financial year cannot be modified"
						+ demand.getConsumerCode());
			}
			
			if(oldDemand.getIsPaymentCompleted()) {
				throw new CustomException("INVALID_DEMAND_UPDATE", "Demand has already been paid for Number: "
						+ consumerCodes.toString());
			}
			
			BigDecimal totalOldTaxAmount = oldDemand.getDemandDetails().stream().map(DemandDetail::getTaxAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			BigDecimal totalOldCollectedAmount = oldDemand.getDemandDetails().stream().map(DemandDetail::getCollectionAmount)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
			
			if(totalOldTaxAmount.compareTo(totalOldCollectedAmount) == 0) {
				throw new CustomException("INVALID_DEMAND_UPDATE", "Demand has already been paid for Number: "
						+ consumerCodes.toString());
			}
			
			BigDecimal totalNewTaxAmount = BigDecimal.ZERO;
			
			boolean isExistingDemandDetails = false;
			
			for(DemandDetail newDemandDetail :  demand.getDemandDetails()) {
				isExistingDemandDetails = false;
				for(DemandDetail oldDemandDetail :  oldDemand.getDemandDetails()) {
					if(!Objects.isNull(newDemandDetail.getId()) && oldDemandDetail.getId().equals(newDemandDetail.getId())) {
						isExistingDemandDetails = true;
						if(oldDemandDetail.getCollectionAmount().compareTo(newDemandDetail.getCollectionAmount()) != 0) {
							throw new CustomException("INVALID_DEMAND_UPDATE", "Collected amount cannot be modified");
						}else if(oldDemandDetail.getTaxAmount().compareTo(newDemandDetail.getTaxAmount()) != 0) {
							throw new CustomException("INVALID_DEMAND_UPDATE",
									"Existing tax amount cannot be modified please provide separate line item");
						}
						break;
					}
				}
				if(!isExistingDemandDetails && !Objects.isNull(newDemandDetail.getId())) {
					throw new CustomException("INVALID_DEMAND_UPDATE",
							"Invalid Demand details. No demand existed with id [" + newDemandDetail.getId() + "]");
				}else if(!isExistingDemandDetails 
						&& !(Objects.isNull(newDemandDetail.getCollectionAmount())
								|| newDemandDetail.getCollectionAmount().compareTo(BigDecimal.ZERO) == 0)) {
					throw new CustomException("INVALID_DEMAND_UPDATE",
							"Collection amount not allowed for new tax head");
				}else if(!isExistingDemandDetails 
						&& TAXES_NOT_ALLOWED_TO_MODIFY.contains(newDemandDetail.getTaxHeadMasterCode())) {
					throw new CustomException("INVALID_DEMAND_UPDATE",
							"Tax amount not allowed to be modified for tax head ["+newDemandDetail.getTaxHeadMasterCode()+"]");
				}
				/*if(newDemandDetail.getTaxAmount().compareTo(newDemandDetail.getCollectionAmount()) < 0) {
					throw new CustomException("INVALID_DEMAND_UPDATE", "Collected amount is less than tax amount for tax head "
							+ newDemandDetail.getTaxHeadMasterCode());
				}*/
				totalNewTaxAmount = totalNewTaxAmount.add(newDemandDetail.getTaxAmount());
			}
			
//			if(totalNewTaxAmount.compareTo(totalOldCollectedAmount) < 0) {
//				throw new CustomException("INVALID_DEMAND_UPDATE",
//						"Total tax amount cannot be greater than total collected amount");
//			}
			
		}
		
		
	}
	
	/**
	 * Searches demand for the given consumerCode and tenantIDd
	 * 
	 * @param tenantId
	 *            The tenantId of the tradeLicense
	 * @param consumerCodes
	 *            The set of consumerCode of the demands
	 * @param requestInfo
	 *            The RequestInfo of the incoming request
	 * @return Lis to demands for the given consumerCode
	 */
	private List<Demand> searchDemand(String tenantId, Set<String> consumerCodes, Long taxPeriodFrom, Long taxPeriodTo,
			String businessService, RequestInfo requestInfo) {
        
		Object result = repository.fetchResult(
				utils.getDemandSearchURL(tenantId, consumerCodes, taxPeriodFrom, taxPeriodTo, businessService),
				RequestInfoWrapper.builder().requestInfo(requestInfo).build());
		try {
			return mapper.convertValue(result, DemandResponse.class).getDemands();
		} catch (IllegalArgumentException e) {
			throw new CustomException("PARSING_ERROR", "Failed to parse response from Demand Search");
		}

	}
	
	/**
	 * Returns the list of new DemandDetail to be added for updating the demand
	 * 
	 * @param calculation
	 *            The calculation object for the update request
	 * @param oldDemandDetails
	 *            The list of demandDetails from the existing demand
	 * @return The list of new DemandDetails
	 */
	private List<DemandDetail> getUpdatedDemandDetails(List<DemandDetail> updatedDemandDetails, Demand oldDemand, RequestInfo requestInfo) {

		List<DemandDetail> oldDemandDetails = oldDemand.getDemandDetails();
		List<DemandDetail> newDemandDetails = new ArrayList<>();
		Map<String, List<DemandDetail>> taxHeadToDemandDetail = new HashMap<>();
		

		oldDemandDetails.forEach(demandDetail -> {
			if (!taxHeadToDemandDetail.containsKey(demandDetail.getTaxHeadMasterCode())) {
				List<DemandDetail> demandDetailList = new LinkedList<>();
				demandDetailList.add(demandDetail);
				taxHeadToDemandDetail.put(demandDetail.getTaxHeadMasterCode(), demandDetailList);
			} else
				taxHeadToDemandDetail.get(demandDetail.getTaxHeadMasterCode()).add(demandDetail);
		});

		List<DemandDetail> demandDetailList;
		BigDecimal total;
		BigDecimal totalCollected;
		BigDecimal totalDue;
		

		for (DemandDetail updatedDemandDetail : updatedDemandDetails) {
			if (!taxHeadToDemandDetail.containsKey(updatedDemandDetail.getTaxHeadMasterCode()))
				newDemandDetails.add(DemandDetail.builder().taxAmount(updatedDemandDetail.getTaxAmount())
						.taxHeadMasterCode(updatedDemandDetail.getTaxHeadMasterCode()).tenantId(updatedDemandDetail.getTenantId())
						.collectionAmount(BigDecimal.ZERO).additionalDetails(updatedDemandDetail.getAdditionalDetails()).build());
			else {
				demandDetailList = taxHeadToDemandDetail.get(updatedDemandDetail.getTaxHeadMasterCode());
				total = demandDetailList.stream().map(DemandDetail::getTaxAmount).reduce(BigDecimal.ZERO,
						BigDecimal::add);
				totalCollected = demandDetailList.stream().map(DemandDetail::getCollectionAmount).reduce(BigDecimal.ZERO,
						BigDecimal::add);
				totalDue = total.subtract(totalCollected);
				
				
					
				if(!TAXES_WITH_EXCEMPTION.contains(updatedDemandDetail.getTaxHeadMasterCode()) && totalDue.compareTo(BigDecimal.ZERO) < 0) {
					throw new CustomException("INVALID_DEMAND_UPDATE",
							"Negative Tax amount not allowed for taxhead ["+updatedDemandDetail.getTaxHeadMasterCode()+"]");
				}else if(TAXES_WITH_EXCEMPTION.contains(updatedDemandDetail.getTaxHeadMasterCode()) && totalDue.compareTo(BigDecimal.ZERO) > 0) {
					throw new CustomException("INVALID_DEMAND_UPDATE",
							"Positive Tax amount not allowed for taxhead ["+updatedDemandDetail.getTaxHeadMasterCode()+"]");
				}
				
				//Add new adjusted tax heads
				if (Objects.isNull(updatedDemandDetail.getId()) && 
						(!TAXES_WITH_EXCEMPTION.contains(updatedDemandDetail.getTaxHeadMasterCode()) && totalDue.compareTo(BigDecimal.ZERO) >= 0)) {
					//For tax having that are not of exception total due must always be positive
					newDemandDetails.add(DemandDetail.builder().taxAmount(updatedDemandDetail.getTaxAmount())
							.taxHeadMasterCode(updatedDemandDetail.getTaxHeadMasterCode()).tenantId(updatedDemandDetail.getTenantId())
							.collectionAmount(BigDecimal.ZERO).additionalDetails(updatedDemandDetail.getAdditionalDetails()).build());
				}else if(Objects.isNull(updatedDemandDetail.getId()) && 
						(TAXES_WITH_EXCEMPTION.contains(updatedDemandDetail.getTaxHeadMasterCode()) && totalDue.compareTo(BigDecimal.ZERO) <= 0)) {
					
					if(updatedDemandDetail.getTaxAmount().compareTo(BigDecimal.ZERO) > 0 && 
							(totalDue.compareTo(BigDecimal.ZERO) == 0 
								|| updatedDemandDetail.getTaxAmount().add(totalDue).compareTo(BigDecimal.ZERO) > 0 ) ) {
						throw new CustomException("INVALID_DEMAND_UPDATE",
								"Positive Tax amount not allowed for taxhead ["+updatedDemandDetail.getTaxHeadMasterCode()+"]");
					}
					
					//For tax having exception total due must always be negative
					newDemandDetails.add(DemandDetail.builder().taxAmount(updatedDemandDetail.getTaxAmount())
							.taxHeadMasterCode(updatedDemandDetail.getTaxHeadMasterCode()).tenantId(updatedDemandDetail.getTenantId())
							.collectionAmount(BigDecimal.ZERO).additionalDetails(updatedDemandDetail.getAdditionalDetails()).build());
				}
			}
		}
		List<DemandDetail> combinedBillDetails = new LinkedList<>(oldDemandDetails);
		combinedBillDetails.addAll(newDemandDetails);
		
		BigDecimal diffInCollectedAmount = BigDecimal.ZERO;
		
		Map<String, List<DemandDetail>> newTaxHeadToDemandDetail = new HashMap<>();
		
		combinedBillDetails.forEach(demandDetail -> {
			if (!newTaxHeadToDemandDetail.containsKey(demandDetail.getTaxHeadMasterCode())) {
				List<DemandDetail> demandDetails = new LinkedList<>();
				demandDetails.add(demandDetail);
				newTaxHeadToDemandDetail.put(demandDetail.getTaxHeadMasterCode(), demandDetails);
			} else {
				newTaxHeadToDemandDetail.get(demandDetail.getTaxHeadMasterCode()).add(demandDetail);
			}
		});
		
		BigDecimal totalTaxAmntTaxHeadWise = BigDecimal.ZERO;
		BigDecimal totalCollectedAmntTaxHeadWise = BigDecimal.ZERO;
		
		/**
		 * Get total difference in collected amount if the tax amount has been reduced 
		 * from collected amount for a particular taxhead and
		 * adjust collected amount by reducting it upto tax amount
		 */
		for(DemandDetail updatedDemandDetail : combinedBillDetails) {
			
			demandDetailList = newTaxHeadToDemandDetail.get(updatedDemandDetail.getTaxHeadMasterCode());
			totalTaxAmntTaxHeadWise = demandDetailList.stream().map(DemandDetail::getTaxAmount).reduce(BigDecimal.ZERO,
					BigDecimal::add);
			totalCollectedAmntTaxHeadWise = demandDetailList.stream().map(DemandDetail::getCollectionAmount).reduce(BigDecimal.ZERO,
					BigDecimal::add);
			
			if(updatedDemandDetail.getCollectionAmount().compareTo(BigDecimal.ZERO) > 0
					&& updatedDemandDetail.getTaxAmount().compareTo(BigDecimal.ZERO) >= 0
					&& totalTaxAmntTaxHeadWise.compareTo(totalCollectedAmntTaxHeadWise) < 0) {
				diffInCollectedAmount = diffInCollectedAmount
						.add(totalCollectedAmntTaxHeadWise.subtract(totalTaxAmntTaxHeadWise));
				if(!Objects.isNull(updatedDemandDetail.getId())) {
					updatedDemandDetail.setCollectionAmount(updatedDemandDetail.getCollectionAmount().subtract(diffInCollectedAmount));
				}
			}
		}
		
		BigDecimal carryForwardAmount = diffInCollectedAmount;
		BigDecimal dueAmount = BigDecimal.ZERO;
		
		//Adjust colllected amount from diffInCollectedAmount
		
		if(diffInCollectedAmount.compareTo(BigDecimal.ZERO) > 0) {
			for(DemandDetail updatedDemandDetail : combinedBillDetails) {
				demandDetailList = newTaxHeadToDemandDetail.get(updatedDemandDetail.getTaxHeadMasterCode());
				totalTaxAmntTaxHeadWise = demandDetailList.stream().map(DemandDetail::getTaxAmount).reduce(BigDecimal.ZERO,
						BigDecimal::add);
				totalCollectedAmntTaxHeadWise = demandDetailList.stream().map(DemandDetail::getCollectionAmount).reduce(BigDecimal.ZERO,
						BigDecimal::add);
				dueAmount = totalTaxAmntTaxHeadWise.subtract(totalCollectedAmntTaxHeadWise);
				if(carryForwardAmount.compareTo(BigDecimal.ZERO) > 0) {
					if(updatedDemandDetail.getTaxAmount().compareTo(updatedDemandDetail.getCollectionAmount()) > 0 
							&& dueAmount.compareTo(BigDecimal.ZERO) > 0) {
						//adjust the amount from the total extra collected amount
						if(dueAmount.compareTo(carryForwardAmount) > 0) {
							//add remaining collected amount into any of the existing taxheads
							updatedDemandDetail.setCollectionAmount(updatedDemandDetail.getCollectionAmount().add(carryForwardAmount));
						}else {
							updatedDemandDetail.setCollectionAmount(updatedDemandDetail.getCollectionAmount().add(dueAmount));
						}
						carryForwardAmount = carryForwardAmount.subtract(dueAmount);
					}
				}
			}
		}
		
		return combinedBillDetails;
	}
	
	public List<Demand> updateDemand(RequestInfo requestInfo, List<Demand> demands){
        StringBuilder url = new StringBuilder(configs.getBillingServiceHost());
        url.append(configs.getDemandUpdateEndPoint());
        DemandRequest request = new DemandRequest(requestInfo,demands);
        Object result = repository.fetchResult(url, request);
        try{
            return mapper.convertValue(result,DemandResponse.class).getDemands();
        }
        catch(IllegalArgumentException e){
            throw new CustomException("PARSING_ERROR","Failed to parse response of update demand");
        }
    }

}
