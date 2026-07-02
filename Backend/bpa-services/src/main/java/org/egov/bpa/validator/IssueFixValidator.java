package org.egov.bpa.validator;

import org.apache.commons.lang.StringUtils;
import org.egov.bpa.util.BPAConstants;
import org.egov.bpa.util.IssueFixConstants;
import org.egov.bpa.web.model.BPA;
import org.egov.bpa.web.model.DscDetails;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.regularization.Regularization;
import org.egov.bpa.web.model.regularization.RegularizationDscDetails;
import org.egov.bpa.web.model.workflow.ProcessInstance;
import org.egov.tracer.model.CustomException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Component
public class IssueFixValidator {

    public void validateIssueFix(IssueFixRequest issueFixRequest){

        if(StringUtils.isEmpty(issueFixRequest.getIssueFix().getIssueName())){
            throw new CustomException("EMPTY_ISSUE_NAME","Issue Name passed is null or empty");
        }

        if(StringUtils.isEmpty(issueFixRequest.getIssueFix().getApplicationNo()) && StringUtils.isEmpty(issueFixRequest.getIssueFix().getApprovalNo())){
            throw new CustomException("INVALID_DATA","Application Number and Approval Number passed both can't be empty");
        }
    }

    public void validateIssueFixForCorruptPDF(List<BPA> bpaList){
        if(bpaList.size()==0)
            throw new CustomException("INVALID_DATA","No BPA Applications were found for the given criteria.");

        if(bpaList.size()>=2)
            throw new CustomException("INVALID_DATA","Multiple BPA Applications were found for the given criteria.");

        List<DscDetails> dscDetails=bpaList.get(0).getDscDetails();

        if(dscDetails.size()==0)
            throw new CustomException("INVALID_DATA","DSC details is empty for the given BPA Application.");

        if(dscDetails.size()>=2)
            throw new CustomException("INVALID_DATA","Multiple DSC Details record were found for the given BPA Application.");

        if(dscDetails.get(0).getDocumentType()==null && dscDetails.get(0).getDocumentId()==null)
            throw new CustomException("INVALID_DATA","DSC document type and id is already null for the given BPA Application.");

    }
    
    public void validateIssueFixForCorruptPlanPDF(List<BPA> bpaList){
        if(bpaList.size()==0)
            throw new CustomException("INVALID_DATA","No BPA Applications were found for the given criteria.");

        if(bpaList.size()>=2)
            throw new CustomException("INVALID_DATA","Multiple BPA Applications were found for the given criteria.");

        List<DscDetails> dscDetails=bpaList.get(0).getPlanDscDetails();

        if(dscDetails.size()==0)
            throw new CustomException("INVALID_DATA","DSC details is empty for the given BPA Application.");

        if(dscDetails.size()>=2)
            throw new CustomException("INVALID_DATA","Multiple DSC Details record were found for the given BPA Application.");

        if(dscDetails.get(0).getDocumentType()==null && dscDetails.get(0).getDocumentId()==null)
            throw new CustomException("INVALID_DATA","DSC document type and id is already null for the given BPA Application.");

    }
    
    public void validatePaymentIssueRequest(IssueFixRequest issueFixRequest) {

		IssueFix issueFix = issueFixRequest.getIssueFix();
		if (StringUtils.isEmpty(issueFix.getApplicationNo())) {
			throw new CustomException("INVALID_REQUEST",
					"Application Number is mandatory for Payment Issues, Kindly provide application number to proceed !!");
		}
	}

    public void validateProcessInstanceApplicationStatusMismatch(BPA bpa, List<ProcessInstance> processInstance) {

        if(CollectionUtils.isEmpty(processInstance)){
            throw new CustomException("INVALID_DATA","No Data was found in Process Instances");
        }
        ProcessInstance currentProcessInstance = processInstance.get(0);
        if(currentProcessInstance.getState().getApplicationStatus().equalsIgnoreCase(bpa.getStatus())){
            throw new CustomException("INVALID_INPUT","The Application data is having no mismatch");
        }
    }

    public void validateWaterConnectionApplicationStatusMismatch(List<BPA> bpaList) {
        if(CollectionUtils.isEmpty(bpaList)){
            throw new CustomException("INVALID_INPUT","No Water connection Data was Found ");
        }
        if(bpaList.size()>=2){
            throw new CustomException("INVALID_DATA","Multiple Water Connection applications were found");
        }}
        
