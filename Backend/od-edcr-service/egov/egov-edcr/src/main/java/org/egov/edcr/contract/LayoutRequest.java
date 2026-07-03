package org.egov.edcr.contract;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.egov.infra.microservice.models.RequestInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LayoutRequest {
	@JsonProperty("Layout")
	private ArrayList<LinkedHashMap<String, Object>> layout;
	
	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;

	public ArrayList<LinkedHashMap<String, Object>> getLayout() {
		return layout;
	}

	public RequestInfo getRequestInfo() {
		return requestInfo;
	}

	public void setLayout(ArrayList<LinkedHashMap<String, Object>> layout) {
		this.layout = layout;
	}

	public void setRequestInfo(RequestInfo requestInfo) {
		this.requestInfo = requestInfo;
	}
}
