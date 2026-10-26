package org.egov.dsc.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.groups.Default;

import org.egov.common.contract.request.RequestInfo;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRequest {

	public interface Decrypt extends Default {}

	@JsonProperty("tenantId")
	private String tennantId;
	
    @NotNull
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;
    
    @NotBlank(groups = Decrypt.class)
    @JsonProperty("responseData")
    private String responseData;

    

}
