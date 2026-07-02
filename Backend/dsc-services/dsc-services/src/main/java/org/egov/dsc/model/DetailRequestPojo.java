package org.egov.dsc.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetailRequestPojo {

	@JsonProperty("encryptedRequest")
    private String encryptedRequest;
    
	@JsonProperty("encryptionKeyId")
    private String encryptionKeyId;
    
	@JsonProperty("fileName")
	private String fileName;
	
	@JsonProperty("tempFilePath")
	private String tempFilePath;
	
	@JsonProperty("dscErrorCode")
	private String dscErrorCode;
	
	@JsonProperty("emudhraErrorCode")
	private String emudhraErrorCode;
	
}
