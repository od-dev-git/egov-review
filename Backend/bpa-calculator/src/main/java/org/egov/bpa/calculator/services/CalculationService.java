package org.egov.bpa.calculator.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.edcr.model.Occupancy;
import org.egov.bpa.calculator.kafka.broker.BPACalculatorProducer;
import org.egov.bpa.calculator.repository.InstallmentRepository;
import org.egov.bpa.calculator.repository.OCOutsideRepository;
import org.egov.bpa.calculator.repository.PreapprovedPlanRepository;
import org.egov.bpa.calculator.repository.RevisionRepository;
import org.egov.bpa.calculator.repository.ServiceRequestRepository;
import org.egov.bpa.calculator.utils.BPACalculatorConstants;
import org.egov.bpa.calculator.utils.CalculationUtils;
import org.egov.bpa.calculator.utils.EdcrHelperUtils;
import org.egov.bpa.calculator.validators.InstallmentValidator;
import org.egov.bpa.calculator.web.models.BillingSlabSearchCriteria;
import org.egov.bpa.calculator.web.models.Calculation;
import org.egov.bpa.calculator.web.models.CalculationReq;
import org.egov.bpa.calculator.web.models.CalculationRes;
import org.egov.bpa.calculator.web.models.CalulationCriteria;
import org.egov.bpa.calculator.web.models.ExemptionFeeDetails;
import org.egov.bpa.calculator.web.models.Installment;
import org.egov.bpa.calculator.web.models.Installment.StatusEnum;
import org.egov.bpa.calculator.web.models.InstallmentRequest;
import org.egov.bpa.calculator.web.models.InstallmentSearchCriteria;
import org.egov.bpa.calculator.web.models.OtherFeeDetails;
import org.egov.bpa.calculator.web.models.PreapprovedPlan;
import org.egov.bpa.calculator.web.models.PreapprovedPlanSearchCriteria;
import org.egov.bpa.calculator.web.models.Revision;
import org.egov.bpa.calculator.web.models.RevisionSearchCriteria;
import org.egov.bpa.calculator.web.models.bpa.BPA;
import org.egov.bpa.calculator.web.models.bpa.EstimatesAndSlabs;
import org.egov.bpa.calculator.web.models.bpa.OtherFee;
import org.egov.bpa.calculator.web.models.demand.Category;
import org.egov.bpa.calculator.web.models.demand.Demand;
import org.egov.bpa.calculator.web.models.demand.DemandDetail;
import org.egov.bpa.calculator.web.models.demand.TaxHeadEstimate;
import org.egov.bpa.calculator.web.models.oc.Fee;
import org.egov.bpa.calculator.web.models.oc.OutsideOCDetails;
import org.egov.bpa.calculator.web.models.oc.ScrutinyDetails;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

@Service
@Slf4j
public class CalculationService {

	@Autowired
	private MDMSService mdmsService;

	@Autowired
	private DemandService demandService;

	@Autowired
	private EDCRService edcrService;

	@Autowired
	private BPACalculatorConfig config;

	@Autowired
	private CalculationUtils utils;

	@Autowired
	private BPACalculatorProducer producer;

	@Autowired
	private BPAService bpaService;
	
	@Autowired
	private AlterationCalculationService alterationCalculationService;
	
	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	@Autowired
	private PreapprovedPlanRepository preapprovedPlanRepository;
	
	@Autowired
	private InstallmentRepository installmentRepository;
	
	@Autowired
	private InstallmentValidator installmentValidator;
	
	@Autowired
	private RevisionRepository revisionRepository;
	
	
	@Autowired
	private EdcrHelperUtils edcrHelperUtils;
	
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private OCOutsideRepository ocOutsideRepository;

	private static final BigDecimal ZERO_TWO_FIVE = new BigDecimal("0.25");// BigDecimal.valueOf(0.25);
	private static final BigDecimal ZERO_FIVE = new BigDecimal("0.5");// BigDecimal.valueOf(0.5);
	private static final BigDecimal TEN = new BigDecimal("10");// BigDecimal.valueOf(10);
	private static final BigDecimal FIFTEEN = new BigDecimal("15");// BigDecimal.valueOf(15);
//	private static final BigDecimal SEVENTEEN_FIVE = new BigDecimal("17.50");// BigDecimal.valueOf(17.50);
	//private static final BigDecimal SEVENTEEN_POINT_EIGHT_FIVE = new BigDecimal("17.85");// BigDecimal.valueOf(17.50);
	private static final BigDecimal EIGHTEEN_POINT_TWO_ONE = new BigDecimal("18.21");
	private static final BigDecimal EIGHTEEN_POINT_FIVE_SEVEN = new BigDecimal("18.57");
	private static final BigDecimal EIGHTEEN_POINT_NINE_FOUR = new BigDecimal("18.94");
	private static final BigDecimal TWENTY = new BigDecimal("20");// BigDecimal.valueOf(20);
	private static final BigDecimal TWENTY_FIVE = new BigDecimal("25");// BigDecimal.valueOf(25);
	private static final BigDecimal THIRTY = new BigDecimal("30");// BigDecimal.valueOf(30);
	private static final BigDecimal FIFTY = new BigDecimal("50");// BigDecimal.valueOf(50);
	private static final BigDecimal HUNDRED = new BigDecimal("100");// BigDecimal.valueOf(100);
	private static final BigDecimal TWO_HUNDRED = new BigDecimal("200");// BigDecimal.valueOf(200);
	private static final BigDecimal TWO_HUNDRED_FIFTY = new BigDecimal("250");// BigDecimal.valueOf(250);
	private static final BigDecimal THREE_HUNDRED = new BigDecimal("300");// BigDecimal.valueOf(300);
	private static final BigDecimal FIVE_HUNDRED = new BigDecimal("500");// BigDecimal.valueOf(500);
	private static final BigDecimal FIFTEEN_HUNDRED = new BigDecimal("1500");// BigDecimal.valueOf(1500);
	private static final BigDecimal SEVENTEEN_FIFTY = new BigDecimal("1750");// BigDecimal.valueOf(1750);
	private static final BigDecimal EIGHTEEN_FIFTY_SEVEN = new BigDecimal("1857.42");// BigDecimal.valueOf(1857.42);
	private static final BigDecimal TWO_THOUSAND = new BigDecimal("2000");// BigDecimal.valueOf(2000);
	private static final BigDecimal THOUSAND = new BigDecimal("1000");// BigDecimal.valueOf(2000);
	private static final BigDecimal TWENTY_FIVE_HUNDRED = new BigDecimal("2500");// BigDecimal.valueOf(2500);
	private static final BigDecimal TEN_LAC = new BigDecimal("1000000");// BigDecimal.valueOf(1000000);
	private static final BigDecimal SQMT_SQFT_MULTIPLIER = new BigDecimal("10.764");// BigDecimal.valueOf(10.764);
	private static final BigDecimal ACRE_SQMT_MULTIPLIER = new BigDecimal("4046.85");// BigDecimal.valueOf(4046.85);

	private static final BigDecimal ONE_HUNDRED_FIFTY = new BigDecimal("150");

	private static final BigDecimal TWO_HUNDRED_TWENTYFIVE = new BigDecimal("225");

	private static final BigDecimal SIX_HUNDRED = new BigDecimal("600");

	private static final BigDecimal EIGHT_HUNDRED_SVENTYFIVE = new BigDecimal("875");

	private static final BigDecimal THREE_HUNDRED_SEVENTYFIVE = new BigDecimal("375");
	private static final String INSTALLMENTS_FIELD = "installments";

	private static final BigDecimal ONE_HUNDRED_TWENTYFIVE =  new BigDecimal("125");

	private static final BigDecimal SEVEN_HUNDRED_FIFTY = new BigDecimal("750");

	private static final BigDecimal SEVEN_POINT_FIVE = new BigDecimal("7.50");

	private static final BigDecimal FIVE = new BigDecimal("5.0");

	private static final BigDecimal TWELVE_POINT_FIVE = new BigDecimal("12.5");
	
	private static final BigDecimal ONE_FORTY_PERCENT = new BigDecimal("1.40");
	
	private static final BigDecimal EIGHTEN_TWENTY_ONE = new BigDecimal("1821");
	
	private static final BigDecimal EIGHTEN_FIFTY_SEVEN = new BigDecimal("1857.24");
	
	private static final BigDecimal EIGHTEN_NINTY_FOUR = new BigDecimal("1894");

	private static final BigDecimal SIX_HUNDRED_SEVENTY_FIVE = new BigDecimal("675");

	private static final BigDecimal SIX_HUNDRED_TWENTY_FIVE = new BigDecimal("625");


	/**
	 * Calculates tax estimates and creates demand
	 * 
	 * @param calculationReq The calculationCriteria request
	 * @return List of calculations for all applicationNumbers or tradeLicenses in
	 *         calculationReq
	 */
	public List<Calculation> calculate(CalculationReq calculationReq) {
		
		setBpaFromDBForCalculation(calculationReq);
		utils.validateOwnerDetails(calculationReq);
		String tenantId = calculationReq.getCalulationCriteria().get(0).getTenantId();
		Object mdmsData = mdmsService.mDMSCall(calculationReq, tenantId);
		Boolean isSparit = mdmsService.getMdmsSparitValue(calculationReq,tenantId);
		// List<Calculation> calculations =
		// getCalculation(calculationReq.getRequestInfo(),calculationReq.getCalulationCriteria(),
		// mdmsData);
		Map<String,Object> extraParamsForCalculationMap = new HashMap<>();
		extraParamsForCalculationMap.put("tenantId", tenantId);
		extraParamsForCalculationMap.put("mdmsData", mdmsData);
		extraParamsForCalculationMap.put(BPACalculatorConstants.SPARIT_CHECK, isSparit);
		if(Objects.nonNull(calculationReq.getCalulationCriteria().get(0).getRevision())) {
			extraParamsForCalculationMap.put(BPACalculatorConstants.REVISION,
					calculationReq.getCalulationCriteria().get(0).getRevision());
		}
		//System.out.println("checkSparit:"+isSparit);
		List<Calculation> calculations = getCalculationV2(calculationReq.getRequestInfo(),
				calculationReq.getCalulationCriteria(), extraParamsForCalculationMap);
		log.info("@Class: CalculationService @method:calculate @message Before Generating Demand @Object: {}",calculations);
		demandService.generateDemand(calculationReq.getRequestInfo(), calculations, mdmsData);
		CalculationRes calculationRes = CalculationRes.builder().calculations(calculations).build();
		producer.push(config.getSaveTopic(), calculationRes);
		return calculations;
	}
	
	/**
	 * Calculates tax estimates without creating demand
	 * 
	 * @param calculationReq The calculationCriteria request
	 * @return List of calculations for all applicationNumbers in calculationReq
	 */
	public List<Calculation> getEstimate(CalculationReq calculationReq) {
		if(calculationReq.getCalulationCriteria().get(0).getBpa()!=null && !calculationReq.getCalulationCriteria().get(0).getBpa().getBusinessService().equalsIgnoreCase("BPA6") && 
				calculationReq.getCalulationCriteria().get(0).getApplicationNo()==null) {
			throw new CustomException("applno is mandatory", "application number is mandatory");
		}else if(calculationReq.getCalulationCriteria().get(0).getBpa()==null && calculationReq.getCalulationCriteria().get(0).getApplicationNo()==null) {
			throw new CustomException("applno is mandatory", "application number is mandatory");
		}
			
		setBpaFromDBForCalculation(calculationReq);
		
			
		if(!(calculationReq.getCalulationCriteria().get(0).getBpa().getBusinessService().equalsIgnoreCase("BPA6"))) {
		     utils.validateOwnerDetails(calculationReq);
			}
		//utils.validateOwnerDetails(calculationReq);
		String tenantId = calculationReq.getCalulationCriteria().get(0).getTenantId();
		Object mdmsData = mdmsService.mDMSCall(calculationReq, tenantId);
		Boolean isSparit = mdmsService.getMdmsSparitValue(calculationReq,tenantId);
		
		Map<String,Object> extraParamsForCalculationMap = new HashMap<>();
		extraParamsForCalculationMap.put("tenantId", tenantId);
		extraParamsForCalculationMap.put("mdmsData", mdmsData);
		extraParamsForCalculationMap.put(BPACalculatorConstants.SPARIT_CHECK, isSparit);
		//System.out.println("checkSparit:"+isSparit);
		List<Calculation> calculations = getCalculationV2(calculationReq.getRequestInfo(),
				calculationReq.getCalulationCriteria(), extraParamsForCalculationMap);
		CalculationRes calculationRes = CalculationRes.builder().calculations(calculations).build();
		return calculations;
	}
	
	/**
	 * Calculates tax estimates and stores them in installments table
	 * 
	 * @param calculationReq The calculationCriteria request
	 * @return List of calculations for all applicationNumbers in calculationReq
	 */
	public List<Calculation> calculateInInstallments(CalculationReq calculationReq) {
		setBpaFromDBForCalculation(calculationReq);
		utils.validateOwnerDetails(calculationReq);
		String tenantId = calculationReq.getCalulationCriteria().get(0).getTenantId();
		Object mdmsData = mdmsService.mDMSCall(calculationReq, tenantId);
		Boolean isSparit = mdmsService.getMdmsSparitValue(calculationReq,tenantId);
		Map<String,Object> extraParamsForCalculationMap = new HashMap<>();
		extraParamsForCalculationMap.put("tenantId", tenantId);
		extraParamsForCalculationMap.put("mdmsData", mdmsData);
		extraParamsForCalculationMap.put(BPACalculatorConstants.SPARIT_CHECK, isSparit);
		List<Calculation> calculations = getCalculationV2(calculationReq.getRequestInfo(),
				calculationReq.getCalulationCriteria(), extraParamsForCalculationMap);
		//store the calculations in installments table
		List<Installment> installmentsToInsert = generateInstallmentsFromCalculations(calculationReq, calculations);
		
		/*uncomment to generate installments script
		StringBuilder query = new StringBuilder();
		installmentsToInsert.forEach(installment->{
			query.append("("+"'"+installment.getId()+"','"+"od.cuttack"+"',"+installment.getInstallmentNo()+",'ACTIVE','"+installment.getConsumerCode()
			+"','"+installment.getTaxHeadCode()+"',"+installment.getTaxAmount()+",null,false,null,'"+installment.getAuditDetails().getCreatedBy()
			+"','"+installment.getAuditDetails().getLastModifiedBy()+"',"+installment.getAuditDetails().getCreatedTime()+","
			+installment.getAuditDetails().getLastModifiedTime()+"),\n");
		});
		System.out.println("********\n"+query.toString());
		*/
		
		Map<String, List<Installment>> persisterMap = new HashMap<>();
		persisterMap.put(INSTALLMENTS_FIELD, installmentsToInsert);
		producer.push(config.getSaveInstallmentTopic(), persisterMap);
		return calculations;
	}
	
	
	
	/**
	 * Fetch all installments from db
	 * 
	 */
	public Object getAllInstallmentsV2(InstallmentRequest request) {
		List<Installment> installments = installmentRepository.getInstallments(request.getInstallmentSearchCriteria());
		Map<Integer, List<Installment>> groupedInstallmentsMap = installments.stream()
				.collect(Collectors.groupingBy(installment -> installment.getInstallmentNo()));
		Map<String, Object> returnMap = new HashMap<>();
		if(groupedInstallmentsMap.containsKey(-1)) {
			returnMap.put("fullPayment", groupedInstallmentsMap.get(-1));
		}
		groupedInstallmentsMap.remove(-1);
		Collection<List<Installment>> installmentsGroupedByInstallmentNo = groupedInstallmentsMap
				.values();
		returnMap.put(INSTALLMENTS_FIELD, installmentsGroupedByInstallmentNo);
		return returnMap;
	}
	
	/**
	 * Generate demands from installment
	 * 
	 */
	public Object generateDemandsFromInstallment(InstallmentRequest request) {
		installmentValidator.validateConsumerCode(request);
		InstallmentSearchCriteria allInstallmentsCriteria = new InstallmentSearchCriteria();
		allInstallmentsCriteria.setConsumerCode(request.getInstallmentSearchCriteria().getConsumerCode());
		List<Installment> allInstallments = installmentRepository.getInstallments(allInstallmentsCriteria);
		if (CollectionUtils.isEmpty(allInstallments))
			throw new CustomException(
					"no installments found for this consumercode:"
							+ request.getInstallmentSearchCriteria().getConsumerCode(),
					"no installments found for this consumercode:"
							+ request.getInstallmentSearchCriteria().getConsumerCode());
		installmentValidator.validateInstallmentNoSequence(request, allInstallments);
		List<Installment> installmentsToGenerateDemand = installmentRepository
				.getInstallments(request.getInstallmentSearchCriteria());
		installmentValidator.validateForDemandGeneration(request, allInstallments, installmentsToGenerateDemand);
		//
		List<Demand> demands = demandService.createDemandFromInstallment(request.getRequestInfo(),
				installmentsToGenerateDemand);
		
		// update installments demandId,additionalDetails(for document on 2nd
		// installment onwards) and auditDetails field-
		Map<String,Object> additionalDetailsToUpdateFromRequest = null;
		if (Objects.nonNull(request.getInstallmentSearchCriteria().getAdditionalDetails())
				&& request.getInstallmentSearchCriteria().getAdditionalDetails() instanceof Map
				&& !CollectionUtils.isEmpty((Map) request.getInstallmentSearchCriteria().getAdditionalDetails())) {
			additionalDetailsToUpdateFromRequest = (Map<String, Object>) request.getInstallmentSearchCriteria()
					.getAdditionalDetails();
		}
		String demandId = demands.get(0).getId();
		String lastModifiedBy = request.getRequestInfo().getUserInfo().getUuid();
		updateInstallments(installmentsToGenerateDemand, demandId, lastModifiedBy,
				additionalDetailsToUpdateFromRequest);
		
		//add installment audit table
		Map<String, Object> returnObject = new HashMap<>();
		returnObject.put("demands", demands);
		return returnObject;
	}
	
	private void updateInstallments(List<Installment> installments, String demandId, String modifiedBy,
			Map<String, Object> additionalDetails) {
		Long time = System.currentTimeMillis();
		for (Installment installment : installments) {
			installment.setDemandId(demandId);
			if (Objects.nonNull(additionalDetails)) {
				Map<String, Object> additionalDetailsExisting = Objects.nonNull(installment.getAdditionalDetails())
						? (Map<String, Object>) installment.getAdditionalDetails()
						: new HashMap<>();
				additionalDetailsExisting.putAll(additionalDetails);
				installment.setAdditionalDetails(additionalDetailsExisting);
			}
			installment.getAuditDetails().setLastModifiedBy(modifiedBy);
			installment.getAuditDetails().setLastModifiedTime(time);
		}
		Map<String, List<Installment>> persisterMap = new HashMap<>();
		persisterMap.put(INSTALLMENTS_FIELD, installments);
		installmentRepository.update(persisterMap);
	}
	
	private List<Installment> generateInstallmentsFromCalculations(CalculationReq calculationReq, List<Calculation> calculations) {
		List<Installment> installmentsToInsert = new ArrayList<>();
		List<OtherFee> otherFees = extractOtherFee(calculationReq);
		String tenantId = calculationReq.getCalulationCriteria().get(0).getTenantId();
		Object installmentsMdms = mdmsService.fetchInstallmentsApplicableForTaxheads(calculationReq, tenantId);
		for (Calculation calculation : calculations) {
			if (!CollectionUtils.isEmpty(otherFees)) {
				addOtherFeesToTaxHeadEstimate(calculation, otherFees);
				}
			for (TaxHeadEstimate taxHeadEstimate : calculation.getTaxHeadEstimates()) {
				Map<String,Object> installmentDetail=mdmsService.getInstallmentforTaxHeadCode(taxHeadEstimate.getTaxHeadCode(), installmentsMdms);
				//full payment installment -1--
				Installment installmentEntryForFullPayment = Installment.builder().id(UUID.randomUUID().toString())
						.tenantId(calculation.getTenantId())
						.installmentNo(-1)
						.status(StatusEnum.ACTIVE)
						.consumerCode(calculationReq.getCalulationCriteria().get(0).getApplicationNo())
						.taxHeadCode(taxHeadEstimate.getTaxHeadCode())
						.taxAmount(taxHeadEstimate.getEstimateAmount())
						.auditDetails(utils.getAuditDetails(calculationReq.getRequestInfo().getUserInfo().getUuid(),
								true))
						.build();
				installmentsToInsert.add(installmentEntryForFullPayment);
				int noOfInstallments = (int) installmentDetail.get(BPACalculatorConstants.MDMS_NO_OF_INSTALLMENTS);
				
				if (taxHeadEstimate.getEstimateAmount().compareTo(TEN_LAC) < 0 && taxHeadEstimate.getTaxHeadCode()
						.equalsIgnoreCase(BPACalculatorConstants.TAXHEAD_BPA_WORKER_WELFARE_CESS)) {
					noOfInstallments = 1;
				}
				
				for (int i = 0; i < noOfInstallments; i++) {
					// create installment only if estimateAmount is not 0-
					if (BigDecimal.ZERO.compareTo(taxHeadEstimate.getEstimateAmount()) != 0) {
						Installment installment = Installment.builder().id(UUID.randomUUID().toString())
								.tenantId(calculation.getTenantId()).installmentNo(i + 1).status(StatusEnum.ACTIVE)
								.consumerCode(calculationReq.getCalulationCriteria().get(0).getApplicationNo())
								.taxHeadCode(taxHeadEstimate.getTaxHeadCode())
								.taxAmount(taxHeadEstimate.getEstimateAmount().divide(new BigDecimal(noOfInstallments),
										0, BigDecimal.ROUND_HALF_UP))
								.auditDetails(utils
										.getAuditDetails(calculationReq.getRequestInfo().getUserInfo().getUuid(), true))
								.build();
						installmentsToInsert.add(installment);
					}
				}
			}
		}
		return installmentsToInsert;
	}

	private List<OtherFee> extractOtherFee(CalculationReq calculationReq) {

		BPA bpa = calculationReq.getCalulationCriteria().get(0).getBpa();

		List<OtherFee> otherFees = new ArrayList<>();

		if (bpa.getAdditionalDetails() != null && !ObjectUtils.isEmpty(bpa.getAdditionalDetails())) {

			Map<String, Object> additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();

			Object otherFeesObj = additionalDetails.get("otherFees");

			if (otherFeesObj instanceof List) {

				List<Map<String, Object>> otherFeesList = (List<Map<String, Object>>) otherFeesObj;

				otherFeesList.stream().filter(Objects::nonNull).forEach(fee -> {

					String reason = Objects.toString(fee.get("reason"), null);
					String amount = Objects.toString(fee.get("amount"), null);

					Integer order = fee.get("order") == null ? null : Integer.valueOf(fee.get("order").toString());

					if (reason != null && amount != null) {
						otherFees.add(new OtherFee(reason, order, new BigDecimal(amount)));
					}
				});
			}

			log.info("Other Fees for Application {} : {}", bpa.getApplicationNo(), otherFees);
		}
		return otherFees;
	}
	
	

	private void addOtherFeesToTaxHeadEstimate(Calculation calculation, List<OtherFee> otherFees) {
		List<Integer> otherFeesOrderToBeRemoved = new ArrayList<>();		
		Map<String, OtherFee> outputMap = otherFees.stream()
				.filter(fee -> BPACalculatorConstants.OTHER_FEES_MAP.containsKey(fee.getReason())).collect(Collectors
						.toMap(fee -> BPACalculatorConstants.OTHER_FEES_MAP.get(fee.getReason()), Function.identity()));
        log.info("outputMap : "+outputMap);
		for (TaxHeadEstimate taxHead : calculation.getTaxHeadEstimates()) {

			OtherFee otherFee = outputMap.get(taxHead.getTaxHeadCode());

			if (otherFee != null) {
				log.info("Processing "+taxHead.getTaxHeadCode()+" before other Fee Add"+ taxHead.getEstimateAmount());
				taxHead.setEstimateAmount(taxHead.getEstimateAmount().add(otherFee.getAmount())); 
				log.info("Processing "+taxHead.getTaxHeadCode()+" after add other Fee of amount "+otherFee);
				otherFeesOrderToBeRemoved.add(otherFee.getOrder());
			}
		}
		removeOtherFeeFromExistingTaxHead(calculation, otherFeesOrderToBeRemoved);

	}

	private void removeOtherFeeFromExistingTaxHead(Calculation calculation, List<Integer> otherFeesOrderToBeRemoved) {
		List<String> otherFeesTaxheadToBeRemoved = new ArrayList<>();
		if (!CollectionUtils.isEmpty(otherFeesOrderToBeRemoved)) {
			for (Integer i : otherFeesOrderToBeRemoved) {
				otherFeesTaxheadToBeRemoved.add("BPA_SANC_ADJUSTMENT_AMOUNT_" + i);
			}
		}
        log.info("Set 0 Amount for the tax head code"+otherFeesTaxheadToBeRemoved);
		for (TaxHeadEstimate taxHead : calculation.getTaxHeadEstimates()) {
			if (otherFeesTaxheadToBeRemoved.contains(taxHead.getTaxHeadCode())) {
				taxHead.setEstimateAmount(BigDecimal.ZERO);
			}
		}
	}

	/**
	 * @param requestInfo
	 * @param calulationCriteria
	 * @param extraParamsForCalculationMap
	 * @return
	 */
	private List<Calculation> getCalculationV2(RequestInfo requestInfo, List<CalulationCriteria> calulationCriteria,
			Map<String, Object> extraParamsForCalculationMap) {
		List<Calculation> calculations = new LinkedList<>();
		if (!CollectionUtils.isEmpty(calulationCriteria)) {
			for (CalulationCriteria criteria : calulationCriteria) {
				BPA bpa;
				if (criteria.getBpa() == null && criteria.getApplicationNo() != null) {
					bpa = bpaService.getBuildingPlan(requestInfo, criteria.getTenantId(), criteria.getApplicationNo(),
							null);
					criteria.setBpa(bpa);
				}
				extraParamsForCalculationMap.put("BPA", criteria.getBpa());

				EstimatesAndSlabs estimatesAndSlabs = getTaxHeadEstimatesV2(criteria, requestInfo, extraParamsForCalculationMap);
				List<TaxHeadEstimate> taxHeadEstimates = estimatesAndSlabs.getEstimates();

				Calculation calculation = new Calculation();
				calculation.setBpa(criteria.getBpa());
				calculation.setTenantId(criteria.getTenantId());
				calculation.setTaxHeadEstimates(taxHeadEstimates);
				calculation.setFeeType(criteria.getFeeType());
				calculations.add(calculation);

			}

		}
		return calculations;
	}

	/**
	 * @param criteria
	 * @param requestInfo
	 * @param extraParamsForCalculationMap
	 * @return
	 */
	private EstimatesAndSlabs getTaxHeadEstimatesV2(CalulationCriteria criteria, RequestInfo requestInfo,
			Map<String, Object> extraParamsForCalculationMap) {
		List<TaxHeadEstimate> estimates = new LinkedList<>();
		EstimatesAndSlabs estimatesAndSlabs;
		estimatesAndSlabs = getBaseTaxV2(criteria, requestInfo, extraParamsForCalculationMap);
		estimates.addAll(estimatesAndSlabs.getEstimates());
		estimatesAndSlabs.setEstimates(estimates);

		return estimatesAndSlabs;
	}

	/***
	 * Calculates tax estimates
	 * 
	 * @param requestInfo The requestInfo of the calculation request
	 * @param criterias   list of CalculationCriteria containing the tradeLicense or
	 *                    applicationNumber
	 * @return List of calculations for all applicationNumbers or tradeLicenses in
	 *         criterias
	 */
	public List<Calculation> getCalculation(RequestInfo requestInfo, List<CalulationCriteria> criterias,
			Object mdmsData) {
		List<Calculation> calculations = new LinkedList<>();
		for (CalulationCriteria criteria : criterias) {
			BPA bpa;
			if (criteria.getBpa() == null && criteria.getApplicationNo() != null) {
				bpa = bpaService.getBuildingPlan(requestInfo, criteria.getTenantId(), criteria.getApplicationNo(),
						null);
				criteria.setBpa(bpa);
			}

			EstimatesAndSlabs estimatesAndSlabs = getTaxHeadEstimates(criteria, requestInfo, mdmsData);
			List<TaxHeadEstimate> taxHeadEstimates = estimatesAndSlabs.getEstimates();

			Calculation calculation = new Calculation();
			calculation.setBpa(criteria.getBpa());
			calculation.setTenantId(criteria.getTenantId());
			calculation.setTaxHeadEstimates(taxHeadEstimates);
			calculation.setFeeType(criteria.getFeeType());
			calculations.add(calculation);

		}
		return calculations;
	}

