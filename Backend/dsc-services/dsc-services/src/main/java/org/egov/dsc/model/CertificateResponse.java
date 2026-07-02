package org.egov.dsc.model;


import java.util.List;

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
public class CertificateResponse {

	@JsonProperty("ResponseInfo")
    private ResponseInfo responseInfo;

    @JsonProperty("certificates")
    private List<CertificateResponsePojo> certificates;
    
	@JsonProperty("dscErrorCode")
	private String dscErrorCode;
	
	@JsonProperty("emudhraErrorCode")
	private String emudhraErrorCode;
    

}
