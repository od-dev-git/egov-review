package org.egov.bpa.service.issuefix;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.DscDetails;
import org.egov.bpa.web.model.Installment;
import org.egov.bpa.web.model.InstallmentSearchCriteria;
import org.egov.bpa.web.model.collection.Demand;
import org.egov.bpa.web.model.collection.DemandSearchCriteria;
import org.egov.bpa.web.model.collection.Payment;
import org.egov.bpa.web.model.collection.PaymentSearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.web.model.workflow.WorkFlowSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service("oneStepBackService")
@Slf4j
public class OneStepBackServiceFix implements IIssueFixService {

	@Autowired
	private IssueFixValidator validator;

	@Autowired
	private IssueFixRepository repository;

	@Autowired
	private BPARepository bpaRepository;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		validator.validateIssueFix(issueFixRequest);

		String applicationNo = issueFixRequest.getIssueFix().getApplicationNo();

		String tenantId = issueFixRequest.getIssueFix().getTenantId();

		Map<String, Boolean> isDataUpdateNeeded = new HashMap<>();

		BPASearchCriteria searchCriteria = BPASearchCriteria.builder()
				.tenantId(tenantId)
				.applicationNo(applicationNo)
				.build();

		List<BPA> bpa = bpaRepository.getBPAData(searchCriteria, new ArrayList<String>());
		validator.validateApplicationStatus(bpa);
		log.info("Status for Application no : " + applicationNo + " is -" + bpa.get(0).getStatus());
		Boolean isPaymentReceived = false;
		Demand demandToBeUpdated = null;
		Installment installmentToBeDeleted = null;
		DscDetails dscToBeDeleted = null;
		
		if (bpa.get(0).getStatus().equalsIgnoreCase(IssueFixConstants.PENDING_SANC_FEE)) {
			
			isPaymentReceived = checkIfPaymentReceived(issueFixRequest);
			
			demandToBeUpdated = checkIfDemandUpdatedProperly(issueFixRequest, isDataUpdateNeeded);
			
			installmentToBeDeleted = checkIfInstallmentToBeDeleted(issueFixRequest, isDataUpdateNeeded);
			
			dscToBeDeleted = checkIfDscToBeDeleted(issueFixRequest, isDataUpdateNeeded, searchCriteria);
		}
		
		log.info("Demand To Be Updated " + demandToBeUpdated);
		log.info("Installment To Be Deleted " + installmentToBeDeleted);
		log.info("DSC To Be Deleted " + dscToBeDeleted);
		
		ProcessInstance processInstance = checkIfWorkFlowUpdatedProperly(issueFixRequest, isDataUpdateNeeded);

		BPA applicationToBeUpdated = checkIfApplicationUpdatedProperly(issueFixRequest, bpa.get(0), isDataUpdateNeeded);
		
		updateDataIfNeeded(demandToBeUpdated, processInstance, applicationToBeUpdated, isPaymentReceived, isDataUpdateNeeded);

		return issueFixRequest.getIssueFix();
	}

	private DscDetails checkIfDscToBeDeleted(IssueFixRequest issueFixRequest, Map<String, Boolean> isDataUpdateNeeded,
			BPASearchCriteria searchCriteria) {
		DscDetails dsc = new DscDetails();
		List<DscDetails> dscDetails = repository.getDscDetails(searchCriteria);

		if (!CollectionUtils.isEmpty(dscDetails)) {
			dsc = dscDetails.get(0);
			isDataUpdateNeeded.put(IssueFixConstants.IS_DSC_DELETE_NEEDED, true);

		}
		return dsc;

	}

	private Installment checkIfInstallmentToBeDeleted(IssueFixRequest issueFixRequest,
			Map<String, Boolean> isDataUpdateNeeded) {
		Installment installment = new Installment();
		InstallmentSearchCriteria installmentSearchCriteria = InstallmentSearchCriteria.builder()
				.consumerCode(issueFixRequest.getIssueFix().getApplicationNo()).build();
		List<Installment> installments = repository.getInstallments(installmentSearchCriteria);
		if (!CollectionUtils.isEmpty(installments)) {
			installment = installments.get(0);
			isDataUpdateNeeded.put(IssueFixConstants.IS_INSTALLMENT_DELETE_NEEDED, true);

		}
		return installment;
	}

	/**
	 * Execute the update queries here
	 * 
	 * @param demandToBeDeleted
	 * @param processInstance
	 * @param applicationToBeUpdated
	 * @param isPaymentReceived
	 * @param isDataUpdateNeeded
	 */
	@Transactional
	private void updateDataIfNeeded(Demand demandToBeDeleted, ProcessInstance processInstance,
			BPA applicationToBeUpdated, Boolean isPaymentReceived, Map<String, Boolean> isDataUpdateNeeded) {

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)) {
			repository.deleteDemandDetail(demandToBeDeleted);
			repository.deleteDemand(demandToBeDeleted);
		}

		if ((isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DEMAND_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DELETE_NEEDED))) {
			repository.expireBill(demandToBeDeleted.getConsumerCode());
		}
		
		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_INSTALLMENT_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_INSTALLMENT_DELETE_NEEDED)) {
			repository.deleteInstallments(applicationToBeUpdated);
		}
		
		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_DSC_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_DSC_DELETE_NEEDED)) {
			repository.deleteDsc(applicationToBeUpdated);
		}

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED)) {
			repository.updateWorkflowForStepBack(processInstance);
		}

		if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED)
				&& isDataUpdateNeeded.get(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED)) {
			repository.updateApplicationForOneStepBack(applicationToBeUpdated);
		}
		
	}

	/**
	 * check if the application is updated properly, if not update the application
	 * 
	 * @param issueFixRequest
	 * @param bpa
	 * @param isDataUpdateNeeded
	 */
	private BPA checkIfApplicationUpdatedProperly(IssueFixRequest issueFixRequest, BPA bpa,
			Map<String, Boolean> isDataUpdateNeeded) {

		if (IssueFixConstants.INVALID_STEP_BACK_STATUS.contains(bpa.getStatus())) {
			throw new CustomException("APPLICATION_UPDATE_ISSUE", "Current Status of the application is : " + bpa.getStatus() + ". Can't step back !");
		} else {
			isDataUpdateNeeded.put(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED, true);
		}
		return bpa;
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

			if (lastestProcessInstance.getAction().equalsIgnoreCase(IssueFixConstants.PAY)) {
				log.error("Process instance can't be deleted for action PAY");
			} else {
				isDataUpdateNeeded.put(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED, true);
			}
		} else {
			throw new CustomException("PROCESS_INSTANCE_ISSUE", "No workflow found for application no :" + applicationNumber);
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
				.businessService(IssueFixConstants.BPA_SAN_FEE).tenantId(tenantId).build();

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
				.businessService(IssueFixConstants.BPA_SAN_FEE).tenantId(tenantId).build();

		List<Payment> payments = repository.getPayments(paymentSearchCriteria);

		if (payments.size() >= 1) {
			isPaymentReceived = Boolean.TRUE;
			throw new CustomException("PAYMENT_ISSUE", "Sanc fee payment has been done for application no :" + applicationNumber + " Can't step back !");
		}

		log.info("Sanc Fee payment has not been done.. Moving to next step !!");

		return isPaymentReceived;
	}

}
