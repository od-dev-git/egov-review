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
public class DetailResponsePojo {

	@JsonProperty("responseData")
    private String responseData;
	
	@JsonProperty("status")
    private String status;
	
	@JsonProperty("errorMessage")
    private String errorMessage;
	
	@JsonProperty("version")
    private String version;

	@JsonProperty("errorCode")
    private String errorCode;

}
