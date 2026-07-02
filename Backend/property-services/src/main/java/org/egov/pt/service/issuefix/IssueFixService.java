package org.egov.pt.service.issuefix;


import lombok.extern.slf4j.Slf4j;
import org.egov.pt.validator.IssueFixValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.pt.config.PropertyConfiguration;
import org.egov.pt.models.issuefix.IssueFix;
import org.egov.pt.models.issuefix.IssueFixRequest;
import org.egov.pt.models.issuefix.PaymentIssueFix;
import org.egov.pt.models.issuefix.StatusMismatchIssueFix;
import org.egov.pt.repository.IssueFixRepository;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Slf4j
@Service("issueFixService")
public class IssueFixService {

    @Autowired
    @Qualifier("applicationStatusMismatchIssueFix")
    private IIssueFixService applicationStatusMismatchIssueFix;

    @Autowired
    @Qualifier("paymentIssueFixService")
    private IIssueFixService paymentIssueFixService;

    @Autowired
    @Qualifier("applicationInactiveIssueFixService")
    private IIssueFixService applicationInactiveIssueFixService;
    
    @Autowired
    @Qualifier("stepBackService")
    private IIssueFixService stepBackService;

    @Autowired
    @Qualifier("assesmentStatusMismatchIssueFix")
    private IIssueFixService assesmentStatusMismatchIssueFix;
    
    @Autowired
    @Qualifier("assesmentCreateWithoutWorkflowIssueFix")
    private IIssueFixService assesmentCreateWithoutWorkflowIssueFix;
    
    @Autowired
    private IssueFixValidator issueFixValidator;
    
    @Autowired
    private PropertyConfiguration config;
    
    @Autowired
    private IssueFixRepository repository;

    public IssueFix issueFix(IssueFixRequest issueFixRequest, HttpHeaders headers) {

    	log.info("Fixing Property Issue ...");
        issueFixValidator.validateIssueFix(issueFixRequest);
        String issueName=issueFixRequest.getIssueFix().getIssueName();
        IssueFix issueFix=null;
        switch (issueName){

            case "APPLICATION_STATUS_MISMATCH_ISSUE":
                return applicationStatusMismatchIssueFix.issueFix(issueFixRequest);

            case "PAYMENT_ISSUE":
                return paymentIssueFixService.issueFix(issueFixRequest);

            case "APPLICATION_INACTIVE_ISSUE":
                return applicationInactiveIssueFixService.issueFix(issueFixRequest);
                
            case "STEP_BACK":
            	return stepBackService.issueFix(issueFixRequest);
            	
            case "ASSESSMENT_STATUS_MISMATCH_ISSUE":
            	return assesmentStatusMismatchIssueFix.issueFix(issueFixRequest);
            	
            case "ASSESSMENT_CREATE_WITHOUT_WORKFLOW":
            	return assesmentCreateWithoutWorkflowIssueFix.issueFix(issueFixRequest);

            default:
                throw new CustomException("UNKNOWN_ISSUE","The issue is unknown to the system");
        }
        
    }
    
	public void automatePaymentIssueFix(RequestInfo requestInfo) {
		if (config.getPtPaymentIssueFIx()) {
			List<PaymentIssueFix> paymentIssueApplications = new ArrayList<>();
			setUserDetails(requestInfo);
			log.info("Payment Issue Fix Scheduler Started Successfully. Time: {}", System.currentTimeMillis());
			paymentIssueApplications = repository.getPaymentIssueApplications();
			if (!CollectionUtils.isEmpty(paymentIssueApplications)) {
				log.info("Number of Payment Issue Found in PT at : {} :{}", System.currentTimeMillis(),
						paymentIssueApplications.size());

				processMutationFeePaymentIssues(paymentIssueApplications, "PAYMENT_ISSUE", requestInfo);
				log.info("Payment Issue Fix Scheduler Completed Successfully. Time: {}", System.currentTimeMillis());
			} else {
				log.info("No Payment Issue Found in PT at : {}", System.currentTimeMillis());
			}
		} else {
			log.info("PT Payment Issue Fix Config is : {}", config.getPtPaymentIssueFIx());
		}

	}
	
	private void processMutationFeePaymentIssues(List<PaymentIssueFix> mutationFeePaymentIssues, String issueType, RequestInfo requestInfo) {
		if (!CollectionUtils.isEmpty(mutationFeePaymentIssues)) {
			mutationFeePaymentIssues.forEach(paymentIssue -> {
				try {
					paymentIssueFixService
							.issueFix(IssueFixRequest.builder()
									.issueFix(IssueFix.builder().applicationNo(paymentIssue.getAcknowledgementNumber())
											.issueName(issueType).tenantId(paymentIssue.getTenantId()).build())
									.requestInfo(requestInfo)
									.build());
				} catch (Exception e) {
					log.error("Error processing mutation fee payment issue for application no: "
							+ paymentIssue.getAcknowledgementNumber() + " - " + e.getMessage());
					e.printStackTrace();
				}
			});
		}

	}
	
	private void setUserDetails(RequestInfo requestInfo) {
		Role role = Role.builder().code(config.getPtIssueFixRoleCode()).tenantId(config.getPtIssueFixTenantId())
				.build();
		User userInfo = User.builder().uuid(config.getPtIssueFixUUID()).type("EMPLOYEE").roles(Arrays.asList(role))
				.id(0L).build();
		requestInfo.setUserInfo(userInfo);

	}
	
	public void automateStatusMismatchIssueFix(RequestInfo requestInfo) {
		if (config.getPtStatusMismatchIssueFIx()) {
			setUserDetails(requestInfo);
			log.info("Status Mismatch Issue Fix Scheduler Started Successfully. Time: {}", System.currentTimeMillis());
			List<StatusMismatchIssueFix> statusMismatchIssueApplications = repository.getStatusMismatchApplications();
			if (!CollectionUtils.isEmpty(statusMismatchIssueApplications)) {
				log.info("Number of Status Mismatch Issue Found in PT at : {} :{}", System.currentTimeMillis(),
						statusMismatchIssueApplications.size());

				processStatusMismatchIssues(statusMismatchIssueApplications, "APPLICATION_STATUS_MISMATCH_ISSUE", requestInfo);
				log.info("Status Mismatch Issue Fix Scheduler Completed Successfully. Time: {}", System.currentTimeMillis());
			} else {
				log.info("No Status Mismatch Issue Found in PT at : {}", System.currentTimeMillis());
			}
		} else {
			log.info("PT Status Mismatch Issue Fix Config is : {}", config.getPtStatusMismatchIssueFIx());
		}

	}

	private void processStatusMismatchIssues(List<StatusMismatchIssueFix> statusMismatchIssues, String issueType, RequestInfo requestInfo) {
		if(!CollectionUtils.isEmpty(statusMismatchIssues)) {
			statusMismatchIssues.forEach(mismatchIssue -> {
			    try {
			    	applicationStatusMismatchIssueFix.issueFix(IssueFixRequest.builder()
			            .issueFix(IssueFix.builder()
			                .applicationNo(mismatchIssue.getApplicationNo())
			                .issueName(issueType)
			                .tenantId(mismatchIssue.getTenantId())
			                .build())
			                .requestInfo(requestInfo)
			                .build());
			    } catch (Exception e) {			       
			    	log.error("Error processing status mismatch issue for application number: " + mismatchIssue.getApplicationNo());
			        e.printStackTrace();
			    }
			});
		}

	}

}
