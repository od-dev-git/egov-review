package org.egov.bpa.service.issuefix;

import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.repository.RegularizationIssueFixRepository;
import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Service("regApplicationStatusMismatchIssueFixService")
@Slf4j
public class RegularizationApplicationStatusMismatchIssueFixService implements RegularizationIIssueFixService {

    @Autowired
    private IssueFixValidator issueFixValidator;

    @Autowired
    private WorkflowService workflowService;
    
    @Autowired
	private RegularizationIssueFixRepository issueFixRepository;

	@Autowired
	private RegularizationRepository regularizationRepository;

    @Override
    public IssueFix issueFix(IssueFixRequest issueFixRequest) {
    	RegularizationSearchCriteria searchCriteria = createRegularizationSearchCriteria(issueFixRequest);

    	List<Regularization> regularizations = regularizationRepository.searchRegularization(searchCriteria, issueFixRequest.getRequestInfo());

        log.info("@Class: RegularizationApplicationStatusMismatchIssueFixService @method:issueFix @message: Regularization Search Completed");
        issueFixValidator.validateRegularizationApplicationStatusMismatch(regularizations);
        Regularization regularization = regularizations.get(0);

        List<ProcessInstance> processInstance = workflowService.getProcessInstanceForIssueFix(
                issueFixRequest.getRequestInfo(),issueFixRequest.getIssueFix().getApplicationNo(),
                issueFixRequest.getIssueFix().getTenantId(),regularization.getBusinessService(),true
        );
        
        log.info("@Class: RegularizationApplicationStatusMismatchIssueFixService @method:issueFix @message: Received Process Instances",processInstance);
        issueFixValidator.validateProcessInstanceRegularizationStatusMismatch(regularization,processInstance);

        List<String> idList = getProcessInstancesForDeletion(regularization, processInstance);
        fixApplicationStatusMismatch(idList);

        return issueFixRequest.getIssueFix();

    }

    @Transactional
    private void fixApplicationStatusMismatch(List<String> idList) {
        issueFixRepository.updateApplicationStatusMismatch(idList);
    }

    private List<String> getProcessInstancesForDeletion(Regularization regularization, List<ProcessInstance> processInstanceList) {
        List<String> processInstanceIdList = new ArrayList<>();
        for (ProcessInstance processInstance : processInstanceList) {
            if (processInstance.getState().getApplicationStatus().equalsIgnoreCase(regularization.getStatus())) {
                break;
            }
            else {
                checkIfInstanceIsOfPayment(processInstance);
                processInstanceIdList.add(processInstance.getId());
            }
        }
		if (processInstanceIdList.size() > 1) {
			throw new CustomException("INVALID_APPLICATION_STATE", "This issue is not under status mismatch.. Kindly reach out to dev team !!");
		}
        return processInstanceIdList;
    }

    private void checkIfInstanceIsOfPayment(ProcessInstance processInstance) {

        if(processInstance.getAction().equalsIgnoreCase(BPAConstants.ACTION_PAY)){
            throw new CustomException("INVALID_PROCESS_INSTANCE","Payment is already completed so it can't be reversed!");
        }
    }
}
