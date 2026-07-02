package org.egov.bpa.web.model;

import java.util.List;

import org.egov.common.contract.response.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public class BPADocUploadResponse {

	@JsonProperty("ResponseInfo")
	private ResponseInfo responseInfo;

	@JsonProperty("BPADocuments")
	private List<DocumentList> bpaDocuments;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails;

}