	public void validateIssueFixForDSCChange(List<BPA> bpaList, List<String> data) {
		if (CollectionUtils.isEmpty(bpaList))
			throw new CustomException("INVALID_DATA", "No BPA Applications were found for the given criteria.");

		if (bpaList.size() >= 2)
			throw new CustomException("INVALID_DATA", "Multiple BPA Applications were found for the given criteria.");

		List<DscDetails> dscDetails = bpaList.get(0).getDscDetails();

		if (CollectionUtils.isEmpty(dscDetails))
			throw new CustomException("INVALID_DATA", "DSC details is empty for the given BPA Application.");

		if (dscDetails.size() >= 2)
			throw new CustomException("INVALID_DATA",
					"Multiple DSC Details record were found for the given BPA Application.");

		if (CollectionUtils.isEmpty(data))
			throw new CustomException("INVALID_DATA", "No approvers were found for the given ID.");

		if (data.size() >= 2)
			throw new CustomException("INVALID_DATA", "Multiple approvers were found for the given ID.");

	}
	
	public void validateIssueFixForPlanDSCChange(List<BPA> bpaList, List<String> data) {
		if (CollectionUtils.isEmpty(bpaList))
			throw new CustomException("INVALID_DATA", "No BPA Applications were found for the given criteria.");

		if (bpaList.size() >= 2)
			throw new CustomException("INVALID_DATA", "Multiple BPA Applications were found for the given criteria.");

		List<DscDetails> planDscDetails = bpaList.get(0).getPlanDscDetails();

		if (CollectionUtils.isEmpty(planDscDetails))
			throw new CustomException("INVALID_DATA", "Plan DSC details is empty for the given BPA Application.");

		if (planDscDetails.size() >= 2)
			throw new CustomException("INVALID_DATA",
					"Multiple Plan DSC Details record were found for the given BPA Application.");

		if (CollectionUtils.isEmpty(data))
			throw new CustomException("INVALID_DATA", "No approvers were found for the given ID.");

		if (data.size() >= 2)
			throw new CustomException("INVALID_DATA", "Multiple approvers were found for the given ID.");

	}

	  public void validateTenantId(IssueFixRequest issueFixRequest){

         if(StringUtils.isEmpty(issueFixRequest.getIssueFix().getTenantId())){
            throw new CustomException("INVALID_DATA","Tenant id is mandatory for village List");
        }
    }

	public void validateDscDuplicateIssueFix(List<String> dscList ){

    if(CollectionUtils.isEmpty(dscList)){
        throw new CustomException("INVALID_DATA","DSC details is empty for the given BPA Application.");
    }
}
	
	public void validateIssueFixForEmpMapping(List<String> data, List<String> processInstanceId) {
		
		if(CollectionUtils.isEmpty(data)){
	        throw new CustomException("INVALID_DATA","No assignees were found for the given ID. ");
	    }
		
		if(data.size()>=2)
            throw new CustomException("INVALID_DATA","Multiple assignees were found wigth given ID.");
		
		if(CollectionUtils.isEmpty(processInstanceId)){
	        throw new CustomException("INVALID_DATA","Application is in different action state");
		
	}
		}
	
    
    public void validateTechnicalPersonDocIssueFix(List<String> data) {
		
		if(CollectionUtils.isEmpty(data)){
	        throw new CustomException("INVALID_DATA","No application is found with details given");
		}
    }
    
    public void validateInstallmentDuplicateIssueFix(List<String> installmentList,List<String> duplicates ){

	    if(CollectionUtils.isEmpty(installmentList)){
	        throw new CustomException("INVALID_DATA","No installments were found for given details.");
	    }
	    
	    if(CollectionUtils.isEmpty(duplicates)){
	        throw new CustomException("INVALID_DATA","Installment details has no duplicates.");
	    }
	}

    
	public void validateIssueFixForDeletePermitLetter(List<BPA> bpaList) {
		List<DscDetails> dscDetails=bpaList.get(0).getDscDetails();

        if(dscDetails.size()==0)
            throw new CustomException("INVALID_DATA","DSC details is empty for the given BPA Application.");

        if(dscDetails.size()>=2)
            throw new CustomException("INVALID_DATA","Multiple DSC Details record were found for the given BPA Application.");

        if(dscDetails.get(0).getDocumentType()==null && dscDetails.get(0).getDocumentId()==null)
            throw new CustomException("INVALID_DATA","DSC document type and id is already null for the given BPA Application.");
	
	}

	
	public void validateRegularizationApplicationStatusMismatch(List<Regularization> regularizations) {
		if (CollectionUtils.isEmpty(regularizations)) {
			throw new CustomException("INVALID_INPUT", "No Data was Found for the given Regularization Application.");
		} 
		if (regularizations.size() >= 2) {
			throw new CustomException("INVALID_DATA", "Multiple Regularization applications were found");
		}
	}

