package org.egov.bpa.service.issuefix;


import org.apache.commons.lang.StringUtils;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;

public interface IIssueFixService {

    IssueFix issueFix(IssueFixRequest issueFixRequest);

    default  BPASearchCriteria createBPASearchCriteria(IssueFixRequest issueFixRequest) {
        BPASearchCriteria bpaSearchCriteria= BPASearchCriteria.builder()
                .tenantId(issueFixRequest.getIssueFix().getTenantId()).build();

        if(!StringUtils.isEmpty(issueFixRequest.getIssueFix().getApplicationNo()))
            bpaSearchCriteria.setApplicationNo(issueFixRequest.getIssueFix().getApplicationNo());

        if(!StringUtils.isEmpty(issueFixRequest.getIssueFix().getApprovalNo()))
            bpaSearchCriteria.setApprovalNo(issueFixRequest.getIssueFix().getApprovalNo());

        return bpaSearchCriteria;
    }
}

