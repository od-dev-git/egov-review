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
public class DataSignRequest {

	@NotNull
    @Valid
    @JsonProperty("RequestInfo")
    private RequestInfo requestInfo;
	
    
    @JsonProperty("tokenDisplayName")
	private String tokenDisplayName;
	
    
    @JsonProperty("keyStorePassPhrase")
	private String keyStorePassPhrase;
	
    
    @JsonProperty("keyId")
	private String keyId;
	
    
    @JsonProperty("channelId")
	private String channelId;
    
    @JsonProperty("responseData")
    private String responseData;

    @JsonProperty("file")
    private String fileBytes;
    
    @JsonProperty("fileName")
    private String fileName;
    
    @JsonProperty("tenantId")
    private String tenantId;
    
    @JsonProperty("moduleName")
    private String moduleName;
    
    @JsonProperty("tempFilePath")
	private String tempFilePath;
    
    @JsonProperty("userId")
    private Long userId;
}
