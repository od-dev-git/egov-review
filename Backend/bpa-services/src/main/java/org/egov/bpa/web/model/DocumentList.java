package org.egov.bpa.web.model;

import java.util.List;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@ApiModel(description = "BPA application object to capture the details of document.")
@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-06-23T05:52:32.717Z[GMT]")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter

public class DocumentList {
	
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
	  
	  @JsonProperty("buildingPlanid")
	  private String buildingPlanid = null;

}
