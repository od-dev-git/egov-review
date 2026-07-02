package org.egov.pt.service.issuefix;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;
import org.egov.pt.models.Property;
import org.egov.pt.models.collection.Payment;
import org.egov.pt.models.collection.PaymentSearchCriteria;
import org.egov.pt.models.issuefix.IssueFix;
import org.egov.pt.models.issuefix.IssueFixRequest;
import org.egov.pt.models.workflow.ProcessInstance;
import org.egov.pt.models.workflow.WorkFlowSearchCriteria;
import org.egov.pt.repository.IssueFixRepository;
import org.egov.pt.service.DemandService;
import org.egov.pt.util.HandleTransaction;
import org.egov.pt.util.IssueFixConstants;
import org.egov.pt.util.PTConstants;
import org.egov.pt.validator.IssueFixValidator;
import org.egov.pt.web.contracts.Demand;
import org.egov.pt.web.contracts.DemandDetail;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Service("stepBackService")
@Slf4j
public class StepBackService implements IIssueFixService {

	@Autowired
	private IssueFixValidator validator;

	@Autowired
	private IssueFixRepository repository;

	@Autowired
	private DemandService demandService;

	@Autowired
	private HandleTransaction transact;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		// Validate the input IssueFixRequest object
		validator.validateIssueFix(issueFixRequest);

		Map<String, Boolean> isDataUpdateNeeded = new HashMap<>();
		List<Property> propertyList = new ArrayList<>();

		// Validate that the application number is not empty (Mandatory field)
		if (StringUtils.isEmpty(issueFixRequest.getIssueFix().getApplicationNo())) {
			throw new CustomException("INVALID_DATA", "Application Number passed can't be empty");
		}

		// Search for the property based on the application number
		propertyList = repository.searchPropertyByAcknowledgementNoAndTenantid(issueFixRequest.getIssueFix().getApplicationNo(),issueFixRequest.getIssueFix().getTenantId());

		// Ensure that exactly one property is found with the provided
		// application number
		if (CollectionUtils.isEmpty(propertyList) || propertyList.size() > 1) {
			throw new CustomException("SEARCH_ERROR",
					"Either no or multiple applications found with the mentioned application number !!");
		}

		Property property = propertyList.get(0);

		// Check if any payment has been received
		checkIfPaymentReceived(issueFixRequest);

		ProcessInstance processInstance = checkIfWorkFlowUpdatedProperly(issueFixRequest, property, isDataUpdateNeeded);
		Property applicationToBeUpdated = checkIfApplicationUpdatedProperly(issueFixRequest, property,
				isDataUpdateNeeded);

		String creationReason = validatePropertyStatusAndCreationReason(property, processInstance);

		List<Demand> demands = new ArrayList<>();
		List<DemandDetail> demandDetails = new ArrayList<>();

		if (("MUTATION".equalsIgnoreCase(creationReason)) && processInstance.getAction().equals("APPROVE")) {
			demands = demandService.searchDemand(property.getTenantId(),
					Collections.singleton(property.getAcknowldgementNumber()), null, null,
					PTConstants.MUTATION_BUSINESSSERVICE, issueFixRequest.getRequestInfo());

			demandDetails = demands.get(0).getDemandDetails();

			checkIfDemandAndDemandDetailUpdateRequired(demands, demandDetails, isDataUpdateNeeded);
		}

		transact.updateDate(issueFixRequest, isDataUpdateNeeded, property, processInstance, applicationToBeUpdated,
				creationReason, demands, demandDetails);

