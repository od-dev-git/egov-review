package org.egov.bpa.web.model.regularization;

import org.egov.bpa.web.model.AuditDetails;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class RegularizationDocRemark {

	@JsonProperty("id")
	@Default
	private String id = null;

	@JsonProperty("documentCode")
	@Default
	private String documentCode = null;

	@JsonProperty("businessId")
	@Default
	private String businessId = null;
	
	@JsonProperty("isUpdatable")
	@Default
	private Boolean isUpdatable = Boolean.FALSE;

	@JsonProperty("additionalDetails")
	@Default
	private Object additionalDetails = null;

	@JsonProperty("auditDetails")
	@Default
	private AuditDetails auditDetails = null;

}