	public void validateProcessInstanceRegularizationStatusMismatch(Regularization regularization, List<ProcessInstance> processInstance) {

        if(CollectionUtils.isEmpty(processInstance)){
            throw new CustomException("INVALID_DATA","No Data was found in Process Instances");
        }
        ProcessInstance currentProcessInstance = processInstance.get(0);
        if(currentProcessInstance.getState().getApplicationStatus().equalsIgnoreCase(regularization.getStatus())){
            throw new CustomException("INVALID_INPUT","The Application data is having no mismatch");
        }
    }

	public void validateIssueFixForRegularizationDSCChange(List<Regularization> regularizations, List<String> data) {
		if(CollectionUtils.isEmpty(regularizations))
            throw new CustomException("INVALID_DATA","No Regularization Applications were found for the given criteria.");

        if(regularizations.size()>=2)
            throw new CustomException("INVALID_DATA","Multiple Regularization Applications were found for the given criteria.");

        List<RegularizationDscDetails> dscDetails= regularizations.get(0).getDscDetails();

        if(CollectionUtils.isEmpty(dscDetails))
            throw new CustomException("INVALID_DATA","DSC details is empty for the given Regularization Application.");

        if(dscDetails.size()>=2)
            throw new CustomException("INVALID_DATA","Multiple DSC Details record were found for the given Regularization Application.");
        
        if(CollectionUtils.isEmpty(data))
            throw new CustomException("INVALID_DATA","No approvers were found for the given ID.");

        if(data.size()>=2)
            throw new CustomException("INVALID_DATA","Multiple approvers were found for the given ID.");
		
	}

	public void validateRegularizationApplication(List<Regularization> regularizations) {
		if(CollectionUtils.isEmpty(regularizations))
            throw new CustomException("INVALID_DATA","No Regularization Applications were found for the given criteria.");

        if(regularizations.size()>=2)
            throw new CustomException("INVALID_DATA","Multiple Regularization Applications were found for the given criteria.");
	}

	public void validateRegularizationDeletePermitLetter(List<Regularization> regularizations) {
		
		if (!regularizations.get(0).getStatus().equalsIgnoreCase(IssueFixConstants.APPROVED)) {
			throw new CustomException("APPLICATION_STATUS_ERROR", "Application is not APPROVED Kindly Check !!");
		}
		
		List<RegularizationDscDetails> dscDetails=regularizations.get(0).getDscDetails();

        if(dscDetails.size()==0)
            throw new CustomException("INVALID_DATA","DSC details is empty for the given Regularization Application.");

        if(dscDetails.size()>=2)
            throw new CustomException("INVALID_DATA","Multiple DSC Details record were found for the given Regularization Application.");

        if(dscDetails.get(0).getDocumentType()==null && dscDetails.get(0).getDocumentId()==null)
            throw new CustomException("INVALID_DATA","DSC document type and id is already null for the given Regularization Application.");
	}
	
	public void validateApprovedApplication(List<Regularization> regularizations) {
		
		if (regularizations.get(0).getStatus().equalsIgnoreCase(IssueFixConstants.APPROVED)) {
			throw new CustomException("APPLICATION_STATUS_ERROR", "Application is already approved. Demand can't be modified for approved application !!");
		}
	}

	public void validateApplicationStatus(List<BPA> bpa) {
		
		if (CollectionUtils.isEmpty(bpa) || bpa.size() > 1) {
			throw new CustomException("SEARCH_ERROR", "Either no or multiple applications found with the mentioned application number !!");
		}
		
		if(IssueFixConstants.INVALID_STEP_BACK_STATUS.contains(bpa.get(0).getStatus()) ) {
			throw new CustomException("APPLICATION_STATUS_ERROR", "Application is at " + bpa.get(0).getStatus() + " Stage. Can't revert to 1 step back !!");
		}
	}

	
	public void validateRegularizationApplicationStatus(List<Regularization> regularizations) {
		if (CollectionUtils.isEmpty(regularizations) || regularizations.size() > 1) {
			throw new CustomException("SEARCH_ERROR", "Either no or multiple applications found with the mentioned application number !!");
		}
		
		if(IssueFixConstants.INVALID_STEP_BACK_STATUS.contains(regularizations.get(0).getStatus()) ) {
			throw new CustomException("APPLICATION_STATUS_ERROR", "Application is at " + regularizations.get(0).getStatus() + " Stage. Can't revert to 1 step back !!");
		}
		
	}
	
}