	/**
	 * Creates TacHeadEstimates
	 * 
	 * @param calulationCriteria CalculationCriteria containing the tradeLicense or
	 *                           applicationNumber
	 * @param requestInfo        The requestInfo of the calculation request
	 * @return TaxHeadEstimates and the billingSlabs used to calculate it
	 */
	private EstimatesAndSlabs getTaxHeadEstimates(CalulationCriteria calulationCriteria, RequestInfo requestInfo,
			Object mdmsData) {
		List<TaxHeadEstimate> estimates = new LinkedList<>();
		EstimatesAndSlabs estimatesAndSlabs;
		if (calulationCriteria.getFeeType().equalsIgnoreCase(BPACalculatorConstants.LOW_RISK_PERMIT_FEE_TYPE)) {

//			 stopping Application fee for lowrisk applicaiton according to BBI-391
			calulationCriteria.setFeeType(BPACalculatorConstants.MDMS_CALCULATIONTYPE_LOW_APL_FEETYPE);
			estimatesAndSlabs = getBaseTax(calulationCriteria, requestInfo, mdmsData);

			estimates.addAll(estimatesAndSlabs.getEstimates());

			calulationCriteria.setFeeType(BPACalculatorConstants.MDMS_CALCULATIONTYPE_LOW_SANC_FEETYPE);
			estimatesAndSlabs = getBaseTax(calulationCriteria, requestInfo, mdmsData);

			estimates.addAll(estimatesAndSlabs.getEstimates());

			calulationCriteria.setFeeType(BPACalculatorConstants.LOW_RISK_PERMIT_FEE_TYPE);

		} else {
			estimatesAndSlabs = getBaseTax(calulationCriteria, requestInfo, mdmsData);
			estimates.addAll(estimatesAndSlabs.getEstimates());
		}

		estimatesAndSlabs.setEstimates(estimates);

		return estimatesAndSlabs;
	}

	/**
	 * Calculates base tax and cretaes its taxHeadEstimate
	 * 
	 * @param calulationCriteria CalculationCriteria containing the tradeLicense or
	 *                           applicationNumber
	 * @param requestInfo        The requestInfo of the calculation request
	 * @return BaseTax taxHeadEstimate and billingSlabs used to calculate it
	 */
	@SuppressWarnings({ "rawtypes" })
	private EstimatesAndSlabs getBaseTax(CalulationCriteria calulationCriteria, RequestInfo requestInfo,
			Object mdmsData) {
		BPA bpa = calulationCriteria.getBpa();
		EstimatesAndSlabs estimatesAndSlabs = new EstimatesAndSlabs();
		BillingSlabSearchCriteria searchCriteria = new BillingSlabSearchCriteria();
		searchCriteria.setTenantId(bpa.getTenantId());

		Map calculationTypeMap = mdmsService.getCalculationType(requestInfo, bpa, mdmsData,
				calulationCriteria.getFeeType());
		int calculatedAmout = 0;
		ArrayList<TaxHeadEstimate> estimates = new ArrayList<TaxHeadEstimate>();
		if (calculationTypeMap.containsKey("calsiLogic")) {
			LinkedHashMap ocEdcr = edcrService.getEDCRDetails(requestInfo, bpa);
			String jsonString = new JSONObject(ocEdcr).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			JSONArray permitNumber = context.read("edcrDetail.*.permitNumber");
			String jsonData = new JSONObject(calculationTypeMap).toString();
			DocumentContext calcContext = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonData);
			JSONArray parameterPaths = calcContext.read("calsiLogic.*.paramPath");
			JSONArray tLimit = calcContext.read("calsiLogic.*.tolerancelimit");
			System.out.println("tolerance limit in: " + tLimit.get(0));
			DocumentContext edcrContext = null;
			if (!CollectionUtils.isEmpty(permitNumber)) {
				BPA permitBpa = bpaService.getBuildingPlan(requestInfo, bpa.getTenantId(), null,
						permitNumber.get(0).toString());
				if (permitBpa.getEdcrNumber() != null) {
					LinkedHashMap edcr = edcrService.getEDCRDetails(requestInfo, permitBpa);
					String edcrData = new JSONObject(edcr).toString();
					edcrContext = JsonPath.using(Configuration.defaultConfiguration()).parse(edcrData);
				}
			}

			for (int i = 0; i < parameterPaths.size(); i++) {
				Double ocTotalBuitUpArea = context.read(parameterPaths.get(i).toString());
				Double bpaTotalBuitUpArea = edcrContext.read(parameterPaths.get(i).toString());
				Double diffInBuildArea = ocTotalBuitUpArea - bpaTotalBuitUpArea;
				System.out.println("difference in area: " + diffInBuildArea);
				Double limit = Double.valueOf(tLimit.get(i).toString());
				if (diffInBuildArea > limit) {
					JSONArray data = calcContext.read("calsiLogic.*.deviation");
					System.out.println(data.get(0));
					JSONArray data1 = (JSONArray) data.get(0);
					for (int j = 0; j < data1.size(); j++) {
						LinkedHashMap diff = (LinkedHashMap) data1.get(j);
						Integer from = (Integer) diff.get("from");
						Integer to = (Integer) diff.get("to");
						Integer uom = (Integer) diff.get("uom");
						Integer mf = (Integer) diff.get("MF");
						if (diffInBuildArea >= from && diffInBuildArea <= to) {
							calculatedAmout = (int) (diffInBuildArea * mf * uom);
							break;
						}
					}
				} else {
					calculatedAmout = 0;
				}
				TaxHeadEstimate estimate = new TaxHeadEstimate();
				BigDecimal totalTax = BigDecimal.valueOf(calculatedAmout);
				if (totalTax.compareTo(BigDecimal.ZERO) == -1)
					throw new CustomException(BPACalculatorConstants.INVALID_AMOUNT, "Tax amount is negative");

				estimate.setEstimateAmount(totalTax);
				estimate.setCategory(Category.FEE);

				String taxHeadCode = utils.getTaxHeadCode(bpa.getBusinessService(), calulationCriteria.getFeeType());
				estimate.setTaxHeadCode(taxHeadCode);
				estimates.add(estimate);
			}
		} else {
			TaxHeadEstimate estimate = new TaxHeadEstimate();
			calculatedAmout = Integer
					.parseInt(calculationTypeMap.get(BPACalculatorConstants.MDMS_CALCULATIONTYPE_AMOUNT).toString());

			BigDecimal totalTax = BigDecimal.valueOf(calculatedAmout);
			if (totalTax.compareTo(BigDecimal.ZERO) == -1)
				throw new CustomException(BPACalculatorConstants.INVALID_AMOUNT, "Tax amount is negative");

			estimate.setEstimateAmount(totalTax);
			estimate.setCategory(Category.FEE);

			String taxHeadCode = utils.getTaxHeadCode(bpa.getBusinessService(), calulationCriteria.getFeeType());
			estimate.setTaxHeadCode(taxHeadCode);
			estimates.add(estimate);
		}
		estimatesAndSlabs.setEstimates(estimates);
		return estimatesAndSlabs;
	}

	/**
	 * @param criteria
	 * @param requestInfo
	 * @param extraParamsForCalculationMap
	 * @return
	 */
	private EstimatesAndSlabs getBaseTaxV2(CalulationCriteria criteria, RequestInfo requestInfo, Map<String, Object> extraParamsForCalculationMap) {
		BPA bpa = criteria.getBpa();
		String applicationNo = bpa.getApplicationNo();
		String feeType = criteria.getFeeType();
		
		EstimatesAndSlabs estimatesAndSlabs = new EstimatesAndSlabs();
		BillingSlabSearchCriteria searchCriteria = new BillingSlabSearchCriteria();
		searchCriteria.setTenantId(bpa.getTenantId());

		ArrayList<TaxHeadEstimate> estimates = new ArrayList<TaxHeadEstimate>();

		if(BPACalculatorConstants.BUILDING_PLAN_OC.equalsIgnoreCase(criteria.getApplicationType())) {
			Object mdmsData = extraParamsForCalculationMap.get("mdmsData");
			if (StringUtils.hasText(feeType)
					&& feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE)) {
				calculateBpaOcFee(requestInfo, criteria, estimates, mdmsData,
						BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE, extraParamsForCalculationMap);
			}
			if (StringUtils.hasText(feeType)
					&& feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)) {
	
				calculateBpaOcFee(requestInfo, criteria, estimates, mdmsData,
						BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE, extraParamsForCalculationMap);
			}
		
		} else {
			if (StringUtils.hasText(feeType)
					&& feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE)) {
				calculateTotalFee(requestInfo, criteria, estimates,
						BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE, extraParamsForCalculationMap);
	
			}
			if (StringUtils.hasText(feeType)
					&& feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)) {
	
				calculateTotalFee(requestInfo, criteria, estimates,
						BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE, extraParamsForCalculationMap);
			}
		}
		if (!StringUtils.isEmpty(applicationNo)
				&& applicationNo.startsWith(BPACalculatorConstants.REVALIDATION_APPLICATION_PREFIX)) {
			if (StringUtils.hasText(feeType)
					&& feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE)) {
				for (TaxHeadEstimate estimate : estimates) {
					BigDecimal estimateAmount = estimate.getEstimateAmount();
					BigDecimal halfAmount = estimateAmount.divide(BigDecimal.valueOf(2), 0, BigDecimal.ROUND_HALF_UP);
					estimate.setEstimateAmount(halfAmount);
					if(halfAmount.intValue() == 0) {
						estimate.setEstimateAmount(BigDecimal.valueOf(613));
					}
				}
				estimatesAndSlabs.setEstimates(estimates);
				return estimatesAndSlabs;
			}
			if (StringUtils.hasText(feeType)
					&& feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)) {
				ArrayList<TaxHeadEstimate> revEtimates = new ArrayList<TaxHeadEstimate>();
				for (TaxHeadEstimate taxHeadEstimate : estimates) {
					if(StringUtils.hasText(taxHeadEstimate.getTaxHeadCode()) &&   
							taxHeadEstimate.getTaxHeadCode().contains(BPACalculatorConstants.BPA_SANC_ADJUSTMENT_AMOUNT)) {
						revEtimates.add(taxHeadEstimate);
					}
				}
				estimatesAndSlabs.setEstimates(revEtimates);
				return estimatesAndSlabs;
			}
		}

		estimatesAndSlabs.setEstimates(estimates);
		return estimatesAndSlabs;

	}
	
	/**
	 * Calculate BPA OC fees
	 * @param requestInfo
	 * @param criteria
	 * @param estimates
	 * @param mdmsData
	 * @param feeType
	 * @param extraParamsForCalculationMap 
	 */
	private void calculateBpaOcFee(RequestInfo requestInfo, CalulationCriteria criteria,
			ArrayList<TaxHeadEstimate> estimates, Object mdmsData, String feeType, Map<String, Object> extraParamsForCalculationMap) {
		Map<String, Object> paramMap = prepareBpaOcParamMap(requestInfo, criteria, feeType);
		
		paramMap.put(BPACalculatorConstants.SPARIT_CHECK,
				extraParamsForCalculationMap.get(BPACalculatorConstants.SPARIT_CHECK));
		paramMap.put("mdmsData", extraParamsForCalculationMap.get("mdmsData"));
		paramMap.put("tenantId", extraParamsForCalculationMap.get("tenantId"));
		paramMap.put("BPA", extraParamsForCalculationMap.get("BPA"));
		paramMap.put("requestInfo", requestInfo);
		
		BigDecimal calculatedTotalAmout = calculateTotalBpaOcFeeAmount(paramMap, estimates, mdmsData, criteria);
		if (calculatedTotalAmout.compareTo(BigDecimal.ZERO) == -1) {
			throw new CustomException(BPACalculatorConstants.INVALID_AMOUNT, "Tax amount is negative");
		}
	}

	/**
	 * Calculate Total BPA OC
	 * @param paramMap
	 * @param estimates
	 * @param mdmsData
	 * @return
	 */
	private BigDecimal calculateTotalBpaOcFeeAmount(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates, Object mdmsData, CalulationCriteria criteria) {
		BigDecimal calculatedTotalOcAmout = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String feeType = null;
		String occupancyType = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.FEE_TYPE)) {
			feeType = (String) paramMap.get(BPACalculatorConstants.FEE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if ((StringUtils.hasText(applicationType) && (StringUtils.hasText(serviceType))
				&& StringUtils.hasText(occupancyType) && (StringUtils.hasText(feeType)))|| (criteria.getIsOCOutsideSujogApplication()||criteria.getBpa().getOCOutsideSujogApplication())) {
			if (feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE)) {
				calculatedTotalOcAmout = calculateTotalOcApplicationFee(paramMap, estimates, criteria);
			} else if(feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)
					&& (criteria.getBpa().getOCOutsideSujogApplication() || criteria.getIsOCOutsideSujogApplication())){
				calculatedTotalOcAmout = calculateTotalOcSanctionFeeOutsideSujog(paramMap, estimates, mdmsData,criteria);
			}
			else if (feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)) {
				calculatedTotalOcAmout = calculateTotalOcSanctionFee(paramMap, estimates, mdmsData, criteria);
			}
		}
		
		return calculatedTotalOcAmout;
	}

	private BigDecimal calculateTotalOcSanctionFeeOutsideSujog(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates, Object mdmsData, CalulationCriteria criteria) {

		BigDecimal totalOcSanctionFee = BigDecimal.ZERO;
		BigDecimal ocSanctionFee = BigDecimal.ZERO;
		BigDecimal ocGrandCertificateFee = BigDecimal.ZERO;
		BigDecimal otherFees=BigDecimal.ZERO;


		BPA bpa = null;
		if (null != paramMap.get("BPA")) {
			bpa = (BPA) paramMap.get("BPA");
		}
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				totalOcSanctionFee.add(totalAmountAfterExemption);
			}
			generateTaxHeadEstimate(estimates, totalOcSanctionFee, BPACalculatorConstants.TAXHEAD_BPA_OC_GRAND_OC_CERT, Category.FEE);
		}else {
			ocGrandCertificateFee = getOCGrandCertificateFeeOutsideSujog(estimates, criteria, ocSanctionFee);
		}
		otherFees=calculateOCOtherFees(paramMap,estimates, criteria);
		totalOcSanctionFee=(totalOcSanctionFee.add(ocGrandCertificateFee).add(otherFees)).setScale(2, BigDecimal.ROUND_UP);
		return totalOcSanctionFee;
	}

	private BigDecimal getOCGrandCertificateFeeOutsideSujog(ArrayList<TaxHeadEstimate> estimates, CalulationCriteria criteria, BigDecimal ocSanctionFee) {
		BigDecimal totalOcSanctionFee;
		List<ScrutinyDetails> permitScrutinyDetail = criteria.getBpa().getOutsideOCDetails().getScrutinyDetails().stream().filter(scrutinyDetail -> scrutinyDetail.getScrutinyType().equalsIgnoreCase("PERMIT")).collect(Collectors.toList());
		List<Fee> permitFee=permitScrutinyDetail.get(0).getPermitFee();
		for(Fee fee:permitFee) {
			if(fee.getFeeAmount()!=null && fee.getFeeType()!=null) {
				if (fee.getFeeType().equalsIgnoreCase(BPACalculatorConstants.OC_LAND_DEVELOPMENT_FEE) || fee.getFeeType().equalsIgnoreCase(BPACalculatorConstants.OC_BUILDING_OPERATION_FEE))
					ocSanctionFee = ocSanctionFee.add(fee.getFeeAmount());
			}
		}

		ocSanctionFee = ocSanctionFee.divide(BigDecimal.valueOf(2));
		totalOcSanctionFee = ocSanctionFee;
		generateTaxHeadEstimate(estimates, totalOcSanctionFee, BPACalculatorConstants.TAXHEAD_BPA_OC_GRAND_OC_CERT, Category.FEE);
		return totalOcSanctionFee;
	}

	/**
	 * Calculate Sanction Fee for BPA OC
	 * @param paramMap
	 * @param estimates
	 * @param mdmsData
	 * @return
	 */
	private BigDecimal calculateTotalOcSanctionFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates, Object mdmsData, CalulationCriteria criteria) {
		BigDecimal totalOcSanctionFee = BigDecimal.ZERO;
		BigDecimal compSetbackFee = BigDecimal.ZERO;
		BigDecimal compFARFee = BigDecimal.ZERO;
		BigDecimal eidpFee = BigDecimal.ZERO;
		BigDecimal cessFee = BigDecimal.ZERO;
		BigDecimal grandOccupancyCertFee = BigDecimal.ZERO;
		BigDecimal otherFees = BigDecimal.ZERO;


		compSetbackFee = calculateOccupancyCompoundingFeeForSetback(paramMap, estimates, mdmsData);
		compFARFee = calculateOccupancyCompoundingFeeForFAR(paramMap, estimates, mdmsData);
		eidpFee = calculateOccupancyEidpFee(paramMap, estimates);
		cessFee = calculateOccupancyConstructionWorkerWelfareCess(paramMap, estimates);
		grandOccupancyCertFee = calculateOccupancyGrandOccupancyCertificateFee(paramMap, estimates);
		otherFees=calculateOCOtherFees(paramMap,estimates, criteria);
		
		
		totalOcSanctionFee = compFARFee.add(compSetbackFee).add(eidpFee).add(cessFee).add(grandOccupancyCertFee).add(otherFees);
		
		return totalOcSanctionFee;
	}

	private BigDecimal calculateOccupancyGrandOccupancyCertificateFee(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal grandOccupancyCertificateFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& ((StringUtils.hasText(serviceType))
				&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION)))) {
			Map<String, Object> edcrParamMap = (Map<String, Object>) paramMap.get(BPACalculatorConstants.EDCR_PARAM_MAP);
			
			edcrParamMap.put(BPACalculatorConstants.SPARIT_CHECK, paramMap.get(BPACalculatorConstants.SPARIT_CHECK));
			edcrParamMap.put("BPA", paramMap.get("BPA"));
			ArrayList<TaxHeadEstimate> voidEstimates = new ArrayList<>();
			BigDecimal permitOrderScrutinyFee = calculateTotalScrutinyFee(edcrParamMap, voidEstimates);
			BigDecimal ocScrutinyFee = calculateOccupancyScrutinyFee(paramMap, voidEstimates);
			BigDecimal totalScrutinyFee = permitOrderScrutinyFee.add(ocScrutinyFee);
			grandOccupancyCertificateFee = totalScrutinyFee.multiply(ZERO_FIVE).setScale(2, RoundingMode.UP);
			
		}
		
		if(BigDecimal.ZERO.compareTo(grandOccupancyCertificateFee) < 0)
			generateTaxHeadEstimate(estimates, grandOccupancyCertificateFee, BPACalculatorConstants.TAXHEAD_BPA_OC_GRAND_OC_CERT, Category.FEE);
		return grandOccupancyCertificateFee;
	}

	/**
	 * Calculate BPA OC application fee
	 *
	 * @param paramMap
	 * @param estimates
	 * @param criteria
	 * @return
	 */
	private BigDecimal calculateTotalOcApplicationFee(Map<String, Object> paramMap,
													  ArrayList<TaxHeadEstimate> estimates, CalulationCriteria criteria) {
		BigDecimal totalOCApplicationFee = BigDecimal.ZERO;
		BigDecimal scrutinyFee = BigDecimal.ZERO;
		BigDecimal ocCertificateFee = BigDecimal.ZERO;
		
		ocCertificateFee = calculateOccupancyCertificateFee(paramMap, estimates);
		if(!criteria.getIsOCOutsideSujogApplication()&& !criteria.getBpa().getOCOutsideSujogApplication()) {
			scrutinyFee = calculateOccupancyScrutinyFee(paramMap, estimates);
			totalOCApplicationFee = ocCertificateFee.add(scrutinyFee);
		}
		else{
			totalOCApplicationFee = ocCertificateFee;
		}
		return totalOCApplicationFee;
	}

	/**
	 * Prepare required data for BPA OC
	 * @param requestInfo
	 * @param criteria
	 * @param feeType
	 * @return
	 */
	private Map<String, Object> prepareBpaOcParamMap(RequestInfo requestInfo, CalulationCriteria criteria, String feeType) {
		BPA bpa = criteria.getBpa();
		String applicationType = criteria.getApplicationType();
		String serviceType = criteria.getServiceType();
		String riskType = criteria.getBpa().getRiskType();

		LinkedHashMap ocEdcr = null;
		if(bpa.getOCOutsideSujogApplication() || criteria.getIsOCOutsideSujogApplication()) {
			/*
			 * Executed when both the edcrDetails and the requestScruitny details are null
			 */
			if(bpa.getOcEdcrDetail()==null) {
				enrichOCSearchOutsideDetailsFromDB(bpa);
				edcrHelperUtils.enrichOCOutsideEdcrDetailsFromRequest(bpa);
			}

			/*
			 * Executed when the edcrDetails details are null
			 */
			if(bpa.getOcEdcrDetail()==null && !StringUtils.isEmpty(bpa.getOutsideOCDetails().getScrutinyDetails())){
				edcrHelperUtils.enrichOCOutsideEdcrDetailsFromRequest(bpa);
			}
			ocEdcr= mapper.convertValue(bpa.getOcEdcrDetail(),LinkedHashMap.class);
		}
		else {
			ocEdcr = edcrService.getEDCRDetails(requestInfo, bpa);
		}
		String jsonString = new JSONObject(ocEdcr).toString();
		DocumentContext ocContext = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		JSONArray permitNumber = ocContext.read("edcrDetail.*.permitNumber");
		DocumentContext edcrContext = null;
		
		LinkedHashMap edcr = null;
		if (!CollectionUtils.isEmpty(permitNumber)) {
			BPA permitBpa = bpaService.getBuildingPlan(requestInfo, bpa.getTenantId(), null,
					permitNumber.get(0).toString());
			if (permitBpa.getEdcrNumber() != null) {
				edcr = edcrService.getEDCRDetails(requestInfo, permitBpa);
				String edcrData = new JSONObject(edcr).toString();
				edcrContext = JsonPath.using(Configuration.defaultConfiguration()).parse(edcrData);
			}
		}

		if(bpa.getOCOutsideSujogApplication() || criteria.getIsOCOutsideSujogApplication()) {
			String edcrData = new JSONObject(bpa.getPermitEdcrDetail()).toString();
			edcrContext = JsonPath.using(Configuration.defaultConfiguration()).parse(edcrData);
		}

		Map<String, Object> paramMap = new HashMap<>();
		
		if(!bpa.getOCOutsideSujogApplication()) {
			paramMap.put(BPACalculatorConstants.EDCR_PARAM_MAP, prepareParamMapForPermitOrder(edcrContext, riskType, serviceType,edcr));
		}

		JSONArray occupancyTypeJSONArray = ocContext.read(BPACalculatorConstants.OCCUPANCY_TYPE_PATH);
		if (!CollectionUtils.isEmpty(occupancyTypeJSONArray)) {
			if (null != occupancyTypeJSONArray.get(0)) {
				String occupancyType = occupancyTypeJSONArray.get(0).toString();
				paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
			}
		}

		JSONArray subOccupancyTypeJSONArray = ocContext.read(BPACalculatorConstants.SUB_OCCUPANCY_TYPE_PATH);
		if (!CollectionUtils.isEmpty(subOccupancyTypeJSONArray)) {
			if (null != subOccupancyTypeJSONArray.get(0)) {
				String subOccupancyType = subOccupancyTypeJSONArray.get(0).toString();
				paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE, subOccupancyType);
			}

		}

		JSONArray ocPlotAreas = ocContext.read(BPACalculatorConstants.PLOT_AREA_PATH);
		if (!CollectionUtils.isEmpty(ocPlotAreas)) {
			if (null != ocPlotAreas.get(0)) {
				String plotAreaString = ocPlotAreas.get(0).toString();
				Double plotArea = Double.parseDouble(plotAreaString);
				paramMap.put(BPACalculatorConstants.PLOT_AREA, plotArea);
			}
		}
		
		JSONArray plotAreas = edcrContext.read(BPACalculatorConstants.PLOT_AREA_PATH);
		if (!CollectionUtils.isEmpty(plotAreas)) {
			if (null != plotAreas.get(0)) {
				String plotAreaString = plotAreas.get(0).toString();
				Double plotArea = Double.parseDouble(plotAreaString);
				paramMap.put(BPACalculatorConstants.PLOT_AREA_EDCR, plotArea);
			}
		}
		
		JSONArray edcrTotalBuitUpAreas = edcrContext.read(BPACalculatorConstants.TOTAL_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(edcrTotalBuitUpAreas)) {
			if (null != edcrTotalBuitUpAreas.get(0)) {
				String edcrTotalBuitUpAreaString = edcrTotalBuitUpAreas.get(0).toString();
				Double totalBuitUpArea = Double.parseDouble(edcrTotalBuitUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA_EDCR, totalBuitUpArea);
			}
		}
		
		JSONArray edcrTotalBuiltUpAreas = edcrContext.read(BPACalculatorConstants.TOTAL_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(edcrTotalBuiltUpAreas)) {
			if (null != edcrTotalBuiltUpAreas.get(0)) {
				String edcrTotalBuiltUpAreaString = edcrTotalBuiltUpAreas.get(0).toString();
				Double totalBuiltUpArea = Double.parseDouble(edcrTotalBuiltUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR, totalBuiltUpArea);
			}
		}
		
		//totalBuiltUpArea of OC-
		JSONArray totalBuiltUpAreasOC = ocContext.read(BPACalculatorConstants.TOTAL_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalBuiltUpAreasOC)) {
			if (null != totalBuiltUpAreasOC.get(0)) {
				String ocTotalBuiltUpAreaString = totalBuiltUpAreasOC.get(0).toString();
				Double totalBuiltUpArea = Double.parseDouble(ocTotalBuiltUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA, totalBuiltUpArea);
			}
		}

		JSONArray totalBuitUpAreas = ocContext.read(BPACalculatorConstants.TOTAL_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalBuitUpAreas)) {
			if (null != totalBuitUpAreas.get(0)) {
				String totalBuitUpAreaString = totalBuitUpAreas.get(0).toString();
				Double totalBuitUpArea = Double.parseDouble(totalBuitUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA, totalBuitUpArea);
			}
		}
		
		JSONArray existingBuitUpAreas = edcrContext.read(BPACalculatorConstants.TOTAL_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalBuitUpAreas) && !CollectionUtils.isEmpty(existingBuitUpAreas)) {
			if (null != totalBuitUpAreas.get(0) && null != existingBuitUpAreas.get(0)) {
				String totalBuitUpAreaString = totalBuitUpAreas.get(0).toString();
				String existingBuitUpAreaString = existingBuitUpAreas.get(0).toString();
				Double totalBuitUpArea = Double.parseDouble(totalBuitUpAreaString);
				Double exisitingBuitUpArea = Double.parseDouble(existingBuitUpAreaString);
				paramMap.put(BPACalculatorConstants.DEVIATION_FLOOR_AREA, totalBuitUpArea-exisitingBuitUpArea);
			}
		}
		
		//use builtup area rather than floor area for some OC calculations-(this is stable wrt application fees do not change anything)
		JSONArray totalExistingBuiltUpArea=edcrContext.read(BPACalculatorConstants.TOTAL_BUILTUP_AREA_PATH);
		JSONArray totalBuiltUpAreas = ocContext.read(BPACalculatorConstants.TOTAL_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalBuiltUpAreas) && !CollectionUtils.isEmpty(totalExistingBuiltUpArea)) {
			if (null != totalBuiltUpAreas.get(0) && null != totalExistingBuiltUpArea.get(0)) {
				String totalBuiltUpAreaString = totalBuiltUpAreas.get(0).toString();
				String totalExistingBuiltUpAreaString = totalExistingBuiltUpArea.get(0).toString();
				Double totalBuiltUpArea = Double.parseDouble(totalBuiltUpAreaString);
				Double totalExisitingBuiltUpArea = Double.parseDouble(totalExistingBuiltUpAreaString);
				paramMap.put(BPACalculatorConstants.DEVIATION_BUILTUP_AREA, totalBuiltUpArea-totalExisitingBuiltUpArea);
			}
		}
		
		JSONArray totalbenchmarkValuePerAcre = ocContext.read(BPACalculatorConstants.BENCHMARK_VALUE_PATH);
		if (!CollectionUtils.isEmpty(totalbenchmarkValuePerAcre)) {
			if (null != totalbenchmarkValuePerAcre.get(0)) {
				String benchmarkValuePerAcreString = totalbenchmarkValuePerAcre.get(0).toString();
				Double benchmarkValuePerAcre = Double.parseDouble(benchmarkValuePerAcreString);
				paramMap.put(BPACalculatorConstants.BMV_ACRE, benchmarkValuePerAcre);
			}
		}

		JSONArray totalbaseFar = ocContext.read(BPACalculatorConstants.BASE_FAR_PATH);
		if (!CollectionUtils.isEmpty(totalbaseFar)) {
			if (null != totalbaseFar.get(0)) {
				String baseFarString = totalbaseFar.get(0).toString();
				Double baseFar = Double.parseDouble(baseFarString);
				paramMap.put(BPACalculatorConstants.BASE_FAR, baseFar);
			}
		}

		JSONArray totalProvidedFar = ocContext.read(BPACalculatorConstants.PROVIDED_FAR_PATH);
		if (!CollectionUtils.isEmpty(totalProvidedFar)) {
			if (null != totalProvidedFar.get(0)) {
				String providedFarString = totalProvidedFar.get(0).toString();
				Double providedFar = Double.parseDouble(providedFarString);
				paramMap.put(BPACalculatorConstants.PROVIDED_FAR, providedFar);
			}
		}
		
		JSONArray totalpermissibleFar = ocContext.read(BPACalculatorConstants.PERMISSABLE_FAR_PATH);
		if (!CollectionUtils.isEmpty(totalpermissibleFar)) {
			if (null != totalpermissibleFar.get(0)) {
				String permissibleFarString = totalpermissibleFar.get(0).toString();
				Double permissibleFar = Double.parseDouble(permissibleFarString);
				paramMap.put(BPACalculatorConstants.PERMISSABLE_FAR, permissibleFar);
			}
		}

		JSONArray totalProjectValueForEIDPOcArray = ocContext.read(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP_PATH);
		if (!CollectionUtils.isEmpty(totalProjectValueForEIDPOcArray)) {
			String totalProjectValueForEIDPOc = totalProjectValueForEIDPOcArray.get(0).toString();
			Double projectValueForEIDPOc = Double.parseDouble(totalProjectValueForEIDPOc);
			paramMap.put(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP_OC, projectValueForEIDPOc);
		}
		
		JSONArray totalProjectValueForEIDPArray = edcrContext.read(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP_PATH);
		if (!CollectionUtils.isEmpty(totalProjectValueForEIDPArray)) {
			String totalProjectValueForEIDP = totalProjectValueForEIDPArray.get(0).toString();
			Double projectValueForEIDP = Double.parseDouble(totalProjectValueForEIDP);
			paramMap.put(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP, projectValueForEIDP);
		}

		JSONArray isProjectUndertakingByGovtArray = ocContext.read(BPACalculatorConstants.PROJECT_UNDERTAKING_BY_GOVT_PATH);
		if (!CollectionUtils.isEmpty(isProjectUndertakingByGovtArray)) {
			boolean isProjectUndertakingByGovt = ((String) isProjectUndertakingByGovtArray.get(0)).equalsIgnoreCase("YES") ? true : false;
			paramMap.put(BPACalculatorConstants.PROJECT_UNDERTAKING_BY_GOVT, isProjectUndertakingByGovt);
		}
		
		JSONArray edcrBlockDetailArray = edcrContext.read(BPACalculatorConstants.BLOCKS_PATH);
		if (!CollectionUtils.isEmpty(edcrBlockDetailArray)) {
			paramMap.put(BPACalculatorConstants.BLOCK_DETAILS_EDCR, edcrBlockDetailArray.get(0));
		}
		
		JSONArray ocBlockDetailArray = ocContext.read(BPACalculatorConstants.BLOCKS_PATH);
		if (!CollectionUtils.isEmpty(ocBlockDetailArray)) {
			paramMap.put(BPACalculatorConstants.BLOCK_DETAILS_OC, ocBlockDetailArray.get(0));
		}
		
		paramMap.put(BPACalculatorConstants.APPLICATION_TYPE, applicationType);
		paramMap.put(BPACalculatorConstants.SERVICE_TYPE, serviceType);
		paramMap.put(BPACalculatorConstants.RISK_TYPE, riskType);
		paramMap.put(BPACalculatorConstants.FEE_TYPE, feeType);
		return paramMap;
	}

	private Map<String, Object> prepareParamMapForPermitOrder(DocumentContext edcrContext, String riskType, String serviceType, LinkedHashMap edcr) {
		Map<String, Object> paramMap = new HashMap<>();

		JSONArray occupancyTypeJSONArray = edcrContext.read(BPACalculatorConstants.OCCUPANCY_TYPE_PATH);
		if (!CollectionUtils.isEmpty(occupancyTypeJSONArray)) {
			if (null != occupancyTypeJSONArray.get(0)) {
				String occupancyType = occupancyTypeJSONArray.get(0).toString();
				paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
			}
		}

		JSONArray subOccupancyTypeJSONArray = edcrContext.read(BPACalculatorConstants.SUB_OCCUPANCY_TYPE_PATH);
		if (!CollectionUtils.isEmpty(subOccupancyTypeJSONArray)) {
			if (null != subOccupancyTypeJSONArray.get(0)) {
				String subOccupancyType = subOccupancyTypeJSONArray.get(0).toString();
				paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE, subOccupancyType);
			}
		}
		
		JSONArray plotAreas = edcrContext.read(BPACalculatorConstants.PLOT_AREA_PATH);
		if (!CollectionUtils.isEmpty(plotAreas)) {
			if (null != plotAreas.get(0)) {
				String plotAreaString = plotAreas.get(0).toString();
				Double plotArea = Double.parseDouble(plotAreaString);
				paramMap.put(BPACalculatorConstants.PLOT_AREA, plotArea);
			}
		}
		
		JSONArray totalBuitUpAreas = edcrContext.read(BPACalculatorConstants.TOTAL_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalBuitUpAreas)) {
			if (null != totalBuitUpAreas.get(0)) {
				String totalBuitUpAreaString = totalBuitUpAreas.get(0).toString();
				Double totalBuitUpArea = Double.parseDouble(totalBuitUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA, totalBuitUpArea);
			}
		}
		
		List<Occupancy> occupancy = edcrHelperUtils.getOccupancieswiseDetails(edcr);
		paramMap.put(BPACalculatorConstants.OCCUPANCYLIST, occupancy);

		JSONArray edcrTotalBuiltUpAreas = edcrContext.read(BPACalculatorConstants.TOTAL_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(edcrTotalBuiltUpAreas)) {
			if (null != edcrTotalBuiltUpAreas.get(0)) {
				String edcrTotalBuiltUpAreaString = edcrTotalBuiltUpAreas.get(0).toString();
				Double totalBuiltUpArea = Double.parseDouble(edcrTotalBuiltUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR, totalBuiltUpArea);
			}
		}

		JSONArray carpetAreaJson = edcrContext.read(BPACalculatorConstants.CARPET_AREA_PATH);
		if (!CollectionUtils.isEmpty(carpetAreaJson)) {
			if (null != carpetAreaJson.get(0)) {
				String carpetAreaString = carpetAreaJson.get(0).toString();
				Double carpetArea = Double.parseDouble(carpetAreaString);
				paramMap.put(BPACalculatorConstants.CARPET_AREA, carpetArea);
			}
		}

		// alteration builtup area related parameters-
		Double alterationTotalBuiltupArea = null;
		Double alterationExistingBuiltupArea = null;
		JSONArray alterationTotalBuiltupAreaJson = edcrContext
				.read(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(alterationTotalBuiltupAreaJson)) {
			String alterationTotalBuiltupAreaString = alterationTotalBuiltupAreaJson.get(0).toString();
			alterationTotalBuiltupArea = Double.parseDouble(alterationTotalBuiltupAreaString);
			paramMap.put(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA, alterationTotalBuiltupArea);
		}

		JSONArray alterationExistingBuiltupAreaJson = edcrContext
				.read(BPACalculatorConstants.ALTERATION_EXISTING_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(alterationExistingBuiltupAreaJson)) {
			String alterationExistingBuiltupAreaString = alterationExistingBuiltupAreaJson.get(0).toString();
			alterationExistingBuiltupArea = Double.parseDouble(alterationExistingBuiltupAreaString);
			paramMap.put(BPACalculatorConstants.ALTERATION_EXISTING_BUILTUP_AREA, alterationExistingBuiltupArea);
		}
		// subtract above two and put as proposed builtup area-
		if (Objects.nonNull(alterationTotalBuiltupArea) && Objects.nonNull(alterationExistingBuiltupArea)) {
			Double alterationProposedBuiltupArea = alterationTotalBuiltupArea - alterationExistingBuiltupArea;
			paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA, alterationProposedBuiltupArea);
		}

		JSONArray alterationSubServiceJson = edcrContext.read(BPACalculatorConstants.ALTERATION_SUBSERVICE_PATH);
		if (!CollectionUtils.isEmpty(alterationSubServiceJson)) {
			if (null != alterationSubServiceJson.get(0)) {
				String alterationSubService = alterationSubServiceJson.get(0).toString();
				paramMap.put(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY, alterationSubService);
			}
		}
		
		paramMap.put(BPACalculatorConstants.APPLICATION_TYPE, BPACalculatorConstants.BUILDING_PLAN_SCRUTINY);
		paramMap.put(BPACalculatorConstants.SERVICE_TYPE, serviceType);
		paramMap.put(BPACalculatorConstants.RISK_TYPE, riskType);
		//paramMap.put(BPACalculatorConstants.FEE_TYPE, feeType);
		return paramMap;
	}

	/**
	 * @param requestInfo
	 * @param criteria
	 * @param estimates
	 * @param feeType
	 * @param extraParamsForCalculationMap
	 */
	private void calculateTotalFee(RequestInfo requestInfo, CalulationCriteria criteria,
			ArrayList<TaxHeadEstimate> estimates, String feeType, Map<String, Object> extraParamsForCalculationMap) {
		Map<String, Object> paramMap = prepareMaramMap(requestInfo, criteria, feeType, extraParamsForCalculationMap);
		//Mdms call for Welfare Cess Fee rate
		Object mdmsData = mdmsService.mDMSCall(
				CalculationReq.builder().requestInfo(requestInfo).calulationCriteria(Arrays.asList(criteria)).build(),
				criteria.getTenantId());
		List listNode = JsonPath.read(mdmsData, "$.MdmsRes.BPA.ConstructionCostRate");

		String filterExp = "$.[?(@.name == '" + BPACalculatorConstants.PERSQFTCOST + "')]";
		List<Map<String, String>> constructionCostPerSqftJson = JsonPath.read(listNode, filterExp);

		String cwcRate = constructionCostPerSqftJson.get(0).get("rate");
		
		String filterExpresion = "$.[?(@.name == '" + BPACalculatorConstants.CONSTRUCTION_COST + "')]";
		List<Map<String, String>> constructionCostRateJson = JsonPath.read(listNode, filterExpresion);
		
		String  constructionCost = constructionCostRateJson.get(0).get("rate");
		
		
		//move all extra parameters from extraParamsForCalculationMap to paramMap-
		paramMap.put("mdmsData", extraParamsForCalculationMap.get("mdmsData"));
		paramMap.put("tenantId", extraParamsForCalculationMap.get("tenantId"));
		paramMap.put("BPA", extraParamsForCalculationMap.get("BPA"));
		paramMap.put("requestInfo", requestInfo);
		paramMap.put(BPACalculatorConstants.CWCFEE, cwcRate);
		paramMap.put(BPACalculatorConstants.COST_OF_CONSTRUCTION, constructionCost);
		paramMap.put(BPACalculatorConstants.SPARIT_CHECK, extraParamsForCalculationMap.get(BPACalculatorConstants.SPARIT_CHECK));
		BigDecimal calculatedTotalAmout = calculateTotalFeeAmount(paramMap, estimates);
		if (calculatedTotalAmout.compareTo(BigDecimal.ZERO) == -1) {
			// throw new CustomException(BPACalculatorConstants.INVALID_AMOUNT, "Tax amount is negative");
			calculatedTotalAmout = BigDecimal.ZERO;
		}
		// TaxHeadEstimate estimate = new TaxHeadEstimate();
		// estimate.setEstimateAmount(calculatedTotalAmout.setScale(0,
		// BigDecimal.ROUND_UP));
		// estimate.setCategory(Category.FEE);
		// String taxHeadCode =
		// utils.getTaxHeadCode(criteria.getBpa().getBusinessService(),
		// criteria.getFeeType());
		// estimate.setTaxHeadCode(taxHeadCode);
		// estimates.add(estimate);
	}
	
	private void setRevisionDataInParamMap(BPA bpa, Map<String, Object> paramMap,
			Map<String, Object> extraParamsForCalculationMap) {
		if (Objects.nonNull(extraParamsForCalculationMap.get(BPACalculatorConstants.REVISION))) {
			// if revision already passed in calculationCriteria for applicationFee-
			paramMap.put(BPACalculatorConstants.REVISION,
					extraParamsForCalculationMap.get(BPACalculatorConstants.REVISION));
		}
		else {
			// fetch revision data and set in paramMap-
			RevisionSearchCriteria revisionSearchCriteria = RevisionSearchCriteria.builder()
					.bpaApplicationNo(bpa.getApplicationNo()).build();
			List<Revision> revisions = revisionRepository.getRevisionData(revisionSearchCriteria);
			if (CollectionUtils.isEmpty(revisions)) {
				throw new CustomException(
						"No revision data found for refBpaApplicationNo:" + bpa.getApplicationNo() + ",refPermitNo:"
								+ bpa.getApprovalNo(),
						"No revision data found for refBpaApplicationNo:" + bpa.getApplicationNo() + ",refPermitNo:"
								+ bpa.getApprovalNo());
			}
			// TODO: could there be multiple revisions for one application as using first element-
			paramMap.put(BPACalculatorConstants.REVISION, revisions.get(0));
		}
			
			
	}
	
	private String getRefBpaApprovalNo(BPA bpa) {
		Map<String, Object> additionalDetails = new HashMap<>();
		if (Objects.nonNull(bpa) && Objects.nonNull(bpa.getAdditionalDetails())) {
			additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
		}
		String refBpaApprovalNo = null;
		if (Objects.nonNull(additionalDetails)
				&& Objects.nonNull(additionalDetails.get(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY))
				&& additionalDetails.get(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY) instanceof Map
				&& !StringUtils
						.isEmpty(((Map) additionalDetails.get(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY))
								.get(BPACalculatorConstants.BPA_ADD_DETAILS_PERMIT_NO_KEY))) {
			refBpaApprovalNo = String
					.valueOf(((Map) additionalDetails.get(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY))
							.get(BPACalculatorConstants.BPA_ADD_DETAILS_PERMIT_NO_KEY));

		}
		return refBpaApprovalNo;
	}
	
	private String getSubService(BPA bpa) {
		Map<String,Object> additionalDetails = new HashMap<>();
		if (Objects.nonNull(bpa) && Objects.nonNull(bpa.getAdditionalDetails())){
			additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
		}
		String subservice = null;
		if (Objects.nonNull(additionalDetails.get(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY))
				&& additionalDetails.get(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY) instanceof Map
				&& !StringUtils
						.isEmpty(((Map) additionalDetails.get(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY))
								.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subservice = String
					.valueOf(((Map) additionalDetails.get(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY))
							.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY));
		}
		return subservice;
	}
		
	private boolean isRevision(String subService) {
		Boolean isRevision = Boolean.FALSE;
		switch (subService) {
		case BPACalculatorConstants.ALTERATION_SUBSERVICE_A:
			isRevision = Boolean.TRUE;
			break;
		case BPACalculatorConstants.ALTERATION_SUBSERVICE_B:
			isRevision = Boolean.TRUE;
			break;
		case BPACalculatorConstants.ALTERATION_SUBSERVICE_C:
			isRevision = Boolean.FALSE;
			break;
		case BPACalculatorConstants.ALTERATION_SUBSERVICE_D:
			isRevision = Boolean.FALSE;
			break;
		}
		return isRevision;
	}
	
	private LinkedHashMap<String, Object> getEdcrDetails(RequestInfo requestInfo, String edcrNo, String tenantId) {
		LinkedHashMap<String, Object> edcr = null;
		try {
			edcr = edcrService.getEDCRDetails(requestInfo, edcrNo, tenantId);
		} catch (Exception ex) {
			log.error("error while fetching edcr details", ex);
		}

		return edcr;
	}
	
	private BPA getRefBpaApplication(RequestInfo requestInfo, BPA bpa) {
		BPA refBpa = null;
		try {
			refBpa = bpaService.getBuildingPlan(requestInfo, bpa.getTenantId(), null, getRefBpaApprovalNo(bpa));
		} catch (Exception ex) {
			log.error("exception while fetching reference bpa application", ex);
		}
		return refBpa;
	}
	
	private Map<String, Object> prepareMaramMap(RequestInfo requestInfo, CalulationCriteria criteria, String feeType,
			Map<String, Object> extraParamsForCalculationMap) {
		String businessService = "";
		String subService = "";
		Boolean isRevisionApplication = Boolean.FALSE;
		Boolean isRefApplicationPresentInSujog = Boolean.FALSE;
		BPA bpa = null;
		BPA refBpa = null;
		Map<String, Object> paramMap = new HashMap<>();
		if (Objects.nonNull(extraParamsForCalculationMap.get("BPA"))
				&& !StringUtils.isEmpty(((BPA) extraParamsForCalculationMap.get("BPA")).getBusinessService())) {
			bpa = (BPA) extraParamsForCalculationMap.get("BPA");
			businessService = bpa.getBusinessService();
			subService = getSubService(bpa);
			paramMap.put(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY, subService);
			if (subService != null) {
				isRevisionApplication = isRevision(subService);
				paramMap.put(BPACalculatorConstants.IS_REVISION_APPLICATION, isRevisionApplication);
				if (isRevisionApplication) {
					refBpa = getRefBpaApplication(requestInfo, bpa);
					if (Objects.nonNull(refBpa)) {
						isRefApplicationPresentInSujog = Boolean.TRUE;
						paramMap.put(BPACalculatorConstants.REF_BPA_APPLICATION, refBpa);
						paramMap.put(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG,
								isRefApplicationPresentInSujog);
					}
				}
			}
		}
		LinkedHashMap edcr = null;
		if (Objects.nonNull(criteria.getBpa().getEdcrNumber())) {
			edcr = getEdcrDetails(requestInfo, criteria.getBpa().getEdcrNumber(), criteria.getBpa().getTenantId());
			paramMap.put(BPACalculatorConstants.EDCR_DETAILS, edcr);
		}
		LinkedHashMap refEdcr = null;
		if(isRefApplicationPresentInSujog) {
			refEdcr = getEdcrDetails(requestInfo, refBpa.getEdcrNumber(), bpa.getTenantId());
			paramMap.put(BPACalculatorConstants.REF_EDCR_DETAILS, refEdcr);
		}
		if (BPACalculatorConstants.BUSINESSSERVICE_PREAPPROVEDPLAN.equalsIgnoreCase(businessService)) {
			// for BPA 6(preapproved plan) -
			paramMap.putAll( prepareParamMapForPreapprovedPlan(requestInfo, criteria, feeType, extraParamsForCalculationMap));
			return paramMap;
		} else if (Boolean.TRUE.equals(isRevisionApplication)) {
			
			if(isRefApplicationPresentInSujog) {
				// need to prepare paramMap as usual for BPA1,2,3,4 -
				prepareParamMapForBpa1to4(requestInfo, criteria, feeType, paramMap);
			}
			else {
				// need to prepare paramMap from refApplicationDetails - 
				setRevisionDataInParamMap(bpa, paramMap, extraParamsForCalculationMap);
				prepareParamMapForRevisionNonSujogApplication(requestInfo, criteria, feeType, paramMap, bpa);
			}
			
		} else if (criteria.getBpa().isRevalidationApplication()) {
			if (criteria.getIsNonSujogRevalidation()) {
				prepareParamMapForRevalidationNonSujogApplication(requestInfo, criteria, feeType, paramMap);
			}else {
				// For Inside Sujog Revalidation , calculating the fee normally
				prepareParamMapForBpa1to4(requestInfo, criteria, feeType, paramMap);
			}
		}
		else {
			// for BPA 1,2,3,4 -
			prepareParamMapForBpa1to4(requestInfo, criteria, feeType, paramMap);
		}
		return paramMap;
	}
	
	private void prepareParamMapForRevalidationNonSujogApplication(RequestInfo requestInfo, CalulationCriteria criteria,
			String feeType, Map<String, Object> paramMap) {

		log.info("Inside method prepareParamMapForRevalidationNonSujogApplication ==");

		BPA bpa = criteria.getBpa();
		Map<String, Object>	additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
		Map<String, Object> revalidationPlanNonSujog = (Map<String, Object>) additionalDetails.get("RevalidationPlanNonSujog");
		List<Map<String, Object>> occupancyList = (List<Map<String, Object>>) revalidationPlanNonSujog.get("occupancyList");

		DocumentContext context = JsonPath.parse(bpa.getAdditionalDetails());
		boolean isSpecialBuilding = context.read("$.RevalidationPlanNonSujog.isSpecialBuilding");
		boolean isSparit = context.read("$.RevalidationPlanNonSujog.isSparit");

		List<Occupancy> ocL = new ArrayList<>();
		//Occupancy oc = new Occupancy();
//
//	    for (Map<String, Object> occupancyData : occupancyList) {hereoc
//	        Occupancy oc = new Occupancy();
//
//	        oc.setOccupancyCode((String) occupancyData.get("OccupancyCode"));
//	        oc.setFloorArea(Double.valueOf((String) occupancyData.get("floorArea")));
//	        oc.setSubOccupancyCode((String) occupancyData.get("subOccupancyCode"));
//	        oc.setBuiltUpArea(Double.valueOf((String) occupancyData.get("builtUpArea")));
//
//	        ocL.add(oc);
//	    }
	    
	    ocL = groupOccupancies(occupancyList);

//		paramMap.put(BPACalculatorConstants.OCCUPANCYLIST,
//				revalidationPlanNonSujog.get("OccupancyList"));
		paramMap.put(BPACalculatorConstants.OCCUPANCYLIST,
		ocL);
		paramMap.put(BPACalculatorConstants.APPLICATION_TYPE,
				revalidationPlanNonSujog.get("applicationType"));
		paramMap.put(BPACalculatorConstants.SERVICE_TYPE,
				revalidationPlanNonSujog.get("serviceType"));
		paramMap.put(BPACalculatorConstants.RISK_TYPE,
				bpa.getRiskType());
		paramMap.put(BPACalculatorConstants.PLOT_AREA,
				Double.valueOf((String) revalidationPlanNonSujog.get(BPACalculatorConstants.PLOT_AREA)));
		paramMap.put(BPACalculatorConstants.SPARIT_CHECK,
						revalidationPlanNonSujog.get("isSparit"));
		paramMap.put(BPACalculatorConstants.FEE_TYPE,
				feeType);
		paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR,
				Double.valueOf((String) revalidationPlanNonSujog.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR)));

	}

	private void prepareParamMapForRevisionNonSujogApplication(RequestInfo requestInfo, CalulationCriteria criteria,
			String feeType, Map<String, Object> paramMap, BPA bpa) {
		Revision revision = (Revision) paramMap.get(BPACalculatorConstants.REVISION);
		if(revision.isSujogExistingApplication())
			return;
		String applicationType = criteria.getApplicationType();
		String serviceType = criteria.getServiceType();
		// assumption : applicationType, serviceType of old non-sujog application was same as that of new revision application- 
		paramMap.put(BPACalculatorConstants.APPLICATION_TYPE, applicationType);
		paramMap.put(BPACalculatorConstants.SERVICE_TYPE, serviceType);
		paramMap.put(BPACalculatorConstants.FEE_TYPE, feeType);
		if (Objects.isNull(revision.getRefApplicationDetails()) || !(revision.getRefApplicationDetails() instanceof Map)
				|| CollectionUtils.isEmpty(((Map) revision.getRefApplicationDetails()))) {
			throw new CustomException("refApplicationDetails must not be null or empty for non-sujog permit numbers",
					"refApplicationDetails must not be null or empty for non-sujog permit numbers");
		}
		String subService = "";
		Boolean isRefApplicationPresentInSujog = Boolean.FALSE;
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subService=String.valueOf(paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY));
		}
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG))) {
			isRefApplicationPresentInSujog=(Boolean) paramMap.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG);
		}
		
		Map<String, Object> refApplicationDetails = (Map<String, Object>) revision.getRefApplicationDetails();

		// edcrDetail.*.planDetail.virtualBuilding.mostRestrictiveFarHelper.type.code
		paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE,
				refApplicationDetails.get(BPACalculatorConstants.OCCUPANCY_TYPE));
		// edcrDetail.*.planDetail.virtualBuilding.mostRestrictiveFarHelper.subtype.code"
		paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,
				refApplicationDetails.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE));
		// edcrDetail.*.planDetail.plot.area
		paramMap.put(BPACalculatorConstants.PLOT_AREA,
				getValueInDoubleFormat(BPACalculatorConstants.PLOT_AREA,
						refApplicationDetails.get(BPACalculatorConstants.PLOT_AREA)));
		// edcrDetail.*.planDetail.virtualBuilding.totalFloorArea
		paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA,
				getValueInDoubleFormat(BPACalculatorConstants.TOTAL_FLOOR_AREA,
						refApplicationDetails.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)));
		// edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea
		paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR,
				getValueInDoubleFormat(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR,
						refApplicationDetails.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR)));
		// edcrDetail.*.planDetail.totalEWSFeeEffectiveArea
		paramMap.put(BPACalculatorConstants.EWS_AREA,
				getValueInDoubleFormat(BPACalculatorConstants.EWS_AREA,
						refApplicationDetails.get(BPACalculatorConstants.EWS_AREA)));
		// edcrDetail.*.planDetail.planInformation.benchmarkValuePerAcre
		paramMap.put(BPACalculatorConstants.BMV_ACRE,
				getValueInDoubleFormat(BPACalculatorConstants.BMV_ACRE,
						refApplicationDetails.get(BPACalculatorConstants.BMV_ACRE)));
		// edcrDetail.*.planDetail.farDetails.baseFar
		paramMap.put(BPACalculatorConstants.BASE_FAR,
				getValueInDoubleFormat(BPACalculatorConstants.BASE_FAR,
						refApplicationDetails.get(BPACalculatorConstants.BASE_FAR)));
		// edcrDetail.*.planDetail.farDetails.providedFar
		paramMap.put(BPACalculatorConstants.PROVIDED_FAR,
				getValueInDoubleFormat(BPACalculatorConstants.PROVIDED_FAR,
						refApplicationDetails.get(BPACalculatorConstants.PROVIDED_FAR)));
		// edcrDetail.*.planDetail.farDetails.permissableFar
		paramMap.put(BPACalculatorConstants.PERMISSABLE_FAR,
				getValueInDoubleFormat(BPACalculatorConstants.PERMISSABLE_FAR,
						refApplicationDetails.get(BPACalculatorConstants.PERMISSABLE_FAR)));
		// edcrDetail.*.planDetail.farDetails.tdrFarRelaxation
		paramMap.put(BPACalculatorConstants.TDR_FAR_RELAXATION,
				getValueInDoubleFormat(BPACalculatorConstants.TDR_FAR_RELAXATION,
						refApplicationDetails.get(BPACalculatorConstants.TDR_FAR_RELAXATION)));
		// edcrDetail.*.planDetail.planInformation.totalNoOfDwellingUnits
		paramMap.put(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS,
				getValueInIntegerFormat(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS,
						refApplicationDetails.get(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS)));
		// edcrDetail.*.planDetail.planInformation.shelterFeeRequired
		//paramMap.put(BPACalculatorConstants.SHELTER_FEE, refApplicationDetails.get(BPACalculatorConstants.SHELTER_FEE));
		
