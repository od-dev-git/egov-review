package org.egov.bpa.service.issuefix;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.IssueFixRepository;
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

@Service("issueFixService")
@Slf4j
public class IssueFixService {

    @Autowired
    @Qualifier("corruptPdfIssueFixService")
    private IIssueFixService corruptPdfIssueFixService;
    
    @Autowired
    @Qualifier("corruptPlanPdfIssueFixService")
    private IIssueFixService corruptPlanPdfIssueFixService;
    
    @Autowired
    @Qualifier("appFeePaymentIssueFixService")
    private IIssueFixService appFeePaymentIssueFixService;
    
    @Autowired
    @Qualifier("sancFeePaymentIssueFixService")
    private IIssueFixService sancFeePaymentIssueFixService;

    @Autowired
    @Qualifier("applicationStatusMismatchIssueFixService")
    private IIssueFixService applicationStatusMismatchIssueFixService;
    
    @Autowired
    @Qualifier("employeeChangeDSC")
    private IIssueFixService employeeChangeDSC;

    @Autowired
    @Qualifier("duplicateDscIssueFixService")
    private IIssueFixService duplicateDscIssueFixService;
    
    @Autowired
    @Qualifier("DuplicateDemandIssue")
    private IIssueFixService DuplicateDemandIssue;

    @Autowired
    @Qualifier("employeeMappingChange")
    private IIssueFixService employeeMappingChange;
    
    @Autowired
    @Qualifier("technicalPersonDocIssueFix")
    private IIssueFixService technicalPersonDocIssueFix;
    
    @Autowired
    @Qualifier("stepBackToApprovalInprogress")
    private IIssueFixService stepBackFix;
    
    @Autowired
    @Qualifier("duplicateInstallmentIssueFixService")
    private IIssueFixService duplicateInstallmentIssueFixService;
    
    @Autowired
    @Qualifier("incorrectPermitFeeService")
    private IIssueFixService incorrectPermitFeeService;
    
    @Autowired
    @Qualifier("bpasignedbpldocumentdeletion")
    private IIssueFixService bpasignedbpldocumentdeletion;
    
    @Autowired
    @Qualifier("deleteBpaApplicationService")
    private IIssueFixService deleteBpaApplicationService;
    
    @Autowired
    @Qualifier("deleteOtherFeesService")
    private IIssueFixService deleteOtherFeesService;
    
    @Autowired
    @Qualifier("deletePermitLetterService")
    private IIssueFixService deletePermitLetterService;
    
    @Autowired
    @Qualifier("oneStepBackService")
    private IIssueFixService oneStepBackService;
    
    @Autowired
    @Qualifier("reworkDeletionService")
    private IIssueFixService reworkDeletionService;
      
    @Autowired
    private RevenueVillageService revenueVillageService;
    
    @Autowired
	private IssueFixRepository repository;

    @Autowired
    private IssueFixValidator issueFixValidator;
    
    @Autowired
    private BPAConfiguration config;    
    

    public IssueFix issueFix(IssueFixRequest issueFixRequest) {

        issueFixValidator.validateIssueFix(issueFixRequest);
        String issueName=issueFixRequest.getIssueFix().getIssueName();
        IssueFix issueFix=null;
        switch (issueName){

            case "CORRUPT_PDF_ISSUE":
                issueFix=corruptPdfIssueFixService.issueFix(issueFixRequest);
                break;
                
            case "CORRUPT_PLAN_PDF_ISSUE":
                issueFix=corruptPlanPdfIssueFixService.issueFix(issueFixRequest);
                break;    
                
            case "APP_FEE_PAYMENT_ISSUES":
            	return appFeePaymentIssueFixService.issueFix(issueFixRequest);
            	
            case "SAN_FEE_PAYMENT_ISSUES":
            	return sancFeePaymentIssueFixService.issueFix(issueFixRequest);

            case "APPLICATION_STATUS_MISMATCH_ISSUE":
                return applicationStatusMismatchIssueFixService.issueFix(issueFixRequest);
             
            case "DSC_ISSUE":
                issueFix=employeeChangeDSC.issueFix(issueFixRequest);
                break;
            
            case "DUPLICATE_DSC_ISSUE":
                return duplicateDscIssueFixService.issueFix(issueFixRequest);
                
            case "DUPLICATE_DEMAND_ISSUE":
                return DuplicateDemandIssue.issueFix(issueFixRequest);

            case "EMPLOYEE_MAPPING_CHANGE":
                return employeeMappingChange.issueFix(issueFixRequest);
                
            case "STEP_BACK_TO_APPROVAL_PENDING":
            	return stepBackFix.issueFix(issueFixRequest);
            	
            case "TECHNICAL_PERSON_DOC_ISSUE_FIX":
                return technicalPersonDocIssueFix.issueFix(issueFixRequest);
                
            case "DUPLICATE_INSTALLMENT_ISSUE":
            	return duplicateInstallmentIssueFixService.issueFix(issueFixRequest);
            	
            case "INCORRECT_PERMIT_FEE":
            	return incorrectPermitFeeService.issueFix(issueFixRequest);
            	
            case "BPA_SIGNED_BPL_DOCUMENT_DELETION":
            	return bpasignedbpldocumentdeletion.issueFix(issueFixRequest);
            
            case "DELETE_BPA_APPLICATION":
            	return deleteBpaApplicationService.issueFix(issueFixRequest);
            	
            case "DELETE_OTHER_FEES_IN_ADDITIONAL_DETAILS":
            	return deleteOtherFeesService.issueFix(issueFixRequest);
                
			case "DELETE_PERMIT_LETTER":
            	return deletePermitLetterService.issueFix(issueFixRequest);
            	
			case "ONE_STEP_BACK":
            	return oneStepBackService.issueFix(issueFixRequest);
            	
			case "REWORK_HISTORY_DELETION_ISSUE":
            	return reworkDeletionService.issueFix(issueFixRequest);
            	
            default:
                throw new CustomException("UNKNOWN_ISSUE","The issue is unknown to the system");
        }
        return issueFix;
    }
    
