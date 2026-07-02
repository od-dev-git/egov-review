package org.egov.bpa.web.model.regularization;

import org.egov.bpa.web.model.AuditDetails;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Validated
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class RegularizationFieldInspection {

	@JsonProperty("id")
	@Default
	private String id = null;

	@JsonProperty("applicationno")
	@Default
	private String applicationno = null;

	@JsonProperty("tenantId")
	@Default
	private String tenantId = null;

	@JsonProperty("approachRoad")
	@Default
	private Object approachRoad = null;

	@JsonProperty("siteSituation")
	@Default
	private Object siteSituation = null;

	@JsonProperty("buildingSituation")
	@Default
	private Object buildingSituation = null;

	@JsonProperty("reportDetails")
	@Default
	private Object reportDetails = null;

	@JsonProperty("additionalDetails")
	@Default
	private Object additionalDetails = null;

	@JsonProperty("buildingRegularizationSetback")
	@Default
	private Object buildingRegularizationSetback = null;

	@JsonProperty("auditDetails")
	@Default
	private AuditDetails auditDetails = null;

}
