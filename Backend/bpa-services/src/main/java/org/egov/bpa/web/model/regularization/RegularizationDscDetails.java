package org.egov.bpa.web.model.regularization;

import javax.validation.constraints.Size;

import org.egov.bpa.web.model.AuditDetails;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Validated
@EqualsAndHashCode
public class RegularizationDscDetails {
	
	@Size(max = 64)
	@JsonProperty("id")
	private String id;

	@Size(max = 64)
	@JsonProperty("tenantId")
	@Default
	private String tenantId = null;

	@Size(max = 64)
	@JsonProperty("documentType")
	@Default
	private String documentType = null;

	@Size(max = 64)
	@JsonProperty("documentId")
	@Default
	private String documentId = null;

	@Size(max = 64)
	@JsonProperty("applicationNo")
	private String applicationNo;

	@Size(max = 64)
	@JsonProperty("approvedBy")
	private String approvedBy;

	@JsonProperty("additionalDetails")
	@Default
	private JsonNode additionalDetails = null;

	@JsonProperty("auditDetails")
	@Default
	private AuditDetails auditDetails = null;
	
}
