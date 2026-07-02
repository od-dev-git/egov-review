package org.egov.pt.service.issuefix;

import java.util.ArrayList;
import java.util.List;

import org.egov.pt.models.Assessment;
import org.egov.pt.models.AssessmentSearchCriteria;
import org.egov.pt.models.issuefix.IssueFix;
import org.egov.pt.models.issuefix.IssueFixRequest;
import org.egov.pt.models.workflow.ProcessInstance;
import org.egov.pt.repository.IssueFixRepository;
import org.egov.pt.service.AssessmentService;
import org.egov.pt.service.WorkflowService;
import org.egov.pt.util.PTConstants;
import org.egov.pt.validator.IssueFixValidator;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;

@Service("assesmentStatusMismatchIssueFix")
@Slf4j
public class AssessmentStatusMismatchIssueFixService implements IIssueFixService {

    @Autowired
    private WorkflowService workflowService;


    @Autowired
    private IssueFixValidator issueFixValidator;

    @Autowired
    private IssueFixRepository issueFixRepository;

    @Autowired
	private AssessmentService assessmentService;


    @Override
    public IssueFix issueFix(IssueFixRequest issueFixRequest) {
    	issueFixValidator.validateAssessmentIssueFixRequest(issueFixRequest);
    	AssessmentSearchCriteria searchriteria = AssessmentSearchCriteria.builder()
    			.assessmentNumbers(Sets.newHashSet(issueFixRequest.getIssueFix().getAssessmentNo()))
    			.build();
        List<Assessment> assessmentList = assessmentService.searchAssessments(searchriteria);

        log.info("@Class: ApplicationStatusMismatchIssueFixService @method:issueFix @message: Property Search Completed");
        issueFixValidator.validateAssessmentSearch(assessmentList);
        Assessment assessment = assessmentList.get(0);

        List<ProcessInstance> processInstance = workflowService.getProcessInstanceForIssueFix(
                issueFixRequest.getRequestInfo(), issueFixRequest.getIssueFix().getAssessmentNo(),
                issueFixRequest.getIssueFix().getTenantId(), true
        );
        log.info("@Class: ApplicationStatusMismatchIssueFixService @method:issueFix @message: Received Process Instances", processInstance);
        issueFixValidator.validateAssesmentProcessInstanceApplicationStatusMismatch(assessment, processInstance);

        List<String> idList = getAssesmentProcessInstancesForDeletion(assessment, processInstance);

        issueFixRepository.updateApplicationStatusMismatch(idList);

        return issueFixRequest.getIssueFix();
    }


    private List<String> getAssesmentProcessInstancesForDeletion(Assessment assessment, List<ProcessInstance> processInstanceList) {
        List<String> processInstanceIdList = new ArrayList<>();
        for (ProcessInstance processInstance : processInstanceList) {
            if (processInstance.getState().getApplicationStatus().equalsIgnoreCase(assessment.getStatus().toString())) {
                break;
            }
            else {
                checkIfInstanceIsOfPayment(processInstance);
                processInstanceIdList.add(processInstance.getId());
            }
        }
        return processInstanceIdList;
    }

    private void checkIfInstanceIsOfPayment(ProcessInstance processInstance) {

        if(processInstance.getAction().equalsIgnoreCase(PTConstants.ACTION_PAY)){
            throw new CustomException("INVALID_PROCESS_INSTANCE","Payment is already completed so it can't be reversed!");
        }
    }


}
