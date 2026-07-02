package org.egov.bpa.calculator.web.models.regularization;

import org.egov.bpa.calculator.web.models.AuditDetails;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BuildingRegularizationInfo {

	@JsonProperty("id")
	@Default
	private String id = null;

	@JsonProperty("tenantId")
	@Default
	private String tenantId = null;

	@JsonProperty("regularizationId")
	@Default
	private String regularizationId = null;
	
	@JsonProperty("farDetails")
	@Default
	private FARDetails farDetails = null;
	
	@JsonProperty("buaDetails")
	@Default
	private BUADetails buaDetails = null;
	
	@JsonProperty("otherDetails")
	@Default
	private BuildingOtherDetails otherDetails = null;
	
	@JsonProperty("buildingBlocks")
	@Default
	private Object buildingBlocks=null;
	
	@JsonProperty("additionalDetails")
	@Default
	private Object additionalDetails=null;

	@JsonProperty("auditDetails")
	@Default
	private AuditDetails auditDetails = null;
	
}
