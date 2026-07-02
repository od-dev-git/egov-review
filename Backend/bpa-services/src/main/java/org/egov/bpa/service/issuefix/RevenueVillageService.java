package org.egov.bpa.service.issuefix;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.egov.bpa.config.BPAConfiguration;
import org.egov.bpa.repository.ServiceRequestRepository;
import org.egov.bpa.validator.IssueFixValidator;
import org.egov.bpa.web.model.RequestInfoWrapper;
import org.egov.bpa.web.model.issuefix.IssueFix;
import org.egov.bpa.web.model.issuefix.IssueFixRequest;
import org.egov.bpa.web.model.issuefix.TenantBoundary;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RevenueVillageService {

	@Autowired
	private IssueFixValidator validator;

	@Autowired
	private ServiceRequestRepository serviceRequestRepository;

	@Autowired
	private BPAConfiguration config;

	public IssueFix issueFix(IssueFixRequest issueFixRequest) {
		validator.validateTenantId(issueFixRequest);
		List<String> villageList = new ArrayList<>();
		String tenantId = issueFixRequest.getIssueFix().getTenantId();
		List<TenantBoundary> tenantBoundary = boundaryDataSearch(issueFixRequest.getRequestInfo(), tenantId);
		if (!CollectionUtils.isEmpty(tenantBoundary)) {
			Map<String, Object> boundaryData = (Map<String, Object>) tenantBoundary.get(0);
			List<LinkedHashMap<String, String>> boundaryList = (List<LinkedHashMap<String, String>>) boundaryData
					.get("boundary");
			for (LinkedHashMap<String, String> boundary : boundaryList) {
				villageList.add(boundary.get("name"));
			}
		} else {
			throw new CustomException("DATA_NOT_FOUND", "Unable to find the village list for ULB : " + tenantId);
		}
		return IssueFix.builder().villages(villageList).tenantId(tenantId).build();
	}

	private StringBuilder getBoundaryDataSearchURL(RequestInfo requestInfo, String tenantId) {
		// TODO Auto-generated method stub
		StringBuilder uri = new StringBuilder(config.getLocationHost());
		uri.append(config.getLocationContextPath());
		uri.append(config.getLocationEndpoint());
		uri.append("?hierarchyTypeCode=");
		uri.append(config.getHierarchyTypeCode());
		uri.append("&boundaryType=Locality");
		uri.append("&tenantId=");
		uri.append(tenantId);
		return uri;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public List<TenantBoundary> boundaryDataSearch(RequestInfo requestInfo, String tenantId) {
		List<TenantBoundary> tenantBoundary = new ArrayList<TenantBoundary>();
		StringBuilder url = getBoundaryDataSearchURL(requestInfo, tenantId);
		log.info("URL : " + url);
		RequestInfoWrapper requestInfoWrapper = RequestInfoWrapper.builder().requestInfo(requestInfo).build();
		LinkedHashMap responseMap = null;
		responseMap = (LinkedHashMap) serviceRequestRepository.fetchResult(url, requestInfoWrapper);
		ArrayList<String> villageList = new ArrayList<String>();
		if (responseMap != null && responseMap.get("TenantBoundary") != null) {
			tenantBoundary = (List<TenantBoundary>) responseMap.get("TenantBoundary");
		}
		return tenantBoundary;
	}

}
