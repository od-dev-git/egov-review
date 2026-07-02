package org.egov.bpa.service.issuefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.bpa.repository.RegularizationIssueFixRepository;
import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.collection.Demand;
import org.egov.bpa.web.model.collection.DemandDetail;
import org.egov.bpa.web.model.collection.DemandSearchCriteria;
import org.egov.bpa.web.model.collection.Payment;
import org.egov.bpa.web.model.collection.PaymentSearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.web.model.workflow.WorkFlowSearchCriteria;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service("regSancFeePaymentIssueFixService")
@Slf4j
public class RegularizationSancFeePaymentIssueFix implements RegularizationIIssueFixService {

	@Autowired
	private IssueFixValidator validator;

	@Autowired
	private RegularizationIssueFixRepository repository;

	@Autowired
	private RegularizationRepository regularizationRepository;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		validator.validatePaymentIssueRequest(issueFixRequest);
		String applicationNo = issueFixRequest.getIssueFix().getApplicationNo();
		String tenantId = issueFixRequest.getIssueFix().getTenantId();

		Map<String, Boolean> isDataUpdateNeeded = new HashMap<>();

		RegularizationSearchCriteria searchCriteria = RegularizationSearchCriteria.builder()
				.tenantId(tenantId)
				.applicationNo(applicationNo)
				.build();

		List<Regularization> regularizations = regularizationRepository.searchRegularization(searchCriteria, issueFixRequest.getRequestInfo());

		if (CollectionUtils.isEmpty(regularizations) || regularizations.size() > 1) {
			throw new CustomException("SEARCH_ERROR", "Either no or multiple applications found with the mentioned application number !!");
		}

		if (!regularizations.get(0).getStatus().equalsIgnoreCase(IssueFixConstants.PENDING_SANC_FEE)) {
			throw new CustomException("APPLICATION_STATUS_ERROR", "Application is not at Sanction Fee Pending Payment Stage. Kindly Check !!");
		}

		Payment payment = checkIfPaymentReceived(issueFixRequest);

		Demand demandToBeUpdated = checkIfDemandUpdatedProperly(issueFixRequest, isDataUpdateNeeded);

		ProcessInstance processInstance = checkIfWorkFlowUpdatedProperly(issueFixRequest, isDataUpdateNeeded);

		Regularization applicationToBeUpdated = checkIfApplicationUpdatedProperly(issueFixRequest, regularizations.get(0), isDataUpdateNeeded);

		updateDataIfNeeded(demandToBeUpdated, processInstance, applicationToBeUpdated, payment, isDataUpdateNeeded, issueFixRequest.getRequestInfo());

