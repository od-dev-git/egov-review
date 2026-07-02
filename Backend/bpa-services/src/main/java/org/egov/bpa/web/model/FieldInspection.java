package org.egov.bpa.web.model;

import org.egov.common.contract.request.RequestInfo;
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

public class FieldInspection {
	
	
	  @JsonProperty("id")
	  private String id = null;

	  @JsonProperty("applicationno")
	  private String applicationno = null;
	  
	  @JsonProperty("tenantId")
	  private String tenantId = null;
	  
	  @JsonProperty("approachRoad")
	  private Object approachRoad = null;
	  
	  @JsonProperty("siteSituation")
	  private Object siteSituation = null;
	  
	  
	  @JsonProperty("buildingSituation")
	  private Object buildingSituation = null;
	  
	  
	  @JsonProperty("reportDetails")
	  private Object reportDetails = null;
	  
	  
	  @JsonProperty("additionalDetails")
	  private Object additionalDetails = null;
	  
	  @JsonProperty("auditDetails")
	  private AuditDetails auditDetails = null;
	  
	  

}