	public IssueFix getRevenueVillages(IssueFixRequest issueFixRequest) {
		IssueFix issueFix = revenueVillageService.issueFix(issueFixRequest);
		return issueFix;
	}
	
	public void automatePaymentIssueFix(RequestInfo requestInfo) {
		if (config.getBpaPaymentIssuefix()) {
			setUserDetails(requestInfo);
			log.info("Payment Issue Fix Scheduler Started Successfully. Time: {}", System.currentTimeMillis());
			List<PaymentIssueFix> paymentIssueApplications = repository.getPaymentIssueApplications();
			if (!CollectionUtils.isEmpty(paymentIssueApplications)) {
				log.info("Number of Payment Issue Found in BPA at : {} :{}", System.currentTimeMillis(),
						paymentIssueApplications.size());
				Map<String, List<PaymentIssueFix>> groupedByBusinessService = paymentIssueApplications.stream()
						.collect(Collectors.groupingBy(PaymentIssueFix::getBusinessService));

				processAppFeePaymentIssues(groupedByBusinessService.get("BPA.NC_APP_FEE"), "APP_FEE_PAYMENT_ISSUES", requestInfo);
				processSancFeePaymentIssues(groupedByBusinessService.get("BPA.NC_SAN_FEE"), "SANC_FEE_PAYMENT_ISSUES", requestInfo);
				log.info("Payment Issue Fix Scheduler Completed Successfully. Time: {}", System.currentTimeMillis());
			} else {
				log.info("No Payment Issue Found in BPA at : {}", System.currentTimeMillis());
			}
		} else {
			log.info("BPA Payment Issue Fix Config is : {}", config.getBpaPaymentIssuefix());
		}

	}

	private void processAppFeePaymentIssues(List<PaymentIssueFix> appFeePaymentIssues, String issueType, RequestInfo requestInfo) {
		if (!CollectionUtils.isEmpty(appFeePaymentIssues)) {
			appFeePaymentIssues.forEach(paymentIssue -> {
				try {
					appFeePaymentIssueFixService
							.issueFix(IssueFixRequest.builder()
									.issueFix(IssueFix.builder().applicationNo(paymentIssue.getApplicationNo())
											.issueName(issueType).tenantId(paymentIssue.getTenantId()).build())
									.requestInfo(requestInfo)
									.build());
				} catch (Exception e) {
					log.error("Error processing app fee payment issue for application no: "
							+ paymentIssue.getApplicationNo() + " - " + e.getMessage());
					e.printStackTrace();
				}
			});
		}

	}
	
	private void processSancFeePaymentIssues(List<PaymentIssueFix> sancFeePaymentIssues, String issueType, RequestInfo requestInfo) {
		if(!CollectionUtils.isEmpty(sancFeePaymentIssues)) {
			sancFeePaymentIssues.forEach(paymentIssue -> {
			    try {
			        sancFeePaymentIssueFixService.issueFix(IssueFixRequest.builder()
			            .issueFix(IssueFix.builder()
			                .applicationNo(paymentIssue.getApplicationNo())
			                .issueName(issueType)
			                .tenantId(paymentIssue.getTenantId())
			                .build())
			                .requestInfo(requestInfo)
			            .build());
			    } catch (Exception e) {			       
			    	log.error("Error processing sanc fee payment issue for application number: " + paymentIssue.getApplicationNo());
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
		if (config.getBpaStatusMismatchIssuefix()) {
			setUserDetails(requestInfo);
			log.info("Status Mismatch Issue Fix Scheduler Started Successfully. Time: {}", System.currentTimeMillis());
			List<StatusMismatchIssueFix> statusMismatchIssueApplications = repository.getStatusMismatchApplications();
			if (!CollectionUtils.isEmpty(statusMismatchIssueApplications)) {
				log.info("Number of Status Mismatch Issue Found in BPA at : {} :{}", System.currentTimeMillis(),
						statusMismatchIssueApplications.size());

				processStatusMismatchIssues(statusMismatchIssueApplications, "APPLICATION_STATUS_MISMATCH_ISSUE", requestInfo);
				log.info("Status Mismatch Issue Fix Scheduler Completed Successfully. Time: {}", System.currentTimeMillis());
			} else {
				log.info("No Status Mismatch Issue Found in BPA at : {}", System.currentTimeMillis());
			}
		} else {
			log.info("BPA Status Mismatch Issue Fix Config is : {}", config.getBpaPaymentIssuefix());
		}

	}
	
	private void processStatusMismatchIssues(List<StatusMismatchIssueFix> statusMismatchIssues, String issueType, RequestInfo requestInfo) {
		if(!CollectionUtils.isEmpty(statusMismatchIssues)) {
			statusMismatchIssues.forEach(mismatchIssue -> {
			    try {
			    	applicationStatusMismatchIssueFixService.issueFix(IssueFixRequest.builder()
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
