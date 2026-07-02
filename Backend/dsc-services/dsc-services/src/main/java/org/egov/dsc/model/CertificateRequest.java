package org.egov.dsc.model;




import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.egov.common.contract.request.RequestInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateRequest {

	@NotNull
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;
	
	@NotNull
    @Valid
    @JsonProperty("tokenDisplayName")
	private String tokenDisplayName;
	
	@JsonProperty("responseData")
	private String responseData;

    

}
