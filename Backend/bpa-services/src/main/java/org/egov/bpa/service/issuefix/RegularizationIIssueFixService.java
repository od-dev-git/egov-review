package org.egov.bpa.service.issuefix;


import org.apache.commons.lang.StringUtils;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;

public interface RegularizationIIssueFixService {

    IssueFix issueFix(IssueFixRequest issueFixRequest);

    default  RegularizationSearchCriteria createRegularizationSearchCriteria(IssueFixRequest issueFixRequest) {
    	RegularizationSearchCriteria regularizationSearchCriteria = RegularizationSearchCriteria.builder()
                .tenantId(issueFixRequest.getIssueFix().getTenantId()).build();

        if(!StringUtils.isEmpty(issueFixRequest.getIssueFix().getApplicationNo()))
        	regularizationSearchCriteria.setApplicationNo(issueFixRequest.getIssueFix().getApplicationNo());

       
        return regularizationSearchCriteria;
    }
}

