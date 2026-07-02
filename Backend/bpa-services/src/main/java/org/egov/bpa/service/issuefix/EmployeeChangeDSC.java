package org.egov.bpa.service.issuefix;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.service.BPAService;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service("employeeChangeDSC")
public class EmployeeChangeDSC implements IIssueFixService{

    @Autowired
    private IssueFixValidator issueFixValidator;

    @Autowired
    private BPAService bpaService;
    
    @Autowired
    private IssueFixRepository issueFixRepository;

    @Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		BPASearchCriteria bpaSearchCriteria = createBPASearchCriteria(issueFixRequest);
		
		String issueSubtype = Optional.ofNullable(issueFixRequest).map(IssueFixRequest::getIssueFix)
				.map(IssueFix::getIssueSubtype)
				.orElseThrow(() -> new IllegalArgumentException("Issue Subtype is missing in the request"));
		
		List<BPA> bpaList = bpaService.search(bpaSearchCriteria, issueFixRequest.getRequestInfo());
		List<String> data = issueFixRepository.getUUID(issueFixRequest.getIssueFix().getNewApprover(), null);

		if (!CollectionUtils.isEmpty(data) && data.size() > 1) {
			issueFixValidator.validateTenantId(issueFixRequest);
			data = issueFixRepository.getUUID(issueFixRequest.getIssueFix().getNewApprover(),
					issueFixRequest.getIssueFix().getTenantId());
		}

		switch (issueSubtype.toUpperCase()) {
		case "PERMIT_DSC":
			issueFixValidator.validateIssueFixForDSCChange(bpaList, data);
			break;

		case "BASE_LAYER_DSC":
			issueFixValidator.validateIssueFixForPlanDSCChange(bpaList, data);			
			break;

		default:
			throw new IllegalArgumentException("Unknown Issue Subtype: " + issueSubtype);
		}
		
		issueFixRepository.updateDscApprover(bpaList.get(0), data.get(0), issueSubtype);

		return issueFixRequest.getIssueFix();
	}


	public BPASearchCriteria createBPASearchCriteria(IssueFixRequest issueFixRequest) {
		BPASearchCriteria bpaSearchCriteria = BPASearchCriteria.builder()
				.tenantId(issueFixRequest.getIssueFix().getTenantId()).build();

		if (!StringUtils.isEmpty(issueFixRequest.getIssueFix().getApplicationNo()))
			bpaSearchCriteria.setApplicationNo(issueFixRequest.getIssueFix().getApplicationNo());

		if (!StringUtils.isEmpty(issueFixRequest.getIssueFix().getApprovalNo()))
			bpaSearchCriteria.setApprovalNo(issueFixRequest.getIssueFix().getApprovalNo());

		return bpaSearchCriteria;
	}
}
