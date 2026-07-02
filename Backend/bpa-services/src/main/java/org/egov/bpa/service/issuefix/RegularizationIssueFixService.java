package org.egov.bpa.service.issuefix;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.RegularizationIssueFixRepository;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.issuefix.PaymentIssueFix;
import org.egov.bpa.web.model.issuefix.StatusMismatchIssueFix;
import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.request.Role;
import org.egov.common.contract.request.User;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("regularizationIssueFixService")
public class RegularizationIssueFixService {
    
	@Autowired
    @Qualifier("regAppFeePaymentIssueFixService")
    private RegularizationIIssueFixService regAppFeePaymentIssueFixService;
    
    @Autowired
    @Qualifier("regSancFeePaymentIssueFixService")
    private RegularizationIIssueFixService regSancFeePaymentIssueFixService;

    @Autowired
    @Qualifier("regApplicationStatusMismatchIssueFixService")
    private RegularizationIIssueFixService regApplicationStatusMismatchIssueFixService;
	
    @Autowired
    @Qualifier("regStepBackToApprovalInprogress")
    private RegularizationIIssueFixService stepBackFix;
    
    @Autowired
    @Qualifier("regEmployeeChangeDSC")
    private RegularizationIIssueFixService regEmployeeChangeDSC;

    @Autowired
    @Qualifier("regDuplicateDscIssueFixService")
    private RegularizationIIssueFixService regDuplicateDscIssueFixService;

    @Autowired
    @Qualifier("regEmployeeMappingChange")
    private RegularizationIIssueFixService regEmployeeMappingChange;
    
    @Autowired
    @Qualifier("regDeletePermitLetterService")
    private RegularizationIIssueFixService regDeletePermitLetterService;
    
    @Autowired
    @Qualifier("regDuplicateDemandIssue")
    private RegularizationIIssueFixService regDuplicateDemandIssue;
    
    @Autowired
    @Qualifier("regIncorrectPermitFeeFixService")
    private RegularizationIIssueFixService regIncorrectPermitFeeFixService;
    
    @Autowired
    @Qualifier("regOneStepBackService")
    private RegularizationIIssueFixService regOneStepBackService;
    
    @Autowired
    private IssueFixValidator issueFixValidator;
    
    @Autowired
    private BPAConfiguration config;
    
    @Autowired
    private RegularizationIssueFixRepository repository;

    public IssueFix issueFix(IssueFixRequest issueFixRequest) {

        issueFixValidator.validateIssueFix(issueFixRequest);
        String issueName=issueFixRequest.getIssueFix().getIssueName();
        IssueFix issueFix=null;
        switch (issueName){

        	case "APP_FEE_PAYMENT_ISSUES":
        		issueFix =  regAppFeePaymentIssueFixService.issueFix(issueFixRequest);
            	break;
            case "SAN_FEE_PAYMENT_ISSUES":
            	issueFix = regSancFeePaymentIssueFixService.issueFix(issueFixRequest);
            	break;
            case "APPLICATION_STATUS_MISMATCH_ISSUE":
            	issueFix = regApplicationStatusMismatchIssueFixService.issueFix(issueFixRequest);
            	break;
            case "STEP_BACK_TO_APPROVAL_PENDING":
            	issueFix = stepBackFix.issueFix(issueFixRequest);
            	break;
            case "DSC_MAPPING_ISSUE":
                issueFix=regEmployeeChangeDSC.issueFix(issueFixRequest);
                break;
            case "DUPLICATE_DSC_ISSUE":
            	issueFix= regDuplicateDscIssueFixService.issueFix(issueFixRequest);
            	break;
            case "EMPLOYEE_MAPPING_CHANGE":
            	issueFix= regEmployeeMappingChange.issueFix(issueFixRequest);
            	break;
            case "DELETE_PERMIT_LETTER":
            	issueFix= regDeletePermitLetterService.issueFix(issueFixRequest);
            	break;
            case "DUPLICATE_DEMAND_ISSUE":
            	issueFix= regDuplicateDemandIssue.issueFix(issueFixRequest);
            	break;
            case "INCORRECT_PERMIT_FEE":
            	issueFix=  regIncorrectPermitFeeFixService.issueFix(issueFixRequest);
            	break;
            case "ONE_STEP_BACK":
            	issueFix=  regOneStepBackService.issueFix(issueFixRequest);
            	break;
            default:
                throw new CustomException("UNKNOWN_ISSUE","The issue is unknown to the system");
        }
        return issueFix;
    }

