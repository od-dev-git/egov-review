package org.egov.bpa.calculator.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.repository.ServiceRequestRepository;
import org.egov.bpa.calculator.web.models.AuditDetails;
import org.egov.bpa.calculator.web.models.CalculationReq;
import org.egov.bpa.calculator.web.models.CalulationCriteria;
import org.egov.bpa.calculator.web.models.RequestInfoWrapper;
import org.egov.bpa.calculator.web.models.bpa.BPA;
import org.egov.bpa.calculator.web.models.bpa.BPAResponse;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CalculationUtils {

	


    @Autowired
    private BPACalculatorConfig config;

    @Autowired
    private ServiceRequestRepository serviceRequestRepository;

    @Autowired
    private ObjectMapper mapper;


    /**
     * Creates demand Search url based on tenanatId,businessService and ConsumerCode
     * @return demand search url
     */
    public String getDemandSearchURL(){
        StringBuilder url = new StringBuilder(config.getBillingHost());
        url.append(config.getDemandSearchEndpoint());
        url.append("?");
        url.append("tenantId=");
        url.append("{1}");
        url.append("&");
        url.append("businessService=");
        url.append("{2}");
        url.append("&");
        url.append("consumerCode=");
        url.append("{3}");
        return url.toString();
    }


    /**
     * Creates generate bill url using tenantId,consumerCode and businessService
     * @return Bill Generate url
     */
    public String getBillGenerateURI(){
        StringBuilder url = new StringBuilder(config.getBillingHost());
        url.append(config.getBillGenerateEndpoint());
        url.append("?");
        url.append("tenantId=");
        url.append("{1}");
        url.append("&");
        url.append("consumerCode=");
        url.append("{2}");
        url.append("&");
        url.append("businessService=");
        url.append("{3}");

        return url.toString();
    }

    public AuditDetails getAuditDetails(String by, Boolean isCreate) {
        Long time = System.currentTimeMillis();
        if(isCreate)
            return AuditDetails.builder().createdBy(by).lastModifiedBy(by).createdTime(time).lastModifiedTime(time).build();
        else
            return AuditDetails.builder().lastModifiedBy(by).lastModifiedTime(time).build();
    }

    
    /**
     * identify the billingBusinessService matching to the calculation FeeType
     */
	public String getBillingBusinessService(String businessService, String feeType) {

		String billingBusinessService;
		switch (feeType) {
		case BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE:
			if (businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA1) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA2) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA3) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA4) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA5) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA6) ||
					businessService.equalsIgnoreCase("BPA1_RV") ||
					businessService.equalsIgnoreCase("BPA2_RV") ||
					businessService.equalsIgnoreCase("BPA3_RV") ||
					businessService.equalsIgnoreCase("BPA4_RV") || businessService.equalsIgnoreCase("BPA7")) {
				billingBusinessService = config.getApplFeeBusinessService();
			} 
			else if (businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA_LOW)) {
				billingBusinessService = config.getApplFeeBusinessService();
			}else {
				billingBusinessService = config.getOCApplBusinessservice();
			}
			break;
		case BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE:
			if (businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA1) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA2) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA3) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA4) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA5) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA6) ||
					businessService.equalsIgnoreCase("BPA1_RV") ||
					businessService.equalsIgnoreCase("BPA2_RV") ||
					businessService.equalsIgnoreCase("BPA3_RV") ||
					businessService.equalsIgnoreCase("BPA4_RV") || businessService.equalsIgnoreCase("BPA7")) {
				billingBusinessService = config.getSanclFeeBusinessService();
			}
			else if (businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA_LOW)) {
				billingBusinessService = config.getSanclFeeBusinessService();
			} else {
				billingBusinessService = config.getOCSancBusinessservice();
			}
			break;
		case BPACalculatorConstants.MDMS_CALCULATIONTYPE_LOW_APL_FEETYPE:
			billingBusinessService = config.getLowRiskPermitFeeBusinessService();
			break;
		case BPACalculatorConstants.MDMS_CALCULATIONTYPE_LOW_SANC_FEETYPE:
			billingBusinessService = config.getLowRiskPermitFeeBusinessService();
			break;
		case BPACalculatorConstants.LOW_RISK_PERMIT_FEE_TYPE:
			billingBusinessService = config.getLowRiskPermitFeeBusinessService();
			break;
		default:
			billingBusinessService = feeType;
			break;
		}
		return billingBusinessService;
	}
	
	/**
	* identify the billingBusinessService matching to the calculation FeeType
	*/
	public String getTaxHeadCode(String businessService, String feeType) {

		String billingTaxHead;
		switch (feeType) {
		case BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE:
			if(businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA1) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA2) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA3) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA4) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA5) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA6)){
			billingTaxHead = config.getBaseApplFeeHead();
			}
			else if(businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA_LOW)){
			billingTaxHead = config.getBaseApplFeeHead();
			}
			else{
				billingTaxHead = config.getOCApplFee();
			}
			break;
		case BPACalculatorConstants.MDMS_CALCULATIONTYPE_SANC_FEETYPE:
			if(businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA1) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA2) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA3) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA4) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA5) || businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA6))
	{
			billingTaxHead = config.getBaseSancFeeHead();
			}
		    else if(businessService.equalsIgnoreCase(BPACalculatorConstants.MDMS_BPA_LOW)){
			billingTaxHead = config.getBaseSancFeeHead();
			}
			else{
			billingTaxHead = config.getOCSancFee();
			}
			break;
		case BPACalculatorConstants.MDMS_CALCULATIONTYPE_LOW_APL_FEETYPE:
			billingTaxHead = config.getBaseLowApplFeeHead();
			break;
		case BPACalculatorConstants.MDMS_CALCULATIONTYPE_LOW_SANC_FEETYPE:
			billingTaxHead = config.getBaseLowSancFeeHead();
			break;
		default:
			billingTaxHead = feeType;
			break;
		}
		return billingTaxHead;
	}
	
	/**
	 * Validates fatherOrHusbandName and gender if ownerType is individual.single
	 * 
	 * @param request
	 */
	public void validateOwnerDetails(CalculationReq calculationReq) {
		Map<String, String> errorMap = new HashMap<>();
		for (CalulationCriteria calculationCriteria : calculationReq.getCalulationCriteria()) {
			/*
			 * if (calculationCriteria.getBpa().getLandInfo().getOwnershipCategory().
			 * toLowerCase().contains("individual.singleowner") &&
			 * Objects.isNull(calculationCriteria.getBpa().getLandInfo().getOwners().get(0).
			 * getGender())) { errorMap.put(BPACalculatorConstants.
			 * BPA_GENDER_MISSING,"Gender is mandatory for Individual Single Owner"); } if
			 * (calculationCriteria.getBpa().getLandInfo().getOwnershipCategory().
			 * toLowerCase().contains("individual.singleowner") &&
			 * Objects.isNull(calculationCriteria.getBpa().getLandInfo().getOwners().get(0).
			 * getFatherOrHusbandName())) {
			 * errorMap.put(BPACalculatorConstants.BPA_FATHER_NAME_MISSING,
			 * "Father or Husband's Name is mandatory for Individual Single Owner"); }
			 */
			// Add mandatory validation
			if(calculationCriteria.getBpa().getOCOutsideSujogApplication() || calculationCriteria.getIsOCOutsideSujogApplication())
				continue;
			if (calculationCriteria.getBpa().getLandInfo().getOwnershipCategory().toLowerCase()
					.contains("individual.singleowner")
					&& Objects
							.isNull(calculationCriteria.getBpa().getLandInfo().getOwners().get(0).getMobileNumber())) {
				errorMap.put(BPACalculatorConstants.BPA_MOBILE_NUMBER_MISSING,
						"Mobile number is mandatory for Individual Single Owner");
			}

			if (calculationCriteria.getBpa().getLandInfo().getOwnershipCategory().toLowerCase()
					.contains("individual.singleowner")
					&& Objects.isNull(calculationCriteria.getBpa().getLandInfo().getOwners().get(0).getName())) {
				errorMap.put(BPACalculatorConstants.BPA_APPLICANT_NAME_MISSING,
						"Applicant name is mandatory for Individual Single Owner");
			}

			if (calculationCriteria.getBpa().getLandInfo().getOwnershipCategory().toLowerCase()
					.contains("individual.singleowner")
					&& Objects.isNull(
							calculationCriteria.getBpa().getLandInfo().getOwners().get(0).getCorrespondenceAddress())) {
				errorMap.put(BPACalculatorConstants.BPA_CORRESPONDENCE_ADDRESS_MISSING,
						"Applicant Correspondence Address is mandatory for Individual Single Owner");
			}

		}
		if (!CollectionUtils.isEmpty(errorMap))
			throw new CustomException(errorMap);
	}
}
