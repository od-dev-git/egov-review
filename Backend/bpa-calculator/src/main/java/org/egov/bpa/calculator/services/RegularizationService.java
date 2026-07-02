package org.egov.bpa.calculator.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.apache.commons.lang3.ObjectUtils;
import org.egov.bpa.calculator.config.BPACalculatorConfig;
import org.egov.bpa.calculator.repository.ServiceRequestRepository;
import org.egov.bpa.calculator.utils.BPACalculatorConstants;
import org.egov.bpa.calculator.utils.RegularizationConstants;
import org.egov.bpa.calculator.utils.RegularizationUtils;
import org.egov.bpa.calculator.web.models.RequestInfoWrapper;
import org.egov.bpa.calculator.web.models.regularization.Regularization;
import org.egov.bpa.calculator.web.models.regularization.RegularizationCalculationCriteria;
import org.egov.bpa.calculator.web.models.regularization.RegularizationResponse;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

@Service
public class RegularizationService {
	
	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private BPACalculatorConfig config;
	
	@Autowired
	private RegularizationUtils utils;
	
	@SuppressWarnings("rawtypes")
	public Regularization getRegularization(@NotNull RequestInfo requestInfo, @NotNull String tenantId,
			@NotNull String applicationNo) {
		
		StringBuilder url = getRegularizationSearchUrl();
		url.append("tenantId=");
		url.append(tenantId);
		url.append("&");
		url.append("applicationNo=");
		url.append(applicationNo);
		
		LinkedHashMap responseMap = null;
		responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(url, new RequestInfoWrapper(requestInfo));

		RegularizationResponse regularizationResponse;
		try {
			regularizationResponse = mapper.convertValue(responseMap, RegularizationResponse.class);
		} catch (IllegalArgumentException e) {
			throw new CustomException(BPACalculatorConstants.PARSING_ERROR, "Error while parsing response of Regularization Search");
		}
		
		if(ObjectUtils.isEmpty(regularizationResponse)) {
			throw new CustomException(BPACalculatorConstants.PARSING_ERROR, "No value found in response of Regularization Search");
		} 
		return regularizationResponse.getRegularizations().get(0);
	}

	private StringBuilder getRegularizationSearchUrl() {
		
		StringBuilder url = new StringBuilder(config.getBpaHost());
		url.append(config.getRegularizationSearchEndpoint());
		url.append("?");
		return url;
	}

	
	
	@SuppressWarnings("rawtypes")
	public void getConstructionWelfareCessRate(RequestInfo requestInfo, RegularizationCalculationCriteria criteria, Map<String, Object> paramMap) {
		// Mdms call for Welfare Cess Fee rate and Construction Cost
		Object mdmsData = mdmsCall(requestInfo, criteria);
		List listNode = JsonPath.read(mdmsData, "$.MdmsRes.BPA.ConstructionCostRate");

		String filterExp = "$.[?(@.name == '" + BPACalculatorConstants.PERSQFTCOST + "')]";
		List<Map<String, String>> constructionCostPerSqftJson = JsonPath.read(listNode, filterExp);

		BigDecimal welfareCessRate = utils.convertToBigDecimal(constructionCostPerSqftJson.get(0).get("rate"));
		paramMap.put(RegularizationConstants.WALFARE_CESS_RATE, welfareCessRate);
		
		String filterExpresion = "$.[?(@.name == '" + BPACalculatorConstants.CONSTRUCTION_COST + "')]";
		List<Map<String, String>> constructionCostRateJson = JsonPath.read(listNode, filterExpresion);
		
		BigDecimal constructionCost = utils.convertToBigDecimal(constructionCostRateJson.get(0).get("rate"));
		paramMap.put(RegularizationConstants.CONSTRUCTION_COST, constructionCost);
	}
	
	

	private Object mdmsCall(RequestInfo requestInfo, RegularizationCalculationCriteria criteria) {
		MdmsCriteriaReq mdmsCriteriaReq = getMDMSRequest(requestInfo,criteria);
        StringBuilder url = getMdmsSearchUrl();
        Object result = serviceRequestRepository.fetchResult(url , mdmsCriteriaReq);
        return result;
	}

	
	private MdmsCriteriaReq getMDMSRequest(RequestInfo requestInfo, RegularizationCalculationCriteria criteria) {
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

        MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(moduleDetails).tenantId(criteria.getTenantId())
                .build();

        return MdmsCriteriaReq.builder().requestInfo(requestInfo).mdmsCriteria(mdmsCriteria).build();
	}

	
	
	
	/**
     * Creates and returns the url for mdms search endpoint
     *
     * @return MDMS Search URL
     */
    private StringBuilder getMdmsSearchUrl() {
        return new StringBuilder().append(config.getMdmsHost()).append(config.getMdmsSearchEndpoint());
    }

    
    
    
	@SuppressWarnings("rawtypes")
	public BigDecimal getRetentionFeeForTenant(RequestInfo requestInfo, RegularizationCalculationCriteria criteria) {
		Object mdmsData = mdmsCall(requestInfo, criteria);
		String tenantId = String.valueOf(criteria.getTenantId());
		List jsonOutput = JsonPath.read(mdmsData, BPACalculatorConstants.MDMS_RETENTION_FEE_PATH);
		String filterExp = "$.[?(@.ulb == '" + tenantId + "')]";
		List<Map<String, String>> retentionFeeForTenantJson = JsonPath.read(jsonOutput, filterExp);
		String retentionFeeForTenant = retentionFeeForTenantJson.get(0)
				.get(BPACalculatorConstants.MDMS_RETENTION_FEE);
		
		return new BigDecimal(retentionFeeForTenant);
	}

}
