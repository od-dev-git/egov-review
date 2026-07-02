package org.egov.bpa.web.model;

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
public class PlanningAssistantChecklist {
	
	  @JsonProperty("id")
	  private String id = null;

	  @JsonProperty("applicationno")
	  private String applicationno = null;
	  
	  @JsonProperty("tenantId")
	  private String tenantId = null;
	  
	  @JsonProperty("documentsSubmitted")
	  private Object documentsSubmitted = null;
	  
	  @JsonProperty("plansSubmitted")
	  private Object plansSubmitted = null;	  
	  
	  @JsonProperty("nocsSubmitted")
	  private Object nocsSubmitted = null;	  
	  
	  @JsonProperty("builtupArea")
	  private Object builtupArea = null;
	  
	  @JsonProperty("setbackDetails")
	  private Object setbackDetails = null;	  
	  
	  @JsonProperty("additionalDetails")
	  private Object additionalDetails = null;
	  
	  @JsonProperty("auditDetails")
	  private AuditDetails auditDetails = null;

}