//		Updated code due to db saving true / false rather then YES/NO
//		String shelterFees =(String)refApplicationDetails.get(BPACalculatorConstants.SHELTER_FEE);
//		if (!shelterFees.isEmpty()) {
//			boolean isShelterFeeRequired=Boolean.FALSE;
//			if(shelterFees.equals(BPACalculatorConstants.NO))
//				isShelterFeeRequired=Boolean.FALSE;
//			if(shelterFees.equals(BPACalculatorConstants.YES))
//				isShelterFeeRequired=Boolean.TRUE;
//			paramMap.put(BPACalculatorConstants.SHELTER_FEE, isShelterFeeRequired);
//			}
		
		Object shelterObj = refApplicationDetails.get(BPACalculatorConstants.SHELTER_FEE);
		Boolean isShelterFeeRequired = Boolean.FALSE;
		if (shelterObj instanceof Boolean) {
		    isShelterFeeRequired = (Boolean) shelterObj;   
		}
		paramMap.put(BPACalculatorConstants.SHELTER_FEE, isShelterFeeRequired);
		
		// edcrDetail.*.planDetail.planInformation.isSecurityDepositRequired
//		paramMap.put(BPACalculatorConstants.SECURITY_DEPOSIT,
//				refApplicationDetails.get(BPACalculatorConstants.SECURITY_DEPOSIT));
		
//		String securityDeposit =(String)refApplicationDetails.get(BPACalculatorConstants.SECURITY_DEPOSIT);
//		if (!securityDeposit.isEmpty()) {
//			boolean isSecurityDepositFeeRequired=Boolean.FALSE;
//			if(securityDeposit.equals(BPACalculatorConstants.NO))
//				isSecurityDepositFeeRequired=Boolean.FALSE;
//			if(securityDeposit.equals(BPACalculatorConstants.YES))
//				isSecurityDepositFeeRequired=Boolean.TRUE;
//			paramMap.put(BPACalculatorConstants.SECURITY_DEPOSIT, isSecurityDepositFeeRequired);
//			}
		
		Object securityDepositObj =
		        refApplicationDetails.get(BPACalculatorConstants.SECURITY_DEPOSIT);
		Boolean isSecurityDepositFeeRequired = Boolean.FALSE;
		if (securityDepositObj instanceof Boolean) {
		    isSecurityDepositFeeRequired = (Boolean) securityDepositObj;
		}
		paramMap.put(BPACalculatorConstants.SECURITY_DEPOSIT,
		        isSecurityDepositFeeRequired);
		
		// edcrDetail.*.planDetail.planInformation.projectValueForEIDP
		paramMap.put(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP,
				getValueInDoubleFormat(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP,
						refApplicationDetails.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP)));
		// edcrDetail.*.planDetail.planInformation.isRetentionFeeApplicable
//		paramMap.put(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE,
//				refApplicationDetails.get(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE));
		
