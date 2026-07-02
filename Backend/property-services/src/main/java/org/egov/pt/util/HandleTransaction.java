package org.egov.pt.util;

import java.util.List;
import java.util.Map;

import org.egov.pt.models.Property;
import org.egov.pt.models.issuefix.IssueFixRequest;
import org.egov.pt.models.workflow.ProcessInstance;
import org.egov.pt.repository.IssueFixRepository;
import org.egov.pt.web.contracts.Demand;
import org.egov.pt.web.contracts.DemandDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class HandleTransaction {

	private static final Logger logger = LoggerFactory.getLogger(HandleTransaction.class);

	@Autowired
	private IssueFixRepository repository;

	@Transactional(rollbackFor = Exception.class)
	public void updateDate(IssueFixRequest issueFixRequest, Map<String, Boolean> isDataUpdateNeeded, Property property,
			ProcessInstance processInstance, Property applicationToBeUpdated, String creationReason,
			List<Demand> demands, List<DemandDetail> demandDetails) {
		logger.info("Starting transaction for updateDate method.");

		try {
			// If data update is needed, perform the following operations:
			if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED)
					&& isDataUpdateNeeded.get(IssueFixConstants.IS_APPLICATION_UPDATE_NEEDED)) {
				repository.updateApplicationForStepBack(applicationToBeUpdated);
			}

			if (isDataUpdateNeeded.containsKey(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED)
					&& isDataUpdateNeeded.get(IssueFixConstants.IS_WORKFLOW_DELETE_NEEDED)) {
				repository.updateWorkflowForStepBack(processInstance);
			}

			// Check if demand details update is needed
			if (Boolean.TRUE.equals(isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_DETAILS_UPDATE_NEEDED))
					&& Boolean.TRUE.equals(isDataUpdateNeeded.get(IssueFixConstants.IS_DEMAND_UPDATE_NEEDED))) {

				if (demands == null || demands.isEmpty() || demandDetails == null) {
					throw new IllegalArgumentException("Demands and demand details cannot be null or empty.");
				}

				Demand demand = demands.get(0);
				String consumerCode = demand.getConsumerCode();
				String demandID = demand.getId();

				repository.insertDemandForApplicationStepBackInAudit(demand);
				repository.insertDemandDetailsForApplicationStepBackInAudit(demandDetails);

				repository.deleteDemandDetailsForApplicationStepBack(demandDetails);
				repository.deleteDemandForApplicationStepBack(demandID);

				if (Boolean.TRUE.equals(isDataUpdateNeeded.get(IssueFixConstants.IS_BILL_EXPIRATION_NEEDED))) {
					repository.expireBill(consumerCode);
				}
			}

			logger.info("Transaction completed successfully for updateDate method.");
		} catch (Exception e) {
			logger.error("Transaction rolled back due to an error in updateDate method: {}", e.getMessage(), e);
			throw e;
		}
	}
}
