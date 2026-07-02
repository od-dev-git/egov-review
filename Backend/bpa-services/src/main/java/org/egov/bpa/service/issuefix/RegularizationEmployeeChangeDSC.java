package org.egov.bpa.service.issuefix;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.repository.RegularizationIssueFixRepository;
import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service("regEmployeeChangeDSC")
public class RegularizationEmployeeChangeDSC implements RegularizationIIssueFixService{

    @Autowired
    private IssueFixValidator issueFixValidator;

    @Autowired
    private IssueFixRepository issueFixRepository;
    
    @Autowired
	private RegularizationIssueFixRepository repository;
    
	@Autowired
	private RegularizationRepository regularizationRepository;

    @Override
    public IssueFix issueFix(IssueFixRequest issueFixRequest) {

    	RegularizationSearchCriteria searchCriteria = createRegularizationSearchCriteria(issueFixRequest);

		List<Regularization> regularizations = regularizationRepository.searchRegularization(searchCriteria, issueFixRequest.getRequestInfo());

        List<String> data = issueFixRepository.getUUID(issueFixRequest.getIssueFix().getNewApprover(), null);
        
        if(!CollectionUtils.isEmpty(data) && data.size() > 1)  {
        	issueFixValidator.validateTenantId(issueFixRequest);
        	data = issueFixRepository.getUUID(issueFixRequest.getIssueFix().getNewApprover(), issueFixRequest.getIssueFix().getTenantId());
        }
        
        issueFixValidator.validateIssueFixForRegularizationDSCChange(regularizations, data);
        repository.updateRegularizationDscApprover(regularizations.get(0),data.get(0));
        
        return issueFixRequest.getIssueFix();
    }

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
