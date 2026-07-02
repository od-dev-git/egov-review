package org.egov.pt.service.issuefix;

import org.egov.pt.models.Property;
import org.egov.pt.models.PropertyCriteria;
import org.egov.pt.models.issuefix.IssueFix;
import org.egov.pt.models.issuefix.IssueFixRequest;
import org.egov.pt.repository.IssueFixRepository;
import org.egov.pt.service.PropertyService;
import org.egov.pt.validator.IssueFixValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service("applicationInactiveIssueFixService")
public class ApplicationInactiveIssueFixService implements IIssueFixService{

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private IssueFixValidator issueFixValidator;

    @Autowired
    private IssueFixRepository issueFixRepository;
    @Override
    public IssueFix issueFix(IssueFixRequest issueFixRequest) {

        issueFixValidator.validateRequestForInactiveIssue(issueFixRequest.getIssueFix());
        List<Property> propertyList = issueFixRepository.searchProperty(issueFixRequest.getIssueFix().getPropertyId());
        Property property=issueFixValidator.validatePropertySearchForInactiveProperty(propertyList);
        updateInactiveApplication(property);
        return issueFixRequest.getIssueFix();
    }

    @Transactional
    private void updateInactiveApplication(Property property) {
        issueFixRepository.updateInactiveApplicationStatus(property);
    }
}
