package org.egov.bpa.service.issuefix;

import java.util.List;

import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service("duplicateInstallmentIssueFixService")
@Slf4j
public class DuplicateInstallmentIssueFixService implements IIssueFixService {

	@Autowired
	private IssueFixValidator validator;

	@Autowired
	private IssueFixRepository repository;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		List<String> data = repository.getInstallment(issueFixRequest.getIssueFix());
		List<String> duplicates = repository.checkDuplicates(issueFixRequest.getIssueFix());
		validator.validateInstallmentDuplicateIssueFix(data,duplicates);
		repository.updateInstallment(issueFixRequest.getIssueFix());

		return issueFixRequest.getIssueFix();

	}

}