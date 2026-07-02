package org.egov.bpa.service.issuefix;

import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.DscDetails;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service("duplicateDscIssueFixService")
@Slf4j
public class DuplicateDscIssueFixService implements IIssueFixService {

	@Autowired
	private IssueFixValidator validator;

	@Autowired
	private IssueFixRepository repository;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		List<String> data = repository.getDSC(issueFixRequest.getIssueFix());
		validator.validateDscDuplicateIssueFix(data);
		repository.updateDSC(issueFixRequest.getIssueFix());

		return null;

	}

}