//		String retentionFees =(String)refApplicationDetails.get(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE);
//		if (!retentionFees.isEmpty()) {
//			boolean isretentionFeesFeeRequired=Boolean.FALSE;
//			if(retentionFees.equals(BPACalculatorConstants.NO))
//				isretentionFeesFeeRequired=Boolean.FALSE;
//			if(retentionFees.equals(BPACalculatorConstants.YES))
//				isretentionFeesFeeRequired=Boolean.TRUE;
//			paramMap.put(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE, isretentionFeesFeeRequired);
//			}
		
		Object retentionFeeObj = refApplicationDetails.get(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE);
		Boolean isRetentionFeeRequired = Boolean.FALSE;
		if (retentionFeeObj instanceof Boolean) {
			isRetentionFeeRequired = (Boolean) retentionFeeObj;
		}
		paramMap.put(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE, isRetentionFeeRequired);
		
		// edcrDetail.*.planDetail.planInformation.numberOfTemporaryStructures
		paramMap.put(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES,
				getValueInDoubleFormat(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES,
						refApplicationDetails.get(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES)));
		paramMap.put(BPACalculatorConstants.RISK_TYPE, refApplicationDetails.get(BPACalculatorConstants.RISK_TYPE));
		
		//for non-sujog application for proposed area-
		paramMap.put(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA,
				getValueInDoubleFormat(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR,
						refApplicationDetails.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR)));
		paramMap.put(BPACalculatorConstants.ALTERATION_EXISTING_BUILTUP_AREA,
				getValueInDoubleFormat(BPACalculatorConstants.TOTAL_EXISTING_BUILTUP_AREA_EDCR_NON_SUJOG,
						refApplicationDetails.get(BPACalculatorConstants.TOTAL_EXISTING_BUILTUP_AREA_EDCR_NON_SUJOG)));
		paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA,
				(Double) paramMap.get(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA)
						- (Double) paramMap.get(BPACalculatorConstants.ALTERATION_EXISTING_BUILTUP_AREA));
		
		// occupancy-wise breakup list -
		List<Occupancy> occupancyList = new ArrayList<>();
		Double totalBuiltupArea = new Double(0);
		Double totalExistingBuiltupArea = new Double(0);
		Double totaExistingFloorArea = new Double(0);
		Double totalProposedFloorArea = new Double(0);
		if (Objects
				.nonNull(((Map) revision.getRefApplicationDetails())
						.get(BPACalculatorConstants.REVISION_REF_APPL_DETAILS_OCCUPANCIES))
				&& ((Map) revision.getRefApplicationDetails())
						.get(BPACalculatorConstants.REVISION_REF_APPL_DETAILS_OCCUPANCIES) instanceof List
				&& !((List) ((Map) revision.getRefApplicationDetails())
						.get(BPACalculatorConstants.REVISION_REF_APPL_DETAILS_OCCUPANCIES)).isEmpty()) {
			List<Map<String, Object>> occupancies = (List) ((Map) revision.getRefApplicationDetails())
					.get(BPACalculatorConstants.REVISION_REF_APPL_DETAILS_OCCUPANCIES);
			for(Map<String,Object> occupancyMap:occupancies) {
				Occupancy occupancy = new Occupancy();
				occupancy.setOccupancyCode(String.valueOf(occupancyMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)));
				occupancy.setSubOccupancyCode(String.valueOf(occupancyMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)));
				occupancy.setFloorArea(getValueInDoubleFormat(BPACalculatorConstants.TOTAL_FLOOR_AREA,
						occupancyMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)));
				Double totalBuiltUpAreaForOccupancy=getValueInDoubleFormat(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR,
						occupancyMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR));
				occupancy.setBuiltUpArea(totalBuiltUpAreaForOccupancy);
				Double totalExistingBuiltupAreaForOccupancy = getValueInDoubleFormat(
						BPACalculatorConstants.TOTAL_EXISTING_BUILTUP_AREA_EDCR_NON_SUJOG,
						occupancyMap.get(BPACalculatorConstants.TOTAL_EXISTING_BUILTUP_AREA_EDCR_NON_SUJOG));
				occupancy.setExistingBuiltUpArea(totalExistingBuiltupAreaForOccupancy);
				
				Double totaExistingFloorAreaForOccupancy=getValueInDoubleFormat(BPACalculatorConstants.TOTAL_EXISTING_FLOOR_AREA_EDCR,
						occupancyMap.get(BPACalculatorConstants.TOTAL_EXISTING_FLOOR_AREA_EDCR));
				occupancy.setExistingFloorArea(totaExistingFloorAreaForOccupancy);
				
				Double totalProposedFloorAreaForOccupancy=getValueInDoubleFormat(BPACalculatorConstants.TOTAL_PROPOSED_FLOOR_AREA_EDCR,
						occupancyMap.get(BPACalculatorConstants.TOTAL_PROPOSED_FLOOR_AREA_EDCR));
				occupancy.setProposedFloorArea(totalProposedFloorAreaForOccupancy);
				
				occupancyList.add(occupancy);
				totalBuiltupArea = totalBuiltupArea + totalBuiltUpAreaForOccupancy;
				totalExistingBuiltupArea = totalExistingBuiltupArea + totalExistingBuiltupAreaForOccupancy;
				totaExistingFloorArea = totaExistingFloorArea + totaExistingFloorAreaForOccupancy;
				totalProposedFloorArea = totalProposedFloorArea +totalProposedFloorAreaForOccupancy;
			}
		}
		paramMap.put(BPACalculatorConstants.OCCUPANCYLIST,occupancyList);
		paramMap.put(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA, totalBuiltupArea);
		paramMap.put(BPACalculatorConstants.ALTERATION_EXISTING_BUILTUP_AREA, totalExistingBuiltupArea);
		paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA,
				totalBuiltupArea - totalExistingBuiltupArea);
		if (BPACalculatorConstants.ALTERATION_SUBSERVICE_A.equals(subService) && !isRefApplicationPresentInSujog) {
			paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR, totalBuiltupArea);
		}
		if (BPACalculatorConstants.ALTERATION_SUBSERVICE_B.equals(subService) && !isRefApplicationPresentInSujog) {
			paramMap.put(BPACalculatorConstants.TOTAL_EXISTING_FLOOR_AREA_EDCR, totaExistingFloorArea);
			paramMap.put(BPACalculatorConstants.TOTAL_PROPOSED_FLOOR_AREA_EDCR, totalProposedFloorArea);		}
	}
	
	private Double getValueInDoubleFormat(String key, Object value) {
		Double doubleValue = Double.parseDouble("0");
		if (StringUtils.isEmpty(value)) {
			log.info("setting 0 value of key:" + key + " as value is null/empty");
			;
			return doubleValue;
		}
		try {
			doubleValue = Double.parseDouble(String.valueOf(value));
		} catch (NumberFormatException nfe) {
			log.error("NumberFormatException: setting value as 0 as value of key:" + key + " is not a Double:" + value);
		}
		return doubleValue;
	}
	
	private Integer getValueInIntegerFormat(String key, Object value) {
		Integer integerValue = Integer.parseInt("0");
		if (StringUtils.isEmpty(value)) {
			log.info("setting 0 value of key:" + key + " as value is null/empty");
			;
			return integerValue;
		}
		try {
			integerValue = Integer.parseInt(String.valueOf(value));
		} catch (NumberFormatException nfe) {
			log.error(
					"NumberFormatException: setting value as 0 as value of key:" + key + " is not a Integer:" + value);
		}
		return integerValue;
	}
	
	/**
	 * @param requestInfo
	 * @param criteria
	 * @param feeType
	 * @return
	 */
	private Map<String, Object> prepareParamMapForPreapprovedPlan(RequestInfo requestInfo, CalulationCriteria criteria,
			String feeType, Map<String, Object> extraParamsForCalculationMap) {
		String applicationType = criteria.getApplicationType();
		String serviceType = criteria.getServiceType();
		String riskType = criteria.getBpa().getRiskType();
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put(BPACalculatorConstants.APPLICATION_TYPE, applicationType);
		paramMap.put(BPACalculatorConstants.SERVICE_TYPE, serviceType);
		paramMap.put(BPACalculatorConstants.RISK_TYPE, riskType);
		paramMap.put(BPACalculatorConstants.FEE_TYPE, feeType);
		
		//fetch preapproved plan-
		PreapprovedPlan preapprovedPlan = fetchPreapprovedPlanFromDrawingNo(criteria.getBpa().getEdcrNumber());
		Map<String, String> drawingDetail = (Map) preapprovedPlan.getDrawingDetail();
		

		List<Occupancy> occupancy = edcrHelperUtils.getOccupancieswiseDetailsforpreApproved(preapprovedPlan.getDrawingDetail());
		
		//put the data occupancy wise
		paramMap.put(BPACalculatorConstants.OCCUPANCYLIST, occupancy);
		//TODO remove hardcoded parameters and replace with proper parameters from preapproved plan
		//"edcrDetail.*.planDetail.virtualBuilding.totalBuitUpArea"
		paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR, drawingDetail.get("totalBuitUpArea"));
		//edcrDetail.*.planDetail.virtualBuilding.mostRestrictiveFarHelper.type.code
		
		paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, "A");
		//
		//edcrDetail.*.planDetail.virtualBuilding.mostRestrictiveFarHelper.subtype.code"
		paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE, "A-P");
		//paramMap.put("", );
		
		
		return paramMap;
	}
	
	private PreapprovedPlan fetchPreapprovedPlanFromDrawingNo(String drawingNo) {
		PreapprovedPlanSearchCriteria criteria = new PreapprovedPlanSearchCriteria();
		criteria.setDrawingNo(drawingNo);
		List<PreapprovedPlan> preapprovedPlans = preapprovedPlanRepository.getPreapprovedPlansData(criteria);
		if (CollectionUtils.isEmpty(preapprovedPlans)) {
			log.error("No preapproved plan with provided drawingNo:" + drawingNo);
			throw new CustomException("No preapproved plan with provided drawingNo",
					"No preapproved plan with provided drawingNo");
		}
		return preapprovedPlans.get(0);
	}

	/**
	 * @param requestInfo
	 * @param criteria
	 * @param feeType
	 * @return
	 */
	public void prepareParamMapForBpa1to4(RequestInfo requestInfo, CalulationCriteria criteria,
			String feeType,Map<String, Object> paramMap) {
		String applicationType = criteria.getApplicationType();
		String serviceType = criteria.getServiceType();
		String riskType = criteria.getRiskType();
		@SuppressWarnings("unchecked")
		LinkedHashMap<String, Object> edcr = (LinkedHashMap<String, Object>) paramMap.get(BPACalculatorConstants.EDCR_DETAILS);
		
		String jsonString = new JSONObject(edcr).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		
		List<Occupancy> occupancy = edcrHelperUtils.getOccupancieswiseDetails(edcr);
		
		//List<Occupancy> occupancy =

		//Map<String, Object> paramMap = new HashMap<>();
/*
		JSONArray occupancyTypeJSONArray = context.read(BPACalculatorConstants.OCCUPANCY_TYPE_PATH);
		if (!CollectionUtils.isEmpty(occupancyTypeJSONArray)) {
			if (null != occupancyTypeJSONArray.get(0)) {
				String occupancyType = occupancyTypeJSONArray.get(0).toString();
				paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
			}
		}

		JSONArray subOccupancyTypeJSONArray = context.read(BPACalculatorConstants.SUB_OCCUPANCY_TYPE_PATH);
		if (!CollectionUtils.isEmpty(subOccupancyTypeJSONArray)) {
			if (null != subOccupancyTypeJSONArray.get(0)) {
				String subOccupancyType = subOccupancyTypeJSONArray.get(0).toString();
				paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE, subOccupancyType);
			}

		}*/
		paramMap.put(BPACalculatorConstants.OCCUPANCYLIST, occupancy);

		JSONArray plotAreas = context.read(BPACalculatorConstants.PLOT_AREA_PATH);
		if (!CollectionUtils.isEmpty(plotAreas)) {
			if (null != plotAreas.get(0)) {
				String plotAreaString = plotAreas.get(0).toString();
				Double plotArea = Double.parseDouble(plotAreaString);
				paramMap.put(BPACalculatorConstants.PLOT_AREA, plotArea);
			}
		}

		JSONArray totalBuitUpAreas = context.read(BPACalculatorConstants.TOTAL_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalBuitUpAreas)) {
			if (null != totalBuitUpAreas.get(0)) {
				String totalBuitUpAreaString = totalBuitUpAreas.get(0).toString();
				Double totalBuitUpArea = Double.parseDouble(totalBuitUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA, totalBuitUpArea);
			}
		}
		
		JSONArray edcrTotalBuiltUpAreas = context.read(BPACalculatorConstants.TOTAL_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(edcrTotalBuiltUpAreas)) {
			if (null != edcrTotalBuiltUpAreas.get(0)) {
				String edcrTotalBuiltUpAreaString = edcrTotalBuiltUpAreas.get(0).toString();
				Double totalBuiltUpArea = Double.parseDouble(edcrTotalBuiltUpAreaString);
				paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR, totalBuiltUpArea);
			}
		}

		JSONArray totalEWSAreas = context.read(BPACalculatorConstants.EWS_AREA_PATH);
		if (!CollectionUtils.isEmpty(totalEWSAreas)) {
			if (null != totalEWSAreas.get(0)) {
				String totalEWSAreaString = totalEWSAreas.get(0).toString();
				Double totalEWSArea = Double.parseDouble(totalEWSAreaString);
				paramMap.put(BPACalculatorConstants.EWS_AREA, totalEWSArea);
			}
		}

		JSONArray totalbenchmarkValuePerAcre = context.read(BPACalculatorConstants.BENCHMARK_VALUE_PATH);
		if (!CollectionUtils.isEmpty(totalbenchmarkValuePerAcre)) {
			if (null != totalbenchmarkValuePerAcre.get(0)) {
				String benchmarkValuePerAcreString = totalbenchmarkValuePerAcre.get(0).toString();
				Double benchmarkValuePerAcre = Double.parseDouble(benchmarkValuePerAcreString);
				paramMap.put(BPACalculatorConstants.BMV_ACRE, benchmarkValuePerAcre);
			}
		}

		JSONArray totalbaseFar = context.read(BPACalculatorConstants.BASE_FAR_PATH);
		if (!CollectionUtils.isEmpty(totalbaseFar)) {
			if (null != totalbaseFar.get(0)) {
				String baseFarString = totalbaseFar.get(0).toString();
				Double baseFar = Double.parseDouble(baseFarString);
				paramMap.put(BPACalculatorConstants.BASE_FAR, baseFar);
			}
		}

		JSONArray totalpermissibleFar = context.read(BPACalculatorConstants.PROVIDED_FAR_PATH);
		if (!CollectionUtils.isEmpty(totalpermissibleFar)) {
			if (null != totalpermissibleFar.get(0)) {
				String permissibleFarString = totalpermissibleFar.get(0).toString();
				Double permissibleFar = Double.parseDouble(permissibleFarString);
				paramMap.put(BPACalculatorConstants.PROVIDED_FAR, permissibleFar);
			}
		}
		
		JSONArray maxPermissibleFarJson = context.read(BPACalculatorConstants.PERMISSABLE_FAR_PATH);
		if (!CollectionUtils.isEmpty(maxPermissibleFarJson)) {
			if (null != maxPermissibleFarJson.get(0)) {
				String maxPermissibleFarString = maxPermissibleFarJson.get(0).toString();
				Double maxPermissibleFar = Double.parseDouble(maxPermissibleFarString);
				paramMap.put(BPACalculatorConstants.PERMISSABLE_FAR, maxPermissibleFar);
			}
		}
		
		JSONArray tdrFarRelaxationJson = context.read(BPACalculatorConstants.TDR_FAR_RELAXATION_PATH);
		if (!CollectionUtils.isEmpty(tdrFarRelaxationJson)) {
			if (null != tdrFarRelaxationJson.get(0)) {
				String tdrFarRelaxationString = tdrFarRelaxationJson.get(0).toString();
				Double tdrFarRelaxation = Double.parseDouble(tdrFarRelaxationString);
				paramMap.put(BPACalculatorConstants.TDR_FAR_RELAXATION, tdrFarRelaxation);
			}
		}
		
		JSONArray additionalTdrJson = context.read(BPACalculatorConstants.ADDITIONAL_TDR_PATH);
		if (!CollectionUtils.isEmpty(additionalTdrJson)) {
			if (null != additionalTdrJson.get(0)) {
				String additionalTdrString = additionalTdrJson.get(0).toString();
				Double additionalTdr = Double.parseDouble(additionalTdrString);
				paramMap.put(BPACalculatorConstants.ADDITIONAL_TDR, additionalTdr);
			}
		}

		JSONArray totalNoOfDwellingUnitsArray = context.read(BPACalculatorConstants.DWELLING_UNITS_PATH);
		if (!CollectionUtils.isEmpty(totalNoOfDwellingUnitsArray)) {
			if (null != totalNoOfDwellingUnitsArray.get(0)) {
				String totalNoOfDwellingUnitsString = totalNoOfDwellingUnitsArray.get(0).toString();
				Integer totalNoOfDwellingUnits = Integer.parseInt(totalNoOfDwellingUnitsString);
				paramMap.put(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS, totalNoOfDwellingUnits);
			}
		}

		JSONArray isShelterFeeRequiredArray = context.read(BPACalculatorConstants.SHELTER_FEE_PATH);
		if (!CollectionUtils.isEmpty(isShelterFeeRequiredArray)) {
			boolean isShelterFeeRequired = (boolean) isShelterFeeRequiredArray.get(0);
			paramMap.put(BPACalculatorConstants.SHELTER_FEE, isShelterFeeRequired);
		}

		JSONArray isSecurityDepositRequiredArray = context.read(BPACalculatorConstants.SECURITY_DEPOSIT_PATH);
		if (!CollectionUtils.isEmpty(isSecurityDepositRequiredArray)) {
			boolean isSecurityDepositRequired = (boolean) isSecurityDepositRequiredArray.get(0);
			paramMap.put(BPACalculatorConstants.SECURITY_DEPOSIT, isSecurityDepositRequired);
		}
		
		JSONArray totalProjectValueForEIDPArray = context.read(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP_PATH);
		if (!CollectionUtils.isEmpty(totalProjectValueForEIDPArray)) {
			String totalProjectValueForEIDP = totalProjectValueForEIDPArray.get(0).toString();
			Double projectValueForEIDP = Double.parseDouble(totalProjectValueForEIDP);
			paramMap.put(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP, projectValueForEIDP);
		}
		
		JSONArray isRetentionFeeApplicableJson = context.read(BPACalculatorConstants.RETENTION_FEE_APPLICABLE_PATH);
		if (!CollectionUtils.isEmpty(isRetentionFeeApplicableJson)) {
			boolean isRetentionFeeApplicable = (boolean) isRetentionFeeApplicableJson.get(0);
			paramMap.put(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE, isRetentionFeeApplicable);
		}
		
		JSONArray numberOfTemporaryStructuresJson = context.read(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES_PATH);
		if (!CollectionUtils.isEmpty(numberOfTemporaryStructuresJson)) {
			String numberOfTemporaryStructuresString = numberOfTemporaryStructuresJson.get(0).toString();
			Double numberOfTemporaryStructures = Double.parseDouble(numberOfTemporaryStructuresString);
			paramMap.put(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES, numberOfTemporaryStructures);
		}
		
		JSONArray carpetAreaJson = context.read(BPACalculatorConstants.CARPET_AREA_PATH);
		if (!CollectionUtils.isEmpty(carpetAreaJson)) {
			if (null != carpetAreaJson.get(0)) {
				String carpetAreaString = carpetAreaJson.get(0).toString();
				Double carpetArea = Double.parseDouble(carpetAreaString);
				paramMap.put(BPACalculatorConstants.CARPET_AREA, carpetArea);
			}
		}
		
		//get Public washroom details here
		JSONArray occupancyPercentages = context.read(BPACalculatorConstants.OCCUPANCY_PERCENTAGES_PATH);
		edcrHelperUtils.getPublicWashroomDetails(paramMap, occupancyPercentages);
		edcrHelperUtils.getICTDetails(paramMap, occupancyPercentages);
		
		
		// Get if the project is under affordable housing scheme or not
		JSONArray isProjectUnderAffordableHousingSchemeJSON = context
				.read(BPACalculatorConstants.AFFORDABLE_HOUSING_PATH);
		if (!CollectionUtils.isEmpty(isProjectUnderAffordableHousingSchemeJSON)) {
			boolean isProjectUnderAffordableHousingScheme = ((String) isProjectUnderAffordableHousingSchemeJSON.get(0))
					.equalsIgnoreCase("YES") ? true : false;
			paramMap.put(BPACalculatorConstants.IS_PROJECT_UNDER_AFFORDABLE_HOUSING_SCHEME,
					isProjectUnderAffordableHousingScheme);
		}
		
		// Get if the project is under affordable housing scheme or not
		JSONArray isAffordableHousingRequirePurchasableFARJSON = context
				.read(BPACalculatorConstants.AFFORDABLE_HOUSING_PURCHASABLE_FAR_PATH);
		if (!CollectionUtils.isEmpty(isAffordableHousingRequirePurchasableFARJSON)) {
			boolean isPurchsableFARRequired = ((String) isAffordableHousingRequirePurchasableFARJSON.get(0))
					.equalsIgnoreCase("YES") ? true : false;
			paramMap.put(BPACalculatorConstants.IS_AFFORDABLE_HOUSING_REQUIRE_PURCHASABLE_FAR,
					isPurchsableFARRequired);
		}
		
		// Get if the Eidp Fee Exempted For The Project (Govt Plot)
		JSONArray isEidpFeeExemptedForTheProjectJSON = context.read(BPACalculatorConstants.EIDP_FEE_EXEMPTION_PATH);
		if (!CollectionUtils.isEmpty(isEidpFeeExemptedForTheProjectJSON)) {
			boolean isEidpFeeExemptedForTheProject = ((String) isEidpFeeExemptedForTheProjectJSON.get(0))
					.equalsIgnoreCase("YES") ? true : false;
			paramMap.put(BPACalculatorConstants.IS_EIDP_EXEMPTED_FOR_THE_PROJECT, isEidpFeeExemptedForTheProject);
		}
		
		// Get if the CWWC Fee is already paid or not
		JSONArray isTheCwwcFeeAlreadyPaidJSON = context.read(BPACalculatorConstants.CWWC_FEE_ALREADY_PAID_PATH);
		if (!CollectionUtils.isEmpty(isTheCwwcFeeAlreadyPaidJSON)) {
			boolean isTheCwwcFeeAlreadyPaid = ((String) isTheCwwcFeeAlreadyPaidJSON.get(0)).equalsIgnoreCase("YES")
					? true
					: false;
			paramMap.put(BPACalculatorConstants.IS_THE_CWWC_FEE_ALREADY_PAID, isTheCwwcFeeAlreadyPaid);
		}
		
		// Get if Layout Approved under section 16 of ODA amendments
		JSONArray isLayoutApprovedJSON = context.read(BPACalculatorConstants.LAYOUT_APPROVED_UNDER_SEC_16_PATH);
		if (!CollectionUtils.isEmpty(isLayoutApprovedJSON)) {
			boolean isLayoutApproved = ((String) isLayoutApprovedJSON.get(0)).equalsIgnoreCase("YES") ? true : false;
			paramMap.put(BPACalculatorConstants.IS_LAYOUT_APPROVED_UNDER_SEC_16, isLayoutApproved);
		}

		// alteration builtup area related parameters-
		Double alterationTotalBuiltupArea = null;
		Double alterationExistingBuiltupArea = null;
		JSONArray alterationTotalBuiltupAreaJson = context
				.read(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(alterationTotalBuiltupAreaJson)) {
			String alterationTotalBuiltupAreaString = alterationTotalBuiltupAreaJson.get(0).toString();
			alterationTotalBuiltupArea = Double.parseDouble(alterationTotalBuiltupAreaString);
			paramMap.put(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA, alterationTotalBuiltupArea);
		}

		JSONArray alterationExistingBuiltupAreaJson = context
				.read(BPACalculatorConstants.ALTERATION_EXISTING_BUILTUP_AREA_PATH);
		if (!CollectionUtils.isEmpty(alterationExistingBuiltupAreaJson)) {
			String alterationExistingBuiltupAreaString = alterationExistingBuiltupAreaJson.get(0).toString();
			alterationExistingBuiltupArea = Double.parseDouble(alterationExistingBuiltupAreaString);
			paramMap.put(BPACalculatorConstants.ALTERATION_EXISTING_BUILTUP_AREA, alterationExistingBuiltupArea);
		}
		// subtract above two and put as proposed builtup area-
		if (Objects.nonNull(alterationTotalBuiltupArea) && Objects.nonNull(alterationExistingBuiltupArea)) {
			Double alterationProposedBuiltupArea = alterationTotalBuiltupArea - alterationExistingBuiltupArea;
			paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA, alterationProposedBuiltupArea);
		}
		// alteration floor area related parameters-
		Double alterationTotalFloorArea = null;
		Double alterationExistingFloorArea = null;
		JSONArray alterationTotalFloorAreaJson = context.read(BPACalculatorConstants.ALTERATION_TOTAL_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(alterationTotalFloorAreaJson)) {
			String alterationTotalFloorAreaString = alterationTotalFloorAreaJson.get(0).toString();
			alterationTotalFloorArea = Double.parseDouble(alterationTotalFloorAreaString);
			paramMap.put(BPACalculatorConstants.ALTERATION_TOTAL_FLOOR_AREA, alterationTotalFloorArea);
		}

		JSONArray alterationExistingFloorAreaJson = context
				.read(BPACalculatorConstants.ALTERATION_EXISTING_FLOOR_AREA_PATH);
		if (!CollectionUtils.isEmpty(alterationExistingFloorAreaJson)) {
			String alterationExistingFloorAreaString = alterationExistingFloorAreaJson.get(0).toString();
			alterationExistingFloorArea = Double.parseDouble(alterationExistingFloorAreaString);
			paramMap.put(BPACalculatorConstants.ALTERATION_EXISTING_FLOOR_AREA, alterationExistingFloorArea);
		}
		// subtract above two and put as proposed builtup area-
		if (Objects.nonNull(alterationTotalFloorArea) && Objects.nonNull(alterationExistingFloorArea)) {
			Double alterationProposedFloorArea = alterationTotalFloorArea - alterationExistingFloorArea;
			paramMap.put(BPACalculatorConstants.ALTERATION_PROPOSED_FLOOR_AREA, alterationProposedFloorArea);
		}
		// Alteration - Occupancy(colour code) wise approved area mapping-
		Map<Integer, BigDecimal> approvedConstructionData = edcrHelperUtils.prepareApprovedConstructionData(edcr);
		paramMap.put(BPACalculatorConstants.APPROVED_CONSTRUCTION_OCCUPANCYWISE, approvedConstructionData);

		paramMap.put(BPACalculatorConstants.APPLICATION_TYPE, applicationType);
		paramMap.put(BPACalculatorConstants.SERVICE_TYPE, serviceType);
		paramMap.put(BPACalculatorConstants.RISK_TYPE, riskType);
		paramMap.put(BPACalculatorConstants.FEE_TYPE, feeType);
