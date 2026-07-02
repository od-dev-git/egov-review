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
public class BPADraft {

	@JsonProperty("id")
	private String id = null;

	@JsonProperty("edcrNo")
	private String edcrNo = null;
	
	@JsonProperty("tenantId")
	private String tenantId = null;
	
	@JsonProperty("status")
	private String status = null;
	
	@JsonProperty("bpaApplicationNo")
	private String bpaApplicationNo = null;

	@JsonProperty("additionalDetails")
	private Object additionalDetails = null;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;

}
