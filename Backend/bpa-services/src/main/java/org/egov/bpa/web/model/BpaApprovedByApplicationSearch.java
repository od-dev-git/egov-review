package org.egov.bpa.web.model;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.egov.bpa.web.model.landInfo.LandInfo;
import org.egov.common.contract.request.User;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * BPA application object to capture the details of land, land owners, and address of the land.
 */
@ApiModel(description = "BPA application object to capture the details of land, land owners, and address of the land.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-06-23T05:52:32.717Z[GMT]")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter

public class BpaApprovedByApplicationSearch {
	
	
	@JsonProperty("dscDetails")
	private DscDetails dscDetails = null;
	
	@JsonProperty("buildingAdditionalDetails")
	private Object buildingAdditionalDetails = null;
	
	@JsonProperty("documents")
	@Valid
	private List<DocumentList> documents = null;
	 
	 
	 @JsonProperty("applicationstatus")
	 private String applicationstatus = null;
	 
	 @JsonProperty("workflowstate")
	 private String workflowstate;
	 
	 @JsonProperty("id")
	 private String id =null;
	 
	 @JsonProperty("bpaid")
	 private String bpaid =null;
	 
	 @JsonProperty("reworkHistory")
	 private Object reworkHistory = null;
	 
	 @JsonProperty("businessService")
	 private String businessService = null;
	 
	 

}
