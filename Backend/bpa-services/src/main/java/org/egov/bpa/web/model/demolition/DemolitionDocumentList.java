package org.egov.bpa.web.model.demolition;

import org.egov.bpa.web.model.AuditDetails;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class DemolitionDocumentList {

	@JsonProperty("id")
	private String id = null;

	@JsonProperty("documentType")
	private String documentType = null;

	@JsonProperty("fileStoreId")
	private String fileStoreId = null;

	@JsonProperty("documentUid")
	private String documentUid = null;

	@JsonProperty("additionalDetails")
	private Object additionalDetails = null;

	@JsonProperty("auditDetails")
	private AuditDetails auditDetails = null;

	@JsonProperty("demolitionId")
	private String demolitionId = null;

}
