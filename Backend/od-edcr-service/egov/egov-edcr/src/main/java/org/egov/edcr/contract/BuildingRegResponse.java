package org.egov.edcr.contract;

import java.util.List;

import org.egov.infra.microservice.contract.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BuildingRegResponse {
	
	@JsonProperty("ResponseInfo")
	private ResponseInfo responseInfo;

	private String message;

	private List<String> filestoreIds;

	private String tenantid;
	
	public ResponseInfo getResponseInfo() {
		return responseInfo;
	}

	public void setResponseInfo(ResponseInfo responseInfo) {
		this.responseInfo = responseInfo;
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
