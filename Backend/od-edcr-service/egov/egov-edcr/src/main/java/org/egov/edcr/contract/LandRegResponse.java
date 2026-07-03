package org.egov.edcr.contract;

import java.util.List;

import org.egov.infra.microservice.models.RequestInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LandRegResponse {
	@JsonProperty("ResponseInfo")
	private RequestInfo requestInfo;

	private String message;

	private List<String> filestoreIds;

	private String tenantid;

	public RequestInfo getRequestInfo() {
		return requestInfo;
	}

	public String getMessage() {
		return message;
	}

	public List<String> getFilestoreIds() {
		return filestoreIds;
	}

	public String getTenantid() {
		return tenantid;
	}

	public void setRequestInfo(RequestInfo requestInfo) {
		this.requestInfo = requestInfo;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setFilestoreIds(List<String> filestoreIds) {
		this.filestoreIds = filestoreIds;
	}

	public void setTenantid(String tenantid) {
		this.tenantid = tenantid;
	}
}
