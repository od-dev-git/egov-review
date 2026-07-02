package org.egov.bpa.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.web.model.AuditDetails;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.workflow.Action;
import org.egov.bpa.web.model.workflow.BusinessService;
import org.egov.bpa.web.model.workflow.State;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DemolitionUtil {
	
	@Autowired
	private BPAConfiguration config;
	
	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	/**
	 * Methof to set Audit details for create & update requests
	 * 
	 * @param by
	 * @param isCreate
	 * @return
	 */
	public AuditDetails getAuditDetails(String by, Boolean isCreate) {
		Long time = System.currentTimeMillis();
		if (isCreate)
			return AuditDetails.builder().createdBy(by).lastModifiedBy(by).createdTime(time).lastModifiedTime(time)
					.build();
		else
			return AuditDetails.builder().lastModifiedBy(by).lastModifiedTime(time).build();
	}
	
	/**
	 * Generic Method to call MDMS
	 * 
	 * @param requestInfo
	 * @param tenantId
	 * @return
	 */
	public Object mDMSCall(RequestInfo requestInfo, String tenantId) {
		MdmsCriteriaReq mdmsCriteriaReq = getMDMSRequest(requestInfo, tenantId);
		Object result = serviceRequestRepository.fetchResult(getMdmsSearchUrl(), mdmsCriteriaReq);
		return result;
	}
	
	/**
	 * Get MDMS url with this method
	 * 
	 * @return string url
	 */
	public StringBuilder getMdmsSearchUrl() {
		return new StringBuilder().append(config.getMdmsHost()).append(config.getMdmsEndPoint());
	}
	
	/**
	 * Method to prepare the MDMS request
	 * 
	 * @param requestInfo
	 * @param tenantId
	 * @return
	 */
	public MdmsCriteriaReq getMDMSRequest(RequestInfo requestInfo, String tenantId) {
		List<ModuleDetail> moduleRequest = getBPAModuleRequest();

		List<ModuleDetail> moduleDetails = new LinkedList<>();
		moduleDetails.addAll(moduleRequest);

		MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(moduleDetails).tenantId(tenantId).build();

		MdmsCriteriaReq mdmsCriteriaReq = MdmsCriteriaReq.builder().mdmsCriteria(mdmsCriteria).requestInfo(requestInfo)
				.build();
		return mdmsCriteriaReq;
	}
	
	/**
	 * Method where we provide the path and fileName for MDMS request
	 * 
	 * @return list of Module Details to be used for MDMS Request
	 */
	public List<ModuleDetail> getBPAModuleRequest() {

		List<MasterDetail> bpaMasterDtls = new ArrayList<>();

		// filter to only get code field from master data
		final String filterCode = "$.[?(@.active==true)].code";

		// master details for Demolition
		bpaMasterDtls.add(MasterDetail.builder().name(DemolitionConstants.DOCUMENT_TYPE_MAPPING).build());

		List<MasterDetail> commonMasterDetails = new ArrayList<>();
		commonMasterDetails.add(
				MasterDetail.builder().name(DemolitionConstants.OWNERSHIP_CATEGORY).filter(filterCode).build());
		commonMasterDetails
				.add(MasterDetail.builder().name(DemolitionConstants.DOCUMENT_TYPE).filter(filterCode).build());

		ModuleDetail bpaModuleDtls = ModuleDetail.builder().masterDetails(bpaMasterDtls)
				.moduleName(BPAConstants.BPA_MODULE).build();

		ModuleDetail commonMasterMDtl = ModuleDetail.builder().masterDetails(commonMasterDetails)
				.moduleName(BPAConstants.COMMON_MASTERS_MODULE).build();

		return Arrays.asList(bpaModuleDtls, commonMasterMDtl);
	}

	public Map<String, List<String>> getAttributeValues(Object mdmsData) {
		
		List<String> modulepaths = Arrays.asList(BPAConstants.BPA_JSONPATH_CODE,
				BPAConstants.COMMON_MASTER_JSONPATH_CODE);
		final Map<String, List<String>> mdmsResMap = new HashMap<>();
		modulepaths.forEach(modulepath -> {
			try {
				mdmsResMap.putAll(JsonPath.read(mdmsData, modulepath));
			} catch (Exception e) {
				throw new CustomException(BPAErrorConstants.INVALID_TENANT_ID_MDMS_KEY,
						BPAErrorConstants.INVALID_TENANT_ID_MDMS_MSG);
			}
		});
		return mdmsResMap;
	}

	public String getNextValidUserUUIDByNextState(String stateUUID, BusinessService businessService,
			RequestInfo requestInfo) {

		StringBuilder roles = new StringBuilder();
		State nextState = businessService.getStateFromUuid(stateUUID);
		Set<String> vroles = new HashSet<>();
		for (Action action : nextState.getActions()) {
			vroles.addAll(action.getRoles());
		}

		for (String s : vroles) {
			roles.append(s + ",");
		}
		return roles.toString().substring(0, roles.length());
	}

	public List<String> getNextValidUserUUID(String roles, String tenantId, boolean isActive, RequestInfo requestInfo) {

		StringBuilder uri = new StringBuilder(config.getHrmshost());
		uri.append(config.getHrmsContextPath());
		uri.append(config.getHrmsSearchEndpoint());
		if (roles != null) {
			uri.append("?roles=");
			uri.append(roles);
		}
		if (tenantId != null) {
			uri.append("&tenantId=");
			uri.append(tenantId);
		}
		if (isActive = true) {
			uri.append("&isActive=true");
		}

		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
		LinkedHashMap fetchResult = null;
		fetchResult = (LinkedHashMap) serviceRequestRepository.fetchResult(uri, requestInfoWrapper);
		String jsonString = new JSONObject(fetchResult).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		List<String> UUID = context.read("Employees.*.uuid");
		System.out.println(UUID.size());
		return UUID;
	}

	public String getRandomValue(List<String> nextAssignees) {

		try {
			Random rand = new Random();
			int randomNum = rand.nextInt(nextAssignees.size());
			return nextAssignees.get(randomNum);
		} catch (Exception e) {
			log.info("Exception caught while generating random assignee : " + String.valueOf(e));
			throw new CustomException("UNABLE_TO_GET_ASSIGNEE", e.getMessage());
		}
	}

}
