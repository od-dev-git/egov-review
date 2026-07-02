package org.egov.dsc.model;

import java.util.ArrayList;
import java.util.List;

import org.egov.common.contract.response.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponse {

    @JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("tokens")
    private List<String> tokens;
    
	@JsonProperty("dscErrorCode")
	private String dscErrorCode;
	
	@JsonProperty("emudhraErrorCode")
	private String emudhraErrorCode;

}
