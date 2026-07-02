package org.egov.pt.service.issuefix;

import lombok.extern.slf4j.Slf4j;
import org.egov.pt.models.Property;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.models.workflow.ProcessInstance;
import org.egov.pt.models.workflow.WorkFlowSearchCriteria;
import org.egov.pt.repository.IssueFixRepository;
import org.egov.pt.service.PropertyService;
import org.egov.pt.service.WorkflowService;
import org.egov.pt.util.PTConstants;
import org.egov.pt.validator.IssueFixValidator;
import org.egov.pt.models.issuefix.IssueFix;
import org.egov.pt.models.issuefix.IssueFixRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("applicationStatusMismatchIssueFix")
@Slf4j
public class ApplicationStatusMismatchIssueFixService implements IIssueFixService {

    @Autowired
    private WorkflowService workflowService;


    @Autowired
    private IssueFixValidator issueFixValidator;

    @Autowired
    private IssueFixRepository issueFixRepository;

    @Autowired
    private PropertyService propertyService;


    @Override
    public IssueFix issueFix(IssueFixRequest issueFixRequest) {
        PropertyCriteria propertyCriteria = getSearchCriteria(issueFixRequest);
        List<Property> propertyList = propertyService.searchProperty(propertyCriteria, issueFixRequest.getRequestInfo());

        log.info("@Class: ApplicationStatusMismatchIssueFixService @method:issueFix @message: Property Search Completed");
        issueFixValidator.validatePropertySearch(propertyList);
        Property property = propertyList.get(0);

        List<ProcessInstance> processInstance = workflowService.getProcessInstanceForIssueFix(
                issueFixRequest.getRequestInfo(), issueFixRequest.getIssueFix().getApplicationNo(),
                issueFixRequest.getIssueFix().getTenantId(), true
        );
        log.info("@Class: ApplicationStatusMismatchIssueFixService @method:issueFix @message: Received Process Instances", processInstance);
        issueFixValidator.validateProcessInstanceApplicationStatusMismatch(property, processInstance);

        List<String> idList = getProcessInstancesForDeletion(property, processInstance);

        issueFixRepository.updateApplicationStatusMismatch(idList);

        return issueFixRequest.getIssueFix();
    }


    private List<String> getProcessInstancesForDeletion(Property property, List<ProcessInstance> processInstanceList) {
        List<String> processInstanceIdList = new ArrayList<>();
        for (ProcessInstance processInstance : processInstanceList) {
            if (processInstance.getState().getApplicationStatus().equalsIgnoreCase(property.getStatus().toString())) {
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
