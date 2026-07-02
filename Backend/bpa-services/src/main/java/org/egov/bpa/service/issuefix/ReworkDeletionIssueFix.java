package org.egov.bpa.service.issuefix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service("reworkDeletionService")
@Slf4j
public class ReworkDeletionIssueFix implements IIssueFixService {
	
	@Autowired
	private IssueFixValidator validator;

	@Autowired
	private IssueFixRepository repository;
	
	@Autowired
	private BPARepository bpaRepository;
	
	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {
		validator.validateIssueFix(issueFixRequest);

		String applicationNo = issueFixRequest.getIssueFix().getApplicationNo();

		String tenantId = issueFixRequest.getIssueFix().getTenantId();

		Integer reworkDeletionCount = issueFixRequest.getIssueFix().getReworkCount();

		if (reworkDeletionCount == null || reworkDeletionCount == 0 || reworkDeletionCount > 4) {
			throw new CustomException("INVALID_DATA", "Invalid Rework Deletion Count");
		}

		BPASearchCriteria searchCriteria = BPASearchCriteria.builder().tenantId(tenantId).applicationNo(applicationNo)
				.build();

		List<BPA> bpas = bpaRepository.getBPAData(searchCriteria, new ArrayList<String>());
		
		if (CollectionUtils.isEmpty(bpas) || bpas.size() > 1) {
			throw new CustomException("SEARCH_ERROR", "Either no or multiple applications found with the mentioned application number !!");
		}
		
		BPA bpa = bpas.get(0);
		
		List<Map<String, Object>> reworkHistory =  (List<Map<String, Object>>) bpa.getReWorkHistory();
		if(CollectionUtils.isEmpty(reworkHistory)) {
			throw new CustomException("NO_REWORK_HISTORY_FOUND", "Rework History is not exist");
		}
		List<Map<String, Object>> sortedReworkHistory = reworkHistory.stream()
		            .sorted(Comparator.comparing(entry -> (Long) entry.get("createdtime")))
		            .skip(reworkDeletionCount) 
		            .collect(Collectors.toList());
		
		bpa.setReWorkHistory(sortedReworkHistory);
		
		repository.updateReworkHistory(bpa);
		
		return issueFixRequest.getIssueFix();
	}

}
