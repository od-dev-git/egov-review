package org.egov.bpa.service.issuefix;

import java.util.List;

import org.egov.bpa.repository.RegularizationIssueFixRepository;
import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("regDeletePermitLetterService")
public class RegularizationDeletePermitLetterService implements RegularizationIIssueFixService {

	
	@Autowired
	private RegularizationRepository regularizationRepository;

	@Autowired
	private IssueFixValidator issueFixValidator;

	@Autowired
	private RegularizationIssueFixRepository issueFixRepository;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		RegularizationSearchCriteria searchCriteria = createRegularizationSearchCriteria(issueFixRequest);

		List<Regularization> regularizations = regularizationRepository.searchRegularization(searchCriteria, issueFixRequest.getRequestInfo());
		issueFixValidator.validateRegularizationApplication(regularizations);
		issueFixValidator.validateRegularizationDeletePermitLetter(regularizations);
		
		issueFixRepository.deleteRegularizationDSCDetails(issueFixRequest.getIssueFix().getApplicationNo());
		return issueFixRequest.getIssueFix();
	}

}

