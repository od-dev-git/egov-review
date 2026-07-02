package org.egov.pt.service.issuefix;


import com.google.common.collect.Sets;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.models.issuefix.IssueFix;
import org.egov.pt.models.issuefix.IssueFixRequest;
import org.springframework.util.StringUtils;

public interface IIssueFixService {

    IssueFix issueFix(IssueFixRequest issueFixRequest);

    default PropertyCriteria getSearchCriteria(IssueFixRequest issueFixRequest) {

        PropertyCriteria propertyCriteria = PropertyCriteria.builder().tenantId(issueFixRequest.getIssueFix().getTenantId()).build();

        if (!StringUtils.isEmpty(issueFixRequest.getIssueFix().getApplicationNo())) {
            propertyCriteria.setAcknowledgementIds(Sets.newHashSet(issueFixRequest.getIssueFix().getApplicationNo()));
        }

        if (!StringUtils.isEmpty(issueFixRequest.getIssueFix().getPropertyId())) {
            propertyCriteria.setPropertyIds(Sets.newHashSet(issueFixRequest.getIssueFix().getPropertyId()));
        }

        return propertyCriteria;
    }
}