	public void automatePaymentIssueFix(RequestInfo requestInfo) {

		if (config.getRegularizationPaymentIssuefix()) {
			setUserDetails(requestInfo);
			log.info("Payment Issue Fix Regularization Scheduler Started Successfully. Time: {}",
					System.currentTimeMillis());
			List<PaymentIssueFix> paymentIssueApplications = repository.getPaymentIssueApplications();
			if (!CollectionUtils.isEmpty(paymentIssueApplications)) {
				log.info("Number of Payment Issue Found in Regularization at : {} :{}", System.currentTimeMillis(),
						paymentIssueApplications.size());
				Map<String, List<PaymentIssueFix>> groupedByBusinessService = paymentIssueApplications.stream()
						.collect(Collectors.groupingBy(PaymentIssueFix::getBusinessService));

				processAppFeePaymentIssues(groupedByBusinessService.get("BPA.REG_APP_FEE"), "APP_FEE_PAYMENT_ISSUES",
						requestInfo);
				processSancFeePaymentIssues(groupedByBusinessService.get("BPA.REG_SAN_FEE"), "SAN_FEE_PAYMENT_ISSUES",
						requestInfo);
				log.info("Payment Issue Fix Regularization Scheduler Completed Successfully. Time: {}",
						System.currentTimeMillis());
			} else {
				log.info("No Payment Issue Found in Regularization at : {}", System.currentTimeMillis());
			}
		} else {
			log.info("Regularization Payment Issue Fix Config is : {}", config.getRegularizationPaymentIssuefix());
		}

	}

	private void processSancFeePaymentIssues(List<PaymentIssueFix> sancFeePaymentIssues, String issueType,
			RequestInfo requestInfo) {

		if (!CollectionUtils.isEmpty(sancFeePaymentIssues)) {
			sancFeePaymentIssues.forEach(paymentIssue -> {
				try {
					regSancFeePaymentIssueFixService.issueFix(IssueFixRequest.builder()
							.issueFix(IssueFix.builder().applicationNo(paymentIssue.getApplicationNo())
									.issueName(issueType).tenantId(paymentIssue.getTenantId()).build())
							.requestInfo(requestInfo).build());
				} catch (Exception e) {
					log.error("Error processing sanc fee payment issue for application number: "
							+ paymentIssue.getApplicationNo());
					e.printStackTrace();
				}
			});
		}

	}

	private void processAppFeePaymentIssues(List<PaymentIssueFix> appFeePaymentIssues, String issueType,
			RequestInfo requestInfo) {
		if (!CollectionUtils.isEmpty(appFeePaymentIssues)) {
			appFeePaymentIssues.forEach(paymentIssue -> {
				try {
					regAppFeePaymentIssueFixService.issueFix(IssueFixRequest.builder()
							.issueFix(IssueFix.builder().applicationNo(paymentIssue.getApplicationNo())
									.issueName(issueType).tenantId(paymentIssue.getTenantId()).build())
							.requestInfo(requestInfo).build());
				} catch (Exception e) {
					log.error("Error processing app fee payment issue for application no: "
							+ paymentIssue.getApplicationNo() + " - " + e.getMessage());
					e.printStackTrace();
				}
			});
		}

	}

	private void setUserDetails(RequestInfo requestInfo) {

		Role role = Role.builder().code(config.getBpaIssueFixRoleCode()).tenantId(config.getBpaIssueFixTenantId())
				.build();
		User userInfo = User.builder().uuid(config.getBpaIssueFixUUID()).type("EMPLOYEE").roles(Arrays.asList(role))
				.id(0L).build();
		requestInfo.setUserInfo(userInfo);

	}

	public void automateStatusMismatchIssueFix(RequestInfo requestInfo) {
		
		if (config.getRegularizationStatusMismatchIssuefix()) {
			setUserDetails(requestInfo);
			log.info("Status Mismatch Issue Fix Regularization Scheduler Started Successfully. Time: {}", System.currentTimeMillis());
			List<StatusMismatchIssueFix> statusMismatchIssueApplications = repository.getStatusMismatchApplications();
			if (!CollectionUtils.isEmpty(statusMismatchIssueApplications)) {
				log.info("Number of Status Mismatch Issue Found in Regularization at : {} :{}", System.currentTimeMillis(),
						statusMismatchIssueApplications.size());

				processStatusMismatchIssues(statusMismatchIssueApplications, "APPLICATION_STATUS_MISMATCH_ISSUE", requestInfo);
				log.info("Status Mismatch Issue Fix Regularization Scheduler Completed Successfully. Time: {}", System.currentTimeMillis());
			} else {
				log.info("No Status Mismatch Issue Found in Regularization at : {}", System.currentTimeMillis());
			}
		} else {
			log.info("Regularization Status Mismatch Issue Fix Config is : {}", config.getRegularizationStatusMismatchIssuefix());
		}
		
	}

	private void processStatusMismatchIssues(List<StatusMismatchIssueFix> statusMismatchIssues, String issueType,
			RequestInfo requestInfo) {

		if (!CollectionUtils.isEmpty(statusMismatchIssues)) {
			statusMismatchIssues.forEach(mismatchIssue -> {
				try {
					regApplicationStatusMismatchIssueFixService.issueFix(IssueFixRequest.builder()
							.issueFix(IssueFix.builder().applicationNo(mismatchIssue.getApplicationNo())
									.issueName(issueType).tenantId(mismatchIssue.getTenantId()).build())
							.requestInfo(requestInfo).build());
				} catch (Exception e) {
					log.error("Error processing status mismatch issue for application number: "
							+ mismatchIssue.getApplicationNo());
					e.printStackTrace();
				}
			});
		}

	}

}
