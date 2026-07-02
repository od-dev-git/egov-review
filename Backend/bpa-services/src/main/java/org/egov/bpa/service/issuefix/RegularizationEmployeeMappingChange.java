package org.egov.bpa.service.issuefix;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;


@Service("regEmployeeMappingChange")
public class RegularizationEmployeeMappingChange implements RegularizationIIssueFixService {
	
	@Autowired
	public IssueFixRepository issueFixRepository;
	
	@Autowired
	private RegularizationRepository regularizationRepository;
		
	@Autowired	
	public IssueFixValidator issueFixValidator;
	
	
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {
		
		RegularizationSearchCriteria searchCriteria = createRegularizationSearchCriteria(issueFixRequest);

		List<Regularization> regularizations = regularizationRepository.searchRegularization(searchCriteria, issueFixRequest.getRequestInfo());
		issueFixValidator.validateRegularizationApplication(regularizations);
		
		List<String> data = issueFixRepository.getUUID(issueFixRequest.getIssueFix().getNewApprover(), null);
		if(!CollectionUtils.isEmpty(data) && data.size() > 1)  {
        	issueFixValidator.validateTenantId(issueFixRequest);
        	data = issueFixRepository.getUUID(issueFixRequest.getIssueFix().getNewApprover(), issueFixRequest.getIssueFix().getTenantId());
        }
		
		List<String> processInstanceId=issueFixRepository.getProcessInstanceId(issueFixRequest.getIssueFix().getApplicationNo());
		issueFixValidator.validateIssueFixForEmpMapping(data, processInstanceId);
        
		issueFixRepository.updateAssigner(issueFixRequest.getIssueFix(), data.get(0), processInstanceId.get(0));
		
		return issueFixRequest.getIssueFix();
	}

	/**
	 * Create Regularization SearchCriteria
	 */
	public RegularizationSearchCriteria createRegularizationSearchCriteria(IssueFixRequest issueFixRequest) {
    	RegularizationSearchCriteria searchCriteria= RegularizationSearchCriteria.builder()
                .tenantId(issueFixRequest.getIssueFix().getTenantId()).build();

        if(!StringUtils.isEmpty(issueFixRequest.getIssueFix().getApplicationNo()))
        	searchCriteria.setApplicationNo(issueFixRequest.getIssueFix().getApplicationNo());

        if(!StringUtils.isEmpty(issueFixRequest.getIssueFix().getApprovalNo()))
        	searchCriteria.setApprovalNo(issueFixRequest.getIssueFix().getApprovalNo());

        return searchCriteria;
    }
}
