package org.egov.bpa.web.model;

import org.egov.bpa.web.model.landInfo.LandRecordDTO;
import org.egov.common.contract.response.ResponseInfo;
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
public class LandRecordResponse {
	
	  @JsonProperty("ResponseInfo")
	  private ResponseInfo responseInfo;
	  
	  @JsonProperty("LandRecord")
	  private LandRecordDTO landRecord;

}
