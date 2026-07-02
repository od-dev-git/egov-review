package org.egov.bpa.calculator.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.repository.ServiceRequestRepository;
import org.egov.bpa.calculator.utils.BPACalculatorConstants;
import org.egov.bpa.calculator.web.models.CalculationReq;
import org.egov.bpa.calculator.web.models.bpa.BPA;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;

@Service
@Slf4j
public class MDMSService {

	@Autowired
	 private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private BPACalculatorConfig config;

	@Autowired
	private EDCRService edcrService;
	
    public Object mDMSCall(CalculationReq calculationReq,String tenantId){
        MdmsCriteriaReq mdmsCriteriaReq = getMDMSRequest(calculationReq,tenantId);
        StringBuilder url = getMdmsSearchUrl();
        Object result = serviceRequestRepository.fetchResult(url , mdmsCriteriaReq);
        return result;
    }

    /**
     * Creates and returns the url for mdms search endpoint
     *
     * @return MDMS Search URL
     */
    private StringBuilder getMdmsSearchUrl() {
        return new StringBuilder().append(config.getMdmsHost()).append(config.getMdmsSearchEndpoint());
    }

    /**
     * Creates MDMS request
     * @param requestInfo The RequestInfo of the calculationRequest
     * @param tenantId The tenantId of the tradeLicense
     * @return MDMSCriteria Request
     */
    private MdmsCriteriaReq getMDMSRequest(CalculationReq calculationReq, String tenantId) {
    	RequestInfo requestInfo = 	calculationReq.getRequestInfo();
        List<MasterDetail> bpaMasterDetails = new ArrayList<>();
        
        bpaMasterDetails.add(MasterDetail.builder().name(BPACalculatorConstants.MDMS_CALCULATIONTYPE)
        		.build());
        bpaMasterDetails.add(MasterDetail.builder().name(BPACalculatorConstants.MDMS_OC_COMPOUNDING_FEE).build());
        bpaMasterDetails.add(MasterDetail.builder().name(BPACalculatorConstants.MDMS_RETENTION_FEES_MASTER_NAME).build());
        bpaMasterDetails.add(MasterDetail.builder().name(BPACalculatorConstants.MDMS_CONSTRUCTION_COST_RATE).build());
       // bpaMasterDetails.add(MasterDetail.builder().name(BPACalculatorConstants.MDMS_CATEGORY_MASTER_NAME).build());
        
        ModuleDetail bpaModuleDtls = ModuleDetail.builder().masterDetails(bpaMasterDetails)
                .moduleName(BPACalculatorConstants.MDMS_BPA).build();

        List<ModuleDetail> moduleDetails = new ArrayList<>();
        
        moduleDetails.add(bpaModuleDtls);

        MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(moduleDetails).tenantId(tenantId)
                .build();

        return MdmsCriteriaReq.builder().requestInfo(requestInfo).mdmsCriteria(mdmsCriteria).build();
    }


