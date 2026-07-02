package org.egov.bpa.service.issuefix;

import lombok.extern.slf4j.Slf4j;
import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.service.BPAService;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.bpa.workflow.WorkflowService;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service("applicationStatusMismatchIssueFixService")
@Slf4j
public class ApplicationStatusMismatchIssueFixService implements IIssueFixService{

    @Autowired
    private BPARepository bpaRepository;

    @Autowired
    private IssueFixValidator issueFixValidator;

    @Autowired
    private BPAService bpaService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private IssueFixRepository issueFixRepository;

    @Override
    public IssueFix issueFix(IssueFixRequest issueFixRequest) {
        BPASearchCriteria bpaSearchCriteria = createBPASearchCriteria(issueFixRequest);

        List<BPA> bpaList=bpaService.search(bpaSearchCriteria,issueFixRequest.getRequestInfo());

        log.info("@Class: ApplicationStatusMismatchIssueFixService @method:issueFix @message: Water Search Completed");
        issueFixValidator.validateWaterConnectionApplicationStatusMismatch(bpaList);
        BPA bpa=bpaList.get(0);

        List<ProcessInstance> processInstance = workflowService.getProcessInstanceForIssueFix(
                issueFixRequest.getRequestInfo(),issueFixRequest.getIssueFix().getApplicationNo(),
                issueFixRequest.getIssueFix().getTenantId(),bpa.getBusinessService(),true
        );
        log.info("@Class: ApplicationStatusMismatchIssueFixService @method:issueFix @message: Received Process Instances",processInstance);
        issueFixValidator.validateProcessInstanceApplicationStatusMismatch(bpa,processInstance);

        List<String> idList=getProcessInstancesForDeletion(bpa,processInstance);


        fixApplicationStatusMismatch(idList);

        return issueFixRequest.getIssueFix();

    }

    @Transactional
    private void fixApplicationStatusMismatch(List<String> idList) {
        issueFixRepository.updateApplicationStatusMismatch(idList);
    }

    private List<String> getProcessInstancesForDeletion(BPA bpa, List<ProcessInstance> processInstanceList) {
        List<String> processInstanceIdList = new ArrayList<>();
        for (ProcessInstance processInstance : processInstanceList) {
            if (processInstance.getState().getApplicationStatus().equalsIgnoreCase(bpa.getStatus())) {
                break;
            }
            else {
                checkIfInstanceIsOfPayment(processInstance);
                processInstanceIdList.add(processInstance.getId());
            }
        }
		if (processInstanceIdList.size() > 1) {
			throw new CustomException("INVALID_APPLICATION_STATE",
					"This issue is not under status mismatch.. Kindly reach out to dev team !!");
		}
        return processInstanceIdList;
    }

    private void checkIfInstanceIsOfPayment(ProcessInstance processInstance) {

        if(processInstance.getAction().equalsIgnoreCase(BPAConstants.ACTION_PAY)){
            throw new CustomException("INVALID_PROCESS_INSTANCE","Payment is already completed so it can't be reversed!");
        }
    }
}
