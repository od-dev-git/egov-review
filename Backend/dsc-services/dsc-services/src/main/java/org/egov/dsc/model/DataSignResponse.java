package org.egov.dsc.model;


import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.egov.common.contract.request.RequestInfo;
import org.egov.common.contract.response.ResponseInfo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DataSignResponse {

	@JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;
	
    @Valid
    @JsonProperty("responseString")
	private String responseString;
	
    @Valid
    @JsonProperty("fileStoreId")
	private String fileStoreId;
    
	@JsonProperty("dscErrorCode")
	private String dscErrorCode;
	
	@JsonProperty("emudhraErrorCode")
	private String emudhraErrorCode;
}
