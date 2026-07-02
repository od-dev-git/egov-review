package org.egov.bpa.service.issuefix;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.service.BPAService;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.*;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service("employeeMappingChange")
public class EmployeeMappingChange implements IIssueFixService {
	
	@Autowired
	public IssueFixRepository issueFixRepository;
	
	@Autowired	
	public IssueFixValidator issueFixValidator;
	
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {
		
		List<String> data=issueFixRepository.getUUID(issueFixRequest.getIssueFix().getNewApprover(), null);
		if(!CollectionUtils.isEmpty(data) && data.size() > 1)  {
        	issueFixValidator.validateTenantId(issueFixRequest);
        	data=issueFixRepository.getUUID(issueFixRequest.getIssueFix().getNewApprover(), issueFixRequest.getIssueFix().getTenantId());
        }
		
		List<String> processInstanceId=issueFixRepository.getProcessInstanceId(issueFixRequest.getIssueFix().getApplicationNo());
		issueFixValidator.validateIssueFixForEmpMapping(data, processInstanceId);
        issueFixRepository.updateAssigner(issueFixRequest.getIssueFix(), data.get(0), processInstanceId.get(0));
		
		return issueFixRequest.getIssueFix();
	}
	
	public BPASearchCriteria createBPASearchCriteria(IssueFixRequest issueFixRequest) {
        BPASearchCriteria bpaSearchCriteria= BPASearchCriteria.builder()
                .tenantId(issueFixRequest.getIssueFix().getTenantId()).build();

        if(!StringUtils.isEmpty(issueFixRequest.getIssueFix().getApplicationNo()))
            bpaSearchCriteria.setApplicationNo(issueFixRequest.getIssueFix().getApplicationNo());

        if(!StringUtils.isEmpty(issueFixRequest.getIssueFix().getApprovalNo()))
            bpaSearchCriteria.setApprovalNo(issueFixRequest.getIssueFix().getApprovalNo());

        return bpaSearchCriteria;
    }

}
