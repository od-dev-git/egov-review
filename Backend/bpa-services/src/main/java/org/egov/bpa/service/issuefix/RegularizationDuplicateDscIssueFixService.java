package org.egov.bpa.service.issuefix;

import java.util.List;

import org.egov.bpa.repository.RegularizationIssueFixRepository;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("regDuplicateDscIssueFixService")
public class RegularizationDuplicateDscIssueFixService implements RegularizationIIssueFixService {

	@Autowired
	private IssueFixValidator validator;

	@Autowired
	private RegularizationIssueFixRepository repository;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		List<String> data = repository.getRegularizationDSC(issueFixRequest.getIssueFix());
		validator.validateDscDuplicateIssueFix(data);
		repository.updateRegularizationDSC(issueFixRequest.getIssueFix());

		return issueFixRequest.getIssueFix();
	}

}