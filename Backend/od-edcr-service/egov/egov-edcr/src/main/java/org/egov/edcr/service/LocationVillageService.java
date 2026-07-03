package org.egov.edcr.service;

import java.util.HashMap;
import java.util.Map;

import org.egov.commons.mdms.config.MdmsConfiguration;
import org.egov.commons.service.RestCallService;
import org.egov.infra.microservice.models.RequestInfo;
import org.springframework.stereotype.Service;

@Service
public class LocationVillageService {
	
	private RestCallService serviceRequestRepository;
	private MdmsConfiguration mdmsConfiguration;
	
	public LocationVillageService(RestCallService serviceRequestRepository, MdmsConfiguration mdmsConfiguration) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.mdmsConfiguration = mdmsConfiguration;
	}
	
	
	public StringBuilder getLocationSearchUrl() {
		
		String hostUrl = mdmsConfiguration.getLocationServiceHost();
		
		String url = String.format("%s/egov-location/location/v11/boundarys/_search", hostUrl);
		return new StringBuilder(url);
	}
	
	public Object getLocation(RequestInfo requestInfo, String tenantId) {
		
		StringBuilder searchUrl = getLocationSearchUrl().append("?tenantId=").append(tenantId)
				.append("&boundaryType=").append("Locality")
				.append("&hierarchyTypeCode=").append("REVENUE");
		
		Map<String, Object> requestInfoPayload = new HashMap<>();
		requestInfoPayload.put("RequestInfo", requestInfo);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		
		return result;
		
	} 
	
	
}
