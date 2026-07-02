package org.egov.dsc.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.egov.common.contract.request.RequestInfo;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {

	@JsonProperty("tenantId")
	private String tennantId;
	
    @NotNull
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;
    
    @JsonProperty("responseData")
    private String responseData;

    

}
