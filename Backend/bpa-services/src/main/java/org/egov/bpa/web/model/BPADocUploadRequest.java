package org.egov.bpa.web.model;

import java.util.Map;

import org.egov.common.contract.request.RequestInfo;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Validated
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2020-06-23T05:52:32.717Z[GMT]")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class BPADocUploadRequest {
	
	  @JsonProperty("RequestInfo")
	  private RequestInfo requestInfo = null;

	  @JsonProperty("BPADocUpload")
	  private DocUploadRequest docUploadRequest = null;

}
