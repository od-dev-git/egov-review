package org.egov.bpa.service.issuefix;

import java.util.ArrayList;
import java.util.List;

import org.egov.bpa.repository.BPARepository;
import org.egov.bpa.repository.IssueFixRepository;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.BPASearchCriteria;
import org.egov.bpa.web.model.collection.Payment;
import org.egov.bpa.web.model.collection.PaymentSearchCriteria;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Service("deleteBpaApplicationService")
@Slf4j

public class DeleteBpaApplicationService implements IIssueFixService {

	@Autowired
	private BPARepository bpaRepository;

	@Autowired
	private IssueFixValidator validator;

	@Autowired
	private IssueFixRepository repository;

	@Autowired
	private IssueFixRepository issueFixRepository;

	@Override
	public IssueFix issueFix(IssueFixRequest issueFixRequest) {
		validator.validateIssueFix(issueFixRequest);
		
		String applicationNo = issueFixRequest.getIssueFix().getApplicationNo();
		BPASearchCriteria searchCriteria = BPASearchCriteria.builder().applicationNo(applicationNo).build();
       		
		List<BPA> bpa = bpaRepository.getBPAData(searchCriteria, new ArrayList<String>());

		if (CollectionUtils.isEmpty(bpa)) {
			throw new CustomException("SEARCH_ERROR", "No applications found with the mentioned application number !!");
		}

		Boolean isPaymentReceived = checkIfPaymentReceived(issueFixRequest);
		
		if(!isPaymentReceived) {
			updateApplicationDetails(issueFixRequest);
		} else {
			throw new CustomException("PAYMENT_ISSUE", "Application fee payment has been done for application no :" + applicationNo + " Can't delete mentioned application number !");
		}
		return issueFixRequest.getIssueFix();
	}

	
	/**
	 * @param issueFixRequest
	 * 
	 * Delete all data related with the application no
	 */
	@Transactional
	private void updateApplicationDetails(IssueFixRequest issueFixRequest) {
		issueFixRepository.updateApplicationStatus(issueFixRequest.getIssueFix());
		
	}

	
	/**
	 * @param issueFixRequest
	 * 
	 * @return isPaymentReceived
	 */
	private Boolean checkIfPaymentReceived(IssueFixRequest issueFixRequest) {
		Boolean isPaymentReceived = Boolean.FALSE;
		String tenantId = issueFixRequest.getIssueFix().getTenantId();

		String applicationNumber = issueFixRequest.getIssueFix().getApplicationNo();

		PaymentSearchCriteria paymentSearchCriteria = PaymentSearchCriteria.builder().consumerCode(applicationNumber)
				.businessService(IssueFixConstants.BPA_APP_FEE).tenantId(tenantId).build();

		List<Payment> payments = repository.getPayments(paymentSearchCriteria);
		
		if (payments.size() >= 1) {
			isPaymentReceived = Boolean.TRUE;
			throw new CustomException("PAYMENT_ISSUE", "Application fee payment has been done for application no :" + applicationNumber + " Can't delete mentioned application number !");
		}
		
		log.info("Application Fee payment has not been done.. Moving to next step !!");
		return isPaymentReceived;
	}

	
}

