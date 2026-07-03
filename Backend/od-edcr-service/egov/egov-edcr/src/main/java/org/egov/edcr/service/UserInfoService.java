
package org.egov.edcr.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.egov.commons.mdms.config.MdmsConfiguration;
import org.egov.commons.service.RestCallService;
import org.egov.infra.microservice.models.RequestInfo;
import org.egov.infra.microservice.models.UserInfo;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

@Service
public class UserInfoService {
	
	private RestCallService serviceRequestRepository;
	private MdmsConfiguration mdmsConfiguration;
	
	public UserInfoService(RestCallService serviceRequestRepository,MdmsConfiguration mdmsConfiguration) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.mdmsConfiguration = mdmsConfiguration;
	}


	public StringBuilder getUserSearchUrl() {
		String hostUrl = mdmsConfiguration.getUserServiceHost();
		String url = String.format("%s/user/v1/_search", hostUrl);
		return new StringBuilder(url);
	}
	
	public StringBuilder getUserDetailsUrl() {
		String hostUrl = mdmsConfiguration.getUserServiceHost();
		String url = String.format("%s/user/_details", hostUrl);
		return new StringBuilder(url);
	}

	public Object fetchUserInfo(RequestInfo requestInfo, String tenantId, String businessService, String userUUID) {
		StringBuilder searchUrl = getUserSearchUrl();
		Map<String, Object> requestInfoPayload = new HashMap<>();
		java.util.List<String> uuids = new ArrayList<>();
		uuids.add(userUUID);
		requestInfoPayload.put("RequestInfo", requestInfo);
		requestInfoPayload.put("uuid", uuids);
//		requestInfoPayload.put("tenantId", tenantId);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		return result;
	}

	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
	}
	
	public Object fetchUserInfoFromToken(RequestInfo requestInfo, String authToken) {
		StringBuilder searchUrl = getUserDetailsUrl();
		searchUrl.append("?access_token="+authToken);
		Map<String, Object> requestInfoPayload = new HashMap<>();
		requestInfoPayload.put("RequestInfo", requestInfo);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		return result;
	}
	
	public Object fetchArchitectInfo(RequestInfo requestInfo, String ownerIds) {
		try {
		ObjectMapper mapper = new ObjectMapper();
		StringBuilder searchUrl = getTLSearchUrl();
		searchUrl.append("?tenantId=od&ownerIds="+ownerIds);
		Map<String, Object> requestInfoPayload = new HashMap<>();
		if(requestInfo.getUserInfo()==null) {
			Object userInfo=fetchUserInfoFromToken(requestInfo, requestInfo.getAuthToken());
			if(userInfo!=null) {
				requestInfo.setUserInfo((mapper.convertValue(userInfo, UserInfo.class)));
			}
			else {
				return null;
			}
		
		}
		requestInfoPayload.put("RequestInfo", requestInfo);		
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		return result;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
	}

	private StringBuilder getTLSearchUrl() {
		String hostUrl=mdmsConfiguration.getTlBpaRegServiceHost();
		String url = String.format("%s/tl-services/v1/BPAREG/_search", hostUrl);
		return new StringBuilder(url);
	}
}