		return issueFixRequest.getIssueFix();
	}

	/**
	 * Execute the update queries here
	 * 
	 * @param isDataUpdateNeeded
	 * @param connectionToBeUpdated
	 * @param demandToBeUpdated
	 * @param processInstance
	 */
	@Transactional
	private void updateDataIfNeeded(Demand demandToBeUpdated, ProcessInstance processInstance,
			Regularization applicationToBeUpdated, Payment payment, Map<String, Boolean> isDataUpdateNeeded, RequestInfo requestInfo) {

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DETAILS_UPDATE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DETAILS_UPDATE_NEEDED)) {
			demandToBeUpdated.getDemandDetails().forEach(demandDetail -> {
				repository.updateDemandDetail(demandDetail);
			});
		}
		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_UPDATE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_UPDATE_NEEDED)) {
			repository.updateDemand(demandToBeUpdated);
		}

		if ((isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DETAILS_UPDATE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DETAILS_UPDATE_NEEDED))
				|| (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_UPDATE_NEEDED)
						&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_UPDATE_NEEDED))) {

			repository.expireBill(demandToBeUpdated.getConsumerCode());

		}

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED)) {
			repository.updateApplicationForSancFeeIssueFix(applicationToBeUpdated, payment, requestInfo);
		}
		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_WORKFLOW_UPDATE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_WORKFLOW_UPDATE_NEEDED)) {
			repository.updateWorkflowForSancFee(processInstance, payment);
		}

	}

	/**
	 * check if the application is updated properly, if not update the application
	 * 
	 * @param issueFixRequest
	 * @param regularization
	 * @param isDataUpdateNeeded
	 */
	private Regularization checkIfApplicationUpdatedProperly(IssueFixRequest issueFixRequest, Regularization regularization,
			Map<String, Boolean> isDataUpdateNeeded) {

		if (regularization.getStatus().equalsIgnoreCase(IssueFixConstants.APPROVED)) {
			log.info(" Application state already updated.. No Changes done in Application status...");
		} else {
			isDataUpdateNeeded.put(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED, true);
		}
		return regularization;
	}

	/**
	 * Check if Pay action inserted in workflow, if not insert pay from this method
	 * 
	 * @param issueFixRequest
	 * @param isDataUpdateNeeded
	 */
	private ProcessInstance checkIfWorkFlowUpdatedProperly(IssueFixRequest issueFixRequest,
			Map<String, Boolean> isDataUpdateNeeded) {

		String tenantId = issueFixRequest.getIssueFix().getTenantId();

		String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

		WorkFlowSearchCriteria workFlowSearchCriteria = WorkFlowSearchCriteria.builder().businessId(applicationNumber)
				.tenantId(tenantId).build();

		List<ProcessInstance> processInstances = repository.getProcessInstances(workFlowSearchCriteria);

		if (CollectionUtils.isEmpty(processInstances)) {
			throw new CustomException("WORKFLOW_ISSUE", "Found No Workflow records for mentioned Application Number !!");
		}

		ProcessInstance lastestProcessInstance = processInstances.get(0);

		if (lastestProcessInstance.getAction().equalsIgnoreCase(IssueFixConstants.PAY)) {
			log.info("Workflow is already updated.. Pay Action is already there... Moving to next step...");
		} else {
			isDataUpdateNeeded.put(IssueFixConstants.IS_WORKFLOW_UPDATE_NEEDED, true);
		}

		List<ProcessInstance> processInstanceForApply = processInstances.stream()
				.filter(process -> process.getAction().equalsIgnoreCase(IssueFixConstants.APPROVE))
				.map(process -> process).collect(Collectors.toList());

		return processInstanceForApply.get(0);
	}

	/**
	 * check weather is payment completed flag is updated or not
	 * 
	 * @param issueFixRequest
	 * @param isDataUpdateNeeded
	 */
	private Demand checkIfDemandUpdatedProperly(IssueFixRequest issueFixRequest,
			Map<String, Boolean> isDataUpdateNeeded) {

		String tenantId = issueFixRequest.getIssueFix().getTenantId();
		String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

		DemandSearchCriteria demandSearchCriteria = DemandSearchCriteria.builder()
				.consumerCode(applicationNumber)
				.businessService(IssueFixConstants.REG_SAN_FEE)
				.tenantId(tenantId)
				.build();

		// Search demand here
		List<Demand> demands = repository.getDemands(demandSearchCriteria);

		if (CollectionUtils.isEmpty(demands) || demands.size() > 1) {
			throw new CustomException("DEMAND_ISSUE", "Found No or Multiple Demans for mentioned Application Number. Kindly revalidate the Application State !!");
		}

		// Start Checking if the demand is updated properly, if not update the demand
		Demand demand = demands.get(0);
		List<DemandDetail> demandDetails = demand.getDemandDetails();

		for (DemandDetail demandDetail : demandDetails) {
			if (demandDetail.getTaxAmount().compareTo(demandDetail.getCollectionAmount()) == 0) {
				log.info("Demand Details are already updated for .. " + demandDetail.getTaxHeadMasterCode()
						+ " Moving to next step...");
			} else {
				isDataUpdateNeeded.put(IssueFixConstants.IS_DEMAND_DETAILS_UPDATE_NEEDED, true);
			}
		}

		if (!demand.getIsPaymentCompleted()) {
			isDataUpdateNeeded.put(IssueFixConstants.IS_DEMAND_UPDATE_NEEDED, true);
		} else {
			log.info("Is PaymentCompleted Flag already updated.. Moving to next step...");
		}
		return demand;
	}

	
	/**
	 * check weather payment is done for mentioned application
	 * 
	 * @param issueFixRequest
	 */
	private Payment checkIfPaymentReceived(IssueFixRequest issueFixRequest) {

		String tenantId = issueFixRequest.getIssueFix().getTenantId();
		String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

		PaymentSearchCriteria paymentSearchCriteria = PaymentSearchCriteria.builder()
				.consumerCode(applicationNumber)
				.businessService(IssueFixConstants.REG_SAN_FEE)
				.tenantId(tenantId)
				.build();

		List<Payment> payments = repository.getPayments(paymentSearchCriteria);

		if (CollectionUtils.isEmpty(payments) || payments.size() > 1) {
			throw new CustomException("PAYMENT_ISSUE", "Found No or Multiple Payments for mentioned Application Number. Kindly revalidate the payments !!");
		}

		log.info("Payment received.. Moving to next step !!");

		return payments.get(0);
	}
	
}
