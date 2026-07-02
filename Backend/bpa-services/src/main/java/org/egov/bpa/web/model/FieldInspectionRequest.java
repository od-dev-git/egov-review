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

public class FieldInspectionRequest {
	
	
	@JsonProperty("RequestInfo")
	  private RequestInfo requestInfo = null;

	  @JsonProperty("fieldInspection")
	  private FieldInspection fieldinspection = null;

}
