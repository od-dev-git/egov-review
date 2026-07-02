package org.egov.bpa.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.util.BPAConstants;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MDMSService {

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private BPAConfiguration config;

	/**
	 * Check tenantId SparitValue
	 * 
	 * @param requestInfo
	 * @param tenantId
	 * @return SparitValue
	 */
	@SuppressWarnings("rawtypes")
	public Boolean getMdmsSparitValue(RequestInfo requestInfo, String tenantId) {
		List<MasterDetail> bpaMasterDetails = new ArrayList<>();
		List<ModuleDetail> moduleDetails = new ArrayList<>();
		String tenant = "od";
		Boolean isSparit = null;

		bpaMasterDetails.add(MasterDetail.builder().name(BPAConstants.MDMS_CATEGORY_MASTER_NAME).build());
		ModuleDetail bpaModuleDetail = ModuleDetail.builder().masterDetails(bpaMasterDetails)
				.moduleName(BPAConstants.MDMS_BPA).build();
		moduleDetails.add(bpaModuleDetail);

		MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(moduleDetails).tenantId(tenant).build();
		MdmsCriteriaReq mdmscrieterias = MdmsCriteriaReq.builder().requestInfo(requestInfo).mdmsCriteria(mdmsCriteria)
				.build();
		StringBuilder url = getMdmsSearchUrl();
		Object mdmsSparitData = serviceRequestRepository.fetchResult(url, mdmscrieterias);

		List jsonOutput = JsonPath.read(mdmsSparitData, BPAConstants.MDMS_CATEGORY_SPARIT_RESPONSE_PATH);
		String filterExp = "$.[?(@.ulb == '" + tenantId + "')]";
		List<Map<String, String>> checkCategoryTenantJson = JsonPath.read(jsonOutput, filterExp);

		if (!CollectionUtils.isEmpty(checkCategoryTenantJson)) {
			String sparitCheck = checkCategoryTenantJson.get(0).get(BPAConstants.MDMS_CATEGORY_SPARIT);
			isSparit = Boolean.parseBoolean(sparitCheck);
		}
		return isSparit;
	}

	private StringBuilder getMdmsSearchUrl() {
		return new StringBuilder().append(config.getMdmsHost()).append(config.getMdmsEndPoint());
	}

}