//		return paramMap;
	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateTotalFeeAmount(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal calculatedTotalAmout = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String feeType = null;
		//String occupancyType = null;
		List<Occupancy> occupancyType = null;
		
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.FEE_TYPE)) {
			feeType = (String) paramMap.get(BPACalculatorConstants.FEE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyType = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		
		if (StringUtils.hasText(applicationType) && (StringUtils.hasText(serviceType))
				&& (occupancyType!=null) && (StringUtils.hasText(feeType))) {
			if (feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE)) {
				calculatedTotalAmout = calculateTotalScrutinyFee(paramMap, estimates);

			} else if (feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)) {
				calculatedTotalAmout = calculateTotalPermitFee(paramMap, estimates);
			}

		}

		return calculatedTotalAmout;
	}
	
	/**
	 * Calculate CWWC fee for BPA OC
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateOccupancyConstructionWorkerWelfareCess(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal welfareCess = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double deviationBuitUpArea = null;
		Double totalBuaEdcr = null;
		BigDecimal costOfConstruction = BigDecimal.ZERO;
		BigDecimal welfareCessRate = BigDecimal.ZERO;
		
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.DEVIATION_BUILTUP_AREA)) {
			deviationBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.DEVIATION_BUILTUP_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR)) {
			totalBuaEdcr = (Double) paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR);
		}
		if(null != paramMap.get(BPACalculatorConstants.COST_OF_CONSTRUCTION)) {
			costOfConstruction = new BigDecimal((String)paramMap.get(BPACalculatorConstants.COST_OF_CONSTRUCTION));
		}
		if(null != paramMap.get(BPACalculatorConstants.CWCFEE)) {
			welfareCessRate = new BigDecimal((String)paramMap.get(BPACalculatorConstants.CWCFEE));
		}
		
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& (StringUtils.hasText(serviceType))
				&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))
				&& deviationBuitUpArea.compareTo(0D) > 0) {
			
			BigDecimal totalCostOfConstruction = (costOfConstruction.multiply(BigDecimal.valueOf(totalBuaEdcr))
					.multiply(SQMT_SQFT_MULTIPLIER)).setScale(2, RoundingMode.UP);
			
			if (totalCostOfConstruction.compareTo(TEN_LAC) > 0) {
				welfareCess = (welfareCessRate.multiply(BigDecimal.valueOf(deviationBuitUpArea))
						.multiply(SQMT_SQFT_MULTIPLIER)).setScale(2, RoundingMode.UP);
			}

		}
		if(BigDecimal.ZERO.compareTo(welfareCess) < 0)
			generateTaxHeadEstimate(estimates, welfareCess, BPACalculatorConstants.TAXHEAD_BPA_OC_SANC_CWWC_FEE, Category.FEE);
		return welfareCess;
	}

	/**
	 * Calculate OC EIDP Fee
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateOccupancyEidpFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal eidpFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		BigDecimal projectCost = null;
		Double edcrTotalBUA = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP)) {			
			projectCost = paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP) != null
					? new BigDecimal(paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP) + "")
					: BigDecimal.ZERO;
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA_EDCR)) {
			edcrTotalBUA = (Double) paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR);
		}
		if (null != paramMap.get(BPACalculatorConstants.DEVIATION_BUILTUP_AREA)) {
			deviationBUA = (Double) paramMap.get(BPACalculatorConstants.DEVIATION_BUILTUP_AREA);
		}
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& ((StringUtils.hasText(serviceType))
				&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION) ))
				&& projectCost != null && deviationBUA.compareTo(0D) > 0) {
		
			BigDecimal deviationPercentage = BigDecimal.valueOf(deviationBUA)
					.divide(BigDecimal.valueOf(edcrTotalBUA), 4);
			
			eidpFee = projectCost.multiply(deviationPercentage).divide(HUNDRED,2, RoundingMode.UP);
		}
		if(BigDecimal.ZERO.compareTo(eidpFee) < 0)
			generateTaxHeadEstimate(estimates, eidpFee, BPACalculatorConstants.TAXHEAD_BPA_OC_SANC_EIDP_FEE, Category.FEE);
		return eidpFee;
	}

	/**
	 * Calculate FAR Fee for BPA OC
	 * @param paramMap
	 * @param estimates
	 * @param mdmsData
	 * @return
	 */
	private BigDecimal calculateOccupancyCompoundingFeeForFAR(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates, Object mdmsData) {
		BigDecimal compoundFARFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		BigDecimal baseFAR = null;
		BigDecimal providedFAR = null;
		BigDecimal permissableFAR = null;
		BigDecimal plotArea = null;
		BigDecimal deviation = null;
		String subOccupancyType = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.BASE_FAR)) {
			baseFAR = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.BASE_FAR));
		}
		if (null != paramMap.get(BPACalculatorConstants.PROVIDED_FAR)) {
			providedFAR = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.PROVIDED_FAR));
		}
		if (null != paramMap.get(BPACalculatorConstants.PERMISSABLE_FAR)) {
			permissableFAR = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.PERMISSABLE_FAR));
		}
		if (null != paramMap.get(BPACalculatorConstants.PLOT_AREA)) {
			plotArea = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.PLOT_AREA));
		}
		if (null != paramMap.get(BPACalculatorConstants.DEVIATION_BUILTUP_AREA)) {
			deviation = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.DEVIATION_BUILTUP_AREA));
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& (StringUtils.hasText(serviceType)
				&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION)))
				&& deviation.compareTo(BigDecimal.ZERO) > 0) {
			
			BigDecimal baseFarBUA = plotArea.multiply(baseFAR);
			BigDecimal permissableFarBUA = plotArea.multiply(permissableFAR);
			BigDecimal builtUpAreaBP = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR));
			BigDecimal builtUpAreaOC = BigDecimal.valueOf((Double) paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA));
			
			if(baseFarBUA.compareTo(builtUpAreaOC) >= 0) {
				compoundFARFee = calculateOcCompoundingFar(paramMap, mdmsData, deviation);
			} else if(baseFarBUA.compareTo(builtUpAreaOC) < 0 && permissableFarBUA.compareTo(builtUpAreaOC) >= 0) {
				BigDecimal fee1 = calculateOcCompoundingFar(paramMap, mdmsData,
						(baseFarBUA.subtract(builtUpAreaBP)).compareTo(BigDecimal.ZERO) > 0
								? baseFarBUA.subtract(builtUpAreaBP)
								: BigDecimal.ZERO);
				BigDecimal fee2 = BigDecimal.ZERO;
				// calculation of fee2(Purchasable far) for MIG sub-occupancy-
				if (StringUtils.hasText(subOccupancyType)
						&& BPACalculatorConstants.A_MIH.equalsIgnoreCase(subOccupancyType)) {
					BigDecimal applicableDiscountFarArea = (permissableFarBUA.subtract(baseFarBUA)
							.multiply(new BigDecimal("0.25"))).setScale(2, BigDecimal.ROUND_UP);
					BigDecimal deltaFarBUA = builtUpAreaOC.subtract(baseFarBUA);
					if (deltaFarBUA.compareTo(applicableDiscountFarArea) > 0) {
						fee2 = calculateOcPurchableFAR(paramMap,
								builtUpAreaOC.subtract(baseFarBUA).subtract(applicableDiscountFarArea));
					}
				} else {
					fee2 = calculateOcPurchableFAR(paramMap, builtUpAreaOC.subtract(baseFarBUA));
				}
				compoundFARFee = fee1.add(fee2);
			} else if(builtUpAreaOC.compareTo(permissableFarBUA) > 0) {
				compoundFARFee = calculateOcPurchableFAR(paramMap, deviation);
			}
			
		}
		generateTaxHeadEstimate(estimates, compoundFARFee, BPACalculatorConstants.TAXHEAD_BPA_OC_SANC_COMPOUND_FAR_FEE, Category.FEE);
		return compoundFARFee;
	}

	/**
	 * Calculate Compounding FAR fee for BPA OC
	 * @param paramMap
	 * @param mdmsData
	 * @param applicableBUA
	 * @return
	 */
	private BigDecimal calculateOcCompoundingFar(Map<String, Object> paramMap, Object mdmsData, BigDecimal applicableBUA) {
		BigDecimal compoundingFarFee = BigDecimal.ZERO;
		Map mdmsCompoundingFee = mdmsService.getOcCompoundingFee(mdmsData, BPACalculatorConstants.MDMS_FAR);
		
		if(applicableBUA != null && mdmsCompoundingFee != null) {
			Double rate = getRateForFAR(mdmsCompoundingFee, paramMap);
			compoundingFarFee = applicableBUA.multiply(BigDecimal.valueOf(rate));
		}
		return compoundingFarFee;
	}

	private Double getRateForFAR(Map mdmsCompoundingFee, Map<String, Object> paramMap) {
		Double rate = 0D;
		boolean isProjectUndertakingByGovt = false;
		String occupancyType = null;
		String subOccupancyType = null;
		String applicableRateType = null;
		if (null != paramMap.get(BPACalculatorConstants.PROJECT_UNDERTAKING_BY_GOVT)) {
			isProjectUndertakingByGovt = (boolean) paramMap.get(BPACalculatorConstants.PROJECT_UNDERTAKING_BY_GOVT);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		
		if(isProjectUndertakingByGovt) {
			applicableRateType = BPACalculatorConstants.OC_COMPOUNDING_GOVT;
		} else if(occupancyType != null && subOccupancyType != null) {
			if(BPACalculatorConstants.A.equals(occupancyType)
					&& (BPACalculatorConstants.A_P.equals(subOccupancyType)
					|| BPACalculatorConstants.A_S.equals(subOccupancyType)
					|| BPACalculatorConstants.A_R.equals(subOccupancyType))) {
				applicableRateType = BPACalculatorConstants.OC_COMPOUNDING_INDIVIDUAL_RESIDENTIAL;
			} else {
				applicableRateType = BPACalculatorConstants.OC_COMPOUNDING_OTHER;
			}
		}
		
		
		String jsonData = new JSONObject(mdmsCompoundingFee).toString();
		DocumentContext compoundingContext = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonData);
		JSONArray criteriaArray = compoundingContext.read("criteria");
		LinkedHashMap item = (LinkedHashMap) criteriaArray.get(0);
		JSONArray feeArray = (JSONArray) item.get("fee");
		for (int i = 0; i < feeArray.size(); i++) {
			LinkedHashMap diff = (LinkedHashMap) feeArray.get(i);
			String applicable = diff.get("applicable").toString();
			if(applicable.equalsIgnoreCase(applicableRateType)) {
				rate = Double.parseDouble(diff.get("rate").toString());
				break;
			}
		}
		return rate;
	}

	/**
	 * CAlculate OC purchable FAR fee
	 * @param paramMap
	 * @param applicableBUA
	 * @return
	 */
	private BigDecimal calculateOcPurchableFAR(Map<String, Object> paramMap, BigDecimal applicableBUA) {
		BigDecimal purchasableFARFee = BigDecimal.ZERO;
		Double benchmarkValuePerAcre = null;
		Double baseFar = null;
		Double permissableFar = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.BMV_ACRE)) {
			benchmarkValuePerAcre = (Double) paramMap.get(BPACalculatorConstants.BMV_ACRE);
		}

		if(benchmarkValuePerAcre != null) {
			BigDecimal benchmarkValuePerSQM = BigDecimal.valueOf(benchmarkValuePerAcre).divide(ACRE_SQMT_MULTIPLIER,
					2, RoundingMode.UP);
	
			BigDecimal purchasableFARRate = (benchmarkValuePerSQM.multiply(ZERO_TWO_FIVE)).setScale(2,
					RoundingMode.UP);
	
			purchasableFARFee = (purchasableFARRate.multiply(applicableBUA))
					.setScale(2, RoundingMode.UP);
		}
		log.info("OC PurchasableFARFee:::::::::::::::::" + purchasableFARFee);
		return purchasableFARFee;
		
	}

	/**
	 * Calculate BPA OC Fee for Setback
	 * @param paramMap
	 * @param estimates
	 * @param mdmsData
	 * @return
	 */
	private BigDecimal calculateOccupancyCompoundingFeeForSetback(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates, Object mdmsData) {
		BigDecimal compoundSetbackFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		JSONArray blockDetailOc = null;
		JSONArray blockDetailEdcr = null;
		
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.BLOCK_DETAILS_OC)) {
			blockDetailOc = (JSONArray) paramMap.get(BPACalculatorConstants.BLOCK_DETAILS_OC);
		}
		if (null != paramMap.get(BPACalculatorConstants.BLOCK_DETAILS_EDCR)) {
			blockDetailEdcr = (JSONArray) paramMap.get(BPACalculatorConstants.BLOCK_DETAILS_EDCR);
		}
		
		Map mdmsCompoundingFee = mdmsService.getOcCompoundingFee(mdmsData, BPACalculatorConstants.MDMS_SETBACK);
		
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& (StringUtils.hasText(serviceType)
				&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION)))
				&& blockDetailOc != null && blockDetailEdcr != null) {
			
			compoundSetbackFee = calculateOccupancyCompoundingSetbackFee(mdmsCompoundingFee, blockDetailOc, blockDetailEdcr, paramMap);
			
		}
		generateTaxHeadEstimate(estimates, compoundSetbackFee, BPACalculatorConstants.TAXHEAD_BPA_OC_SANC_COMPOUND_SETBACK_FEE, Category.FEE);
		return compoundSetbackFee;
	}

	/**
	 * Calculate compounding fee for all setback in BPA OC
	 * @param mdmsCompoundingFee
	 * @param blockDetailOc
	 * @param blockDetailEdcr
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyCompoundingSetbackFee(Map mdmsCompoundingFee, JSONArray blockDetailOc,
			JSONArray blockDetailEdcr, Map<String, Object> paramMap) {
		BigDecimal totalSetbackFee = BigDecimal.ZERO;
		BigDecimal frontYardFee = BigDecimal.ZERO;
		BigDecimal rearYardFee = BigDecimal.ZERO;
		BigDecimal sideYard1Fee = BigDecimal.ZERO;
		BigDecimal sideYard2Fee = BigDecimal.ZERO;
		
		for(int i=0; i<blockDetailOc.size(); i++) {
			LinkedHashMap blockOc = (LinkedHashMap) blockDetailOc.get(i);
			LinkedHashMap levelZeroSetBackOc = (LinkedHashMap) blockOc.get("levelZeroSetBack");
			
			LinkedHashMap blockEdcr = (LinkedHashMap) blockDetailEdcr.get(i);
			LinkedHashMap levelZeroSetBackEdcr = (LinkedHashMap) blockEdcr.get("levelZeroSetBack");
			
			frontYardFee = calculateSetbackFee(BPACalculatorConstants.JSON_FRONT_YARD, levelZeroSetBackOc,
					levelZeroSetBackEdcr, mdmsCompoundingFee, paramMap);
			
			rearYardFee = calculateSetbackFee(BPACalculatorConstants.JSON_REAR_YARD, levelZeroSetBackOc,
					levelZeroSetBackEdcr, mdmsCompoundingFee, paramMap);
			
			sideYard1Fee = calculateSetbackFee(BPACalculatorConstants.JSON_SIDE_YARD1, levelZeroSetBackOc,
					levelZeroSetBackEdcr, mdmsCompoundingFee, paramMap);
			
			sideYard2Fee = calculateSetbackFee(BPACalculatorConstants.JSON_SIDE_YARD2, levelZeroSetBackOc,
					levelZeroSetBackEdcr, mdmsCompoundingFee, paramMap);
			
			totalSetbackFee = totalSetbackFee.add(frontYardFee).add(rearYardFee).add(sideYard1Fee).add(sideYard2Fee);
		}
		
		return totalSetbackFee;
	}

	/**
	 * Calculate setback fee in BPA OC
	 * @param setbackSide
	 * @param levelZeroSetBackOc
	 * @param levelZeroSetBackEdcr
	 * @param mdmsCompoundingFee
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSetbackFee(String setbackSide, LinkedHashMap levelZeroSetBackOc,
			LinkedHashMap levelZeroSetBackEdcr, Map mdmsCompoundingFee, Map<String, Object> paramMap) {
		BigDecimal setBackFee = BigDecimal.ZERO;
		BigDecimal meanDeviationPercentage = BigDecimal.ZERO;
		BigDecimal meanOc = BigDecimal.ZERO;
		BigDecimal meanEdcr = BigDecimal.ZERO;
		BigDecimal areaOc = BigDecimal.ZERO;
		BigDecimal areaEdcr = BigDecimal.ZERO;
		
		if(levelZeroSetBackOc != null && levelZeroSetBackEdcr != null) {
			LinkedHashMap setbackItemOc = (LinkedHashMap) levelZeroSetBackOc.get(setbackSide);
			LinkedHashMap setbackItemEdcr = (LinkedHashMap) levelZeroSetBackEdcr.get(setbackSide);
			if(setbackItemOc != null && setbackItemEdcr != null) {
				meanOc = setbackItemOc.get("mean") != null ?new BigDecimal(setbackItemOc.get("mean")+""):BigDecimal.ZERO;
				try {
				    Object meanValue = setbackItemEdcr.get("mean");
				    meanEdcr = (meanValue != null 
				                && meanValue.toString().trim().length() > 0
				                && new BigDecimal(meanValue.toString().trim()).compareTo(BigDecimal.ZERO) != 0) 
				               ? new BigDecimal(meanValue.toString().trim()) 
				               : BigDecimal.ONE;
				} catch (NumberFormatException e) {
				    log.warn("Invalid number format for mean value: {}", setbackItemEdcr.get("mean"), e);
				    meanEdcr = BigDecimal.ONE; // Default to BigDecimal.ONE on error
				}


				meanDeviationPercentage = meanEdcr.subtract(meanOc).multiply(HUNDRED).divide(meanEdcr, 2, RoundingMode.UP);
				Double rate = getRateForSetback(paramMap, mdmsCompoundingFee, meanDeviationPercentage);
				
				areaOc = BigDecimal.valueOf((Double) setbackItemOc.get("area"));
				areaEdcr = BigDecimal.valueOf((Double) setbackItemEdcr.get("area"));
				if(areaEdcr.compareTo(areaOc) > 0) {
					setBackFee = areaEdcr.subtract(areaOc).multiply(BigDecimal.valueOf(rate)).setScale(2, RoundingMode.UP);
				}
			}
		}
		
		return setBackFee;
	}

	/**
	 * Get setback Rate 
	 * @param paramMap
	 * @param mdmsCompoundingFee
	 * @param deviationPercentage
	 * @return
	 */
	private Double getRateForSetback(Map<String, Object> paramMap, Map mdmsCompoundingFee, BigDecimal deviationPercentage) {
		Double rate = 0D;
		boolean isProjectUndertakingByGovt = false;
		String occupancyType = null;
		String subOccupancyType = null;
		String applicableRateType = null;
		if (null != paramMap.get(BPACalculatorConstants.PROJECT_UNDERTAKING_BY_GOVT)) {
			isProjectUndertakingByGovt = (boolean) paramMap.get(BPACalculatorConstants.PROJECT_UNDERTAKING_BY_GOVT);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		
		if(isProjectUndertakingByGovt) {
			applicableRateType = BPACalculatorConstants.OC_COMPOUNDING_GOVT;
		} else if(occupancyType != null && subOccupancyType != null) {
			if(BPACalculatorConstants.A.equals(occupancyType)
					&& (BPACalculatorConstants.A_P.equals(subOccupancyType)
					|| BPACalculatorConstants.A_S.equals(subOccupancyType)
					|| BPACalculatorConstants.A_R.equals(subOccupancyType))) {
				applicableRateType = BPACalculatorConstants.OC_COMPOUNDING_INDIVIDUAL_RESIDENTIAL;
			} else {
				applicableRateType = BPACalculatorConstants.OC_COMPOUNDING_OTHER;
			}
		}
		String jsonData = new JSONObject(mdmsCompoundingFee).toString();
		DocumentContext compoundingContext = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonData);
		JSONArray criteriaArray = compoundingContext.read("criteria");
		for (int j = 0; j < criteriaArray.size(); j++) {
			LinkedHashMap item = (LinkedHashMap) criteriaArray.get(j);
			LinkedHashMap deviationData = (LinkedHashMap) item.get("deviation");
			Double from = Double.parseDouble(deviationData.get("from").toString());
			Double to = Double.parseDouble(deviationData.get("to").toString());
			if(BigDecimal.valueOf(from).compareTo(deviationPercentage) < 0 
					&& BigDecimal.valueOf(to).compareTo(deviationPercentage) >= 0) {
				JSONArray feeArray = (JSONArray) item.get("fee");
				for (int i = 0; i < feeArray.size(); i++) {
					LinkedHashMap diff = (LinkedHashMap) feeArray.get(i);
					String applicableType = diff.get("applicable").toString();
					if(applicableType.equalsIgnoreCase(applicableRateType)) {
						rate = Double.parseDouble(diff.get("rate").toString());
						break;
					}
				}
			}
		}
		return rate;
	}

	/**
	 * Calculate BPA OC Scrutiny fee
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		Double deviationBUA = null;
		String occupancyType = null;
		deviationBUA=getDeviationBUAForOCFeesCalculation(paramMap);
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (deviationBUA != null && deviationBUA.compareTo(0D) > 0) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForResidentialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForCommercialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForPublicSemiPublicInstitutionalOccupancy(
						paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForPublicUtilityOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForIndustrialZoneOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForEducationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForTransportationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
				ocScrutinyFee = calculateOccupancyScrutinyFeeForAgricultureOccupancy(paramMap);
			}

		}
		generateTaxHeadEstimate(estimates, ocScrutinyFee, BPACalculatorConstants.TAXHEAD_BPA_OC_SCRUTINY_FEE, Category.FEE);
		
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for Agriculture
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForAgricultureOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION)))) {
				ocScrutinyFee = calculateVariableFee1(deviationBUA);
			}
		}
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for Transportation
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForTransportationOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION)))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				ocScrutinyFee = calculateConstantFeeNew(deviationBUA, 5);
			}
		}
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for Education
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION)))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				ocScrutinyFee = calculateConstantFeeNew(deviationBUA, 5);
			}
		}
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for Industrial Zone
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForIndustrialZoneOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION)))) {
				ocScrutinyFee = calculateVariableFee3(deviationBUA);
			}
		}
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for public utility
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForPublicUtilityOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION)))) {
				
				ocScrutinyFee = calculateConstantFeeNew(deviationBUA, 5);
			}
		}
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for public semi
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForPublicSemiPublicInstitutionalOccupancy(
			Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if (((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) && (StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION)))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_A))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_B))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CH))) {
					ocScrutinyFee = calculateVariableFee2(deviationBUA);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_O))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_OAH))) {
					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					ocScrutinyFee = calculateConstantFeeNew(deviationBUA, 5);

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
					ocScrutinyFee = calculateVariableFee2(deviationBUA);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PW))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_REB))) {
					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					ocScrutinyFee = calculateConstantFeeNew(deviationBUA, 5);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SPC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_T))) {
					ocScrutinyFee = calculateVariableFee2(deviationBUA);

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

					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					ocScrutinyFee = calculateConstantFeeNew(deviationBUA, 5);

				}
			}

		}
		return ocScrutinyFee;
	}

	/**
	 * OC Scrutiny Fee for Commercial
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION)))) {
				ocScrutinyFee = calculateVariableFee2(deviationBUA);
			}

		}
		return ocScrutinyFee;
	}

	/**
	 * Scrutiny Fee for Residential
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateOccupancyScrutinyFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal ocScrutinyFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double deviationBUA = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		// using deviation in builtup area for calculation of application fees for OC- 
		deviationBUA = getDeviationBUAForOCFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION) || serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION)))) {
				ocScrutinyFee = calculateVariableFee1(deviationBUA);
			}

		}
		return ocScrutinyFee;
	}

	/**
	 * Calculate BPA OC certificate Fee
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateOccupancyCertificateFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal flatFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& (StringUtils.hasText(serviceType)
						&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION))) {
			flatFee = THOUSAND;
		}
		
		// Fee Calc logic for Alteration Applications
		if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_OC))
				&& (StringUtils.hasText(serviceType)
						&& serviceType.equalsIgnoreCase(BPACalculatorConstants.ALTERATION))) {
			flatFee = THOUSAND;
		}
		
		generateTaxHeadEstimate(estimates, flatFee, BPACalculatorConstants.TAXHEAD_BPA_OC_CERT_FEE, Category.FEE);
		return flatFee;
	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateTotalPermitFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		
		// check if alteration application-

		BPA bpa = (BPA) paramMap.get("BPA");
		String subservice = null;
		Boolean isRevisionApplication = Boolean.FALSE;
		Boolean isRefApplicationPresentInSujog = Boolean.FALSE;
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subservice = (String) paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY);
		}
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REVISION_APPLICATION))) {
			isRevisionApplication = (Boolean) paramMap.get(BPACalculatorConstants.IS_REVISION_APPLICATION);
		}
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG))) {
			isRefApplicationPresentInSujog = (Boolean) paramMap
					.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG);
		}
		//pure old alteration for existing applications--
		if (!StringUtils.isEmpty(paramMap.get(BPACalculatorConstants.SERVICE_TYPE))
				&& paramMap.get(BPACalculatorConstants.SERVICE_TYPE).equals(BPACalculatorConstants.ALTERATION)
				&& Objects.isNull(subservice)) {
			return alterationCalculationService.calculateTotalSanctionFeeForPermit(paramMap, estimates);
		}else if(Objects.nonNull(subservice)) {
			switch (subservice) {
			case BPACalculatorConstants.ALTERATION_SUBSERVICE_A:
				//"isRevisionApplication": true,"isApplicationPersentInSujogSystem":true,"isPermitLetterExpried":false,"alterationSubService":"ALTERATION_SERVICE_A","applicationSubType":"NEW_CONSTRUCTION"
				// new construction. continue as new construction only as revision calculation is written under new construction and not alteration flow-
				//paramMap.put(BPACalculatorConstants.SERVICE_TYPE, BPACalculatorConstants.ALTERATION);
				//return alterationCalculationService.calculateTotalSanctionFeeForPermit(paramMap, estimates);
				//for new construction below logic will execute
				break;
			case BPACalculatorConstants.ALTERATION_SUBSERVICE_B:
				//"isRevisionApplication": true,"isApplicationPersentInSujogSystem":true,"isPermitLetterExpried":false,"alterationSubService":"ALTERATION_SERVICE_B","applicationSubType":"ALTERATION"
				return alterationCalculationService.calculateTotalSanctionFeeForPermit(paramMap, estimates);
			case BPACalculatorConstants.ALTERATION_SUBSERVICE_C:
				//"isRevisionApplication": false,"isApplicationPersentInSujogSystem":true,"isPermitLetterExpried":true,"alterationSubService":"ALTERATION_SERVICE_C","applicationSubType":"ALTERATION"
				// this is pure old flow of alteration,but with flag in new applications-
				return alterationCalculationService.calculateTotalSanctionFeeForPermit(paramMap, estimates);
			case BPACalculatorConstants.ALTERATION_SUBSERVICE_D:
				//"isRevisionApplication": false,"isApplicationPersentInSujogSystem":true,"isPermitLetterExpried":false,"alterationSubService":"ALTERATION_SERVICE_D","applicationSubType":"ALTERATION"
				// this involves approved constructions - 
				return alterationCalculationService.calculateTotalSanctionFeeForPermit(paramMap, estimates);
			case "":
				break;
			}
		}
		
			
		
		// calculate application fees again if reworkhistory is there and compare with
		// payment done and add calculations for adjustments in separate taxheads--
		processApplicationFeesAfterRework(paramMap, estimates);
		
		BigDecimal calculatedTotalPermitFee = BigDecimal.ZERO;
		BigDecimal sanctionFee = calculateSanctionFee(paramMap, estimates);
		BigDecimal constructionWorkerWelfareCess = calculateConstructionWorkerWelfareCess(paramMap, estimates);
		BigDecimal shelterFee = calculateShelterFee(paramMap, estimates);
		BigDecimal temporaryRetentionFee = calculateTemporaryRetentionFee(paramMap, estimates);
		BigDecimal securityDeposit = calculateSecurityDeposit(paramMap, estimates);
		BigDecimal purchasableFAR = calculatePurchasableFAR(paramMap, estimates);
		BigDecimal eidpFee = calculateEIDPFee(paramMap, estimates);
		BigDecimal adjustmentAmount = calculateAdjustmentAmount(paramMap, estimates);
		BigDecimal totalOtherFeesAmount = calculateAllOtherFees(paramMap, estimates);

		calculatedTotalPermitFee = (calculatedTotalPermitFee.add(sanctionFee).add(constructionWorkerWelfareCess)
				.add(shelterFee).add(temporaryRetentionFee).add(securityDeposit).add(purchasableFAR).add(eidpFee)
				.add(adjustmentAmount).add(totalOtherFeesAmount))
				.setScale(2, BigDecimal.ROUND_UP);
		// if revision application then calculate total amount after adjustments-
		if (isRevisionApplication) {
			if(!isRefApplicationPresentInSujog) {
				Revision revision = (Revision) paramMap.get(BPACalculatorConstants.REVISION);
				calculateForRevisionNonSujogAppl(paramMap, estimates, revision);
				calculatedTotalPermitFee = calculateTotalAmountForRevision(estimates);
				return calculatedTotalPermitFee;
			}
			else {
				calculateForRevisionSujogExistingAppl(paramMap, estimates);
				calculatedTotalPermitFee = calculateTotalAmountForRevision(estimates);
			}
			
		}
			
		return calculatedTotalPermitFee;
	}
	
	private void calculateForRevisionNonSujogAppl(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates,
			Revision revision) {
		BPA bpa = (BPA) paramMap.get("BPA");
		// set IsRevisionApplication to false for normal calculation-
		//bpa.setIsRevisionApplication(false);
		Map<String, Object>	additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
		Object alterationServiceNode = additionalDetails.get(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY);
		additionalDetails.remove(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY);
		CalulationCriteria calculationCriteria = CalulationCriteria.builder()
				.applicationNo(revision.getBpaApplicationNo())
				.applicationType(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY).bpa(bpa)
				.feeType(BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE)
				.serviceType(BPACalculatorConstants.NEW_CONSTRUCTION).tenantId(bpa.getTenantId()).build();
		List<CalulationCriteria> calculationCriterias = new ArrayList<>();
		calculationCriterias.add(calculationCriteria);
		CalculationReq calculationReq = CalculationReq.builder().calulationCriteria(calculationCriterias)
				.requestInfo((RequestInfo) paramMap.get("requestInfo")).build();
		// note that we calculate the estimates as per old parameters so that we could
		// know how much the user would have paid for old permit.We have to subtract
		// that much amount from current application's calculation-
		List<Calculation> calculationsForCurrentApplicationWithoutRevision = getEstimate(calculationReq);
		List<TaxHeadEstimate> estimatesForCurrentApplicationWithoutRevision = calculationsForCurrentApplicationWithoutRevision
				.get(0).getTaxHeadEstimates();
		for (TaxHeadEstimate estimateAsPerCurrentApplicationWORevision : estimatesForCurrentApplicationWithoutRevision) {
			//note: do not touch other fee(adjustment amount) as it might be required in new estimate
			Optional<TaxHeadEstimate> taxHeadEstimateSearchAsPerOldParameters = estimates.stream()
					.filter(estimate -> StringUtils.hasText(estimate.getTaxHeadCode())
							&& !estimate.getTaxHeadCode().contains(BPACalculatorConstants.BPA_SANC_ADJUSTMENT_AMOUNT)
							&& !BPACalculatorConstants.TAXHEAD_BPA_TEMP_RETENTION_FEE.equals(estimate.getTaxHeadCode())
							&& estimate.getTaxHeadCode()
									.equals(estimateAsPerCurrentApplicationWORevision.getTaxHeadCode()))
					.findFirst();
			if (taxHeadEstimateSearchAsPerOldParameters.isPresent()
					&& estimateAsPerCurrentApplicationWORevision.getEstimateAmount()
							.compareTo(taxHeadEstimateSearchAsPerOldParameters.get().getEstimateAmount()) > 0) {
				if (estimateAsPerCurrentApplicationWORevision.getTaxHeadCode()
						.equalsIgnoreCase(BPACalculatorConstants.TAXHEAD_BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT)
						|| estimateAsPerCurrentApplicationWORevision.getTaxHeadCode()
								.equalsIgnoreCase(BPACalculatorConstants.TAXHEAD_BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT)) {
					estimateAsPerCurrentApplicationWORevision
							.setEstimateAmount(taxHeadEstimateSearchAsPerOldParameters.get().getEstimateAmount());
				} else {
					estimateAsPerCurrentApplicationWORevision
							.setEstimateAmount(estimateAsPerCurrentApplicationWORevision.getEstimateAmount()
									.subtract(taxHeadEstimateSearchAsPerOldParameters.get().getEstimateAmount()));
				}
			}
			else if (taxHeadEstimateSearchAsPerOldParameters.isPresent()) {
				estimateAsPerCurrentApplicationWORevision.setEstimateAmount(BigDecimal.ZERO);
			}
		}
		estimates.clear();
		estimates.addAll(estimatesForCurrentApplicationWithoutRevision);
		additionalDetails.put(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY, alterationServiceNode);
		//bpa.setIsRevisionApplication(true);

	}
	
	private void calculateForRevisionSujogExistingAppl(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		// part1 :if isSujogExistingApplication=true, then fetch old application demand details-
			BPA bpa = (BPA) paramMap.get(BPACalculatorConstants.PARAM_MAP_BPA);
			RequestInfo requestInfo = (RequestInfo) paramMap.get("requestInfo");
			String subservice = (String) paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY);
			String tenantId = String.valueOf(paramMap.get("tenantId"));
			Set<String> consumerCode = new HashSet<>();
			BPA refBpa = null;
			if(Objects.nonNull(paramMap.get(BPACalculatorConstants.REF_BPA_APPLICATION))) {
				refBpa = (BPA) paramMap.get(BPACalculatorConstants.REF_BPA_APPLICATION);
			}
			consumerCode.add(refBpa.getApplicationNo());
			InstallmentSearchCriteria installmentSearchCriteria = InstallmentSearchCriteria.builder()
					.tenantId(tenantId).consumerCode(refBpa.getApplicationNo()).installmentNos(Arrays.asList(-1)).build();
			List<Installment> installments= installmentRepository.getInstallments(installmentSearchCriteria);
			try {
				log.info("Installments Object: "+mapper.writeValueAsString(installments));
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if(CollectionUtils.isEmpty(installments))
				return;
			for (TaxHeadEstimate estimate : estimates) {
				Optional<Installment> installmentOptional = installments.stream()
						.filter(installment -> installment.getTaxHeadCode().equals(estimate.getTaxHeadCode())
								&&  (!BPACalculatorConstants.TAXHEAD_BPA_TEMP_RETENTION_FEE
										.equals(estimate.getTaxHeadCode()) 
										|| !subservice.equals(BPACalculatorConstants.ALTERATION_SUBSERVICE_C)))
						.filter(installment -> StringUtils.hasText(estimate.getTaxHeadCode())
								&& installment.getTaxHeadCode().equals(estimate.getTaxHeadCode())
								&& !estimate.getTaxHeadCode().contains(BPACalculatorConstants.BPA_SANC_ADJUSTMENT_AMOUNT))
						.findFirst();
				if (installmentOptional.isPresent()) {
					Installment installment = installmentOptional.get();
					switch (estimate.getTaxHeadCode()) {
					default:
						if (estimate.getEstimateAmount().compareTo(installment.getTaxAmount()) > 0)
							estimate.setEstimateAmount(estimate.getEstimateAmount().subtract(installment.getTaxAmount()));
						else
							estimate.setEstimateAmount(BigDecimal.ZERO);
						break;
					}
					
				}
				log.info("**CalculationService method calculateForRevisionSujogExistingAppl**taxhead:"
						+ estimate.getTaxHeadCode() + ", taxAmount:" + estimate.getEstimateAmount());
			}

	}

	private BigDecimal calculateTotalAmountForRevision(ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal calculatedTotalPermitFee = BigDecimal.ZERO;
		for (TaxHeadEstimate estimate : estimates) {
			calculatedTotalPermitFee.add(estimate.getEstimateAmount());
		}
		return calculatedTotalPermitFee;
	}
	
	public void processApplicationFeesAfterRework(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		// calculate application fees again if reworkhistory is there--
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.PARAM_MAP_BPA))
				&& Objects.nonNull(((BPA) paramMap.get(BPACalculatorConstants.PARAM_MAP_BPA)).getReWorkHistory())) {
			ArrayList<TaxHeadEstimate> scrutinyFeeEstimates = new ArrayList<>();
			calculateTotalScrutinyFee(paramMap, scrutinyFeeEstimates);
			BigDecimal buildingOperationFeeReCalculated = BigDecimal.ZERO;
			BigDecimal landDevelopmentFeeReCalculated = BigDecimal.ZERO;
			BigDecimal buildingOperationFeePaid = BigDecimal.ZERO;
			BigDecimal landDevelopmentFeePaid = BigDecimal.ZERO;

			for (TaxHeadEstimate estimate : scrutinyFeeEstimates) {
			    // Null check for TaxHeadCode and EstimateAmount
			    if (estimate != null && !ObjectUtils.isEmpty(estimate.getTaxHeadCode())) {
			        if (estimate.getTaxHeadCode().equals(BPACalculatorConstants.TAXHEAD_BPA_BUILDING_OPERATION_FEE)) {
			            buildingOperationFeeReCalculated = estimate.getEstimateAmount();
			        } else if (estimate.getTaxHeadCode().equals(BPACalculatorConstants.TAXHEAD_BPA_LAND_DEVELOPMENT_FEE)) {
			            landDevelopmentFeeReCalculated = estimate.getEstimateAmount();
			        }
			    }

			    // Retrieve BPA object from the paramMap
			    BPA bpa = (BPA) paramMap.get(BPACalculatorConstants.PARAM_MAP_BPA);
			    if (!ObjectUtils.isEmpty(bpa)) {
			        Map<String, Object> additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();

			        // Check if additionalDetails are not empty
			        if (!ObjectUtils.isEmpty(additionalDetails)) {
			            Boolean landDevFeeDetails = (Boolean) additionalDetails
			                    .get(BPACalculatorConstants.IS_LAND_DEVELOPMENT_FEE_EXEMPTED);

			            if (!ObjectUtils.isEmpty(landDevFeeDetails) && Boolean.TRUE.equals(landDevFeeDetails)) {
			                landDevelopmentFeeReCalculated = BigDecimal.ZERO;
			            }
			        } else {
			            log.info("Additional details are missing in BPA object.");
			        }
			    } else {
			        log.info("BPA object is null or empty in the parameter map.");
			    }
			}

			// fetch payment details-
			Object paymentResponse = fetchPaymentDetails(paramMap);
			int paymentsLength = ((List) ((Map) paymentResponse).get("Payments")).size();
			String paymentAmountbyTaxHeadPath = BPACalculatorConstants.PAYMENT_TAXHEAD_AMOUNT_PATH;
			String buildingOpernFeePaidString = getValue((Map) paymentResponse,
					String.format(paymentAmountbyTaxHeadPath, (paymentsLength - 1),
							BPACalculatorConstants.TAXHEAD_BPA_BUILDING_OPERATION_FEE));
			paymentAmountbyTaxHeadPath = BPACalculatorConstants.PAYMENT_TAXHEAD_AMOUNT_PATH;
			buildingOpernFeePaidString = buildingOpernFeePaidString.replace("[", "").replace("]", "");
			buildingOpernFeePaidString = buildingOpernFeePaidString.isEmpty() ? "0" : buildingOpernFeePaidString;
			String landDevelopmentFeePaidString = getValue((Map) paymentResponse,
					String.format(paymentAmountbyTaxHeadPath, (paymentsLength - 1),
							BPACalculatorConstants.TAXHEAD_BPA_LAND_DEVELOPMENT_FEE));
			landDevelopmentFeePaidString = landDevelopmentFeePaidString.replace("[", "").replace("]", "");
			landDevelopmentFeePaidString = landDevelopmentFeePaidString.isEmpty() ? "0" : landDevelopmentFeePaidString;
			buildingOperationFeePaid = new BigDecimal(buildingOpernFeePaidString);
			landDevelopmentFeePaid = new BigDecimal(landDevelopmentFeePaidString);
			calculateBuildingOperationFeeReWorkAdjustment(buildingOperationFeeReCalculated, buildingOperationFeePaid,
					estimates);
			calculateLandDevelopmentFeeReWorkAdjustment(landDevelopmentFeeReCalculated, landDevelopmentFeePaid,
					estimates);
		}
	}
	
	private Object fetchPaymentDetails(Map<String, Object> paramMap) {
		StringBuilder fetchPaymentUrl = new StringBuilder(config.getCollectionServiceHost())
				.append(config.getCollectionServiceSearchPermitFeeEndpoint()).append("?consumerCodes=")
				.append(((BPA) paramMap.get("BPA")).getApplicationNo()).append("&tenantId=")
				.append(paramMap.get("tenantId"));
		Map<String, Object> payload = new HashMap<>();
		payload.put("RequestInfo", paramMap.get("requestInfo"));
		Object paymentResponse = serviceRequestRepository.fetchResult(fetchPaymentUrl, payload);
		return paymentResponse;
	}
	
	private BigDecimal calculateBuildingOperationFeeReWorkAdjustment(BigDecimal buildingOperationFeeReCalculated,
			BigDecimal buildingOperationFeePaid, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal buildingOperationFeeReWorkAdjustmentAmount = buildingOperationFeeReCalculated
				.compareTo(buildingOperationFeePaid) > 0
						? buildingOperationFeeReCalculated.subtract(buildingOperationFeePaid)
						: BigDecimal.ZERO;
		generateTaxHeadEstimate(estimates, buildingOperationFeeReWorkAdjustmentAmount,
				BPACalculatorConstants.TAXHEAD_BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT, Category.FEE);
		return buildingOperationFeeReWorkAdjustmentAmount;
	}

	private BigDecimal calculateLandDevelopmentFeeReWorkAdjustment(BigDecimal landDevelopmentFeeReCalculated,
			BigDecimal landDevelopmentFeePaid, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal landDevelopmentFeeReWorkAdjustmentAmount = landDevelopmentFeeReCalculated
				.compareTo(landDevelopmentFeePaid) > 0 ? landDevelopmentFeeReCalculated.subtract(landDevelopmentFeePaid)
						: BigDecimal.ZERO;
		generateTaxHeadEstimate(estimates, landDevelopmentFeeReWorkAdjustmentAmount,
				BPACalculatorConstants.TAXHEAD_BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT, Category.FEE);
		return landDevelopmentFeeReWorkAdjustmentAmount;
	}
	
	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
	}
	
	private BigDecimal calculateAdjustmentAmount(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal adjustmentAmount = BigDecimal.ZERO;
		BPA bpa = null;
		if (null != paramMap.get("BPA")) {
			bpa = (BPA) paramMap.get("BPA");
		}
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				adjustmentAmount.add(totalAmountAfterExemption);
			}
		} else if (Objects.nonNull(bpa) && Objects.nonNull(bpa.getAdditionalDetails())
				&& !StringUtils.isEmpty(((Map) bpa.getAdditionalDetails())
						.get(BPACalculatorConstants.BPA_ADD_DETAILS_SANCTION_FEE_ADJUSTMENT_AMOUNT_KEY))) {
			adjustmentAmount = new BigDecimal(((Map) bpa.getAdditionalDetails())
					.get(BPACalculatorConstants.BPA_ADD_DETAILS_SANCTION_FEE_ADJUSTMENT_AMOUNT_KEY).toString());
		}
		generateTaxHeadEstimate(estimates, adjustmentAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT,
				Category.FEE);
		log.info("Adjustment Amount:::::::::::::::::" + adjustmentAmount);
		return adjustmentAmount;
	}
	
	
	@SuppressWarnings("rawtypes")
	private BigDecimal calculateAllOtherFees(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal adjustmentAmount = BigDecimal.ZERO;
		BigDecimal otherFeeAmount = BigDecimal.ZERO;
		BPA bpa = null;
		if (null != paramMap.get("BPA")) {
			bpa = (BPA) paramMap.get("BPA");
		}
		
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.BPA_ADD_DETAILS_OTHER_FEES_KEY);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				otherFeeAmount.add(totalAmountAfterExemption);
			}
		} else if (Objects.nonNull(bpa) && Objects.nonNull(bpa.getAdditionalDetails()) && !StringUtils.isEmpty(
				((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OTHER_FEES_KEY))) {
			List<OtherFeeDetails> allOtherFeesDetails = getAllOtherFeesDetails(((Map)bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OTHER_FEES_KEY));
			
			for(OtherFeeDetails otherFeeDetails : allOtherFeesDetails) {
				try {
					otherFeeAmount = new BigDecimal(otherFeeDetails.getAmount().toString());
				} catch(Exception e) {e.printStackTrace();}
				
				switch (otherFeeDetails.getOrder()) {
				case 1:
					
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT_1, Category.FEE);
					break;

				case 2:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT_2, Category.FEE);
					break;

				case 3:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT_3, Category.FEE);
					break;
					
				case 4:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT_4, Category.FEE);
					break;
					
				case 5:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_ADJUSTMENT_AMOUNT_5, Category.FEE);
					break;

				default:
					throw new CustomException("INVALID_OTHER_FEE_DETAILS", "Kindly check the other Fee details !!!");
				}
			}
		}
		
		log.info("Adjustment Amount :::::::::::::::::" + adjustmentAmount);
		return adjustmentAmount;
	}
	
	@SuppressWarnings("rawtypes")
	private BigDecimal calculateOCOtherFees(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates, CalulationCriteria criteria) {
		BigDecimal adjustmentAmount = BigDecimal.ZERO;
		BigDecimal otherFeeAmount = BigDecimal.ZERO;
		BPA bpa = criteria.getBpa();
		 if (Objects.nonNull(bpa) && Objects.nonNull(bpa.getAdditionalDetails()) && !StringUtils.isEmpty(
				((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OTHER_FEES_KEY))) {
			List<OtherFeeDetails> allOtherFeesDetails = getAllOtherFeesDetails(((Map)bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OTHER_FEES_KEY));
			
			for(OtherFeeDetails otherFeeDetails : allOtherFeesDetails) {
				try {
					otherFeeAmount = new BigDecimal(otherFeeDetails.getAmount().toString());
				} catch(Exception e) {e.printStackTrace();}
				
				switch (otherFeeDetails.getOrder()) {
				case 1:
					
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_OC_ADJUSTMENT_AMOUNT_1, Category.FEE);
					break;

				case 2:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_OC_ADJUSTMENT_AMOUNT_2, Category.FEE);
					break;

				case 3:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_OC_ADJUSTMENT_AMOUNT_3, Category.FEE);
					break;
					
				case 4:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_OC_ADJUSTMENT_AMOUNT_4, Category.FEE);
					break;
					
				case 5:
					adjustmentAmount = adjustmentAmount.add(otherFeeAmount);
					generateTaxHeadEstimate(estimates, otherFeeAmount, BPACalculatorConstants.TAXHEAD_BPA_OC_ADJUSTMENT_AMOUNT_5, Category.FEE);
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
	

	private BigDecimal calculateEIDPFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal eidpFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double projectValue = null;
		Boolean isUnderAffordableHousing = Boolean.FALSE;
		Boolean isEidpFeeExemptedForTheProject = Boolean.FALSE;
		
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP)) {
			projectValue = (Double) paramMap.get(BPACalculatorConstants.PROJECT_VALUE_FOR_EIDP);
		}
		if (null != paramMap.get(BPACalculatorConstants.IS_PROJECT_UNDER_AFFORDABLE_HOUSING_SCHEME)) {
			isUnderAffordableHousing = (Boolean) paramMap
					.get(BPACalculatorConstants.IS_PROJECT_UNDER_AFFORDABLE_HOUSING_SCHEME);
		}
		if (null != paramMap.get(BPACalculatorConstants.IS_EIDP_EXEMPTED_FOR_THE_PROJECT)) {
			isEidpFeeExemptedForTheProject = (Boolean) paramMap
					.get(BPACalculatorConstants.IS_EIDP_EXEMPTED_FOR_THE_PROJECT);
		}
		
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_EIDP_FEE);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				eidpFee.add(totalAmountAfterExemption);
			}
		} else if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& ((StringUtils.hasText(serviceType))
						&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
								|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))
				&& projectValue != null) {
			
			log.info("is project under affordable housing EIDP Fee: "+isUnderAffordableHousing);
			log.info("is EIDP Fee Exempted for the Plot (Govt plot): "+isEidpFeeExemptedForTheProject);
			
			// if the project under affordable housing or Govt Plot, then no EIDP fee is applicable
			if (!(isUnderAffordableHousing || isEidpFeeExemptedForTheProject) ) {
				eidpFee = BigDecimal.valueOf(projectValue).divide(HUNDRED);
			}
		}
		generateTaxHeadEstimate(estimates, eidpFee, BPACalculatorConstants.TAXHEAD_BPA_EIDP_FEE, Category.FEE);

		log.info("EIDP Fee:::::::::::::::::" + eidpFee);
		return eidpFee;
	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculatePurchasableFAR(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal purchasableFARFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double benchmarkValuePerAcre = null;
		Double baseFar = null;
		Double providedFar = null;
		Double maxPermissibleFar = null;
		Double tdrFarRelaxation = null;
		Double plotArea = null;
		String subOccupancyType = null;
		Boolean isUnderAffordableHousing = Boolean.FALSE;
		Boolean isAffordableHousingRequirePurchasableFAR = Boolean.FALSE;
		Boolean isLayoutApproved = Boolean.FALSE;
		String riskType = null;
	    List<Occupancy> occupancyies = new ArrayList<>();
	    Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		Set<String>  occCode = occupancyies.stream().filter(o->o.getSubOccupancyCode()!=null).map(Occupancy::getSubOccupancyCode).collect(Collectors.toSet());
		String code = occCode.stream().filter(occ->occ.equalsIgnoreCase(BPACalculatorConstants.A_MIH)).findAny().orElse(null);
		log.info("code:"+code);
		if (code!=null) {
			paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,code);
		}else {
			code =occCode.stream().findFirst().get();
			log.info("code inside else:"+code);
			if(code!=null)
			paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,code);
		}
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.BMV_ACRE)) {
			benchmarkValuePerAcre = (Double) paramMap.get(BPACalculatorConstants.BMV_ACRE);
		}
		if (null != paramMap.get(BPACalculatorConstants.BASE_FAR)) {
			baseFar = (Double) paramMap.get(BPACalculatorConstants.BASE_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.PROVIDED_FAR)) {
			providedFar = (Double) paramMap.get(BPACalculatorConstants.PROVIDED_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.PLOT_AREA)) {
			plotArea = (Double) paramMap.get(BPACalculatorConstants.PLOT_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.PERMISSABLE_FAR)) {
			maxPermissibleFar = (Double) paramMap.get(BPACalculatorConstants.PERMISSABLE_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.TDR_FAR_RELAXATION)) {
			tdrFarRelaxation = (Double) paramMap.get(BPACalculatorConstants.TDR_FAR_RELAXATION);
		}
		if (null != paramMap.get(BPACalculatorConstants.IS_PROJECT_UNDER_AFFORDABLE_HOUSING_SCHEME)) {
			isUnderAffordableHousing = (Boolean) paramMap
					.get(BPACalculatorConstants.IS_PROJECT_UNDER_AFFORDABLE_HOUSING_SCHEME);
		}
		if (null != paramMap.get(BPACalculatorConstants.IS_AFFORDABLE_HOUSING_REQUIRE_PURCHASABLE_FAR)) {
			isAffordableHousingRequirePurchasableFAR  = (Boolean) paramMap
					.get(BPACalculatorConstants.IS_AFFORDABLE_HOUSING_REQUIRE_PURCHASABLE_FAR);
		}
		if (null != paramMap.get(BPACalculatorConstants.IS_LAYOUT_APPROVED_UNDER_SEC_16)) {
			isLayoutApproved = (Boolean) paramMap.get(BPACalculatorConstants.IS_LAYOUT_APPROVED_UNDER_SEC_16);
		}
		if (null != paramMap.get(BPACalculatorConstants.RISK_TYPE)) {
			riskType = (String) paramMap.get(BPACalculatorConstants.RISK_TYPE);
		}
		if(riskType.equalsIgnoreCase(BPACalculatorConstants.RISK_TYPE_LOW)) {
			log.info("Risk Type Low, pur far fee 0 ...");
			purchasableFARFee = BigDecimal.ZERO;
			return purchasableFARFee;
		}
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_PURCHASABLE_FAR);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				purchasableFARFee.add(totalAmountAfterExemption);
			}
		} // calculation for MIG sub-occupancy-
		else if ((null != providedFar) && (null != baseFar) && (providedFar > baseFar) && (null != plotArea)
				&& StringUtils.hasText(subOccupancyType)
				&& BPACalculatorConstants.A_MIH.equalsIgnoreCase(subOccupancyType)
				&& (StringUtils.hasText(applicationType)
						&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& ((StringUtils.hasText(serviceType))
						&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
								|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {

			log.info("A_MIH - is project under affordable housing Purchasable FAR: "+isUnderAffordableHousing 
					+ "calculation required: "+isAffordableHousingRequirePurchasableFAR);
			
			Boolean isPurchasableFARApplicable = checkIfPurchasableFARApplicable(isUnderAffordableHousing,
					isAffordableHousingRequirePurchasableFAR);
				// if the project under affordable housing, then no Purchasable far is applicable
			if (isPurchasableFARApplicable) {
				BigDecimal benchmarkValuePerSQM = BigDecimal.valueOf(benchmarkValuePerAcre).divide(ACRE_SQMT_MULTIPLIER,
						4, BigDecimal.ROUND_UP);

				BigDecimal purchasableFARRate = (benchmarkValuePerSQM.multiply(ZERO_TWO_FIVE)).setScale(4,
						BigDecimal.ROUND_UP);

				BigDecimal deltaFAR = (BigDecimal.valueOf(providedFar).subtract(BigDecimal.valueOf(baseFar)))
						.setScale(4, BigDecimal.ROUND_UP);
				log.info("deltafar:" + deltaFAR);
//				BigDecimal applicableDiscountFar = (BigDecimal.valueOf(maxPermissibleFar)
//						.subtract(BigDecimal.valueOf(baseFar)).multiply(new BigDecimal("0.25"))).setScale(2,
//								BigDecimal.ROUND_UP);
				
				BigDecimal applicableDiscountFar = BigDecimal.ZERO;
				
				if (null != paramMap.get(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS)) {
					int noOfDU = (int) paramMap.get(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS);
					
					log.info("number of dwelling units: " + noOfDU);
					
					if(noOfDU >= 8)
						applicableDiscountFar = new BigDecimal("0.25");
				}
				
				log.info("Is Layout approved for MIG Suboccupancy: "+isLayoutApproved+" maxfar"+maxPermissibleFar);
				if(isLayoutApproved && maxPermissibleFar >= 3.00) {
					applicableDiscountFar = new BigDecimal("1.0");
					log.info("applicable discount after Layout Approval applicable: "+applicableDiscountFar);
				}
								
				if (deltaFAR.compareTo(applicableDiscountFar) > 0) {
					deltaFAR = deltaFAR.subtract(applicableDiscountFar);

				} else {
					deltaFAR = BigDecimal.ZERO;
				}

				// tdr relaxation- decrease deltaFar based on tdrFarRelaxation-
				if (null != tdrFarRelaxation) {
					deltaFAR = deltaFAR.subtract(new BigDecimal(tdrFarRelaxation)).setScale(4, BigDecimal.ROUND_UP);

				}

				if (deltaFAR.compareTo(BigDecimal.ZERO) < 0) {
					deltaFAR = BigDecimal.ZERO;
				}
				log.info("DeltaFar for MIG subOccupancy" + deltaFAR);
				if (subOccupancyType != null && subOccupancyType.contains("E-")) {
					log.info("Sub Occupancy Type : " + subOccupancyType);
					purchasableFARFee = BigDecimal.ZERO;
				} else {
					purchasableFARFee = (purchasableFARRate.multiply(deltaFAR).multiply(BigDecimal.valueOf(plotArea)))
							.setScale(2, BigDecimal.ROUND_UP);
				}
			}
		}
		//calculation for all cases other than MIG sub-occupancy-
		else if ((null != providedFar) && (null != baseFar) && (providedFar > baseFar) && (null != plotArea)) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				
				log.info("is project under affordable housing Purchasable FAR: "+isUnderAffordableHousing 
						+ " isAffordableHousingRequirePurchasableFAR : "+isAffordableHousingRequirePurchasableFAR);
				
				Boolean isPurchasableFARApplicable = checkIfPurchasableFARApplicable(isUnderAffordableHousing,
						isAffordableHousingRequirePurchasableFAR);
				
				if (isPurchasableFARApplicable) {

					BigDecimal benchmarkValuePerSQM = BigDecimal.valueOf(benchmarkValuePerAcre)
							.divide(ACRE_SQMT_MULTIPLIER, 4, BigDecimal.ROUND_UP);
					log.info("benchmark" + benchmarkValuePerSQM);
					BigDecimal purchasableFARRate = (benchmarkValuePerSQM.multiply(ZERO_TWO_FIVE)).setScale(4,
							BigDecimal.ROUND_UP);

					BigDecimal deltaFAR = (BigDecimal.valueOf(providedFar).subtract(BigDecimal.valueOf(baseFar)));

					BigDecimal applicableDiscountFar = BigDecimal.ZERO;
					
					log.info("Is Layout approved : "+isLayoutApproved+" max far"+maxPermissibleFar);
					if(isLayoutApproved && maxPermissibleFar >= 3.00) {
						applicableDiscountFar = new BigDecimal("1.0");
						log.info("applicable discount after Layout Approval applicable: "+applicableDiscountFar);
					}
					
					if (deltaFAR.compareTo(applicableDiscountFar) > 0) {
						deltaFAR = deltaFAR.subtract(applicableDiscountFar);

					} else {
						deltaFAR = BigDecimal.ZERO;
					}
					
					// tdr relaxation- decrease deltaFar based on tdrFarRelaxation-
					if (null != tdrFarRelaxation) {
						deltaFAR = deltaFAR.subtract(new BigDecimal(tdrFarRelaxation)).setScale(4, BigDecimal.ROUND_UP);

					}
					if (deltaFAR.compareTo(BigDecimal.ZERO) < 0) {
						deltaFAR = BigDecimal.ZERO;
					}
					if (subOccupancyType != null && subOccupancyType.contains("E-")) {
						log.info("Sub Occupancy Type : " + subOccupancyType);
						purchasableFARFee = BigDecimal.ZERO;
					} else {
						purchasableFARFee = (purchasableFARRate.multiply(deltaFAR)
								.multiply(BigDecimal.valueOf(plotArea))).setScale(2, BigDecimal.ROUND_UP);
					}
				}

			}

		} 

		generateTaxHeadEstimate(estimates, purchasableFARFee, BPACalculatorConstants.TAXHEAD_BPA_PURCHASABLE_FAR, Category.FEE);

		// System.out.println("PurchasableFARFee:::::::::::::::::" + purchasableFARFee);
		log.info("PurchasableFARFee:::::::::::::::::" + purchasableFARFee);
		return purchasableFARFee;

	}

	private Boolean checkIfPurchasableFARApplicable(Boolean isUnderAffordableHousing,
			Boolean isAffordableHousingRequirePurchasableFAR) {
		
		Boolean isPurchasableFARApplicable = Boolean.FALSE;
		if (!isUnderAffordableHousing || isAffordableHousingRequirePurchasableFAR) {
			isPurchasableFARApplicable = Boolean.TRUE;
		}
		return isPurchasableFARApplicable;
	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateSecurityDeposit(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		
		BigDecimal securityDeposits = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		Double totalBuiltUpAreaBackup = null;
		String occupancyType = null;
		boolean isSecurityDepositRequired = false;
		List<Occupancy> occupancyies = new ArrayList<>();
	
	if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
		occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
	}

	Occupancy subOccupancyWithMaxCoverage = occupancyies.stream()
	        .max((o1, o2) -> Double.compare(o1.getBuiltUpArea(), o2.getBuiltUpArea()))
	        .orElse(null);
	
	for(Occupancy occupancy : occupancyies) {
	BigDecimal securityDeposit = BigDecimal.ZERO;
	totalBuitUpArea = occupancy.getBuiltUpArea();
	log.info("security deposit:builtup area before public washroom addition :"+totalBuitUpArea);
	
	// Add public Washroom area and ICT area in Built Up Area if this subOccupanecy has max coverage
	if(occupancy.equals(subOccupancyWithMaxCoverage)) {
		totalBuitUpArea = addBuiltUpAreaForPublicWashroom(totalBuitUpArea, paramMap, occupancy);
		totalBuitUpArea = addBuiltUpAreaForICT(totalBuitUpArea, paramMap, occupancy);
		
	}

	//totalBuiltUpAreaBackup = occupancy.getFloorArea(); Backup of Existing Code
	log.info("totalBuitUpArea inside:"+totalBuitUpArea);
	occupancyType = occupancy.getOccupancyCode();
	paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
	paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA,totalBuitUpArea);
	paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,occupancy.getSubOccupancyCode());
	log.info("occupancy inside:"+occupancyType);
	log.info("suboccupancy inside:"+occupancy.getSubOccupancyCode());
	
	
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SECURITY_DEPOSIT)) {
			isSecurityDepositRequired = (boolean) paramMap.get(BPACalculatorConstants.SECURITY_DEPOSIT);
		}
		
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_SECURITY_DEPOSIT);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				securityDeposits.add(totalAmountAfterExemption);
			}
		} else	if (totalBuitUpArea != null && isSecurityDepositRequired) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				securityDeposit = calculateSecurityDepositForResidentialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
				securityDeposit = calculateSecurityDepositForCommercialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
				securityDeposit = calculateSecurityDepositForPublicSemiPublicInstitutionalOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
				securityDeposit = calculateSecurityDepositForEducationOccupancy(paramMap);
			}
		}
		securityDeposits=securityDeposits.add(securityDeposit); 
	}
		generateTaxHeadEstimate(estimates, securityDeposits, BPACalculatorConstants.TAXHEAD_BPA_SECURITY_DEPOSIT, Category.FEE);
		// System.out.println("SecurityDeposit::::::::::::::" + securityDeposit);
		log.info("SecurityDeposit::::::::::::::" + securityDeposits);

		return securityDeposits;

	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				if (null != totalBuitUpArea && totalBuitUpArea >= 200) {
					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);
				}
			}
		}
		return securityDeposit;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForPublicSemiPublicInstitutionalOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				if (null != totalBuitUpArea && totalBuitUpArea >= 200) {
					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);

				}
			}
		}
		return securityDeposit;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				if (null != totalBuitUpArea && totalBuitUpArea >= 200) {
					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);

				}
			}
		}
		return securityDeposit;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSecurityDepositForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal securityDeposit = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {

				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_HP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_WCR))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SA))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_E))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_LIH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_MIH))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SQ))) {

					// securityDeposit = calculateConstantFee(paramMap, 100);
					securityDeposit = calculateConstantFeeNew(totalBuitUpArea, 100);

				}

			}

		}
		return securityDeposit;
	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateTemporaryRetentionFee(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal retentionFee = BigDecimal.ZERO;
		Double numberOfTemporaryStructures = null;
		String applicationType = null;
		String serviceType = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES)) {
			numberOfTemporaryStructures = (Double) paramMap.get(BPACalculatorConstants.NUMBER_OF_TEMP_STRUCTURES);
		}
		boolean isRetentionFeeApplicable = false;
		if (null != paramMap.get(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE)) {
			isRetentionFeeApplicable = (boolean) paramMap.get(BPACalculatorConstants.IS_RETENTION_FEE_APPLICABLE);
		}
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_TEMP_RETENTION_FEE);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				retentionFee.add(totalAmountAfterExemption);
			}
		} else if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& ((StringUtils.hasText(serviceType))
						&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
								|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))
				&& isRetentionFeeApplicable && Objects.nonNull(numberOfTemporaryStructures)) {
			Object mdmsData = paramMap.get("mdmsData");
			String tenantId = String.valueOf(paramMap.get("tenantId"));
			List jsonOutput = JsonPath.read(mdmsData, BPACalculatorConstants.MDMS_RETENTION_FEE_PATH);
			String filterExp = "$.[?(@.ulb == '" + tenantId + "')]";
			List<Map<String, String>> retentionFeeForTenantJson = JsonPath.read(jsonOutput, filterExp);
			if (!CollectionUtils.isEmpty(retentionFeeForTenantJson)) {
				String retentionFeeForTenant = retentionFeeForTenantJson.get(0)
						.get(BPACalculatorConstants.MDMS_RETENTION_FEE);
				retentionFee = new BigDecimal(retentionFeeForTenant).multiply(new BigDecimal(numberOfTemporaryStructures));
			}
		}

		generateTaxHeadEstimate(estimates, retentionFee, BPACalculatorConstants.TAXHEAD_BPA_TEMP_RETENTION_FEE, Category.FEE);

		// System.out.println("RetentionFee:::::::::::" + retentionFee);
		log.info("RetentionFee:::::::::::" + retentionFee);
		return retentionFee;

	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateShelterFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		
		BigDecimal shelterFee = BigDecimal.ZERO;
		BigDecimal shelterFees = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		String occupancyType = null;
		Double carpetArea = null;
		List<Occupancy> occupancyies = new ArrayList<>();
		
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		for(Occupancy occupancy : occupancyies) {
		totalBuitUpArea = occupancy.getFloorArea();
		carpetArea = occupancy.getCarpetArea();
		log.info("totalBuitUpArea inside:"+totalBuitUpArea);
		occupancyType = occupancy.getOccupancyCode();
		paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
		paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA,totalBuitUpArea);
		paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,occupancy.getSubOccupancyCode());
		paramMap.put(BPACalculatorConstants.CARPET_AREA, carpetArea);
		log.info("occupancy shelter inside :"+occupancyType);
		log.info("suboccupancy inside:"+occupancy.getSubOccupancyCode());
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if(null!= paramMap.get(BPACalculatorConstants.CARPET_AREA)) {
			carpetArea = (Double) paramMap.get(BPACalculatorConstants.CARPET_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		/*if (totalBuitUpArea != null) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				shelterFee = calculateShelterFeeForResidentialOccupancy(paramMap);
			}
		}*/
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_SHELTER_FEE);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				shelterFee.add(totalAmountAfterExemption);
			}
		} else if(carpetArea != null) {
			log.info("Carpet Area :"+ carpetArea);
			if(occupancyType.equalsIgnoreCase(BPACalculatorConstants.A)) {
				shelterFee = calculateShelterFeeForResidentialOccupancy(paramMap);
			}
		}
		shelterFees=shelterFees.add(shelterFee);
		if(shelterFees.compareTo(BigDecimal.ZERO)>0) {
			break;
		}
		}
		generateTaxHeadEstimate(estimates, shelterFees, BPACalculatorConstants.TAXHEAD_BPA_SHELTER_FEE, Category.FEE);

		// System.out.println("ShelterFee::::::::::::::::" + shelterFee);
		log.info("ShelterFee::::::::::::::::" + shelterFees);
		return shelterFees;

	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateShelterFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal shelterFee = BigDecimal.ZERO;
		BigDecimal costOfConstruction = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalEWSArea = null;
		boolean isShelterFeeRequired = false;
		int totalNoOfDwellingUnits = 0;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.EWS_AREA)) {
			totalEWSArea = (Double) paramMap.get(BPACalculatorConstants.EWS_AREA);
			log.info("shelter part:"+totalEWSArea);
		}
		if (null != paramMap.get(BPACalculatorConstants.SHELTER_FEE)) {
			isShelterFeeRequired = (boolean) paramMap.get(BPACalculatorConstants.SHELTER_FEE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS)) {
			totalNoOfDwellingUnits = (int) paramMap.get(BPACalculatorConstants.TOTAL_NO_OF_DWELLING_UNITS);
			log.info("			totalNoOfDwellingUnits part:"+			totalNoOfDwellingUnits);
		}
		if(null != paramMap.get(BPACalculatorConstants.COST_OF_CONSTRUCTION)) {
			costOfConstruction = new BigDecimal((String)paramMap.get(BPACalculatorConstants.COST_OF_CONSTRUCTION));
		}
		
		if (isShelterFeeRequired && totalNoOfDwellingUnits > 8) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A) && StringUtils.hasText(subOccupancyType))) {
				if ((StringUtils.hasText(applicationType)
						&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
						&& ((StringUtils.hasText(serviceType))
								&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
										|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {

					if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_P))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_S))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_R))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_AB))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_HP))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_WCR))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_SA))
							|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_MIH))) {
                     log.info("shelter part:"+totalEWSArea);
						/*shelterFee = (BigDecimal.valueOf(totalEWSArea).multiply(SQMT_SQFT_MULTIPLIER)
								.multiply(SEVENTEEN_FIFTY).multiply(ZERO_TWO_FIVE)).setScale(2, BigDecimal.ROUND_UP);
						*/
                     
                     BigDecimal totalEWSAreaForShelterFee = BigDecimal.valueOf(totalEWSArea).multiply(ONE_FORTY_PERCENT);
					 shelterFee = (totalEWSAreaForShelterFee.multiply(SQMT_SQFT_MULTIPLIER)
								.multiply(costOfConstruction).multiply(ZERO_TWO_FIVE)).setScale(2, BigDecimal.ROUND_UP);

					}

				}

			}

		}
		return shelterFee;
	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateConstructionWorkerWelfareCess(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal welfareCessRate = BigDecimal.ZERO;
		BigDecimal welfareCess = BigDecimal.ZERO;
		BigDecimal costOfConstruction = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		Double totalFloorArea = null;
		Double totalBuiltupArea = null;
		Double totalBuiltup =0.0;
		Double totalFloor =0.0;
		Boolean isTheCwwcFeeAlreadyPaid = Boolean.FALSE;
		List<Occupancy> occupancyies = new ArrayList<>();
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		
		Occupancy subOccupancyWithMaxCoverage = occupancyies.stream()
		        .max((o1, o2) -> Double.compare(o1.getBuiltUpArea(), o2.getBuiltUpArea()))
		        .orElse(null);
		
		for(Occupancy occupancy : occupancyies) {
			
			totalFloor += occupancy.getFloorArea();
			log.info("totalfllorArea inside:" + totalFloor);
			totalBuiltup += occupancy.getBuiltUpArea();
			log.info("CWC: total builtup area before public washroom addition :" + totalBuiltup);
			
			// Add public Washroom area in Built Up Area if this subOccupanecy has max coverage
			if (occupancy.equals(subOccupancyWithMaxCoverage)) {
				totalBuiltup = addBuiltUpAreaForPublicWashroom(totalBuiltup, paramMap, occupancy);
				totalBuiltup = addBuiltUpAreaForICT(totalBuiltup, paramMap, occupancy);
				
			}
			

			log.info("totalBuitUpArea inside:" + totalBuiltup);
			
		}
		paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR,totalBuiltup);
		paramMap.put(BPACalculatorConstants.TOTAL_FLOOR_AREA,totalFloor);
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalFloorArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);
		}
		if(null != paramMap.get(BPACalculatorConstants.CWCFEE)) {
			welfareCessRate = new BigDecimal((String)paramMap.get(BPACalculatorConstants.CWCFEE));
		}
		if(null != paramMap.get(BPACalculatorConstants.COST_OF_CONSTRUCTION)) {
			costOfConstruction = new BigDecimal((String)paramMap.get(BPACalculatorConstants.COST_OF_CONSTRUCTION));
		}
		// Get the variable to check if CWWC is already paid or not
		if (null != paramMap.get(BPACalculatorConstants.IS_THE_CWWC_FEE_ALREADY_PAID)) {
			isTheCwwcFeeAlreadyPaid = (Boolean) paramMap.get(BPACalculatorConstants.IS_THE_CWWC_FEE_ALREADY_PAID);
		}
		totalBuiltupArea=getAreaParameterForBPFeesCalculation(paramMap);
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),
					BPACalculatorConstants.TAXHEAD_BPA_WORKER_WELFARE_CESS);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				welfareCess.add(totalAmountAfterExemption);
			}
		} else if ((StringUtils.hasText(applicationType)
				&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
				&& ((StringUtils.hasText(serviceType))
						&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
								|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
			// Double costOfConstruction = (1750 * totalBuitUpArea * 10.764);
			//Use builtup area instead of floor area to calculate totalCostOfConstruction and check if totalCostOfConstruction>10Lakh.If true,
			//then use builtup area instead of floor area to calculate ConstructionWorkerWelfareCess-
			
			log.info("CWWC already paid :"+ isTheCwwcFeeAlreadyPaid);
			// if CWWC already paid, then CWWC fee is exempted
			if (!isTheCwwcFeeAlreadyPaid) {
				BigDecimal totalCostOfConstruction = (costOfConstruction.multiply(BigDecimal.valueOf(totalBuiltupArea))
						.multiply(SQMT_SQFT_MULTIPLIER)).setScale(2, BigDecimal.ROUND_UP);
				if (totalCostOfConstruction.compareTo(TEN_LAC) > 0) {
					welfareCess = (welfareCessRate.multiply(BigDecimal.valueOf(totalBuiltupArea))
							.multiply(SQMT_SQFT_MULTIPLIER)).setScale(2, BigDecimal.ROUND_UP);
				}
			}

		}
		generateTaxHeadEstimate(estimates, welfareCess, BPACalculatorConstants.TAXHEAD_BPA_WORKER_WELFARE_CESS, Category.FEE);

		// System.out.println("WelfareCess::::::::::::::" + welfareCess);
		log.info("WelfareCess::::::::::::::" + welfareCess);
		return welfareCess;

	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateSanctionFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		BigDecimal sanctionFees = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		String occupancyType = null; 
		Boolean isRefApplicationPresentInSujog = Boolean.FALSE;
		Boolean isUnderAffordableHousing = Boolean.FALSE;
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		List<Occupancy> occupancyies= new ArrayList<>();
//		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
//			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
//		}
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG))) {
			isRefApplicationPresentInSujog = (Boolean) paramMap
					.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG);
		}
		
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		
		if (null != paramMap.get(BPACalculatorConstants.IS_PROJECT_UNDER_AFFORDABLE_HOUSING_SCHEME)) {
			isUnderAffordableHousing = (Boolean) paramMap
					.get(BPACalculatorConstants.IS_PROJECT_UNDER_AFFORDABLE_HOUSING_SCHEME);
		}
		
		Boolean isOutsideSujogApplExemptionEstimate = isOutsideSujogApplExcemptionEstimate(paramMap, estimates);
		if (isOutsideSujogApplExemptionEstimate && config.getCalculateExemptionForFeeOutsideSujogAppl()) {
			BPA bpa = (BPA) paramMap.get("BPA");
			List<ExemptionFeeDetails> exemptionFeeDetails = getAllExemptionFeesDetails(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL),BPACalculatorConstants.TAXHEAD_BPA_SANCTION_FEE);
			BigDecimal totalAmountAfterExemption = BigDecimal.ZERO;
			for (ExemptionFeeDetails excmptionFeeDetail : exemptionFeeDetails) {
				try {
					totalAmountAfterExemption = totalAmountAfterExemption.add(excmptionFeeDetail.getTotalAmount());
				} catch (Exception e) {
					e.printStackTrace();
				}
				sanctionFees.add(totalAmountAfterExemption);
			}
		} else {
			
			log.info("is project under afforable housing Sanction fee: " + isUnderAffordableHousing);
			// if the project under affordable housing, then no Sanction Fee fee is
			// applicable
			if (!isUnderAffordableHousing) {

				Occupancy subOccupancyWithMaxCoverage = occupancyies.stream()
						.max((o1, o2) -> Double.compare(o1.getBuiltUpArea(), o2.getBuiltUpArea())).orElse(null);

				for (Occupancy occupancy : occupancyies) {

					totalBuitUpArea = occupancy.getBuiltUpArea();
					log.info("Sanction Fee: builtup area before public washroom addition: " + totalBuitUpArea);

					// Add public Washroom area in Built Up Area if this subOccupanecy has max
					// coverage
					if (occupancy.equals(subOccupancyWithMaxCoverage)) {
						totalBuitUpArea = addBuiltUpAreaForPublicWashroom(totalBuitUpArea, paramMap, occupancy);
						totalBuitUpArea = addBuiltUpAreaForICT(totalBuitUpArea, paramMap, occupancy);
					}

					log.info("totalBuitUpArea inside:" + totalBuitUpArea);
					occupancyType = occupancy.getOccupancyCode();
					paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
					paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR, totalBuitUpArea);
					paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE, occupancy.getSubOccupancyCode());
					log.info("occupancy inside:" + occupancyType);
					log.info("suboccupancy inside:" + occupancy.getSubOccupancyCode());

					if (totalBuitUpArea != null) {
						if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
							sanctionFee = calculateSanctionFeeForResidentialOccupancy(paramMap);
						}
						if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
							sanctionFee = calculateSanctionFeeForCommercialOccupancy(paramMap);
						}
						if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
							sanctionFee = calculateSanctionFeeForPublicSemiPublicInstitutionalOccupancy(paramMap);
						}
						if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
							sanctionFee = calculateSanctionFeeForPublicUtilityOccupancy(paramMap);
						}
						if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
							sanctionFee = calculateSanctionFeeForIndustrialZoneOccupancy(paramMap);
						}
						if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
							sanctionFee = calculateSanctionFeeForEducationOccupancy(paramMap);
						}
						if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
							sanctionFee = calculateSanctionFeeForTransportationOccupancy(paramMap);
						}
						if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
							sanctionFee = calculateSanctionFeeForAgricultureOccupancy(paramMap);
						}

					}
					sanctionFees = sanctionFees.add(sanctionFee);
				}
			}
		}
		generateTaxHeadEstimate(estimates, sanctionFees, BPACalculatorConstants.TAXHEAD_BPA_SANCTION_FEE, Category.FEE);

		// System.out.println("SanctionFee::::::::" + sanctionFee);
		log.info("SanctionFee::::::::" + sanctionFees);
		return sanctionFees;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForPublicSemiPublicInstitutionalOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C)) && (StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
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

					// sanctionFee = calculateConstantFee(paramMap, 30);
					if(isSparit)
						sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 15);
					else
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_AB))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_GO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_LSGO))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_P))) {
					// sanctionFee = calculateConstantFee(paramMap, 10);
					if(isSparit)
						sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 5);
					else
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
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
					// sanctionFee = calculateConstantFee(paramMap, 30);
					if(isSparit)
						sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 15);
					else
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
					
				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_FS))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_J))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PO))) {
					// sanctionFee = calculateConstantFee(paramMap, 10);
					if(isSparit)
						sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 5);
					else
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
				}
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForAgricultureOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				// sanctionFee = calculateConstantFee(paramMap, 30);
				if(isSparit)
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 15);
				else
				sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForTransportationOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				// sanctionFee = calculateConstantFee(paramMap, 10);
				if(isSparit)
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 5);	
				else
				 sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}

		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				// sanctionFee = calculateConstantFee(paramMap, 30);
				if(isSparit)
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 15);	
				else
				  sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForPublicUtilityOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				// sanctionFee = calculateConstantFee(paramMap, 10);
				if(isSparit)
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 10);
				else
				  sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 5);
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForIndustrialZoneOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				// sanctionFee = calculateConstantFee(paramMap, 60);
				if(isSparit)
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
				else
				  sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 60);
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				// sanctionFee = calculateConstantFee(paramMap, 60);
				if(isSparit)
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 30);
				else
				 sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 60);
			}
		}
		return sanctionFee;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateSanctionFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal sanctionFee = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A) && StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_P))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.A_R))) {
					// sanctionFee = calculateConstantFee(paramMap, 15);
					if(isSparit)
						sanctionFee = calculateConstantFeeNewSparit(totalBuitUpArea, 7.50);
					else
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 15);

				} else {
					// sanctionFee = calculateConstantFee(paramMap, 50);
					if(isSparit)
						sanctionFee = calculateConstantFeeNewSparit(totalBuitUpArea, 25.0);
					else
					sanctionFee = calculateConstantFeeNew(totalBuitUpArea, 50);
				}

			}

		}
		return sanctionFee;
	}

	private BigDecimal calculateConstantFeeNewSparit(Double totalBuitUpArea, double multiplicationFactor) {
		BigDecimal totalAmount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			totalAmount = (BigDecimal.valueOf(totalBuitUpArea).multiply(BigDecimal.valueOf(multiplicationFactor)))
					.setScale(2, BigDecimal.ROUND_UP);

		}
		return totalAmount;
	}


	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateTotalScrutinyFee(Map<String, Object> paramMap, ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal calculatedTotalScrutinyFee = BigDecimal.ZERO;
		//Boolean isSparit = checkUlbForSparit(paramMap);
		BigDecimal feeForDevelopmentOfLand = calculateFeeForDevelopmentOfLand(paramMap, estimates);
		BigDecimal feeForBuildingOperation = calculateFeeForBuildingOperation(paramMap, estimates);
		calculatedTotalScrutinyFee = (calculatedTotalScrutinyFee.add(feeForDevelopmentOfLand)
				.add(feeForBuildingOperation)).setScale(2, BigDecimal.ROUND_UP);
		return calculatedTotalScrutinyFee;
	}
	


	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateFeeForDevelopmentOfLand(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		BigDecimal feeForDevelopmentOfLand = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String riskType = null;
		Double plotArea = null;
		Boolean isSparit = null;
		Boolean isRevisionApplication = Boolean.FALSE;
		Boolean landDevFeeDetails = Boolean.FALSE;
		Boolean isLandDevelopmentFeeExempted = Boolean.FALSE;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.RISK_TYPE)) {
			riskType = (String) paramMap.get(BPACalculatorConstants.RISK_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.PLOT_AREA)) {
			plotArea = (Double) paramMap.get(BPACalculatorConstants.PLOT_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		if (null != paramMap.get(BPACalculatorConstants.IS_REVISION_APPLICATION)) {
			isRevisionApplication = (Boolean) paramMap.get(BPACalculatorConstants.IS_REVISION_APPLICATION);
		}
		
		BPA bpa = (BPA) paramMap.get("BPA");
		Map<String, Object>	additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
		if (!ObjectUtils.isEmpty(additionalDetails)) {
			landDevFeeDetails = (Boolean) additionalDetails
					.get(BPACalculatorConstants.IS_LAND_DEVELOPMENT_FEE_EXEMPTED);
			log.info("Land Development Fee Exemption : " + landDevFeeDetails);
		}
		
		if(!BPACalculatorConstants.RISK_TYPE_LOW.equalsIgnoreCase(riskType)) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& (StringUtils.hasText(serviceType)
							&& serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)) &&!isRevisionApplication) {
				if (null != plotArea) {
					paramMap.put(BPACalculatorConstants.AREA_TYPE, BPACalculatorConstants.AREA_TYPE_PLOT);
					// feeForDevelopmentOfLand = calculateConstantFee(paramMap, 5);

					if(isSparit) {
									feeForDevelopmentOfLand = calculateConstantFeeNewSparit(plotArea, 2.5);
								}else {
					feeForDevelopmentOfLand = calculateConstantFeeNew(plotArea, 5);
								}
					paramMap.put(BPACalculatorConstants.AREA_TYPE, null);
				}
	
			}
			
			if (!ObjectUtils.isEmpty(landDevFeeDetails)) {
				if (Boolean.TRUE.equals(landDevFeeDetails)) {
					feeForDevelopmentOfLand = BigDecimal.ZERO;
				}
			}
	
			generateTaxHeadEstimate(estimates, feeForDevelopmentOfLand, BPACalculatorConstants.TAXHEAD_BPA_LAND_DEVELOPMENT_FEE, Category.FEE);
		}
		// System.out.println("FeeForDevelopmentOfLand:::::::::::" +
		// feeForDevelopmentOfLand);
		log.info("FeeForDevelopmentOfLand:::::::::::" + feeForDevelopmentOfLand);
		return feeForDevelopmentOfLand;

	}

	/**
	 * @param paramMap
	 * @param estimates
	 * @return
	 */
	private BigDecimal calculateFeeForBuildingOperation(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {

		BPA bpa = (BPA) paramMap.get("BPA");
		String subservice = null;
		Boolean isRevisionApplication = Boolean.FALSE;
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY))) {
			subservice = (String) paramMap.get(BPACalculatorConstants.BPA_ADD_DETAILS_SUBSERVICE_KEY);
		}
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REVISION_APPLICATION))) {
			isRevisionApplication = (Boolean) paramMap.get(BPACalculatorConstants.IS_REVISION_APPLICATION);
		}
		if (!StringUtils.isEmpty(paramMap.get(BPACalculatorConstants.SERVICE_TYPE))
				&& paramMap.get(BPACalculatorConstants.SERVICE_TYPE).equals(BPACalculatorConstants.ALTERATION)
				&& Objects.isNull(subservice)) {
			log.info("pure old alteration application");
			return alterationCalculationService.calculateFeeForBuildingOperation(paramMap, estimates);
		} else if (Objects.nonNull(subservice) && !BPACalculatorConstants.ALTERATION_SUBSERVICE_A.equals(subservice)) {
			// boolean isRevisedAreaLessThanEqualToOldArea = false;
			switch (subservice) {
			case BPACalculatorConstants.ALTERATION_SUBSERVICE_A:
				// "isRevisionApplication":true,"isApplicationPersentInSujogSystem":true,"isPermitLetterExpried":false,"alterationSubService":"ALTERATION_SERVICE_A","applicationSubType":"NEW_CONSTRUCTION"
				// never executed as if condition above
				break;
			case BPACalculatorConstants.ALTERATION_SUBSERVICE_B:
				// "isRevisionApplication":true,"isApplicationPersentInSujogSystem":true,"isPermitLetterExpried":false,"alterationSubService":"ALTERATION_SERVICE_B","applicationSubType":"ALTERATION"
				updateRevisionServiceParam(subservice, paramMap, estimates);
				return alterationCalculationService.calculateFeeForBuildingOperation(paramMap, estimates);
			case BPACalculatorConstants.ALTERATION_SUBSERVICE_C:
			case BPACalculatorConstants.ALTERATION_SUBSERVICE_D:
				return alterationCalculationService.calculateFeeForBuildingOperation(paramMap, estimates);
			}
		}
		else if(Objects.nonNull(subservice) && BPACalculatorConstants.ALTERATION_SUBSERVICE_A.equals(subservice)) {
			updateRevisionServiceParam(subservice, paramMap, estimates);
		}

		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		BigDecimal feesForBuildingOperation = BigDecimal.ZERO;
		Double totalBuitUpArea = null;
		String occupancyType = null;
		List<Occupancy> occupancyies = null;
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		log.info("totalBuitUpArea outside:"+totalBuitUpArea);
//		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
//			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
//		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCYLIST)) {
			occupancyies = (List<Occupancy>) paramMap.get(BPACalculatorConstants.OCCUPANCYLIST);
		}
		
		Occupancy subOccupancyWithMaxCoverage = null;
		
		if (!CollectionUtils.isEmpty(occupancyies)) {

			subOccupancyWithMaxCoverage = occupancyies.stream()
					.max((o1, o2) -> Double.compare(o1.getBuiltUpArea(), o2.getBuiltUpArea())).orElse(null);
		
		
		for(Occupancy occupancy : occupancyies) {
		
		totalBuitUpArea = occupancy.getBuiltUpArea();
		log.info("Building OPS fee: built up area before public washroom addition: "+totalBuitUpArea);
		
		// Add public Washroom area in Built Up Area if this subOccupanecy has max coverage
		if (occupancy.equals(subOccupancyWithMaxCoverage)) {
			totalBuitUpArea = addBuiltUpAreaForPublicWashroom(totalBuitUpArea, paramMap, occupancy);
			totalBuitUpArea = addBuiltUpAreaForICT(totalBuitUpArea, paramMap, occupancy);
		}
		log.info("totalBuitUpArea inside:"+totalBuitUpArea);
		occupancyType = occupancy.getOccupancyCode();
		paramMap.put(BPACalculatorConstants.OCCUPANCY_TYPE, occupancyType);
		paramMap.put(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR,totalBuitUpArea);
		paramMap.put(BPACalculatorConstants.SUB_OCCUPANCY_TYPE,occupancy.getSubOccupancyCode());
		log.info("occupancy inside:"+occupancyType);
		log.info("suboccupancy inside:"+occupancy.getSubOccupancyCode());
		
		if (totalBuitUpArea != null) {
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForResidentialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForCommercialOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForPublicSemiPublicInstitutionalOccupancy(
						paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForPublicUtilityOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForIndustrialZoneOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForEducationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForTransportationOccupancy(paramMap);
			}
			if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
				feeForBuildingOperation = calculateBuildingOperationFeeForAgricultureOccupancy(paramMap);
			}

		}
		log.info("FeeFrBuildingOperation:::::::::::" + feeForBuildingOperation);
		//Summation of fees
		feesForBuildingOperation=feesForBuildingOperation.add(feeForBuildingOperation);
		log.info("FeeFromBuildingOperation:::::::::::" + feesForBuildingOperation);
		}
		}
		if (Objects.nonNull(subservice) && BPACalculatorConstants.ALTERATION_SUBSERVICE_A.equals(subservice)) {
			if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REVISED_AREA_LESS_OR_EQUAL_TO_OLD_AREA))
					&& (boolean) paramMap.get(BPACalculatorConstants.IS_REVISED_AREA_LESS_OR_EQUAL_TO_OLD_AREA)) {
				log.info("setting half of building operation as subservice A and area less or equal");
				feesForBuildingOperation = feesForBuildingOperation.divide(new BigDecimal(2), 0,
						BigDecimal.ROUND_HALF_UP);
			}else {
				//let the calculation be done for current drawing parameters as full payment has to be done- 
				Map<String, Object>	additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
				Object alterationServiceNode = additionalDetails.get(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY);
				additionalDetails.remove(BPACalculatorConstants.BPA_ADD_DETAILS_SERVICE_KEY);
				CalulationCriteria calculationCriteria = CalulationCriteria.builder()
						.applicationNo(bpa.getApplicationNo())
						.applicationType(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY).bpa(bpa)
						.feeType(BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE)
						.serviceType(BPACalculatorConstants.NEW_CONSTRUCTION).tenantId(bpa.getTenantId()).build();
				List<CalulationCriteria> calculationCriterias = new ArrayList<>();
				calculationCriterias.add(calculationCriteria);
				CalculationReq calculationReq = CalculationReq.builder().calulationCriteria(calculationCriterias)
						.requestInfo((RequestInfo) paramMap.get("requestInfo")).build();
				List<Calculation> calculationsForCurrentApplicationParameters = getEstimate(calculationReq);
				Optional<TaxHeadEstimate> estimateOptional = calculationsForCurrentApplicationParameters.get(0)
						.getTaxHeadEstimates().stream()
						.filter(estimate -> BPACalculatorConstants.TAXHEAD_BPA_BUILDING_OPERATION_FEE
								.equals(estimate.getTaxHeadCode()))
						.findAny();
				if(estimateOptional.isPresent()) {
					feesForBuildingOperation = estimateOptional.get().getEstimateAmount();
				}
			}

		}
				generateTaxHeadEstimate(estimates, feesForBuildingOperation, BPACalculatorConstants.TAXHEAD_BPA_BUILDING_OPERATION_FEE, Category.FEE);
		log.info("FeeForBuildingOperation:::::::::::" + feesForBuildingOperation);
		return feesForBuildingOperation;
	
	}
	
	private void updateRevisionServiceParam(String subService, Map<String, Object> paramMap,ArrayList<TaxHeadEstimate> estimates) {
		BPA bpa = (BPA) paramMap.get("BPA");
		Map<String,Object> additionalDetails = new HashMap<>();
		if (Objects.nonNull(bpa) && Objects.nonNull(bpa.getAdditionalDetails())){
			additionalDetails = (Map<String, Object>) bpa.getAdditionalDetails();
		}
		Boolean isRefApplicationPresentInSujog = Boolean.FALSE;
		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG))) {
			isRefApplicationPresentInSujog = (Boolean) paramMap
					.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG);
		}
		Double refBuiltUpArea = new Double(0);
		Double currentBuiltUpArea = new Double(0);
		if (BPACalculatorConstants.ALTERATION_SUBSERVICE_A.equals(subService) && isRefApplicationPresentInSujog) {
			currentBuiltUpArea = Double
					.parseDouble(paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR).toString());
		} else if (BPACalculatorConstants.ALTERATION_SUBSERVICE_A.equals(subService)
				&& !isRefApplicationPresentInSujog) {
			// for non-sujog applications, set current builtup area from drawing and ref
			// from paramMap as paramMap set from revision for non-sujog
			refBuiltUpArea = Double
					.parseDouble(paramMap.get(BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR).toString());
		}
		else if (BPACalculatorConstants.ALTERATION_SUBSERVICE_B.equals(subService) && isRefApplicationPresentInSujog) {
			currentBuiltUpArea = (Double) paramMap.get(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA);
		} else if (BPACalculatorConstants.ALTERATION_SUBSERVICE_B.equals(subService)
				&& !isRefApplicationPresentInSujog) {
			refBuiltUpArea = (Double) paramMap.get(BPACalculatorConstants.ALTERATION_PROPOSED_BUILTUP_AREA);
		}
		
		if(isRefApplicationPresentInSujog) {
			LinkedHashMap refEdcr = (LinkedHashMap) paramMap.get(BPACalculatorConstants.REF_EDCR_DETAILS);
			String jsonString = new JSONObject(refEdcr).toString();
			DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
			String builtUpAreaPathToConsiderBySubService=BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA_PATH;
			Double alterationTotalBuiltupArea = null;
			Double alterationExistingBuiltupArea = null;
			Double alterationProposedBuiltupArea = null;
			JSONArray alterationTotalBuiltupAreaJson = context
					.read(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA_PATH);
			if (!CollectionUtils.isEmpty(alterationTotalBuiltupAreaJson)) {
				String alterationTotalBuiltupAreaString = alterationTotalBuiltupAreaJson.get(0).toString();
				alterationTotalBuiltupArea = Double.parseDouble(alterationTotalBuiltupAreaString);
			}

			JSONArray alterationExistingBuiltupAreaJson = context
					.read(BPACalculatorConstants.ALTERATION_EXISTING_BUILTUP_AREA_PATH);
			if (!CollectionUtils.isEmpty(alterationExistingBuiltupAreaJson)) {
				String alterationExistingBuiltupAreaString = alterationExistingBuiltupAreaJson.get(0).toString();
				alterationExistingBuiltupArea = Double.parseDouble(alterationExistingBuiltupAreaString);
			}
			// subtract above two and put as proposed builtup area-
			if (Objects.nonNull(alterationTotalBuiltupArea) && Objects.nonNull(alterationExistingBuiltupArea)) {
				alterationProposedBuiltupArea = alterationTotalBuiltupArea - alterationExistingBuiltupArea;
			}
			if (subService.equals(BPACalculatorConstants.ALTERATION_SUBSERVICE_A)) {
				refBuiltUpArea = alterationTotalBuiltupArea;
			} else if (subService.equals(BPACalculatorConstants.ALTERATION_SUBSERVICE_B)) {
				refBuiltUpArea = alterationProposedBuiltupArea;
			}
		}
		else {
			//for non-sujog application:
			Revision revision = (Revision) paramMap.get(BPACalculatorConstants.REVISION);
			if (subService.equals(BPACalculatorConstants.ALTERATION_SUBSERVICE_A)) {
				LinkedHashMap edcr = (LinkedHashMap) paramMap.get(BPACalculatorConstants.EDCR_DETAILS);
				String jsonString = new JSONObject(edcr).toString();
				DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
				Double alterationTotalBuiltupArea = null;
				JSONArray alterationTotalBuiltupAreaJson = context
						.read(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA_PATH);
				if (!CollectionUtils.isEmpty(alterationTotalBuiltupAreaJson)) {
					String alterationTotalBuiltupAreaString = alterationTotalBuiltupAreaJson.get(0).toString();
					alterationTotalBuiltupArea = Double.parseDouble(alterationTotalBuiltupAreaString);
				}
				currentBuiltUpArea = alterationTotalBuiltupArea;

			} else if (subService.equals(BPACalculatorConstants.ALTERATION_SUBSERVICE_B)) {
				LinkedHashMap edcr = (LinkedHashMap) paramMap.get(BPACalculatorConstants.EDCR_DETAILS);
				String jsonString = new JSONObject(edcr).toString();
				DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
				Double alterationTotalBuiltupArea = null;
				Double alterationTotalExistingBuiltupArea = null;
				JSONArray alterationTotalBuiltupAreaJson = context
						.read(BPACalculatorConstants.ALTERATION_TOTAL_BUILTUP_AREA_PATH);
				if (!CollectionUtils.isEmpty(alterationTotalBuiltupAreaJson)) {
					String alterationTotalBuiltupAreaString = alterationTotalBuiltupAreaJson.get(0).toString();
					alterationTotalBuiltupArea = Double.parseDouble(alterationTotalBuiltupAreaString);
				}
				JSONArray alterationTotalExistingBuiltupAreaJson = context
						.read(BPACalculatorConstants.ALTERATION_EXISTING_BUILTUP_AREA_PATH);
				if (!CollectionUtils.isEmpty(alterationTotalExistingBuiltupAreaJson)) {
					String alterationTotalExistingBuiltupAreaString = alterationTotalExistingBuiltupAreaJson.get(0)
							.toString();
					alterationTotalExistingBuiltupArea = Double.parseDouble(alterationTotalExistingBuiltupAreaString);
				}
				currentBuiltUpArea = alterationTotalBuiltupArea - alterationTotalExistingBuiltupArea;
			}
		}
		
		boolean isRevisedAreaLessThanEqualToOldArea = false;
		
		
		if(currentBuiltUpArea<=refBuiltUpArea)
			isRevisedAreaLessThanEqualToOldArea=true;
		paramMap.put(BPACalculatorConstants.IS_REVISED_AREA_LESS_OR_EQUAL_TO_OLD_AREA, isRevisedAreaLessThanEqualToOldArea);
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForAgricultureOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.H))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				if(isSparit)
					feeForBuildingOperation = calculateVariableFeeForSparitUlbs1(totalBuitUpArea);
				else
				feeForBuildingOperation = calculateVariableFee1(totalBuitUpArea);
			}
		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForTransportationOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.G))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				if(isSparit)
					feeForBuildingOperation = calculateConstantSparitFee(totalBuitUpArea);	
				else	
				feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);
			}
		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForEducationOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.F))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				if(isSparit) {
					feeForBuildingOperation = calculateConstantSparitFee(totalBuitUpArea);
				}else {
				feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);
				}
			}
		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForIndustrialZoneOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.E))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				if(isSparit)
					feeForBuildingOperation = calculateVariableFeeForSparitUlbs3(totalBuitUpArea);
				else
				feeForBuildingOperation = calculateVariableFee3(totalBuitUpArea);
			}
		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForPublicUtilityOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.D))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
				if(isSparit) {
					feeForBuildingOperation = calculateConstantSparitFee(totalBuitUpArea);
				}else {
				feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);
				}

			}
		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForPublicSemiPublicInstitutionalOccupancy(
			Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		String subOccupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE)) {
			subOccupancyType = (String) paramMap.get(BPACalculatorConstants.SUB_OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if (((occupancyType.equalsIgnoreCase(BPACalculatorConstants.C))) && (StringUtils.hasText(subOccupancyType))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_A))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_B))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_C))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_MP))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_CH))) {
					if(isSparit)
						feeForBuildingOperation = calculateVariableFeeForSparitUlbs2(totalBuitUpArea);
					else
					feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_O))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_OAH))) {
					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					if(isSparit) {
						feeForBuildingOperation = calculateConstantSparitFee(totalBuitUpArea);
					}else {
					feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);
					}

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
						feeForBuildingOperation = calculateVariableFeeForSparitUlbs2(totalBuitUpArea);
					else
					feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PW))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_PL))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_REB))) {
					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					if(isSparit) 
						feeForBuildingOperation = calculateConstantSparitFee(totalBuitUpArea);
					else
					feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);

				} else if ((subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_SPC))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_S))
						|| (subOccupancyType.equalsIgnoreCase(BPACalculatorConstants.C_T))) {
					if(isSparit)
						feeForBuildingOperation = calculateVariableFeeForSparitUlbs2(totalBuitUpArea);	
					else
					feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);

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

					// feeForBuildingOperation = calculateConstantFee(paramMap, 5);
					if(isSparit) 
						feeForBuildingOperation = calculateConstantSparitFee(totalBuitUpArea);
					else
					feeForBuildingOperation = calculateConstantFeeNew(totalBuitUpArea, 5);

				}
			}

		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForCommercialOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.B))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {

				if(isSparit) {
					feeForBuildingOperation = calculateVariableFeeForSparitUlbs2(totalBuitUpArea);
				}else {
				feeForBuildingOperation = calculateVariableFee2(totalBuitUpArea);
				}
			}

		}
		return feeForBuildingOperation;
	}

	/**
	 * @param paramMap
	 * @return
	 */
	private BigDecimal calculateBuildingOperationFeeForResidentialOccupancy(Map<String, Object> paramMap) {
		BigDecimal feeForBuildingOperation = BigDecimal.ZERO;
		String applicationType = null;
		String serviceType = null;
		String occupancyType = null;
		Double totalBuitUpArea = null;
		Boolean isSparit = null;
		if (null != paramMap.get(BPACalculatorConstants.APPLICATION_TYPE)) {
			applicationType = (String) paramMap.get(BPACalculatorConstants.APPLICATION_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SERVICE_TYPE)) {
			serviceType = (String) paramMap.get(BPACalculatorConstants.SERVICE_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE)) {
			occupancyType = (String) paramMap.get(BPACalculatorConstants.OCCUPANCY_TYPE);
		}
		if (null != paramMap.get(BPACalculatorConstants.SPARIT_CHECK)) {
			isSparit = (Boolean) paramMap.get(BPACalculatorConstants.SPARIT_CHECK);
			
		}
		totalBuitUpArea=getAreaParameterForBPFeesCalculation(paramMap);
		if ((occupancyType.equalsIgnoreCase(BPACalculatorConstants.A))) {
			if ((StringUtils.hasText(applicationType)
					&& applicationType.equalsIgnoreCase(BPACalculatorConstants.BUILDING_PLAN_SCRUTINY))
					&& ((StringUtils.hasText(serviceType))
							&& (serviceType.equalsIgnoreCase(BPACalculatorConstants.NEW_CONSTRUCTION)
									|| serviceType.equalsIgnoreCase(BPACalculatorConstants.SERVICE_TYPE_FOR_BPA7)))) {
				if(isSparit) {
					feeForBuildingOperation = calculateVariableFeeForSparitUlbs1(totalBuitUpArea);
					log.info("inside sparit");
				}else {
				feeForBuildingOperation = calculateVariableFee1(totalBuitUpArea);
				}
			}

		}
		return feeForBuildingOperation;
	}

	
	private static BigDecimal calculateVariableFeeForSparitUlbs1(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;		
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 100) {
				amount = ONE_HUNDRED_TWENTYFIVE; // 125
			} else if (totalBuitUpArea <= 300) { // 100-300 = 125 + (additionalSqMt * 7.5)
				double additionalSqMt = totalBuitUpArea - 100;
				amount = ONE_HUNDRED_TWENTYFIVE.add(new BigDecimal(additionalSqMt).multiply(SEVEN_POINT_FIVE));
			}
			if (totalBuitUpArea > 300) {
				double additionalSqMt = totalBuitUpArea - 300; // For >300 = 125 + (200* 7.5) + 5/sq mt
				amount = ONE_HUNDRED_TWENTYFIVE.add(FIFTEEN_HUNDRED).add(new BigDecimal(additionalSqMt).multiply(FIVE));
			}
		}		
		
		log.info("Total Built up area for Sparit: " + totalBuitUpArea);
		log.info("Building Operation Fee for Sparit: " + amount);
		return amount;
	}

	
	private BigDecimal calculateVariableFeeForSparitUlbs2(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 20) {
				amount = TWO_HUNDRED_FIFTY;
			} else if (totalBuitUpArea <= 50) { // 20-50 = (250+ 25/sqmt)
				double additionalSqMt = 50 - 20;
				amount = TWO_HUNDRED_FIFTY.add(new BigDecimal(additionalSqMt).multiply(TWENTY_FIVE));
			} else if (totalBuitUpArea > 50) { // >50 = 250+750+ 10/sqmt
				double additionalSqMt = totalBuitUpArea - 50;
				amount = THOUSAND.add(new BigDecimal(additionalSqMt).multiply(TEN));
			}

		}

		log.info("Total Built up area for Sparit: " + totalBuitUpArea);
		log.info("Building Operation Fee for Sparit: " + amount);
		return amount;

	}
	
	private BigDecimal calculateVariableFeeForSparitUlbs3(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 100) { // 0-100 = 750
				amount = SEVEN_HUNDRED_FIFTY;
			} else if (totalBuitUpArea <= 300) { // 100-300 = 750 + (12.50/sqmt)
				double additionalSqMt = totalBuitUpArea - 100;
				amount = (SEVEN_HUNDRED_FIFTY.add(new BigDecimal(additionalSqMt).multiply(TWELVE_POINT_FIVE)));
			} else if (totalBuitUpArea > 300) {
				double additionalSqMt = totalBuitUpArea - 300; // 750 + 2500 + (7.50/sqmt)
				amount = SEVEN_HUNDRED_FIFTY.add(TWENTY_FIVE_HUNDRED)
						.add(new BigDecimal(additionalSqMt).multiply(SEVEN_POINT_FIVE));
			}

		}
		
		return amount;

	}
	
	private BigDecimal calculateConstantSparitFee(Double totalBuitUpArea) {
		//2.50 is applicable here
		BigDecimal amount = BigDecimal.ZERO;
		amount = (BigDecimal.valueOf(totalBuitUpArea).multiply(new BigDecimal("2.50"))).setScale(2,
				BigDecimal.ROUND_UP);

		return amount;

	}
	/**
	 * @param totalBuitUpArea
	 * @return
	 */
	private BigDecimal calculateVariableFee1(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 100) {
				amount = TWO_HUNDRED_FIFTY;
			} else if (totalBuitUpArea <= 300) {
				amount = (TWO_HUNDRED_FIFTY
						.add(FIFTEEN.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 300) {
				amount = (TWO_HUNDRED_FIFTY.add(FIFTEEN.multiply(TWO_HUNDRED))
						.add(TEN.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(THREE_HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
	}

	/**
	 * @param totalBuitUpArea
	 * @return
	 */
	private BigDecimal calculateVariableFee2(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 20) {
				amount = FIVE_HUNDRED;
			} else if (totalBuitUpArea <= 50) {
				amount = (FIVE_HUNDRED.add(FIFTY.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(TWENTY))))
						.setScale(2, BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 50) {
				amount = (FIVE_HUNDRED.add(FIFTY.multiply(THIRTY))
						.add(TWENTY.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(FIFTY)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
	}

	/**
	 * @param totalBuitUpArea
	 * @return
	 */
	private BigDecimal calculateVariableFee3(Double totalBuitUpArea) {
		BigDecimal amount = BigDecimal.ZERO;
		if (null != totalBuitUpArea) {
			if (totalBuitUpArea <= 100) {
				amount = FIFTEEN_HUNDRED;
			} else if (totalBuitUpArea <= 300) {
				amount = (FIFTEEN_HUNDRED
						.add(TWENTY_FIVE.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			} else if (totalBuitUpArea > 300) {
				amount = (FIFTEEN_HUNDRED.add(TWENTY_FIVE.multiply(TWO_HUNDRED))
						.add(FIFTEEN.multiply(BigDecimal.valueOf(totalBuitUpArea).subtract(THREE_HUNDRED)))).setScale(2,
								BigDecimal.ROUND_UP);
			}

		}
		return amount;
	}

	/**
	 * @param paramMap
	 * @param multiplicationFactor
	 * @return
	 */
	@SuppressWarnings("unused")
	private BigDecimal calculateConstantFee(Map<String, Object> paramMap, int multiplicationFactor) {
		BigDecimal totalAmount = BigDecimal.ZERO;
		Double plotArea = null;
		Double totalBuitUpArea = null;
		if (null != paramMap.get(BPACalculatorConstants.PLOT_AREA)) {
			plotArea = (Double) paramMap.get(BPACalculatorConstants.PLOT_AREA);
		}
		if (null != paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA)) {
			totalBuitUpArea = (Double) paramMap.get(BPACalculatorConstants.TOTAL_FLOOR_AREA);

		}
		if ((null != paramMap.get(BPACalculatorConstants.AREA_TYPE))
				&& (paramMap.get(BPACalculatorConstants.AREA_TYPE).equals(BPACalculatorConstants.AREA_TYPE_PLOT))) {

			totalAmount = (BigDecimal.valueOf(plotArea).multiply(BigDecimal.valueOf(multiplicationFactor))).setScale(2,
					BigDecimal.ROUND_UP);

		} else {
			totalAmount = (BigDecimal.valueOf(totalBuitUpArea).multiply(BigDecimal.valueOf(multiplicationFactor)))
					.setScale(2, BigDecimal.ROUND_UP);
		}

		return totalAmount;
	}

	/**
	 * @param effectiveArea
	 * @param multiplicationFactor
	 * @return
	 */
	private BigDecimal calculateConstantFeeNew(Double effectiveArea, int multiplicationFactor) {
		BigDecimal totalAmount = BigDecimal.ZERO;
		if (null != effectiveArea) {
			totalAmount = (BigDecimal.valueOf(effectiveArea).multiply(BigDecimal.valueOf(multiplicationFactor)))
					.setScale(2, BigDecimal.ROUND_UP);

		}
		return totalAmount;
	}

	private void generateTaxHeadEstimate(ArrayList<TaxHeadEstimate> estimates, BigDecimal feeAmount, String taxHeadCode,
			Category category) {
		TaxHeadEstimate estimate = new TaxHeadEstimate();
		estimate.setEstimateAmount(feeAmount.setScale(0, BigDecimal.ROUND_UP));
		estimate.setCategory(category);
		estimate.setTaxHeadCode(taxHeadCode);
		estimates.add(estimate);
	}
	
	private Double getAreaParameterForBPFeesCalculation(Map<String, Object> paramMap) {
		//String applicableAreaParameterName = BPACalculatorConstants.TOTAL_FLOOR_AREA;
		String applicableAreaParameterName = BPACalculatorConstants.TOTAL_BUILTUP_AREA_EDCR;
		log.info(applicableAreaParameterName);
		return null != paramMap.get(applicableAreaParameterName)?(Double) paramMap.get(applicableAreaParameterName):null;
	}
	
	private Double getDeviationBUAForOCFeesCalculation(Map<String, Object> paramMap) {
		String applicableAreaParameterName = BPACalculatorConstants.DEVIATION_BUILTUP_AREA;
		return null != paramMap.get(applicableAreaParameterName)?(Double) paramMap.get(applicableAreaParameterName):null;
	}

	/*
	 * public BigDecimal calculateTotalFeeAmountDuplicate(Map<String, Object>
	 * paramMap) { return calculateTotalFeeAmount(paramMap);
	 * 
	 * }
	 */
	
	private void setBpaFromDBForCalculation(CalculationReq calculationReq) {
		log.info("inside method setBpaFromDBForCalculation");
		if (Objects.nonNull(calculationReq.getCalulationCriteria().get(0).getBpa())
				&& StringUtils.isEmpty(calculationReq.getCalulationCriteria().get(0).getBpa().getRiskType())
				&& StringUtils.isEmpty(calculationReq.getCalulationCriteria().get(0).getRiskType())) {
			throw new CustomException("riskType must be present either in CalculationCriteria or in BPA",
					"riskType must be present either in CalculationCriteria or in BPA");
		} else if (Objects.isNull(calculationReq.getCalulationCriteria().get(0).getBpa())
				&& StringUtils.isEmpty(calculationReq.getCalulationCriteria().get(0).getRiskType())) {
			throw new CustomException("riskType is mandatory", "riskType is mandatory");
		} 
		if (Objects.nonNull(calculationReq.getCalulationCriteria().get(0).getBpa())) {
			log.info("BPA already present in calculationCriteria");
		} else {
			log.info("fetching bpa from db for applicationNo:"
					+ calculationReq.getCalulationCriteria().get(0).getApplicationNo());
			BPA bpa = bpaService.getBuildingPlan(calculationReq.getRequestInfo(),
					calculationReq.getCalulationCriteria().get(0).getTenantId(),
					calculationReq.getCalulationCriteria().get(0).getApplicationNo(), null);
			calculationReq.getCalulationCriteria().get(0).setBpa(bpa);
		}
		setRiskType(calculationReq);
	}
	
	private void setRiskType(CalculationReq calculationReq) {
		String riskTypeFromBpa = calculationReq.getCalulationCriteria().get(0).getBpa().getRiskType();
		String riskTypeFromCalculationCriteria = calculationReq.getCalulationCriteria().get(0).getRiskType();
		if (StringUtils.isEmpty(riskTypeFromBpa) && !StringUtils.isEmpty(riskTypeFromCalculationCriteria)) {
			calculationReq.getCalulationCriteria().get(0).getBpa().setRiskType(riskTypeFromCalculationCriteria);
		} else if (StringUtils.isEmpty(riskTypeFromCalculationCriteria) && !StringUtils.isEmpty(riskTypeFromBpa)) {
			calculationReq.getCalulationCriteria().get(0).setRiskType(riskTypeFromBpa);
		}
	}

	private void enrichOCSearchOutsideDetailsFromDB(BPA bpa) {

		List<ScrutinyDetails> scrutinyDetails=ocOutsideRepository.getScrutinyDetails(bpa);
		if(StringUtils.isEmpty(scrutinyDetails)){
			throw new CustomException("EDCR_DETAIL_NOT_FOUND","Edcr Detail is not found");
		}
		OutsideOCDetails outsideOCDetails = OutsideOCDetails.builder().scrutinyDetails(scrutinyDetails).build();
		bpa.setOutsideOCDetails(outsideOCDetails);


	}
	
	private Boolean isOutsideSujogApplExcemptionEstimate(Map<String, Object> paramMap,
			ArrayList<TaxHeadEstimate> estimates) {
		Boolean isOutsideSujogAppl = Boolean.FALSE;
		Boolean isRefApplicationPresentInSujog = Boolean.FALSE;
		BPA bpa = null;
		if (null != paramMap.get("BPA")) {
			bpa = (BPA) paramMap.get("BPA");
		}

		if (Objects.nonNull(paramMap.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG))) {
			isRefApplicationPresentInSujog = (Boolean) paramMap
					.get(BPACalculatorConstants.IS_REF_APPLICATION_PRESENT_IN_SUJOG);
		}

		if (!isRefApplicationPresentInSujog) {
			if (Objects.nonNull(bpa) && Objects.nonNull(bpa.getAdditionalDetails()) && !StringUtils.isEmpty(
					((Map) bpa.getAdditionalDetails()).get(BPACalculatorConstants.BPA_ADD_DETAILS_OUTSIDE_SUJOG_APPL))) {
				isOutsideSujogAppl = Boolean.TRUE;
			}

		}
		return isOutsideSujogAppl;
	}
	
	
	private List<ExemptionFeeDetails> getAllExemptionFeesDetails(Object object, String taxHead) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		try {
			String jsonString = mapper.writeValueAsString(object);
			List<ExemptionFeeDetails> allExemptionFeeDetails = mapper.readValue(jsonString, new TypeReference<List<ExemptionFeeDetails>>(){});
			
			allExemptionFeeDetails = allExemptionFeeDetails.stream()
					.filter(exemptionFee -> exemptionFee.getFeeType().equalsIgnoreCase(taxHead))
					.collect(Collectors.toList());
			
			return allExemptionFeeDetails;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}
	
	private Double addBuiltUpAreaForPublicWashroom(Double builtUpArea, Map<String, Object> paramMap,
			Occupancy occupancy) {

		if (paramMap.containsKey(BPACalculatorConstants.IS_PUBLIC_WASHROOM_PRESENT)) {

			Double publicWashroomBuiltUpArea = (Double) paramMap
					.get(BPACalculatorConstants.PUBLIC_WASHROOM_BUILTUP_AREA);
			log.info("public washroom area added in subOccupancy.."
					+ occupancy.getTypeHelper().getOccupancySubType().getName());
			builtUpArea = builtUpArea + publicWashroomBuiltUpArea;

		}
		return builtUpArea;
	}
	
	private Double addBuiltUpAreaForICT(Double builtUpArea, Map<String, Object> paramMap,
			Occupancy occupancy) {

		if (paramMap.containsKey(BPACalculatorConstants.IS_ICT_PRESENT)) {

			Double ICTBuiltUpArea = (Double) paramMap
					.get(BPACalculatorConstants.ICT_BUILTUP_AREA);
			log.info("ict area added in subOccupancy.."
					+ occupancy.getTypeHelper().getOccupancySubType().getName());
			builtUpArea = builtUpArea + ICTBuiltUpArea;

		}
		return builtUpArea;
	}
	
	private String extractSubOccupancyCode(Map<String, Object> occupancyData) {
        String subOccupancyCode = (String) occupancyData.get("subOccupancyCode");
        log.info("Extracted subOccupancyCode: {}", subOccupancyCode);
        return subOccupancyCode;
    }

    private double extractDoubleValue(Map<String, Object> data, String key) {
        if (data.get(key) == null) {
            log.warn("Key {} is null in data: {}", key, data);
            return 0.0;
        }

        try {
            double value = Double.valueOf(data.get(key).toString());
            log.info("Extracted value for {}: {}", key, value);
            return value;
        } catch (NumberFormatException e) {
            log.error("Invalid value for key {} in data: {}", key, data, e);
            return 0.0;
        }
    }

    private void aggregateOccupancy(Occupancy existingOccupancy, double floorArea, double builtUpArea) {
        existingOccupancy.setFloorArea(existingOccupancy.getFloorArea() + floorArea);
        existingOccupancy.setBuiltUpArea(existingOccupancy.getBuiltUpArea() + builtUpArea);
    }

    private Occupancy createNewOccupancy(Map<String, Object> occupancyData, String subOccupancyCode, double floorArea, double builtUpArea) {
        Occupancy newOccupancy = new Occupancy();
        newOccupancy.setSubOccupancyCode(subOccupancyCode);
        newOccupancy.setOccupancyCode((String) occupancyData.get("OccupancyCode"));
        newOccupancy.setFloorArea(floorArea);
        newOccupancy.setBuiltUpArea(builtUpArea);
        return newOccupancy;
    }

	private List<Occupancy> groupOccupancies(List<Map<String, Object>> occupancyList) {
		List<Occupancy> ocL;
		Map<String, Occupancy> groupedOccupancies = new HashMap<>();

        for (Map<String, Object> occupancyData : occupancyList) {
            try {
                if (occupancyData == null) {
                    log.warn("Null occupancyData found in the list, skipping...");
                    continue;
                }

                String subOccupancyCode = extractSubOccupancyCode(occupancyData);
                if (subOccupancyCode == null) {
                    log.warn("SubOccupancyCode is null for occupancyData: {}", occupancyData);
                    continue;
                }

                double floorArea = extractDoubleValue(occupancyData, "floorArea");
                double builtUpArea = extractDoubleValue(occupancyData, "builtUpArea");

                if (groupedOccupancies.containsKey(subOccupancyCode)) {
                    log.info("Aggregating values for subOccupancyCode: {}", subOccupancyCode);
                    aggregateOccupancy(groupedOccupancies.get(subOccupancyCode), floorArea, builtUpArea);
                } else {
                    log.info("Creating new Occupancy for subOccupancyCode: {}", subOccupancyCode);
                    Occupancy newOccupancy = createNewOccupancy(occupancyData, subOccupancyCode, floorArea, builtUpArea);
                    groupedOccupancies.put(subOccupancyCode, newOccupancy);
                }
            } catch (Exception e) {
                log.error("Error processing occupancy data: {}", occupancyData, e);
                throw new CustomException("Occupancy Data Error", "Error processing occupancy data: " + e.getMessage());
            }
        }

        log.info("Final grouped Occupancies count: {}", groupedOccupancies.size());
        ocL = new ArrayList<>(groupedOccupancies.values());
		return ocL;
	}

	
}
