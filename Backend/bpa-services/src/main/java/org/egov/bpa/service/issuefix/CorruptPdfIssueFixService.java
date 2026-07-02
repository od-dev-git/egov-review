package org.egov.bpa.service.issuefix;

import org.apache.commons.lang.StringUtils;
import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.service.BPAService;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.*;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.common.contract.request.RequestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("corruptPdfIssueFixService")
public class CorruptPdfIssueFixService implements IIssueFixService{

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

		BPASearchCriteria bpaSearchCriteria = createBPASearchCriteria(issueFixRequest);

		List<BPA> bpaList = bpaService.search(bpaSearchCriteria, issueFixRequest.getRequestInfo());

		if (issueFixRequest.getIssueFix().getIssueSubtype().equalsIgnoreCase("PERMIT_DSC")) {
			issueFixValidator.validateIssueFixForCorruptPDF(bpaList);
			issueFixRepository.expireDSCDetails(bpaList.get(0).getDscDetails().get(0).getId());
		} else if (issueFixRequest.getIssueFix().getIssueSubtype().equalsIgnoreCase("BASE_LAYER_DSC")) {
			issueFixValidator.validateIssueFixForCorruptPlanPDF(bpaList);
			issueFixRepository.expirePlanDSCDetails(bpaList.get(0).getPlanDscDetails().get(0).getId());

		}

		return issueFixRequest.getIssueFix();
	}




}
