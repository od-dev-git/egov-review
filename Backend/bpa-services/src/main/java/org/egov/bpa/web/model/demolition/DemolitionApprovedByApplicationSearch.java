package org.egov.bpa.web.model.demolition;

import java.util.List;

import javax.validation.Valid;

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
public class DemolitionApprovedByApplicationSearch {

	@JsonProperty("demolitionAdditionalDetails")
	private Object demolitionAdditionalDetails = null;

	@JsonProperty("documents")
	@Valid
	private List<DemolitionDocumentList> documents = null;

	@JsonProperty("applicationstatus")
	private String applicationstatus = null;

	@JsonProperty("workflowstate")
	private String workflowstate;

	@JsonProperty("id")
	private String id = null;
	
	@JsonProperty("tenantId")
	private String tenantId = null;

	@JsonProperty("demolitionId")
	private String demolitionId = null;
	
	@JsonProperty("applicationtype")
	private String applicationType;
	
	@JsonProperty("applicationNo")
	private String applicationNo;

}
