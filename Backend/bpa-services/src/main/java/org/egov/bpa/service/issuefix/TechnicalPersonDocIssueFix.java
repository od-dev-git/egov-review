package org.egov.bpa.service.issuefix;

import java.util.List;

import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("technicalPersonDocIssueFix")
public class TechnicalPersonDocIssueFix implements IIssueFixService{
	
	@Autowired
	public IssueFixRepository repository;
	
	@Autowired	
	public IssueFixValidator issueFixValidator;
	
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {
		
		List<String> data=repository.getIDs(issueFixRequest.getIssueFix().getApplicationNo());
		issueFixValidator.validateTechnicalPersonDocIssueFix(data);
        repository.update(data);
		
		return null;
		
	}

}
