package org.egov.bpa.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.producer.Producer;
import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.DemolitionRepository;
import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.BPAErrorConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPARequest;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.Installment;
import org.egov.bpa.web.model.InstallmentRequest;
import org.egov.bpa.web.model.InstallmentSearchCriteria;
import org.egov.bpa.web.model.InstallmentSearchResponse;
import org.egov.bpa.web.model.Workflow;
import org.egov.bpa.web.model.collection.PaymentDetail;
import org.egov.bpa.web.model.collection.PaymentRequest;
import org.egov.bpa.web.model.demolition.Demolition;
import org.egov.bpa.web.model.demolition.DemolitionRequest;
import org.egov.bpa.web.model.demolition.DemolitionSearchCriteria;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationRequest;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.bpa.web.model.workflow.Action;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.workflow.WorkflowIntegrator;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentUpdateService {

	private BPAConfiguration config;

	private BPARepository repository;

	private WorkflowIntegrator wfIntegrator;

	private EnrichmentService enrichmentService;

	private ObjectMapper mapper;

	private CalculationService calculationService;

	private Producer producer;

	private BPAAutoEscalationService bpaAutoEscalationService;

	private WorkflowService workflowService;

	private RegularizationRepository regularizationRepository;

	private RegularizationEnrichmentService regularizationEnrichmentService;

	private DemolitionRepository demolitionRepository;

	private DemolitionWorkflowService demolitionWorkflowService;

	private DemolitionEnrichmentService demolitionEnrichmentService;

	@Autowired
	public PaymentUpdateService(BPAConfiguration config, BPARepository repository, WorkflowIntegrator wfIntegrator,
			EnrichmentService enrichmentService, ObjectMapper mapper, CalculationService calculationService,
			Producer producer, BPAAutoEscalationService bpaAutoEscalationService, WorkflowService workflowService,
			RegularizationRepository regularizationRepository,
			RegularizationEnrichmentService regularizationEnrichmentService, DemolitionRepository demolitionRepository,
			DemolitionWorkflowService demolitionWorkflowService,
			DemolitionEnrichmentService demolitionEnrichmentService) {
		this.config = config;
		this.repository = repository;
		this.wfIntegrator = wfIntegrator;
		this.enrichmentService = enrichmentService;
		this.mapper = mapper;
		this.calculationService = calculationService;
		this.producer = producer;
		this.bpaAutoEscalationService = bpaAutoEscalationService;
		this.workflowService = workflowService;
		this.regularizationRepository = regularizationRepository;
		this.regularizationEnrichmentService = regularizationEnrichmentService;
		this.demolitionRepository = demolitionRepository;
		this.demolitionWorkflowService = demolitionWorkflowService;
		this.demolitionEnrichmentService = demolitionEnrichmentService;
	}

	final String tenantId = "tenantId";

	final String businessService = "businessService";

	final String consumerCode = "consumerCode";

	/**
	 * Process the message from kafka and updates the status to paid.
	 *
	 * <p>
	 * Flow per payment detail:
	 * <ol>
	 * <li>Route to Regularization, Demolition, or BPA based on business
	 * service.</li>
	 * <li>Update installments if applicable.</li>
	 * <li>Advance workflow and persist updates.</li>
	 * </ol>
	 *
	 * @param record the incoming message from receipt create consumer
	 */
	public void process(HashMap<String, Object> record) {

		try {
			PaymentRequest paymentRequest = mapper.convertValue(record, PaymentRequest.class);
			RequestInfo requestInfo = paymentRequest.getRequestInfo();
			List<PaymentDetail> paymentDetails = paymentRequest.getPayment().getPaymentDetails();
			String tenantId = paymentRequest.getPayment().getTenantId();

			Set<String> regularizationServices = parseConfiguredServices(config.getRegularizationBusinessService());
			Set<String> demolitionServices = parseConfiguredServices(config.getDemolitionBusinessService());
			Set<String> bpaServices = parseConfiguredServices(config.getBusinessService());

			for (PaymentDetail paymentDetail : paymentDetails) {
				// Update Payment Done for Regularization
				if (handleRegularizationPayment(paymentDetail, requestInfo, tenantId, regularizationServices)) {
					return;
				}

				// Update Payment Done for Demolition
				if (handleDemolitionPayment(paymentDetail, requestInfo, tenantId, demolitionServices)) {
					return;
				}

				// Update Payment Done for BPA
				if (handleBpaPayment(paymentDetail, requestInfo, tenantId, bpaServices)) {
					return;
				}
			}
		} catch (Exception e) {
			log.error("KAFKA_PROCESS_ERROR:", e);
		}
	}

	/**
	 * Parses a comma-separated business service list into a set.
	 *
	 * @param configValue comma-separated list from config
	 * @return set of configured business service codes
	 */
	private Set<String> parseConfiguredServices(String configValue) {
		Set<String> services = new HashSet<>();
		if (!StringUtils.hasText(configValue)) {
			return services;
		}
		services.addAll(Arrays.stream(configValue.split(",")).map(String::trim).filter(StringUtils::hasText)
				.collect(Collectors.toSet()));
		return services;
	}

	/**
	 * Handles payment update for regularization applications when business service
	 * matches.
	 *
	 * @return true if handled (flow should stop), false otherwise
	 */
	private boolean handleRegularizationPayment(PaymentDetail paymentDetail, RequestInfo requestInfo, String tenantId,
			Set<String> regularizationServices) {
		if (!regularizationServices.contains(paymentDetail.getBusinessService())) {
			return false;
		}
		updateRegularizationPayment(paymentDetail, requestInfo, tenantId);
		return true;
	}

	/**
	 * Handles payment update for demolition applications when business service
	 * matches.
	 *
	 * @return true if handled (flow should stop), false otherwise
	 */
	private boolean handleDemolitionPayment(PaymentDetail paymentDetail, RequestInfo requestInfo, String tenantId,
			Set<String> demolitionServices) {
		if (!demolitionServices.contains(paymentDetail.getBusinessService())) {
			return false;
		}
		updateDemolitionPayment(paymentDetail, requestInfo, tenantId);
		return true;
	}

	/**
	 * Handles payment update for BPA applications when business service matches.
	 *
	 * @return true if handled (flow should stop), false otherwise
	 */
	private boolean handleBpaPayment(PaymentDetail paymentDetail, RequestInfo requestInfo, String tenantId,
			Set<String> bpaServices) {
		if (!bpaServices.contains(paymentDetail.getBusinessService())) {
			return false;
		}

		BPASearchCriteria searchCriteria = new BPASearchCriteria();
		searchCriteria.setTenantId(tenantId);
		searchCriteria.setApplicationNo(paymentDetail.getBill().getConsumerCode());

		List<BPA> bpas = repository.getBPAData(searchCriteria, null);
		if (CollectionUtils.isEmpty(bpas)) {
			throw new CustomException(BPAErrorConstants.INVALID_RECEIPT,
					"No Building Plan Application found for the comsumerCode " + searchCriteria.getApplicationNo());
		}

		tryUpdateInstallmentsIfNeeded(paymentDetail, requestInfo, bpas.get(0));

		Workflow workflow = Workflow.builder().action("PAY").build();
		bpas.forEach(bpa -> bpa.setWorkflow(workflow));

		// Add system roles to allow payment workflow transition
		addSystemPaymentRoles(requestInfo, bpas.get(0).getTenantId());

		if (!bpas.get(0).getBusinessService().equalsIgnoreCase(BPAConstants.BPA_AC_MODULE_CODE)) {
			BusinessService businessService = bpaAutoEscalationService
					.getBusinessService(bpas.get(0).getBusinessService(), tenantId, requestInfo);
			assignToDocVerifierIfNeeded(bpas, businessService, requestInfo);
		}

		BPARequest updateRequest = BPARequest.builder().requestInfo(requestInfo).BPA(bpas.get(0)).build();

		// Call workflow to update status
		wfIntegrator.callWorkFlow(updateRequest);
		log.debug(" the status of the application is : " + updateRequest.getBPA().getStatus());

		// Enrich and persist after workflow update
		enrichmentService.postStatusEnrichment(updateRequest);
		repository.update(updateRequest, false);

		return true;
	}

	/**
	 * Adds system roles required for payment workflow transitions.
	 *
	 * @param requestInfo the request info to enrich with roles
	 * @param tenantId    tenant id for role scoping
	 */
	private void addSystemPaymentRoles(RequestInfo requestInfo, String tenantId) {
		Role role = Role.builder().code("SYSTEM_PAYMENT").tenantId(tenantId).build();
		requestInfo.getUserInfo().getRoles().add(role);
		role = Role.builder().code("CITIZEN").tenantId(tenantId).build();
		requestInfo.getUserInfo().getRoles().add(role);
	}

	/**
	 * Updates installments if payment is against installment demands.
	 *
	 * @param paymentDetail  payment details from collection
	 * @param requestInfo    request metadata
	 * @param bpaApplication the BPA application to check for installment payments
	 */
	private void tryUpdateInstallmentsIfNeeded(PaymentDetail paymentDetail, RequestInfo requestInfo,
			BPA bpaApplication) {
		// Installment update for payment status
		try {
			// Assumption: payment for only one BPA application is done at a time.
			InstallmentSearchResponse installmentResponse = fetchAllInstallments(bpaApplication.getApplicationNo(),
					requestInfo);
			if (isPaymentForInstallment(installmentResponse)) {
				findAndUpdateInstallments(paymentDetail, installmentResponse);

				// Return if second time payment by installment
				if (!StringUtils.isEmpty(bpaApplication.getApprovalNo())
						&& bpaApplication.getStatus().equalsIgnoreCase(BPAConstants.APPROVED_STATE)) {
					return;
				}
			}
		} catch (Exception ex) {
			log.error("error inside method process method while checking for installment", ex);
		}
	}

	private void assignToDocVerifierIfNeeded(List<BPA> bpas, BusinessService businessService, RequestInfo requestInfo) {

		bpas.forEach(bpa -> {
			if (bpa.getStatus().equals(BPAConstants.APPL_FEE_STATE)) {
				if (StringUtils.isEmpty(bpa.getWorkflow().getAssignes())) {
					List<ProcessInstance> processInstances = workflowService.getProcessInstances(bpa, requestInfo,
							false);
					List<Action> actions = processInstances.get(0).getState().getActions().stream()
							.collect(Collectors.toList());
					String actionFromBPA = bpa.getWorkflow().getAction();
					List<Action> action = actions.stream().filter(a -> a.getAction().equalsIgnoreCase(actionFromBPA))
							.collect(Collectors.toList());
					String roles = bpaAutoEscalationService.getNextValidUserUUIDByNextState(
							action.get(0).getNextState(), businessService, requestInfo);
					List<String> nextAssignees = bpaAutoEscalationService.getNextValidUserUUID(roles, bpa.getTenantId(),
							true, requestInfo);
					String assigneeKey = bpaAutoEscalationService.getRandomValue(nextAssignees);
					bpa.getWorkflow().setAssignes(Arrays.asList(assigneeKey));
				}
			}
		});

	}

	private void assignToDocVerifierIfNeededForRegularization(List<Regularization> regularizations,
			BusinessService businessService, RequestInfo requestInfo) {
		regularizations.forEach(regularization -> {
			if (regularization.getStatus().equals(BPAConstants.APPL_FEE_STATE)) {
				if (StringUtils.isEmpty(regularization.getWorkflow().getAssignes())) {
					List<ProcessInstance> processInstances = workflowService
							.getRegularizationProcessInstances(regularization, requestInfo, false);
					List<Action> actions = processInstances.get(0).getState().getActions().stream()
							.collect(Collectors.toList());
					String actionFromReg = regularization.getWorkflow().getAction();
					List<Action> action = actions.stream().filter(a -> a.getAction().equalsIgnoreCase(actionFromReg))
							.collect(Collectors.toList());
					String roles = bpaAutoEscalationService.getNextValidUserUUIDByNextState(
							action.get(0).getNextState(), businessService, requestInfo);
					List<String> nextAssignees = bpaAutoEscalationService.getNextValidUserUUID(roles,
							regularization.getTenantId(), true, requestInfo);
					String assigneeKey = bpaAutoEscalationService.getRandomValue(nextAssignees);
					regularization.getWorkflow().setAssignes(Arrays.asList(assigneeKey));
				}
			}
		});
	}

	private void findAndUpdateInstallments(PaymentDetail paymentDetail, InstallmentSearchResponse installmentResponse)
			throws JsonProcessingException {
		log.info("inside method findDemandIdAndUpdateInstallment");
		List<List<Installment>> installments = installmentResponse.getInstallments();
		List<Installment> installmentsToUpdate = new ArrayList<>();
		for (List<Installment> installmentsInOneInstallment : installments) {
			installmentsToUpdate.addAll(findInstallmentsToUpdatePerInstallment(installmentsInOneInstallment));
		}
		installmentsToUpdate.addAll(findInstallmentsToUpdatePerInstallment(installmentResponse.getFullPayment()));
		// expecting only one demandId to be present in unpaid installments-
		Set<String> demandIdsFromInstallments = installmentsToUpdate.stream()
				.map(installment -> installment.getDemandId()).collect(Collectors.toSet());
		if (demandIdsFromInstallments.size() > 1)
			throw new CustomException("multiple demandIds found for unpaid installments",
					"multiple demandIds found for unpaid installments");

		Map<String, List<Installment>> persisterMap = new HashMap<>();
		log.info("size of installments to update with payment status:" + installmentsToUpdate.size());
		persisterMap.put(BPAConstants.INSTALLMENTS_FIELD, installmentsToUpdate);
		producer.push(config.getUpdateInstallmentTopic(), persisterMap);
	}

	private List<Installment> findInstallmentsToUpdatePerInstallment(List<Installment> installmentsInOneInstallment) {
		return installmentsInOneInstallment.stream()
				.filter(installment -> !StringUtils.isEmpty(installment.getDemandId())
						&& !installment.isPaymentCompletedInDemand())
				.map(installment -> {
					installment.setPaymentCompletedInDemand(true);
					return installment;
				}).collect(Collectors.toList());
	}

	public InstallmentSearchResponse fetchAllInstallments(String consumerCode, RequestInfo requestInfo) {
		log.info("inside method fetchAllInstallments");
		InstallmentSearchCriteria criteria = new InstallmentSearchCriteria().builder().consumerCode(consumerCode)
				.build();
		InstallmentRequest installmentRequest = InstallmentRequest.builder().installmentSearchCriteria(criteria)
				.requestInfo(requestInfo).build();
		Object installmentResponse = calculationService.getAllInstallments(installmentRequest);
		InstallmentSearchResponse installmentSearchResponse = mapper.convertValue(installmentResponse,
				InstallmentSearchResponse.class);
		return installmentSearchResponse;
	}

	private boolean isPaymentForInstallment(InstallmentSearchResponse installmentResponse) {
		log.info("inside method checkIfPaymentDoneForInstallment");
		if (Objects.nonNull(installmentResponse) && !CollectionUtils.isEmpty(installmentResponse.getInstallments())) {
			List<List<Installment>> installments = installmentResponse.getInstallments();
			for (List<Installment> installmentsInOneInstallment : installments) {
				for (Installment installment : installmentsInOneInstallment) {
					// if any installment is found with demand has been generated ,it means payment
					// is for installment-
					if (!StringUtils.isEmpty(installment.getDemandId()))
						return true;
				}
			}
			log.info("installments exist but no such installment found among non -1 installments");
		}
		if (Objects.nonNull(installmentResponse) && !CollectionUtils.isEmpty(installmentResponse.getFullPayment())) {
			log.info("checking now for full payment");
			List<Installment> fullPaymentInstallments = installmentResponse.getFullPayment();
			for (Installment installment : fullPaymentInstallments) {
				// if any installment is found with demand has been generated ,it means payment
				// is for installment-
				if (!StringUtils.isEmpty(installment.getDemandId()))
					return true;
			}
		}
		log.info("returning false from method checkIfPaymentDoneForInstallment for consumercode:" + consumerCode);
		return false;
	}

	/**
	 * Updates regularization application payment state and advances workflow.
	 *
	 * @param paymentDetail payment details from collection
	 * @param requestInfo   request metadata
	 * @param tenantId      tenant identifier
	 */
	private void updateRegularizationPayment(PaymentDetail paymentDetail, RequestInfo requestInfo, String tenantId) {
		Regularization regularizationApplication = fetchRegularization(paymentDetail, requestInfo, tenantId);

		// Installment update for payment status
		if (tryUpdateInstallmentsForRegularization(paymentDetail, requestInfo, regularizationApplication)) {
			return;
		}

		applyPayActionToRegularization(regularizationApplication);

		// Add system roles to allow payment workflow transition
		addSystemPaymentRoles(requestInfo, regularizationApplication.getTenantId());

		BusinessService businessService = bpaAutoEscalationService
				.getBusinessService(regularizationApplication.getBusinessService(), tenantId, requestInfo);
		assignToDocVerifierIfNeededForRegularization(Arrays.asList(regularizationApplication), businessService,
				requestInfo);

		RegularizationRequest updateRequest = RegularizationRequest.builder().requestInfo(requestInfo)
				.regularization(regularizationApplication).build();

		// Call workflow to update status
		wfIntegrator.callWorkFlowForRegularization(updateRequest);
		log.debug(" the status of the application is : " + updateRequest.getRegularization().getStatus());

		// Enrich and persist
		regularizationEnrichmentService.postStatusEnrichment(updateRequest, null);
		regularizationRepository.update(updateRequest, false);
	}

	/**
	 * Loads a regularization application based on the consumer code.
	 */
	private Regularization fetchRegularization(PaymentDetail paymentDetail, RequestInfo requestInfo, String tenantId) {
		RegularizationSearchCriteria searchCriteria = new RegularizationSearchCriteria();
		searchCriteria.setTenantId(tenantId);
		searchCriteria.setApplicationNo(paymentDetail.getBill().getConsumerCode());
		List<Regularization> regularizations = regularizationRepository.searchRegularization(searchCriteria,
				requestInfo);
		if (CollectionUtils.isEmpty(regularizations)) {
			throw new CustomException(BPAErrorConstants.INVALID_RECEIPT,
					"No Regularization Application found for the comsumerCode " + searchCriteria.getApplicationNo());
		}
		return regularizations.get(0);
	}

	/**
	 * Updates installments for regularization payments if the demand is
	 * installment-based.
	 *
	 * @return true if this is a subsequent installment payment and flow should stop
	 */
	private boolean tryUpdateInstallmentsForRegularization(PaymentDetail paymentDetail, RequestInfo requestInfo,
			Regularization regularizationApplication) {
		try {
			// Assumption: payment for only one application is done at a time.
			InstallmentSearchResponse installmentResponse = fetchAllInstallments(
					regularizationApplication.getApplicationNo(), requestInfo);
			if (isPaymentForInstallment(installmentResponse)) {
				findAndUpdateInstallments(paymentDetail, installmentResponse);

				// Return if second time payment by installment
				if (!StringUtils.isEmpty(regularizationApplication.getApprovalNo())
						&& regularizationApplication.getStatus().equalsIgnoreCase(BPAConstants.APPROVED_STATE)) {
					return true;
				}
			}
		} catch (Exception ex) {
			log.error("error inside method process method while checking for installment", ex);
		}
		return false;
	}

	/**
	 * Applies PAY workflow action to the regularization application.
	 */
	private void applyPayActionToRegularization(Regularization regularizationApplication) {
		Workflow workflow = Workflow.builder().action("PAY").build();
		regularizationApplication.setWorkflow(workflow);
	}

	/**
	 * Updates demolition application payment state and advances workflow.
	 *
	 * @param paymentDetail payment details from collection
	 * @param requestInfo   request metadata
	 * @param tenantId      tenant identifier
	 */
	private void updateDemolitionPayment(PaymentDetail paymentDetail, RequestInfo requestInfo, String tenantId) {
		Demolition demolitionApplication = fetchDemolition(paymentDetail, tenantId);

		// Installment update for payment status
		if (tryUpdateInstallmentsForDemolition(paymentDetail, requestInfo, demolitionApplication)) {
			return;
		}

		applyPayActionToDemolition(demolitionApplication);

		// Add system roles to allow payment workflow transition
		addSystemPaymentRoles(requestInfo, demolitionApplication.getTenantId());

		DemolitionRequest updateRequest = DemolitionRequest.builder().requestInfo(requestInfo)
				.demolition(demolitionApplication).build();

		// Call workflow to update status
		demolitionWorkflowService.callWorkFlow(updateRequest);
		log.debug(" the status of the application is : " + updateRequest.getDemolition().getStatus());

		// Enrich and persist
		demolitionEnrichmentService.postStatusEnrichment(updateRequest);
		demolitionRepository.update(updateRequest);
	}

	/**
	 * Loads a demolition application based on the consumer code.
	 */
	private Demolition fetchDemolition(PaymentDetail paymentDetail, String tenantId) {
		DemolitionSearchCriteria searchCriteria = new DemolitionSearchCriteria();
		searchCriteria.setTenantId(tenantId);
		searchCriteria.setApplicationNo(paymentDetail.getBill().getConsumerCode());
		List<Demolition> demolitions = demolitionRepository.getDemolitions(searchCriteria);
		if (CollectionUtils.isEmpty(demolitions)) {
			throw new CustomException(BPAErrorConstants.INVALID_RECEIPT,
					"No Demolition Application found for the comsumerCode " + searchCriteria.getApplicationNo());
		}
		return demolitions.get(0);
	}

	/**
	 * Updates installments for demolition payments if the demand is
	 * installment-based.
	 *
	 * @return true if this is a subsequent installment payment and flow should stop
	 */
	private boolean tryUpdateInstallmentsForDemolition(PaymentDetail paymentDetail, RequestInfo requestInfo,
			Demolition demolitionApplication) {
		try {
			// Assumption: payment for only one application is done at a time.
			InstallmentSearchResponse installmentResponse = fetchAllInstallments(
					demolitionApplication.getApplicationNo(), requestInfo);
			if (isPaymentForInstallment(installmentResponse)) {
				findAndUpdateInstallments(paymentDetail, installmentResponse);

				// Return if second time payment by installment
				if (!StringUtils.isEmpty(demolitionApplication.getApprovalNo())
						&& demolitionApplication.getStatus().equalsIgnoreCase(BPAConstants.APPROVED_STATE)) {
					return true;
				}
			}
		} catch (Exception ex) {
			log.error("error inside method process method while checking for installment", ex);
		}
		return false;
	}

	/**
	 * Applies PAY workflow action to the demolition application.
	 */
	private void applyPayActionToDemolition(Demolition demolitionApplication) {
		Workflow workflow = Workflow.builder().action("PAY").build();
		demolitionApplication.setWorkflow(workflow);
	}

}