    /**
     * Gets the calculationType for the city for a particular financialYear
     * If for particular financialYear entry is not there previous year is taken
     * If MDMS data is not available default values are returned
     * @param requestInfo The RequestInfo of the calculationRequest
     * @param license The tradeLicense for which calculation is done
     * @return Map contianing the calculationType for TradeUnit and accessory
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	public Map getCalculationType(RequestInfo requestInfo,BPA bpa,Object mdmsData, String feeType){
        HashMap<String,Object> calculationType = new HashMap<>();
        try {
            List jsonOutput = JsonPath.read(mdmsData, BPACalculatorConstants.MDMS_CALCULATIONTYPE_PATH);
            LinkedHashMap responseMap = edcrService.getEDCRDetails(requestInfo, bpa);
            
            String jsonString = new JSONObject(responseMap).toString();
    		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
    		Map<String, String> additionalDetails = new HashMap<String, String>();
    		JSONArray serviceType = context.read("edcrDetail.*.applicationSubType");
    		if (CollectionUtils.isEmpty(serviceType)) {
    			serviceType.add("NEW_CONSTRUCTION");
    		}
    		JSONArray applicationType = context.read("edcrDetail.*.appliactionType");
    		if (StringUtils.isEmpty(applicationType)) {
    			applicationType.add("permit");
    		}
    		additionalDetails.put("serviceType", serviceType.get(0).toString());
    		additionalDetails.put("applicationType", applicationType.get(0).toString());
            
    		
            log.debug("applicationType is " + additionalDetails.get("applicationType"));
            log.debug("serviceType is " + additionalDetails.get("serviceType"));
            String filterExp = "$.[?((@.applicationType == '"+ additionalDetails.get("applicationType")+"' || @.applicationType === 'ALL' ) &&  @.feeType == '"+feeType+"')]";
            List<Object> calTypes = JsonPath.read(jsonOutput, filterExp);
            
            filterExp = "$.[?(@.serviceType == '"+ additionalDetails.get("serviceType")+"' || @.serviceType === 'ALL' )]";
            calTypes = JsonPath.read(calTypes, filterExp);
            
            filterExp = "$.[?(@.riskType == '"+bpa.getRiskType()+"' || @.riskType === 'ALL' )]";
            calTypes = JsonPath.read(calTypes, filterExp);
            
            if(calTypes.size() > 1){
	            	filterExp = "$.[?(@.riskType == '"+bpa.getRiskType()+"' )]";
	            	calTypes  = JsonPath.read(calTypes, filterExp);
            }
            
            if(calTypes.size() == 0) {
            		return defaultMap(feeType);
            }
            
             Object obj = calTypes.get(0);
           
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);
            
           String financialYear = currentYear + "-" + (currentYear + 1);
           System.out.println(financialYear);
            
            calculationType = (HashMap<String, Object>) obj;
        }
        catch (Exception e){
            throw new CustomException(BPACalculatorConstants.CALCULATION_ERROR, "Failed to get calculationType");
        }

        return calculationType;
    }
   
    /**
     * Creates and return default calculationType values as map
     * @return default calculationType Map
     */	
    private Map defaultMap(String feeType){
        Map defaultMap = new HashMap();
        String feeAmount = ( feeType.equalsIgnoreCase(BPACalculatorConstants.MDMS_CALCULATIONTYPE_APL_FEETYPE) ) ? config.getApplFeeDefaultAmount() : config.getSancFeeDefaultAmount();
        defaultMap.put( BPACalculatorConstants.MDMS_CALCULATIONTYPE_AMOUNT,feeAmount);
        return defaultMap;
    }
    
    public Map getOcCompoundingFee(Object mdmsData, String compoundingType) {
		
		List jsonOutput = JsonPath.read(mdmsData, BPACalculatorConstants.MDMS_OC_COMPOUNDING_FEE_PATH);
		String filterExp = "$.[?(@.compoundingType == '"+ compoundingType +"')]";
		List<Object> calTypes = JsonPath.read(jsonOutput, filterExp);
		
		if(calTypes.size() == 0) {
			//return defaultMap(feeType);
		}
		HashMap<String,Object> ocCompoundingFee = (HashMap<String, Object>) calTypes.get(0);
		
		return ocCompoundingFee;
	}

public Boolean getMdmsSparitValue(CalculationReq calculationReq,String tenantIds) {
		
		RequestInfo requestInfo = 	calculationReq.getRequestInfo();
        List<MasterDetail> bpaMasterDetails = new ArrayList<>();
        String tenantId = "od";
        Boolean isSparit =null;
        bpaMasterDetails.add(MasterDetail.builder().name(BPACalculatorConstants.MDMS_CATEGORY_MASTER_NAME).build());
        ModuleDetail bpaModuleDtls = ModuleDetail.builder().masterDetails(bpaMasterDetails)
                .moduleName(BPACalculatorConstants.MDMS_BPA).build();

        List<ModuleDetail> moduleDetails = new ArrayList<>();
        
        moduleDetails.add(bpaModuleDtls);

        MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(moduleDetails).tenantId(tenantId)
                .build();
        MdmsCriteriaReq mdmscrieterias =    MdmsCriteriaReq.builder().requestInfo(requestInfo).mdmsCriteria(mdmsCriteria).build();
        StringBuilder url = getMdmsSearchUrl();
        // String url = "http://localhost:8094/egov-mdms-service/v1/_search";
         // System.out.println("mdmscrieteria:"+url);
          Object mdmsSparitData = serviceRequestRepository.fetchResult(url , mdmscrieterias);
        //  System.out.println("result:"+mdmsSparitData);
      
       List jsonOutput = JsonPath.read(mdmsSparitData, BPACalculatorConstants.MDMS_CATEGORY_SPARIT_RESPONSE_PATH);
		String filterExp = "$.[?(@.ulb == '" + tenantIds + "')]";
		List<Map<String, String>> checkCategoryTenantJson = JsonPath.read(jsonOutput, filterExp);
		if (!CollectionUtils.isEmpty(checkCategoryTenantJson)) {
			String sparitCheck = checkCategoryTenantJson.get(0)
					.get(BPACalculatorConstants.MDMS_CATEGORY_SPARIT);
			
			isSparit = Boolean.parseBoolean(sparitCheck);
			
			
			// System.out.println("spaitcheck:"+isSparit);
		}
		
      return isSparit;
	}

