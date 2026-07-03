package org.egov.edcr.contract;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.egov.infra.microservice.models.RequestInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PermitOrderRequest {
	@JsonProperty("Bpa")
	private ArrayList<LinkedHashMap<String, Object>> bpaList;
	
	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;

	public RequestInfo getRequestInfo() {
		return requestInfo;
	}

	public void setRequestInfo(RequestInfo requestInfo) {
		this.requestInfo = requestInfo;
	}

	public ArrayList<LinkedHashMap<String, Object>> getBpaList() {
		return bpaList;
	}

	public void setBpaList(ArrayList<LinkedHashMap<String, Object>> bpaList) {
		this.bpaList = bpaList;
	}
	
}
