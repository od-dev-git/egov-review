
package org.egov.edcr.service;

import java.util.HashMap;
import java.util.Map;

import org.egov.commons.mdms.config.MdmsConfiguration;
import org.egov.commons.service.RestCallService;
import org.egov.infra.config.core.ApplicationThreadLocals;
import org.egov.infra.microservice.models.RequestInfo;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

@Service
public class NocService {
	private RestCallService serviceRequestRepository;
	private MdmsConfiguration mdmsConfiguration;
	
	public NocService(RestCallService serviceRequestRepository,MdmsConfiguration mdmsConfiguration) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.mdmsConfiguration = mdmsConfiguration;
	}

	public StringBuilder getNocSearchUrl() {
//		String urlHost=ApplicationThreadLocals.getDomainURL();
//		return new StringBuilder().append("https://sujog-dev.odisha.gov.in/").append("noc-services/v1/noc").append("/_search");
		String hostUrl = mdmsConfiguration.getNocHost();
		
		String url = String.format("%s/noc-services/v1/noc/_search", hostUrl);
		return new StringBuilder(url);
	}

	public Object fetchNocs(RequestInfo requestInfo, String tenantId, String bpaApplicationNo) {
		StringBuilder searchUrl = getNocSearchUrl().append("?sourceRefId=").append(bpaApplicationNo)
				.append("&tenantId=").append(tenantId);
		Map<String, Object> requestInfoPayload = new HashMap<>();
		requestInfoPayload.put("RequestInfo", requestInfo);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		return result;
	}

	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
	}
}
