package org.egov.bpa.service.issuefix;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.repository.RegularizationRepository;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.collection.Demand;
import org.egov.bpa.web.model.collection.DemandSearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationSearchCriteria;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service("regDuplicateDemandIssue")
@Slf4j
public class RegularizationDuplicateDemandIssue implements RegularizationIIssueFixService {

	@Autowired
	private IssueFixRepository repository;
	
	@Autowired
	private IssueFixValidator issueFixValidator;

	@Autowired
	private RegularizationRepository regularizationRepository;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {

		String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();
		RegularizationSearchCriteria searchCriteria = createRegularizationSearchCriteria(issueFixRequest);

		List<Regularization> regularizations = regularizationRepository.searchRegularization(searchCriteria, issueFixRequest.getRequestInfo());
		issueFixValidator.validateRegularizationApplication(regularizations);
		DemandSearchCriteria demandSearchCriteria = DemandSearchCriteria.builder().consumerCode(applicationNumber).build();

		List<Demand> demands = repository.getDemands(demandSearchCriteria);

		if (CollectionUtils.isEmpty(demands) || demands.size() == 0) {
			throw new CustomException("DEMAND_ISSUE", "Found No Demands for mentioned Application Number");
		}
		
		Map<String, Integer> businessServiceCount = new HashMap<>();
		for (Demand demand : demands) {
		    String businessService = demand.getBusinessService();
		    businessServiceCount.put(businessService, businessServiceCount.getOrDefault(businessService, 0) + 1);

		    if (businessServiceCount.get(businessService) > 1) {
		        if (demand.getIsPaymentCompleted()) {
		            throw new CustomException("PAYMENT_ALREADY_DONE", "Payment is already done for business service ") ;
		        } else {
		            repository.deleteDemandDetail(demand); 
		            repository.deleteDemandV1(demand);
					log.info("deleted...");
		        }
		    } 
		}
		return IssueFix.builder().applicationNo(applicationNumber)
					.tenantId(issueFixRequest.getIssueFix().getTenantId()).build();

	}
}
		
		
		