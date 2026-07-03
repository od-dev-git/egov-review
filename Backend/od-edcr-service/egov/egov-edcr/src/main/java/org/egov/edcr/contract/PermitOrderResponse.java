package org.egov.edcr.contract;

import java.util.List;

import org.egov.infra.microservice.models.RequestInfo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PermitOrderResponse {

	@JsonProperty("ResponseInfo")
	private RequestInfo requestInfo;

	private String message;

	private List<String> filestoreIds;

	private String tenantid;

	private String key = "buildingpermit";

	public RequestInfo getRequestInfo() {
		return requestInfo;
	}

	public void setRequestInfo(RequestInfo requestInfo) {
		this.requestInfo = requestInfo;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public List<String> getFilestoreIds() {
		return filestoreIds;
	}

	public void setFilestoreIds(List<String> filestoreIds) {
		this.filestoreIds = filestoreIds;
	}

	public String getTenantid() {
		return tenantid;
	}

	public void setTenantid(String tenantid) {
		this.tenantid = tenantid;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

}