		return issueFixRequest.getIssueFix();
	}

	public String validatePropertyStatusAndCreationReason(Property property, ProcessInstance processInstance) {
		if (property.getStatus().name().equalsIgnoreCase(IssueFixConstants.STATUS_ACTIVE)) {
			throw new CustomException("APPLICATION_STATUS_ERROR",
					"Application is at ACTIVE Stage. Can't revert to 1 step back !!");
		}

		// Ensure that only specific creation reasons are allowed
		String creationReason = property.getCreationReason().toString();
		if (!IssueFixConstants.ALLOWED_CREATION_REASONS.contains(creationReason)) {
			throw new CustomException("INVALID_CREATION_REASON",
					"Only 'CREATE', 'UPDATE', and 'MUTATE' creation reasons are allowed.");
		}

		// Perform further checks based on creation reason and status
		if ("CREATE".equalsIgnoreCase(creationReason) || "UPDATE".equalsIgnoreCase(creationReason)) {
			if (!IssueFixConstants.ALLOWED_ACTION_CREATE_UPDATE.contains(processInstance.getAction())) {
				throw new CustomException("STATUS_ERROR", "Invalid status for 'CREATE' or 'UPDATE'.");
			}
		} else if ("MUTATION".equalsIgnoreCase(creationReason)) {
			if (!IssueFixConstants.ALLOWED_ACTION_MUTATE.contains(processInstance.getAction())) {
				throw new CustomException("STATUS_ERROR", "Invalid status for 'MUTATE'.");
			}

		}
		return creationReason;
	}

	private void checkIfDemandAndDemandDetailUpdateRequired(List<Demand> demands, List<DemandDetail> demandDetails,
			Map<String, Boolean> isDataUpdateNeeded) {

		// Null checks for demands and demandDetails
		if (demands == null || demands.isEmpty()) {
			throw new CustomException("INVALID_DEMAND", "Demand list cannot be null or empty.");
		}

		if (demandDetails == null || demandDetails.isEmpty()) {
			throw new CustomException("INVALID_DEMAND_DETAIL", "Demand details list cannot be null or empty.");
		}

		// Check if the demands list has exactly one demand
		if (demands.size() != 1) {
			throw new CustomException("INVALID_DEMAND_COUNT", "There should be exactly one demand for this operation.");
		}

		Demand demand = demands.get(0);

		// Null check for isPaymentCompleted and ensure it's false
		if (demand.getIsPaymentCompleted() == null || demand.getIsPaymentCompleted()) {
			throw new CustomException("INVALID_PAYMENT_STATUS", "Payment should not be completed or null.");
		}

		// Check if the demand details list size is not more than 3
		if (demandDetails.size() > 3) {
			throw new CustomException("INVALID_DEMAND_DETAIL_COUNT", "Demand details size should not exceed 3.");
		}

		// Iterate over each demand detail and check if the collection amount is zero
		for (DemandDetail demandDetail : demandDetails) {
			if (demandDetail.getCollectionAmount() == null
					|| demandDetail.getCollectionAmount().compareTo(BigDecimal.ZERO) != 0) {
				throw new CustomException("INVALID_COLLECTION_AMOUNT",
						"Demand detail collection amount should be zero and not null.");
			}
		}

		// If all conditions are met, set the flags in isDataUpdateNeeded
		isDataUpdateNeeded.put(IssueFixConstants.IS_DEMAND_UPDATE_NEEDED, true);
		isDataUpdateNeeded.put(IssueFixConstants.IS_DEMAND_DETAILS_UPDATE_NEEDED, true);
		isDataUpdateNeeded.put(IssueFixConstants.IS_BILL_EXPIRATION_NEEDED, true);

	}

	/**
	 * check if the application is updated properly, if not update the application
	 * 
	 * @param issueFixRequest
	 * @param property
	 * @param isDataUpdateNeeded
	 */
	private Property checkIfApplicationUpdatedProperly(IssueFixRequest issueFixRequest, Property property,
			Map<String, Boolean> isDataUpdateNeeded) {

		if (property.getStatus().name().equalsIgnoreCase(IssueFixConstants.STATUS_INWORKFLOW)) {
			isDataUpdateNeeded.put(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED, true);
		}

		return property;
	}

	/**
	 * Check if Pay action inserted in workflow, if not insert pay from this method
	 * 
	 * @param issueFixRequest
	 * @param isDataUpdateNeeded
	 */
	private ProcessInstance checkIfWorkFlowUpdatedProperly(IssueFixRequest issueFixRequest, Property property,
			Map<String, Boolean> isDataUpdateNeeded) {
		ProcessInstance lastestProcessInstance = new ProcessInstance();
		ProcessInstance previousProcessInstance = new ProcessInstance();

		String tenantId = issueFixRequest.getIssueFix().getTenantId();
		String acknowledgementNo = property.getAcknowldgementNumber();
		WorkFlowSearchCriteria workFlowSearchCriteria = WorkFlowSearchCriteria.builder().businessId(acknowledgementNo)
				.tenantId(tenantId).build();

		List<ProcessInstance> processInstances = repository.getProcessInstances(workFlowSearchCriteria);

		if (!CollectionUtils.isEmpty(processInstances)) {
			lastestProcessInstance = processInstances.get(0);
			if (processInstances.size() > 1) {
				previousProcessInstance = processInstances.get(1);
			}

			if (!ObjectUtils.isEmpty(previousProcessInstance)
					&& previousProcessInstance.getAction() == IssueFixConstants.PAY) {
				isDataUpdateNeeded.put(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED, false);
				throw new CustomException("APPLICATION_UPDATE_ISSUE", "Can't Step Back. Payment Done");
			}

			if (lastestProcessInstance.getAction().equalsIgnoreCase(IssueFixConstants.ACTION_APPROVE)) {
				isDataUpdateNeeded.put(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED, true);
			} else if (lastestProcessInstance.getAction().equalsIgnoreCase(IssueFixConstants.ACTION_FORWARD)) {
				isDataUpdateNeeded.put(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED, true);
			} else if (lastestProcessInstance.getAction().equalsIgnoreCase(IssueFixConstants.ACTION_SEND_BACK)) {
				isDataUpdateNeeded.put(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED, true);
			} else if (lastestProcessInstance.getAction()
					.equalsIgnoreCase(IssueFixConstants.ACTION_SEND_BACK_TO_CITIZEN)) {
				isDataUpdateNeeded.put(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED, true);
			} else if (lastestProcessInstance.getAction().equalsIgnoreCase(IssueFixConstants.ACTION_VERIFY)) {
				isDataUpdateNeeded.put(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED, true);
			}
		} else {
			throw new CustomException("WORKFLOW_ISSUE", "Found No Workflow records for mentioned Application !!");
		}

		return lastestProcessInstance;
	}

	public void checkIfPaymentReceived(IssueFixRequest issueFixRequest) {

		String tenantId = issueFixRequest.getIssueFix().getTenantId();

		String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

		PaymentSearchCriteria paymentSearchCriteria = PaymentSearchCriteria.builder().consumerCode(applicationNumber)
				.tenantId(tenantId).build();

		List<Payment> payments = repository.getPayments(paymentSearchCriteria);

		if (!CollectionUtils.isEmpty(payments)) {

			throw new CustomException("PAYMENT_ERROR",
					"A payment has already been received for this application. Reverting is not allowed.");
		}

	}

}
