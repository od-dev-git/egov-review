package org.egov.bpa.service.issuefix;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.repository.RegularizationIssueFixRepository;
import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.collection.Demand;
import org.egov.bpa.web.model.collection.DemandSearchCriteria;
import org.egov.bpa.web.model.collection.Payment;
import org.egov.bpa.web.model.collection.PaymentSearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationDscDetails;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.web.model.workflow.WorkFlowSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service("regStepBackToApprovalInprogress")
@Slf4j
public class RegularizationStepBackFix implements RegularizationIIssueFixService {
	
	@Autowired
	private IssueFixValidator validator;

	@Autowired
	private RegularizationIssueFixRepository repository;

	@Autowired
	private RegularizationRepository regularizationRepository;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		validator.validateIssueFix(issueFixRequest);
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
			throw new CustomException("APPLICATION_STATUS_ERROR", "Application is not at Sanc Fee Pending Payment Stage. Can't revert to 1 step back !!");
		}

		Boolean isPaymentReceived = checkIfPaymentReceived(issueFixRequest);
		Demand demandToBeUpdated = checkIfDemandUpdatedProperly(issueFixRequest, isDataUpdateNeeded);
		ProcessInstance processInstance = checkIfWorkFlowUpdatedProperly(issueFixRequest, isDataUpdateNeeded);

		Regularization applicationToBeUpdated = checkIfApplicationUpdatedProperly(issueFixRequest, regularizations.get(0), isDataUpdateNeeded);
		RegularizationDscDetails dscToBeDeleted = checkIfDscToBeDeleted(issueFixRequest, isDataUpdateNeeded, searchCriteria);

		updateDataIfNeeded(demandToBeUpdated, processInstance, applicationToBeUpdated, isPaymentReceived, isDataUpdateNeeded);

		return issueFixRequest.getIssueFix();
	}

	
	/**
	 * @param issueFixRequest
	 * @param isDataUpdateNeeded
	 * @param searchCriteria
	 * @return check If DscDetails To Be Deleted
	 */
	private RegularizationDscDetails checkIfDscToBeDeleted(IssueFixRequest issueFixRequest, Map<String, Boolean> isDataUpdateNeeded,
			RegularizationSearchCriteria searchCriteria) {
		RegularizationDscDetails dscDetails = new RegularizationDscDetails();
		List<RegularizationDscDetails> regularizationDscDetails = repository.searchRegularizationDscDetails(searchCriteria, issueFixRequest.getRequestInfo());

		if (!CollectionUtils.isEmpty(regularizationDscDetails)) {
			dscDetails = regularizationDscDetails.get(0);
			isDataUpdateNeeded.put(IssueFixConstants.IS_DSC_DELETE_NEEDED, true);

		}
		return dscDetails;
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
	private void updateDataIfNeeded(Demand demandToBeDeleted, ProcessInstance processInstance,
			Regularization applicationToBeUpdated, Boolean isPaymentReceived, Map<String, Boolean> isDataUpdateNeeded) {

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)) {
			repository.deleteDemandDetail(demandToBeDeleted);
			repository.deleteDemand(demandToBeDeleted);
		}

		if ((isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DELETE_NEEDED))) {
			repository.expireBill(demandToBeDeleted.getConsumerCode());

		}

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED)) {
			repository.updateApplicationForStepBack(applicationToBeUpdated);
		}

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED)) {
			repository.updateWorkflowForStepBack(processInstance);
		}
		
		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DSC_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DSC_DELETE_NEEDED)) {
			repository.deleteDscDetails(applicationToBeUpdated);
		}

	}

	/**
	 * check if the application is updated properly, if not update the application
	 * 
	 * @param issueFixRequest
	 * @param waterConnection
	 * @param isDataUpdateNeeded
	 */
	private Regularization checkIfApplicationUpdatedProperly(IssueFixRequest issueFixRequest, Regularization regularization,
			Map<String, Boolean> isDataUpdateNeeded) {

		if (regularization.getStatus().equalsIgnoreCase(IssueFixConstants.PENDING_SANC_FEE)) {
			isDataUpdateNeeded.put(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED, true);
		} else {
			throw new CustomException("APPLICATION_UPDATE_ISSUE",
					"Current Status of the application is not in " + IssueFixConstants.PENDING_SANC_FEE + " step");
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
		ProcessInstance lastestProcessInstance = new ProcessInstance();
		String tenantId = issueFixRequest.getIssueFix().getTenantId();
		String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

		WorkFlowSearchCriteria workFlowSearchCriteria = WorkFlowSearchCriteria.builder()
				.businessId(applicationNumber)
				.tenantId(tenantId)
				.build();

		List<ProcessInstance> processInstances = repository.getProcessInstances(workFlowSearchCriteria);

		if (!CollectionUtils.isEmpty(processInstances)) {
			lastestProcessInstance = processInstances.get(0);

			if (lastestProcessInstance.getAction().equalsIgnoreCase(IssueFixConstants.APPROVE)) {
				isDataUpdateNeeded.put(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED, true);
			} else {
				log.info("Process instance is not found for action APPROVE");
			}
		}

		return lastestProcessInstance;
	}

	/**
	 * check weather is payment completed flag is updated or not
	 * 
	 * @param issueFixRequest
	 * @param isDataUpdateNeeded
	 */
	private Demand checkIfDemandUpdatedProperly(IssueFixRequest issueFixRequest,
			Map<String, Boolean> isDataUpdateNeeded) {
		Demand demand = new Demand();

		String tenantId = issueFixRequest.getIssueFix().getTenantId();

		String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

		DemandSearchCriteria demandSearchCriteria = DemandSearchCriteria.builder().consumerCode(applicationNumber)
				.businessService(IssueFixConstants.REG_SAN_FEE).tenantId(tenantId).build();

		// Search demand here
		List<Demand> demands = repository.getDemands(demandSearchCriteria);
		if (demands.size() > 1) {
			throw new CustomException("MULTIPLE_DEMNADS_FOUND", "Multiple Sanc Fee demand found for application no :" + applicationNumber);
		} else if (!CollectionUtils.isEmpty(demands)) {
			if (demands.get(0).getIsPaymentCompleted().equals(Boolean.TRUE)) {
				throw new CustomException("PAYMENT_DONE_FOR_DEMAND", "Payment has been done for sanc fee demand application no :" + applicationNumber);
			} else {
				demand = demands.get(0);
				isDataUpdateNeeded.put(IssueFixConstants.IS_DEMAND_DELETE_NEEDED, true);
			}
		}
		return demand;
	}

	/**
	 * check weather payment is done for mentioned application
	 * 
	 * @param issueFixRequest
	 */
	private Boolean checkIfPaymentReceived(IssueFixRequest issueFixRequest) {
		Boolean isPaymentReceived = Boolean.FALSE;

		String tenantId = issueFixRequest.getIssueFix().getTenantId();

		String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

		PaymentSearchCriteria paymentSearchCriteria = PaymentSearchCriteria.builder().consumerCode(applicationNumber)
				.businessService(IssueFixConstants.REG_SAN_FEE).tenantId(tenantId).build();

		List<Payment> payments = repository.getPayments(paymentSearchCriteria);

		if (payments.size() >= 1) {
			isPaymentReceived = Boolean.TRUE;
			throw new CustomException("PAYMENT_ISSUE", "Sanc fee payment has been done for application no :" + applicationNumber + " Can't step back !");
		}

		log.info("Sanc Fee payment has not been done.. Moving to next step !!");
		return isPaymentReceived;

	}
}
