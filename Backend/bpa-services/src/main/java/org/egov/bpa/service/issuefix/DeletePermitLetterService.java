package org.egov.bpa.service.issuefix;

import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.service.BPAService;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.Document;
import org.egov.bpa.web.model.DocumentList;
import org.egov.bpa.web.model.DscDetails;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service("deletePermitLetterService")
@Slf4j
public class DeletePermitLetterService implements IIssueFixService {

	@Autowired
	private BPARepository bpaRepository;

	@Autowired
	private IssueFixValidator issueFixValidator;

	@Autowired
	private BPAService bpaService;

	@Autowired
	private IssueFixRepository issueFixRepository;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		String applicationNo = issueFixRequest.getIssueFix().getApplicationNo();
		
		BPASearchCriteria searchCriteria = BPASearchCriteria.builder()
				.applicationNo(applicationNo)
				.build();
		List<BPA> bpaList = bpaRepository.getBPAData(searchCriteria, new ArrayList<String>());

		if (CollectionUtils.isEmpty(bpaList)) {
			throw new CustomException("SEARCH_ERROR", "No applications found with the mentioned application number !!");
		}

		if (!bpaList.get(0).getStatus().equalsIgnoreCase(IssueFixConstants.APPROVED)) {
			throw new CustomException("APPLICATION_STATUS_ERROR", "Application is not APPROVED Kindly Check !!");
		}
		issueFixValidator.validateIssueFixForDeletePermitLetter(bpaList);
		 
		issueFixRepository.deleteDSCDetails(applicationNo);

		return issueFixRequest.getIssueFix();
	}

}

