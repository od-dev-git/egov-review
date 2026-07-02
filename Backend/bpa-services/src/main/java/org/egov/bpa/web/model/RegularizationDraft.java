package org.egov.bpa.web.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class RegularizationDraft {
	
	@JsonProperty("id")
	private String id = null;

	@JsonProperty("draftNo")
	private String draftNo = null;

	@JsonProperty("tenantId")
	private String tenantId = null;

	@JsonProperty("status")
	private String status = null;

	@JsonProperty("regularizationApplicationNo")
	private String regularizationApplicationNo = null;

	@JsonProperty("additionalDetails")
	private Object additionalDetails = null;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;
}
