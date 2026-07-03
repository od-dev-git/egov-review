
package org.egov.edcr.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.egov.commons.mdms.config.MdmsConfiguration;
import org.egov.commons.payments.model.CalculationCriteria;
import org.egov.commons.service.RestCallService;
import org.egov.infra.config.core.ApplicationThreadLocals;
import org.egov.infra.microservice.models.RequestInfo;
import org.jfree.util.Log;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

@Service
public class PaymentService {
	private static Logger LOG = Logger.getLogger(PaymentService.class);
	private RestCallService serviceRequestRepository;
	private MdmsConfiguration mdmsConfiguration;
	
	public PaymentService(RestCallService serviceRequestRepository, MdmsConfiguration mdmsConfiguration) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.mdmsConfiguration = mdmsConfiguration;
	}

	public StringBuilder getPaymentsSearchUrl(String businessservice) {
		StringBuilder uri = new StringBuilder().append("%s/collection-services/payments/").append(businessservice)
				.append("/_search");
		String hostUrl = mdmsConfiguration.getCollectionServiceHost();
		String url = String.format(uri.toString(), hostUrl);
		return new StringBuilder(url);
	}
	
	public StringBuilder getPaymentsSearchUrlForRegularization() {
		StringBuilder uri = new StringBuilder().append("%s/collection-services/payments")
				.append("/_search");
		String hostUrl = mdmsConfiguration.getCollectionServiceHost();
		String url = String.format(uri.toString(), hostUrl);
		return new StringBuilder(url);
	}

	public Object fetchApplicationFeePaymentDetails(RequestInfo requestInfo, String consumerCode, String tenantId) {
		StringBuilder searchUrl = getPaymentsSearchUrl("BPA.NC_APP_FEE").append("?consumerCodes=").append(consumerCode)
				.append("&tenantId=").append(tenantId);
		Map<String, Object> requestInfoPayload = new HashMap<>();
		requestInfoPayload.put("RequestInfo", requestInfo);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		return result;
	}

	public Object fetchPermitFeePaymentDetails(RequestInfo requestInfo, String consumerCode, String tenantId) {
		StringBuilder searchUrl = getPaymentsSearchUrl("BPA.NC_SAN_FEE").append("?consumerCodes=").append(consumerCode)
				.append("&tenantId=").append(tenantId);
		Map<String, Object> requestInfoPayload = new HashMap<>();
		requestInfoPayload.put("RequestInfo", requestInfo);
		LOG.info("searchUrl: "+ searchUrl);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		LOG.info("result: "+result);
		return result;
	}
	
	public Object fetchRegularizationPaymentDetails(RequestInfo requestInfo, String consumerCode, String tenantId) {
		StringBuilder searchUrl = getPaymentsSearchUrlForRegularization().append("?consumerCodes=").append(consumerCode)
				.append("&tenantId=").append(tenantId);
		Map<String, Object> requestInfoPayload = new HashMap<>();
		requestInfoPayload.put("RequestInfo", requestInfo);
		LOG.info("searchUrl : "+searchUrl);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		LOG.info("result :"+result);
		return result;
		
	}
	
	public Object fetchBuildingRegularizationPaymentDetails(RequestInfo requestInfo, String consumerCode, String tenantId) {
		StringBuilder searchUrl = getPaymentsSearchUrlForRegularization().append("?consumerCodes=").append(consumerCode)
				.append("&tenantId=").append(tenantId);
		Map<String, Object> requestInfoPayload = new HashMap<>();
		requestInfoPayload.put("RequestInfo", requestInfo);
		LOG.info("searchUrl : "+searchUrl);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		LOG.info("result :"+result);
		return result;
		
	}
	
	public Object fetchOCPaymentDetails(RequestInfo requestInfo, String consumerCode, String tenantId) {
		StringBuilder searchUrl = getPaymentsSearchUrlForRegularization().append("?consumerCodes=").append(consumerCode)
				.append("&tenantId=").append(tenantId);
		Map<String, Object> requestInfoPayload = new HashMap<>();
		requestInfoPayload.put("RequestInfo", requestInfo);
		LOG.info("searchUrl : "+searchUrl);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		LOG.info("result :"+result);
		return result;
		
	}
	
	public StringBuilder getEstimatedPaymentUrl() {
		String hostUrl = mdmsConfiguration.getBpaHost();
		String url = String.format("%sbpa-services/v1/bpa/_estimate",hostUrl);
		return new StringBuilder(url);
	}
	
	public StringBuilder getLREstimatedPaymentUrl() {
		String hostUrl = mdmsConfiguration.getBpaHost();
		String url = String.format("%sbpa-services/v1/regularization/_estimate",hostUrl);
		return new StringBuilder(url);
	}
	
	public Object fetchEstimatedSanctionFeePayment(RequestInfo requestInfo, LinkedHashMap bpaApplication) {
		StringBuilder searchUrl = getEstimatedPaymentUrl();
		LOG.info("searchUrl: "+searchUrl);
		Map<String, Object> requestPayload = new HashMap<>();
		requestPayload.put("RequestInfo", requestInfo);
		CalculationCriteria calculationCriteria = new CalculationCriteria();
		calculationCriteria.setApplicationNo(getValue(bpaApplication, "applicationNo"));
		calculationCriteria.setApplicationType(getValue(bpaApplication, "$.additionalDetails.applicationType"));
		calculationCriteria.setFeeType("SanctionFee");
		calculationCriteria.setRiskType(getValue(bpaApplication, "riskType"));
		calculationCriteria.setServiceType(getValue(bpaApplication, "$.additionalDetails.serviceType"));
		calculationCriteria.setTenantId(getValue(bpaApplication, "tenantId"));

		List<CalculationCriteria> calculationCriteriaList = new ArrayList<>();
		calculationCriteriaList.add(calculationCriteria);
		requestPayload.put("CalulationCriteria", calculationCriteriaList);
		//LOG.info("requestPayload: "+requestPayload);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestPayload);
		LOG.info("result: "+result);
		return result;
	}
	
	public Object fetchEstimatedSanctionFeePaymentOCOutsideSujog(RequestInfo requestInfo, LinkedHashMap bpaApplication) {
		StringBuilder searchUrl = getEstimatedPaymentUrl();
		LOG.info("searchUrl: "+searchUrl);
		Map<String, Object> requestPayload = new HashMap<>();
		requestPayload.put("RequestInfo", requestInfo);
		CalculationCriteria calculationCriteria = new CalculationCriteria();
		calculationCriteria.setApplicationNo(getValue(bpaApplication, "applicationNo"));
		calculationCriteria.setApplicationType(getValue(bpaApplication, "$.additionalDetails.applicationType"));
		calculationCriteria.setFeeType("SanctionFee");
		calculationCriteria.setRiskType(getValue(bpaApplication, "riskType"));
		calculationCriteria.setServiceType(getValue(bpaApplication, "$.additionalDetails.serviceType"));
		if((String) bpaApplication.get("edcrNumber") == null)
			calculationCriteria.setIsOCOutsideSujogApplication(Boolean.TRUE);
		calculationCriteria.setTenantId(getValue(bpaApplication, "tenantId"));

		List<CalculationCriteria> calculationCriteriaList = new ArrayList<>();
		calculationCriteriaList.add(calculationCriteria);
		requestPayload.put("CalulationCriteria", calculationCriteriaList);
		//LOG.info("requestPayload: "+requestPayload);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestPayload);
		LOG.info("result in fetchEstimatedPayment: "+result);
		return result;
	}
	
	public Object fetchEstimatedSanctionFeePaymentOCInsideSujog(RequestInfo requestInfo, LinkedHashMap bpaApplication, String riskType) {
		StringBuilder searchUrl = getEstimatedPaymentUrl();
		LOG.info("searchUrl: "+searchUrl);
		Map<String, Object> requestPayload = new HashMap<>();
		requestPayload.put("RequestInfo", requestInfo);
		CalculationCriteria calculationCriteria = new CalculationCriteria();
		calculationCriteria.setApplicationNo(getValue(bpaApplication, "applicationNo"));
		calculationCriteria.setApplicationType(getValue(bpaApplication, "$.additionalDetails.applicationType"));
		calculationCriteria.setFeeType("SanctionFee");
		calculationCriteria.setRiskType(riskType);
		calculationCriteria.setServiceType(getValue(bpaApplication, "$.additionalDetails.serviceType"));
		if((String) bpaApplication.get("edcrNumber") == null)
			calculationCriteria.setIsOCOutsideSujogApplication(Boolean.TRUE);
		calculationCriteria.setTenantId(getValue(bpaApplication, "tenantId"));

		List<CalculationCriteria> calculationCriteriaList = new ArrayList<>();
		calculationCriteriaList.add(calculationCriteria);
		requestPayload.put("CalulationCriteria", calculationCriteriaList);
		//LOG.info("requestPayload: "+requestPayload);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestPayload);
		LOG.info("result in fetchEstimatedPayment: "+result);
		return result;
	}
	
	public Object fetchEstimatedApplicationFeePayment(RequestInfo requestInfo, LinkedHashMap bpaApplication) {
		StringBuilder searchUrl = getEstimatedPaymentUrl();
		LOG.info("searchUrl: "+searchUrl);
		Map<String, Object> requestPayload = new HashMap<>();
		requestPayload.put("RequestInfo", requestInfo);
		CalculationCriteria calculationCriteria = new CalculationCriteria();
		calculationCriteria.setApplicationNo(getValue(bpaApplication, "applicationNo"));
		calculationCriteria.setApplicationType(getValue(bpaApplication, "$.additionalDetails.applicationType"));
		calculationCriteria.setFeeType("ApplicationFee");
		calculationCriteria.setRiskType(getValue(bpaApplication, "riskType"));
		calculationCriteria.setServiceType(getValue(bpaApplication, "$.additionalDetails.serviceType"));
		calculationCriteria.setTenantId(getValue(bpaApplication, "tenantId"));

		List<CalculationCriteria> calculationCriteriaList = new ArrayList<>();
		calculationCriteriaList.add(calculationCriteria);
		requestPayload.put("CalulationCriteria", calculationCriteriaList);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestPayload);
		LOG.info("result in fetchEstimatedApplicationFeePayment: "+ result);
		return result;
	}
	
	public Object fetchEstimatedApplicationFeePaymentForOCOutsideSujog(RequestInfo requestInfo, LinkedHashMap bpaApplication) {
		StringBuilder searchUrl = getEstimatedPaymentUrl();
		LOG.info("searchUrl: "+searchUrl);
		Map<String, Object> requestPayload = new HashMap<>();
		requestPayload.put("RequestInfo", requestInfo);
		CalculationCriteria calculationCriteria = new CalculationCriteria();
		calculationCriteria.setApplicationNo(getValue(bpaApplication, "applicationNo"));
		calculationCriteria.setApplicationType(getValue(bpaApplication, "$.additionalDetails.applicationType"));
		calculationCriteria.setFeeType("ApplicationFee");
		calculationCriteria.setRiskType(getValue(bpaApplication, "riskType"));
		calculationCriteria.setServiceType(getValue(bpaApplication, "$.additionalDetails.serviceType"));
		calculationCriteria.setTenantId(getValue(bpaApplication, "tenantId"));
		if((String) bpaApplication.get("edcrNumber") == null)
			calculationCriteria.setIsOCOutsideSujogApplication(true);

		List<CalculationCriteria> calculationCriteriaList = new ArrayList<>();
		calculationCriteriaList.add(calculationCriteria);
		requestPayload.put("CalulationCriteria", calculationCriteriaList);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestPayload);
		LOG.info("result in fetchEstimatedApplicationFeePayment: "+ result);
		return result;
	}
	
	public Object fetchEstimatedApplicationFeePaymentForOCinsideSujog(RequestInfo requestInfo, LinkedHashMap bpaApplication, String riskType) {
		StringBuilder searchUrl = getEstimatedPaymentUrl();
		LOG.info("searchUrl: "+searchUrl);
		Map<String, Object> requestPayload = new HashMap<>();
		requestPayload.put("RequestInfo", requestInfo);
		CalculationCriteria calculationCriteria = new CalculationCriteria();
		calculationCriteria.setApplicationNo(getValue(bpaApplication, "applicationNo"));
		calculationCriteria.setApplicationType(getValue(bpaApplication, "$.additionalDetails.applicationType"));
		calculationCriteria.setFeeType("ApplicationFee");
		calculationCriteria.setRiskType(riskType);
		calculationCriteria.setServiceType(getValue(bpaApplication, "$.additionalDetails.serviceType"));
		calculationCriteria.setTenantId(getValue(bpaApplication, "tenantId"));
		if((String) bpaApplication.get("edcrNumber") == null)
			calculationCriteria.setIsOCOutsideSujogApplication(true);

		List<CalculationCriteria> calculationCriteriaList = new ArrayList<>();
		calculationCriteriaList.add(calculationCriteria);
		requestPayload.put("CalulationCriteria", calculationCriteriaList);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestPayload);
		LOG.info("result in fetchEstimatedApplicationFeePayment: "+ result);
		return result;
	}


	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
	}
	
	public Object fetchEstimatedFeePaymentForRegularization(RequestInfo requestInfo, LinkedHashMap regularizations, String feeType) {
		StringBuilder searchUrl = getLREstimatedPaymentUrl();
		Map<String, Object> requestPayload = new HashMap<>();
		requestPayload.put("RequestInfo", requestInfo);
		CalculationCriteria calculationCriteria = new CalculationCriteria();
		calculationCriteria.setApplicationNo(getValue(regularizations, "applicationNo"));
		calculationCriteria.setFeeType(feeType);
		calculationCriteria.setApplicationType(getValue(regularizations, "appType"));
		calculationCriteria.setTenantId(getValue(regularizations, "tenantId"));

		List<CalculationCriteria> calculationCriteriaList = new ArrayList<>();
		calculationCriteriaList.add(calculationCriteria);
		requestPayload.put("CalculationCriteria", calculationCriteriaList);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestPayload);
		return result;
	}
}
