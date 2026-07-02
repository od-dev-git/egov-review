package org.egov.bpa.web.model.regularization;

import java.util.List;

import org.egov.bpa.web.model.AuditDetails;
import org.egov.common.contract.response.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;

@Builder
public class RegularizationDocUploadResponse {
	
	@JsonProperty("ResponseInfo")
	private ResponseInfo responseInfo;

	@JsonProperty("RegularizationDocuments")
	private List<RegularizationDocumentList> regularizationDocuments;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails;


}
