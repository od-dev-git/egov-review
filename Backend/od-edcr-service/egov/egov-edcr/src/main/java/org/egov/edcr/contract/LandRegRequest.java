package org.egov.edcr.contract;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.egov.infra.microservice.models.RequestInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LandRegRequest {
	
	@JsonProperty("Regularizations")
	private ArrayList<LinkedHashMap<String, Object>> regularizations;
	
	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;

	public ArrayList<LinkedHashMap<String, Object>> getRegularizations() {
		return regularizations;
	}

	public RequestInfo getRequestInfo() {
		return requestInfo;
	}

	public void setRegularizations(ArrayList<LinkedHashMap<String, Object>> regularizations) {
		this.regularizations = regularizations;
	}

	public void setRequestInfo(RequestInfo requestInfo) {
		this.requestInfo = requestInfo;
	}
		
}
