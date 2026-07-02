package org.egov.bpa.web.model.regularization;

import java.util.List;

import org.egov.bpa.web.model.AuditDetails;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class RegularizationDocUpload {

	@JsonProperty("tenantId")
	private String tenantId;

	@JsonProperty("applicationNo")
	private String applicationNo;

	@JsonProperty("docUploadType")
	private String docUploadType;

	@JsonProperty("documents")
	private List<RegularizationDocumentList> documents;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;

}