	public Object fetchInstallmentsApplicableForTaxheads(CalculationReq calculationReq, String tenantId) {
		MdmsCriteriaReq mdmsCriteriaReq = prepareMdmsRequestForInstallment(calculationReq, tenantId);
		StringBuilder url = getMdmsSearchUrl();
		Object result = serviceRequestRepository.fetchResult(url, mdmsCriteriaReq);
		return result;
	}
	
	public Map getInstallmentforTaxHeadCode(String taxHeadCode, Object mdmsData) {

		List jsonOutput = JsonPath.read(mdmsData, BPACalculatorConstants.MDMS_INSTALLMENT_PATH);
		String filterExp = "$.[?(@.code == '" + taxHeadCode + "')]";
		List<Object> calTypes = JsonPath.read(jsonOutput, filterExp);

		if (calTypes.size() == 0) {
			return defaultNoOfInstallmentsMap();
		}
		HashMap<String, Object> installmentDetails = (HashMap<String, Object>) calTypes.get(0);
		return installmentDetails;
	}

	private MdmsCriteriaReq prepareMdmsRequestForInstallment(CalculationReq calculationReq, String tenantId) {
		List<MasterDetail> bpaMasterDetails = new ArrayList<>();
		bpaMasterDetails.add(MasterDetail.builder().name(BPACalculatorConstants.MDMS_INSTALLMENTS).build());
		ModuleDetail bpaModuleDtls = ModuleDetail.builder().masterDetails(bpaMasterDetails)
				.moduleName(BPACalculatorConstants.MDMS_BPA).build();
		List<ModuleDetail> moduleDetails = new ArrayList<>();
		moduleDetails.add(bpaModuleDtls);
		MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(moduleDetails).tenantId(tenantId).build();
		return MdmsCriteriaReq.builder().requestInfo(calculationReq.getRequestInfo()).mdmsCriteria(mdmsCriteria)
				.build();
	}
	
	/**
	 * Creates and return default no of installment as map
	 * 
	 * @return default noOfInstallments Map
	 */
	private Map defaultNoOfInstallmentsMap() {
		Map defaultMap = new HashMap();
		int defaultNoOfInstallments = config.getDefaultNoOfInstallments();
		defaultMap.put(BPACalculatorConstants.MDMS_NO_OF_INSTALLMENTS, defaultNoOfInstallments);
		return defaultMap;
	}

	
	
	/**
	 * Check tenantId SparitValue for Regularization
	 * @param requestInfo
	 * @param tenantId
	 * @return SparitValue
	 */
	@SuppressWarnings("rawtypes")
	public Boolean getMdmsSparitValueforRegularization(RequestInfo requestInfo, String tenantId) {
        List<MasterDetail> bpaMasterDetails = new ArrayList<>();
        List<ModuleDetail> moduleDetails = new ArrayList<>();
        String tenant = "od";
        Boolean isSparit =null;
        
        bpaMasterDetails.add(MasterDetail.builder().name(BPACalculatorConstants.MDMS_CATEGORY_MASTER_NAME).build());
        ModuleDetail bpaModuleDetail = ModuleDetail.builder()
        		.masterDetails(bpaMasterDetails)
                .moduleName(BPACalculatorConstants.MDMS_BPA)
                .build();
        moduleDetails.add(bpaModuleDetail);

        MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(moduleDetails).tenantId(tenant).build();
        MdmsCriteriaReq mdmscrieterias =    MdmsCriteriaReq.builder().requestInfo(requestInfo).mdmsCriteria(mdmsCriteria).build();
        StringBuilder url = getMdmsSearchUrl();
        Object mdmsSparitData = serviceRequestRepository.fetchResult(url , mdmscrieterias);
      
        List jsonOutput = JsonPath.read(mdmsSparitData, BPACalculatorConstants.MDMS_CATEGORY_SPARIT_RESPONSE_PATH);
		String filterExp = "$.[?(@.ulb == '" + tenantId + "')]";
		List<Map<String, String>> checkCategoryTenantJson = JsonPath.read(jsonOutput, filterExp);
		
		if (!CollectionUtils.isEmpty(checkCategoryTenantJson)) {
			String sparitCheck = checkCategoryTenantJson.get(0).get(BPACalculatorConstants.MDMS_CATEGORY_SPARIT);
			isSparit = Boolean.parseBoolean(sparitCheck);
		}
		return isSparit;
	}

}
